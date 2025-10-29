/**
 * 删除车辆停车位命令类
 * 
 * 该命令类负责处理删除车辆与停车位绑定关系的业务逻辑，包括：
 * 1. 验证车辆和停车位的状态是否允许删除
 * 2. 检查车辆是否存在未结清的费用
 * 3. 更新车辆状态和停车位状态
 * 4. 释放停车位资源
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "owner.deleteCarParkingSpace")
public class DeleteCarParkingSpaceCmd extends Cmd {

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /**
     * 验证请求参数和业务规则
     * 
     * 该方法主要验证：
     * 1. 请求参数是否完整
     * 2. 车辆状态是否允许删除
     * 3. 是否存在多个车辆绑定同一车位的情况
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理数据流
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填参数
        Assert.jsonObjectHaveKey(reqJson, "communityId", "未包含小区ID");
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");

        // 查询车辆信息
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarId(reqJson.getString("carId"));
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        ownerCarDto.setStatusCd("0"); // 设置状态为有效状态
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        
        // 检查是否存在多个车辆绑定同一车位
        if (ownerCarDtos != null && ownerCarDtos.size() > 1) {
            throw new IllegalArgumentException("有多个车辆绑定此车位，请先删除车辆！");
        }

        // 获取车辆状态
        String state = ownerCarDtos.get(0).getState();

        // 检查车位是否已经释放
        if (StringUtil.isEmpty(state) || state.equals(OwnerCarDto.STATE_FINISH)) {
            throw new IllegalArgumentException("车位已经释放无需释放");
        }

        // 将车辆信息存入请求JSON中，供后续处理使用
        reqJson.put("ownerCarDto", ownerCarDtos.get(0));
    }

    /**
     * 执行删除车辆停车位的主要业务逻辑
     * 
     * 该方法负责：
     * 1. 检查车辆费用情况
     * 2. 更新车辆状态
     * 3. 更新停车位状态为可用
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当执行过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 从请求中获取车辆信息
        OwnerCarDto ownerCarDto = (OwnerCarDto) reqJson.get("ownerCarDto");
        
        // 查询车辆相关费用信息
        FeeDto feeDto = new FeeDto();
        feeDto.setPayerObjId(reqJson.getString("carId"));
        feeDto.setCommunityId(reqJson.getString("communityId"));
        feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_CAR); // 设置缴费对象类型为车辆
        feeDto.setState(FeeDto.STATE_FINISH); // 设置费用状态为已完成
        List<FeeDto> feeDtoList = feeInnerServiceSMOImpl.queryFees(feeDto);
        
        boolean oweFee = false; // 标记是否存在欠费
        
        // 检查费用情况
        if (feeDtoList == null || feeDtoList.size() < 1) {
            oweFee = true; // 没有费用记录，视为欠费
        }
        
        // 遍历费用列表，检查费用状态
        for (FeeDto tmpFeeDto : feeDtoList) {
            // 检查费用结束时间是否早于车辆结束时间
            if (tmpFeeDto.getEndTime().getTime() <= ownerCarDto.getEndTime().getTime()) {
                oweFee = true; // 存在欠费
                break;
            } else {
                throw new IllegalArgumentException("费用未开始！");
            }
        }

        // 如果车辆状态已经是完成状态，则清除欠费标记
        if (OwnerCarDto.STATE_FINISH.equals(ownerCarDto.getState())) {
            oweFee = false;
        }

        // 准备更新车辆信息
        OwnerCarPo ownerCarPo = new OwnerCarPo();
        ownerCarPo.setCarId(reqJson.getString("carId"));
        ownerCarPo.setMemberId(reqJson.getString("memberId"));
        ownerCarPo.setCommunityId(reqJson.getString("communityId"));
        
        // 根据欠费情况设置车辆状态
        if (oweFee) {
            ownerCarPo.setState(OwnerCarDto.STATE_FINISH); // 存在欠费，设置为完成状态
        }
        
        // 更新车辆信息
        int flag = ownerCarV1InnerServiceSMOImpl.updateOwnerCar(ownerCarPo);
        if (flag < 1) {
            throw new IllegalArgumentException("修改车辆出错");
        }
        
        // 设置停车位状态为可用
        reqJson.put("carNumType", ParkingSpaceDto.STATE_FREE);
        reqJson.put("psId", ownerCarDto.getPsId());
        
        // 查询停车位信息
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        parkingSpaceDto.setPsId(reqJson.getString("psId"));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 验证停车位信息
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            return; // 停车位信息异常，直接返回
        }

        parkingSpaceDto = parkingSpaceDtos.get(0);

        // 准备更新停车位信息
        JSONObject businessParkingSpace = new JSONObject();
        businessParkingSpace.putAll(BeanConvertUtil.beanCovertMap(parkingSpaceDto));
        businessParkingSpace.put("state", reqJson.getString("carNumType")); // 设置停车位状态为可用
        
        // 转换为停车位PO对象并更新
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);
        flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        if (flag < 1) {
            throw new IllegalArgumentException("修改车辆出错");
        }
    }
}