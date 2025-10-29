package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.fee.BillOweFeeDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 业主欠费查询命令类
 * 功能：查询指定业主在小区内的房屋和车位欠费信息
 * 用途：用于业主端或管理端查看业主的欠费明细
 */
@Java110Cmd(serviceCode = "fee.listOwnerOweFee")
public class ListOwnerOweFeeCmd extends Cmd {
    
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含小区ID和业主ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "ownerId", "未包含小区业主ID");
    }

    /**
     * 执行命令方法 - 查询业主欠费信息
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 1.0 查询业主房屋关系，检查是否有房屋欠费
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerId(reqJson.getString("ownerId"));
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        List<FeeDto> resultFees = new ArrayList<>();

        if (!ListUtil.isNull(ownerRoomRelDtos)) {
            // 获取房屋欠费信息
            getRoomOweFee(ownerRoomRelDtos, reqJson, resultFees);
        }

        // 2.0 查询业主车位关系，检查是否有车位欠费
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setOwnerId(reqJson.getString("ownerId"));
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        if (!ListUtil.isNull(ownerCarDtos)) {
            // 获取车位欠费信息
            getParkingSpaceOweFee(ownerCarDtos, reqJson, resultFees);
        }

        // 返回查询结果
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(1, resultFees.size(), resultFees);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 获取车位欠费信息
     * @param ownerCarDtos 业主车位关系列表
     * @param reqJson 请求参数
     * @param resultFees 结果费用列表
     */
    private void getParkingSpaceOweFee(List<OwnerCarDto> ownerCarDtos, JSONObject reqJson, List<FeeDto> resultFees) {
        String payObjName = "";
        // 遍历业主的所有车位
        for (OwnerCarDto ownerCarDto : ownerCarDtos) {
            // 查询车位详细信息
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
            parkingSpaceDto.setPsId(ownerCarDto.getPsId());
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            
            // 如果车位不存在，跳过处理
            if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
                continue;
            }
            
            // 获取车位显示名称
            ParkingSpaceDto tmpParkingSpaceDto = parkingSpaceDtos.get(0);
            payObjName = tmpParkingSpaceDto.getAreaNum() + "停车场" + tmpParkingSpaceDto.getNum() + "车位";
            
            // 查询该车位的费用信息
            FeeDto feeDto = new FeeDto();
            feeDto.setCommunityId(reqJson.getString("communityId"));
            feeDto.setPayerObjId(ownerCarDto.getPsId());
            feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_PARKING_SPACE);
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            
            // 获取欠费详情
            getOweFee(feeDtos, reqJson, resultFees, payObjName);
        }
    }

    /**
     * 获取房屋欠费信息
     * @param ownerRoomRelDtos 业主房屋关系列表
     * @param reqJson 请求参数
     * @param resultFees 结果费用列表
     */
    private void getRoomOweFee(List<OwnerRoomRelDto> ownerRoomRelDtos, JSONObject reqJson, List<FeeDto> resultFees) {
        String payObjName = "";
        // 遍历业主的所有房屋
        for (OwnerRoomRelDto ownerRoomRelDto : ownerRoomRelDtos) {
            // 查询房屋详细信息
            RoomDto roomDto = new RoomDto();
            roomDto.setCommunityId(reqJson.getString("communityId"));
            roomDto.setRoomId(ownerRoomRelDto.getRoomId());
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            
            // 如果房屋不存在，跳过处理
            if (roomDtos == null || roomDtos.size() < 1) {
                continue;
            }
            
            // 获取房屋显示名称
            RoomDto tmpRoomDto = roomDtos.get(0);
            payObjName = tmpRoomDto.getFloorNum() + "栋" + tmpRoomDto.getUnitNum() + "单元" + tmpRoomDto.getRoomNum() + "室";
            
            // 查询该房屋的费用信息
            FeeDto feeDto = new FeeDto();
            feeDto.setCommunityId(reqJson.getString("communityId"));
            feeDto.setPayerObjId(ownerRoomRelDto.getRoomId());
            feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_ROOM);
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            
            // 获取欠费详情
            getOweFee(feeDtos, reqJson, resultFees, payObjName);
        }
    }

    /**
     * 获取欠费详情
     * @param feeDtos 费用列表
     * @param reqJson 请求参数
     * @param resultFees 结果费用列表
     * @param payObjName 缴费对象名称
     */
    private void getOweFee(List<FeeDto> feeDtos, JSONObject reqJson, List<FeeDto> resultFees, String payObjName) {
        // 如果没有费用信息，直接返回
        if (feeDtos == null || feeDtos.size() < 1) {
            return;
        }

        // 遍历所有费用项
        for (FeeDto feeDto : feeDtos) {
            Date endTime = feeDto.getEndTime();
            // 检查费用是否已到期（是否欠费）
            if (endTime.getTime() > DateUtil.getCurrentDate().getTime()) {
                // 未到期，没有欠费，跳过处理
                continue;
            }

            // 查询费用配置信息
            FeeConfigDto feeConfigDto = new FeeConfigDto();
            feeConfigDto.setCommunityId(reqJson.getString("communityId"));
            feeConfigDto.setConfigId(feeDto.getConfigId());
            List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
            
            // 如果费用配置不存在，跳过处理
            if (feeConfigDtos == null || feeConfigDtos.size() < 1) {
                continue;
            }

            FeeConfigDto tmpFeeConfigDto = feeConfigDtos.get(0);

            // 处理按量计费类型的费用
            if (FeeConfigDto.BILL_TYPE_EVERY.equals(tmpFeeConfigDto.getBillType())) {
                // 计算房屋或车位的费用单价
                computeFeePriceByRoom(feeDto);
                // 计算欠费月数
                double month = DateUtil.dayCompare(endTime, DateUtil.getCurrentDate());
                // 计算总欠费金额 = 单价 × 欠费月数
                BigDecimal feePrice = new BigDecimal(feeDto.getFeePrice());
                feePrice = feePrice.multiply(new BigDecimal(month));
                
                // 创建欠费记录
                FeeDto tmpFeeDto = new FeeDto();
                tmpFeeDto.setFeeId(feeDto.getFeeId());
                tmpFeeDto.setFeeName(feeDto.getFeeName());
                tmpFeeDto.setOweFee(feePrice.doubleValue());
                tmpFeeDto.setPayerObjType(feeDto.getPayerObjType());
                tmpFeeDto.setPayerObjName(payObjName);
                tmpFeeDto.setPayerObjId(feeDto.getPayerObjId());
                resultFees.add(tmpFeeDto);
                continue;
            }
            
            // 查询账单欠费表，获取预先生成的欠费记录
            BillOweFeeDto billOweFeeDto = new BillOweFeeDto();
            billOweFeeDto.setCommunityId(reqJson.getString("communityId"));
            billOweFeeDto.setFeeId(feeDto.getFeeId());
            List<BillOweFeeDto> billOweFeeDtos = feeInnerServiceSMOImpl.queryBillOweFees(billOweFeeDto);

            // 如果没有欠费记录，跳过处理
            if (billOweFeeDtos == null || billOweFeeDtos.size() < 1) {
                continue;
            }

            // 处理所有欠费账单记录
            for (BillOweFeeDto tmpBillOweFeeDto : billOweFeeDtos) {
                FeeDto tmpFeeDto = new FeeDto();
                tmpFeeDto.setFeeId(feeDto.getFeeId());
                tmpFeeDto.setFeeName(feeDto.getFeeName());
                tmpFeeDto.setOweFee(Double.parseDouble(tmpBillOweFeeDto.getBillAmountOwed()));
                tmpFeeDto.setPayerObjName(payObjName);
                tmpFeeDto.setPayerObjId(feeDto.getPayerObjId());
                tmpFeeDto.setPayerObjType(feeDto.getPayerObjType());
                resultFees.add(tmpFeeDto);
            }
        }
    }

    /**
     * 根据车位计算费用单价
     * @param feeDto 费用对象
     */
    private void computeFeePriceByParkingSpace(FeeDto feeDto) {
        // 查询车位信息
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(feeDto.getCommunityId());
        parkingSpaceDto.setPsId(feeDto.getPayerObjId());
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 如果车位不存在，直接返回
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
            return;
        }

        String computingFormula = feeDto.getComputingFormula();
        double feePrice = 0.00;
        
        // 根据计算公式类型计算费用
        if ("1001".equals(computingFormula)) {
            // 面积*单价+附加费 计算公式
            BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
            BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(parkingSpaceDtos.get(0).getArea()));
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        } else if ("2002".equals(computingFormula)) {
            // 固定费用计算公式
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            feePrice = additionalAmount.setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        } else {
            // 未知计算公式，设置错误标识
            feePrice = -1.00;
        }

        // 设置计算后的费用单价
        feeDto.setFeePrice(feePrice);
    }

    /**
     * 根据房屋计算费用单价
     * @param feeDto 费用对象
     */
    private void computeFeePriceByRoom(FeeDto feeDto) {
        // 查询房屋信息
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(feeDto.getCommunityId());
        roomDto.setRoomId(feeDto.getPayerObjId());
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 如果房屋不存在，直接返回
        if (roomDtos == null || roomDtos.size() < 1) {
            return;
        }

        String computingFormula = feeDto.getComputingFormula();
        double feePrice = 0.00;
        
        // 根据计算公式类型计算费用
        if ("1001".equals(computingFormula)) {
            // 面积*单价+附加费 计算公式
            BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
            BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(roomDtos.get(0).getBuiltUpArea()));
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        } else if ("2002".equals(computingFormula)) {
            // 固定费用计算公式
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            feePrice = additionalAmount.setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        } else {
            // 未知计算公式，设置错误标识
            feePrice = -1.00;
        }

        // 设置计算后的费用单价
        feeDto.setFeePrice(feePrice);
    }
}