/**
 * 停车位退租命令处理类
 * 
 * 该类负责处理停车位退租业务，包括：
 * 1. 解除业主与停车位的绑定关系
 * 2. 更新停车位状态为空闲
 * 3. 删除相关的费用信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.FeeTypeConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "parkingSpace.exitParkingSpace")
public class ExitParkingSpaceCmd extends Cmd {

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;
    
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * 验证请求报文中的必要参数是否存在且不为空
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数校验失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查必要参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "ownerId", "请求报文中未包含ownerId节点");
        Assert.jsonObjectHaveKey(reqJson, "psId", "请求报文中未包含psId节点");
        Assert.jsonObjectHaveKey(reqJson, "storeId", "请求报文中未包含storeId节点");

        // 检查参数值是否为空
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        Assert.hasLength(reqJson.getString("ownerId"), "ownerId不能为空");
        Assert.hasLength(reqJson.getString("psId"), "psId不能为空");
        Assert.hasLength(reqJson.getString("storeId"), "storeId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 执行停车位退租的核心业务逻辑，包括三个主要步骤：
     * 1. 解除业主与停车位的关系
     * 2. 更新停车位状态
     * 3. 删除相关费用信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务执行失败时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 解除业主和停车位之间的绑定关系
        exitParkingSpace(reqJson);

        // 将车位状态改为空闲状态
        modifyParkingSpaceState(reqJson);

        // 删除相关的费用信息
        exitParkingSpaceFee(reqJson);
    }

    /**
     * 解除停车位绑定关系
     * 
     * 删除业主与停车位之间的关联关系，即删除车辆信息
     * 
     * @param paramInJson 接口调用方传入的参数JSON对象
     * @throws CmdException 当删除车辆失败时抛出命令异常
     */
    public void exitParkingSpace(JSONObject paramInJson) {
        // 从参数中获取业主车辆信息
        OwnerCarDto ownerCarDto = (OwnerCarDto) paramInJson.get("ownerCarDto");

        // 构建删除车辆的业务数据
        JSONObject businessOwnerCar = new JSONObject();
        businessOwnerCar.put("carId", ownerCarDto.getCarId());
        
        // 将JSON对象转换为PO对象
        OwnerCarPo ownerCarPo = BeanConvertUtil.covertBean(businessOwnerCar, OwnerCarPo.class);

        // 执行删除操作
        int flag = ownerCarV1InnerServiceSMOImpl.deleteOwnerCar(ownerCarPo);
        
        // 检查删除操作是否成功
        if(flag < 1){
            throw new CmdException("删除车辆失败");
        }
    }

    /**
     * 修改停车位状态
     * 
     * 将停车位状态更新为空闲状态（"F"）
     * 
     * @param paramInJson 接口调用方传入的参数JSON对象
     * @throws ListenerExecuteException 当查询到的停车位信息不唯一时抛出异常
     * @throws CmdException 当更新停车位状态失败时抛出命令异常
     */
    public void modifyParkingSpaceState(JSONObject paramInJson) {
        // 构建停车位查询条件
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(paramInJson.getString("communityId"));
        parkingSpaceDto.setPsId(paramInJson.getString("psId"));
        
        // 查询停车位信息
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 验证查询结果
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, 
                "未查询到停车位信息" + JSONObject.toJSONString(parkingSpaceDto));
        }

        // 获取查询到的停车位信息
        parkingSpaceDto = parkingSpaceDtos.get(0);

        // 构建更新数据
        JSONObject businessParkingSpace = new JSONObject();
        businessParkingSpace.putAll(BeanConvertUtil.beanCovertMap(parkingSpaceDto));
        businessParkingSpace.put("state", "F"); // 设置状态为空闲
        
        // 转换为PO对象
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);

        // 执行更新操作
        int flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);

        // 检查更新操作是否成功
        if(flag < 1){
            throw new CmdException("修改车位失败");
        }
    }

    /**
     * 删除停车位相关费用信息
     * 
     * 根据停车位类型和状态删除对应的费用记录
     * 
     * @param paramInJson 接口调用方传入的参数JSON对象
     * @throws ListenerExecuteException 当费用记录不唯一时抛出异常
     * @throws CmdException 当删除费用失败时抛出命令异常
     */
    public void exitParkingSpaceFee(JSONObject paramInJson) {
        // 从参数中获取停车位信息
        ParkingSpaceDto parkingSpaceDto = (ParkingSpaceDto) paramInJson.get("parkingSpaceDto");
        
        // 构建费用查询条件
        FeeDto feeDto = new FeeDto();
        feeDto.setCommunityId(paramInJson.getString("communityId"));
        feeDto.setIncomeObjId(paramInJson.getString("storeId"));
        feeDto.setPayerObjId(paramInJson.getString("psId"));
        
        // 根据停车位类型和状态确定费用类型编码
        feeDto.setFeeTypeCd("1001".equals(parkingSpaceDto.getTypeCd())
                ? ("H".equals(parkingSpaceDto.getState())
                ? FeeTypeConstant.FEE_TYPE_HIRE_UP_PARKING_SPACE
                : FeeTypeConstant.FEE_TYPE_SELL_UP_PARKING_SPACE)
                : ("H".equals(parkingSpaceDto.getState())
                ? FeeTypeConstant.FEE_TYPE_HIRE_DOWN_PARKING_SPACE
                : FeeTypeConstant.FEE_TYPE_SELL_DOWN_PARKING_SPACE));
        
        // 查询费用信息
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 验证费用记录是否唯一
        if (feeDtos == null || feeDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, 
                "数据存在问题，停车费对应关系不是一条");
        }

        // 构建删除费用的业务数据
        JSONObject businessFee = new JSONObject();
        businessFee.put("feeId", feeDtos.get(0).getFeeId());
        
        // 转换为PO对象
        PayFeePo payFeePo = BeanConvertUtil.covertBean(businessFee, PayFeePo.class);

        // 执行删除操作
        int flag = payFeeV1InnerServiceSMOImpl.deletePayFee(payFeePo);

        // 检查删除操作是否成功
        if(flag < 1){
            throw new CmdException("删除费用失败");
        }
    }
}