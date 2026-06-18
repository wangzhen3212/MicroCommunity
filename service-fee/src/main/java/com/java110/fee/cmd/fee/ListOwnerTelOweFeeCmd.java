/**
 * 业主电话欠费查询命令类
 * 
 * 功能：根据业主手机号查询其在小区内的房屋和车位欠费信息
 * 包括房屋物业费、车位管理费等费用的欠费情况
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.BillOweFeeDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "fee.listOwnerTelOweFee")
public class ListOwnerTelOweFeeCmd extends Cmd {
    
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

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中必须包含小区ID和业主手机号
     * 
     * @param event Cmd事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含小区ID和业主手机号
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "ownerTel", "未包含小区业主手机号");
    }

    /**
     * 执行命令方法
     * 
     * 根据业主手机号查询其在小区内的房屋和车位欠费信息
     * 包括房屋物业费、车位管理费等费用的欠费情况
     * 
     * @param event Cmd事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当执行业务逻辑出错时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 根据手机号查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(reqJson.getString("ownerTel"));
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 验证业主是否存在
        if (ListUtil.isNull(ownerDtos)) {
            throw new CmdException("业主不存在");
        }

        // 1.0 查询业主房屋关系，获取业主拥有的房屋信息
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerId(ownerDtos.get(0).getOwnerId());
        ownerRoomRelDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        List<FeeDto> resultFees = new ArrayList<>(); // 存储最终返回的欠费列表

        // 如果业主有房屋，查询房屋欠费信息
        if (!ListUtil.isNull(ownerRoomRelDtos)) {
            getRoomOweFee(ownerRoomRelDtos, reqJson, resultFees);
        }

        // 2.0 查询业主车位关系，获取业主拥有的车位信息
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setOwnerId(ownerDtos.get(0).getOwnerId());
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 如果业主有车位，查询车位欠费信息
        if (!ListUtil.isNull(ownerCarDtos)) {
            getParkingSpaceOweFee(ownerCarDtos, reqJson, resultFees);
        }

        // 返回查询结果
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(1, resultFees.size(), resultFees);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询车位欠费信息
     * 
     * 根据业主拥有的车位信息，查询每个车位的欠费情况
     * 
     * @param ownerCarDtos 业主车位关系列表
     * @param reqJson 请求JSON数据
     * @param resultFees 存储查询结果的欠费列表
     */
    private void getParkingSpaceOweFee(List<OwnerCarDto> ownerCarDtos, JSONObject reqJson, List<FeeDto> resultFees) {
        String payObjName = ""; // 缴费对象名称（车位名称）
        for (OwnerCarDto ownerCarDto : ownerCarDtos) {
            // 查询车位详细信息
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
            parkingSpaceDto.setPsId(ownerCarDto.getPsId());
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            
            // 如果车位不存在，跳过当前循环
            if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) {
                continue;
            }
            
            // 获取车位信息并构造缴费对象名称
            ParkingSpaceDto tmpParkingSpaceDto = parkingSpaceDtos.get(0);
            payObjName = tmpParkingSpaceDto.getAreaNum() + "停车场" + tmpParkingSpaceDto.getNum() + "车位";
            
            // 查询该车位的费用信息
            FeeDto feeDto = new FeeDto();
            feeDto.setCommunityId(reqJson.getString("communityId"));
            feeDto.setPayerObjId(ownerCarDto.getPsId());
            feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_PARKING_SPACE);
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            
            // 获取欠费信息
            getOweFee(feeDtos, reqJson, resultFees, payObjName);
        }
    }

    /**
     * 查询房屋欠费信息
     * 
     * 根据业主拥有的房屋信息，查询每个房屋的欠费情况
     * 
     * @param ownerRoomRelDtos 业主房屋关系列表
     * @param reqJson 请求JSON数据
     * @param resultFees 存储查询结果的欠费列表
     */
    private void getRoomOweFee(List<OwnerRoomRelDto> ownerRoomRelDtos, JSONObject reqJson, List<FeeDto> resultFees) {
        String payObjName = ""; // 缴费对象名称（房屋名称）
        for (OwnerRoomRelDto ownerRoomRelDto : ownerRoomRelDtos) {
            // 查询房屋详细信息
            RoomDto roomDto = new RoomDto();
            roomDto.setCommunityId(reqJson.getString("communityId"));
            roomDto.setRoomId(ownerRoomRelDto.getRoomId());
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            
            // 如果房屋不存在，跳过当前循环
            if (roomDtos == null || roomDtos.size() < 1) {
                continue;
            }
            
            // 获取房屋信息并构造缴费对象名称
            RoomDto tmpRoomDto = roomDtos.get(0);
            payObjName = tmpRoomDto.getFloorNum() + "栋" + tmpRoomDto.getUnitNum() + "单元" + tmpRoomDto.getRoomNum() + "室";
            
            // 查询该房屋的费用信息
            FeeDto feeDto = new FeeDto();
            feeDto.setCommunityId(reqJson.getString("communityId"));
            feeDto.setPayerObjId(ownerRoomRelDto.getRoomId());
            feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_ROOM);
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            
            // 获取欠费信息
            getOweFee(feeDtos, reqJson, resultFees, payObjName);
        }
    }

    /**
     * 获取欠费信息核心方法
     * 
     * 根据费用列表判断是否存在欠费，并计算欠费金额
     * 支持两种计费方式：周期性账单和实时计算
     * 
     * @param feeDtos 费用列表
     * @param reqJson 请求JSON数据
     * @param resultFees 存储查询结果的欠费列表
     * @param payObjName 缴费对象名称
     */
    private void getOweFee(List<FeeDto> feeDtos, JSONObject reqJson, List<FeeDto> resultFees, String payObjName) {
        // 如果费用列表为空，直接返回
        if (ListUtil.isNull(feeDtos)) {
            return;
        }

        for (FeeDto feeDto : feeDtos) {
            Date endTime = feeDto.getEndTime();
            // 检查费用是否已到期（是否欠费）
            if (endTime.getTime() > DateUtil.getCurrentDate().getTime()) {// 没有欠费，费用还未到期
                continue;
            }

            // 查询费用配置信息
            FeeConfigDto feeConfigDto = new FeeConfigDto();
            feeConfigDto.setCommunityId(reqJson.getString("communityId"));
            feeConfigDto.setConfigId(feeDto.getConfigId());
            List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
            if (feeConfigDtos == null || feeConfigDtos.size() < 1) {
                continue;
            }

            FeeConfigDto tmpFeeConfigDto = feeConfigDtos.get(0);

            // 判断计费类型：周期性账单还是实时计算
            if (FeeConfigDto.BILL_TYPE_EVERY.equals(tmpFeeConfigDto.getBillType())) {// 实时计算欠费金额
                // 根据缴费对象类型计算费用单价
                if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(feeDto.getPayerObjType())) {
                    computeFeePriceByRoom(feeDto);
                } else {
                    computeFeePriceByParkingSpace(feeDto);
                }
                
                // 计算欠费月数并计算总欠费金额
                double month = DateUtil.dayCompare(endTime, DateUtil.getCurrentDate());
                BigDecimal feePrice = new BigDecimal(feeDto.getFeePrice());
                feePrice = feePrice.multiply(new BigDecimal(month));
                
                // 构造欠费信息对象
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
            
            // 查询档期欠费账单表，获取已生成的欠费账单
            BillOweFeeDto billOweFeeDto = new BillOweFeeDto();
            billOweFeeDto.setCommunityId(reqJson.getString("communityId"));
            billOweFeeDto.setFeeId(feeDto.getFeeId());
            List<BillOweFeeDto> billOweFeeDtos = feeInnerServiceSMOImpl.queryBillOweFees(billOweFeeDto);

            // 如果没有欠费账单，跳过当前费用
            if (billOweFeeDtos == null || billOweFeeDtos.size() < 1) { // 没有欠费
                continue;
            }

            // 处理所有欠费账单
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
     * 
     * 根据车位的面积和费用计算公式计算费用单价
     * 支持两种计算方式：面积*单价+附加费、固定费用
     * 
     * @param feeDto 费用数据传输对象
     */
    private void computeFeePriceByParkingSpace(FeeDto feeDto) {
        // 查询车位信息
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(feeDto.getCommunityId());
        parkingSpaceDto.setPsId(feeDto.getPayerObjId());
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        // 如果车位不存在，直接返回
        if (parkingSpaceDtos == null || parkingSpaceDtos.size() < 1) { // 数据有问题
            return;
        }

        String computingFormula = feeDto.getComputingFormula();
        double feePrice = 0.00;
        
        // 根据计算公式计算费用
        if ("1001".equals(computingFormula)) { // 面积*单价+附加费
            BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
            BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(parkingSpaceDtos.get(0).getArea()));
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        } else if ("2002".equals(computingFormula)) { // 固定费用
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            feePrice = additionalAmount.setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        } else {
            feePrice = -1.00; // 计算公式不支持
        }

        feeDto.setFeePrice(feePrice);
    }

    /**
     * 根据房屋计算费用单价
     * 
     * 根据房屋的面积和费用计算公式计算费用单价
     * 支持两种计算方式：面积*单价+附加费、固定费用
     * 
     * @param feeDto 费用数据传输对象
     */
    private void computeFeePriceByRoom(FeeDto feeDto) {
        // 查询房屋信息
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(feeDto.getCommunityId());
        roomDto.setRoomId(feeDto.getPayerObjId());
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 如果房屋不存在，直接返回
        if (roomDtos == null || roomDtos.size() < 1) { // 数据有问题
            return;
        }

        String computingFormula = feeDto.getComputingFormula();
        double feePrice = 0.00;
        
        // 根据计算公式计算费用
        if ("1001".equals(computingFormula)) { // 面积*单价+附加费
            BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
            BigDecimal builtUpArea = new BigDecimal(Double.parseDouble(roomDtos.get(0).getBuiltUpArea()));
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            feePrice = squarePrice.multiply(builtUpArea).add(additionalAmount).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        } else if ("2002".equals(computingFormula)) { // 固定费用
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            feePrice = additionalAmount.setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        } else {
            feePrice = -1.00; // 计算公式不支持
        }

        feeDto.setFeePrice(feePrice);
    }
}