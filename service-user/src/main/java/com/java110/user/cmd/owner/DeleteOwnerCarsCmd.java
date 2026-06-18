/**
 * 删除业主车辆命令类
 * 
 * 该类用于处理删除业主车辆的请求，主要功能包括：
 * 1. 验证删除车辆的条件（如是否存在未结束的费用）
 * 2. 执行车辆删除操作
 * 3. 释放关联的车位资源
 * 4. 清理车辆相关的费用信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailMonthInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.report.IReportOweFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.po.payFee.PayFeeDetailMonthPo;
import com.java110.po.reportFee.ReportOweFeePo;
import com.java110.po.room.RoomPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "owner.deleteOwnerCars")
public class DeleteOwnerCarsCmd extends Cmd {

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    @Autowired
    private IReportOweFeeInnerServiceSMO reportOweFeeInnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailMonthInnerServiceSMO payFeeDetailMonthInnerServiceSMOImpl;

    /**
     * 验证删除车辆请求参数和业务规则
     * 
     * 主要验证内容：
     * 1. 必填参数验证（车辆ID、成员ID、小区ID）
     * 2. 检查车辆是否存在未结束的费用
     * 3. 验证车辆信息的唯一性
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();

        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "carId", "carId不能为空");
        Assert.hasKeyAndValue(reqJson, "memberId", "memberId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");

        // 检查车辆是否存在未结束的费用
        FeeDto feeDto = new FeeDto();
        feeDto.setPayerObjId(reqJson.getString("memberId"));
        feeDto.setCommunityId(reqJson.getString("communityId"));
        feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_CAR); // 设置缴费对象类型为车辆
        List<FeeDto> feeDtoList = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 遍历费用列表，检查是否存在未结束的费用
        for (FeeDto tmpFeeDto : feeDtoList) {
            if (!FeeDto.STATE_FINISH.equals(tmpFeeDto.getState())) {
                throw new IllegalArgumentException("存在 未结束费用 不能删除");
            }
        }

        // 查询车辆信息并验证唯一性
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarId(reqJson.getString("carId"));
        ownerCarDto.setMemberId(reqJson.getString("memberId"));
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));

        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 验证只能找到一条车辆记录
        Assert.listOnlyOne(ownerCarDtos, "当前未找到需要删除车辆");
        // 将车位ID放入请求参数中，供后续处理使用
        reqJson.put("psId", ownerCarDtos.get(0).getPsId());
    }

    /**
     * 执行删除车辆命令
     * 
     * 主要执行步骤：
     * 1. 检查子母车位情况，防止误删
     * 2. 删除车辆信息
     * 3. 释放关联的车位
     * 4. 删除车辆相关费用
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查是否为子母车位，如果是则验证是否存在子车辆
        if (reqJson.containsKey("psId") && !StringUtil.isEmpty(reqJson.getString("psId"))) {
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setPsId(reqJson.getString("psId"));
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            // 检查车位类型是否为子母车位且车辆类型为主车辆
            if (parkingSpaceDtos != null && parkingSpaceDtos.size() > 0 &&
                    "2".equals(parkingSpaceDtos.get(0).getParkingType())
                    && "1001".equals(reqJson.getString("carTypeCd"))) { // 子母车位
                OwnerCarDto ownerCarDto = new OwnerCarDto();
                ownerCarDto.setCarId(reqJson.getString("carId"));
                ownerCarDto.setPsId(reqJson.getString("psId"));
                ownerCarDto.setOwnerId(reqJson.getString("ownerId"));
                ownerCarDto.setCarTypeCd("1002"); // 成员车辆
                List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
                // 如果存在子车辆，则不允许删除
                if (ownerCarDtos != null && ownerCarDtos.size() > 0) {
                    throw new IllegalArgumentException("该车位下含有子车辆，请先删除子车辆后再进行操作！");
                }
            }
        }
        
        // 删除车辆信息
        OwnerCarPo ownerCarPo = new OwnerCarPo();
        ownerCarPo.setCommunityId(reqJson.getString("communityId"));
        ownerCarPo.setCarId(reqJson.getString("carId"));
        ownerCarPo.setMemberId(reqJson.getString("memberId"));
        int flag = ownerCarV1InnerServiceSMOImpl.deleteOwnerCar(ownerCarPo);
        if (flag < 1) {
            throw new IllegalArgumentException("删除车辆出错");
        }
        
        // 如果车位ID为空或为-1，则不处理车位释放
        if (StringUtil.isEmpty(reqJson.getString("psId")) || "-1".equals(reqJson.getString("psId"))) {
            return;
        }
        
        // 释放车位信息
        releaseParkSpace(reqJson);

        // 删除车辆相关费用
        deleteCarFee(reqJson);
    }

    /**
     * 释放车位信息
     * 
     * 将车位状态修改为空闲状态，以便其他车辆可以使用
     * 
     * @param reqJson 请求参数JSON对象，包含小区ID、车位ID等信息
     */
    private void releaseParkSpace(JSONObject reqJson) {
        int flag;
        // 只有车辆ID和成员ID相同时才处理车位释放
        if (!reqJson.getString("carId").equals(reqJson.getString("memberId"))) {
            return;
        }
        
        // 设置车位状态为空闲
        reqJson.put("carNumType", ParkingSpaceDto.STATE_FREE);
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        parkingSpaceDto.setPsId(reqJson.getString("psId"));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 验证车位信息是否存在且唯一
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() != 1) {
            return;
        }

        // 更新车位状态
        parkingSpaceDto = parkingSpaceDtos.get(0);
        JSONObject businessParkingSpace = new JSONObject();
        businessParkingSpace.putAll(BeanConvertUtil.beanCovertMap(parkingSpaceDto));
        businessParkingSpace.put("state", reqJson.getString("carNumType"));
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);
        flag = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
        if (flag < 1) {
            throw new IllegalArgumentException("修改车辆出错");
        }
    }

    /**
     * 删除车辆相关费用
     * 
     * 删除车辆关联的所有费用信息，包括：
     * 1. 缴费记录
     * 2. 费用信息
     * 3. 欠费记录
     * 4. 离散月数据
     * 
     * @param reqJson 请求参数JSON对象，包含车辆ID、小区ID等信息
     */
    private void deleteCarFee(JSONObject reqJson) {
        // 只有车辆ID和成员ID相同时才处理费用删除
        if (!reqJson.getString("carId").equals(reqJson.getString("memberId"))) {
            return;
        }

        // 验证车辆ID不能为空
        if (StringUtil.isEmpty(reqJson.getString("carId"))) {
            throw new CmdException("车联不能为空");
        }
        
        // 查询车辆相关的费用信息
        PayFeeDto feeDto = new PayFeeDto();
        feeDto.setPayerObjId(reqJson.getString("carId"));
        feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_CAR);
        feeDto.setCommunityId(reqJson.getString("communityId"));
        List<PayFeeDto> feeDtos = payFeeV1InnerServiceSMOImpl.queryPayFees(feeDto);

        // 如果没有费用信息，则直接返回
        if (feeDtos == null || feeDtos.size() < 1) {
            return;
        }

        // 遍历所有费用记录并逐个删除
        for (PayFeeDto payFeeDto : feeDtos) {
            doDeleteFee(payFeeDto);
        }
    }

    /**
     * 执行删除单个费用的详细操作
     * 
     * 删除与费用相关的所有数据，包括：
     * 1. 缴费明细记录
     * 2. 费用主记录
     * 3. 欠费报表记录
     * 4. 费用月明细记录
     * 
     * @param payFeeDto 费用数据传输对象，包含费用ID、小区ID等信息
     */
    private void doDeleteFee(PayFeeDto payFeeDto) {
        // 删除缴费明细记录
        PayFeeDetailPo payFeeDetailPo = new PayFeeDetailPo();
        payFeeDetailPo.setFeeId(payFeeDto.getFeeId());
        payFeeDetailPo.setCommunityId(payFeeDto.getCommunityId());
        payFeeDetailV1InnerServiceSMOImpl.deletePayFeeDetailNew(payFeeDetailPo);

        // 删除费用主记录
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setFeeId(payFeeDto.getFeeId());
        payFeePo.setCommunityId(payFeeDto.getCommunityId());
        payFeeV1InnerServiceSMOImpl.deletePayFee(payFeePo);

        // 删除欠费报表记录
        ReportOweFeePo reportOweFeePo = new ReportOweFeePo();
        reportOweFeePo.setFeeId(payFeeDto.getFeeId());
        reportOweFeePo.setCommunityId(payFeeDto.getCommunityId());
        reportOweFeeInnerServiceSMOImpl.deleteReportOweFee(reportOweFeePo);

        // 删除离散月数据记录
        PayFeeDetailMonthPo payFeeDetailMonthPo = new PayFeeDetailMonthPo();
        payFeeDetailMonthPo.setFeeId(payFeeDto.getFeeId());
        payFeeDetailMonthPo.setCommunityId(payFeeDto.getCommunityId());
        payFeeDetailMonthInnerServiceSMOImpl.deletePayFeeDetailMonth(payFeeDetailMonthPo);
    }
}