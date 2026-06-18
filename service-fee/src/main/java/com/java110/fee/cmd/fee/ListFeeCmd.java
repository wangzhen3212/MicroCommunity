package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.core.smo.IOwnerGetDataCheck;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.UnitDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.api.fee.ApiFeeDataVo;
import com.java110.vo.api.fee.ApiFeeVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 查询费用信息命令类
 * 实现费用列表查询功能，包括费用计算、房屋信息关联、业主权限校验等业务逻辑
 * 服务代码：fee.listFee
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "fee.listFee")
public class ListFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListFeeCmd.class);

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerGetDataCheck ownerGetDataCheckImpl;

    //域
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    //键
    public static final String TOTAL_FEE_PRICE = "TOTAL_FEE_PRICE";

    //键
    public static final String RECEIVED_AMOUNT_SWITCH = "RECEIVED_AMOUNT_SWITCH";

    //禁用电脑端提交收费按钮
    public static final String OFFLINE_PAY_FEE_SWITCH = "OFFLINE_PAY_FEE_SWITCH";

    /**
     * 参数校验方法
     * 验证请求参数的有效性，包括分页信息和小区ID，并进行业主权限校验
     *
     * @param event CMD事件对象
     * @param cmdDataFlowContext CMD数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");

        // todo 业主查询合法性校验
        ownerGetDataCheckImpl.checkOwnerFee(CmdContextUtils.getAppId(cmdDataFlowContext), CmdContextUtils.getLoginUserId(cmdDataFlowContext), reqJson);
    }

    /**
     * 命令执行方法
     * 处理费用查询请求，包括房屋信息处理、费用计算、结果封装等核心业务逻辑
     *
     * @param event CMD事件对象
     * @param cmdDataFlowContext CMD数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证房屋编号格式
        if (reqJson.containsKey("roomNum") && !StringUtil.isEmpty(reqJson.getString("roomNum"))) {
            String[] roomNums = reqJson.getString("roomNum").split("-");
            if (roomNums.length != 3) {
                throw new IllegalArgumentException("房屋编号格式不对！");
            }
            RoomDto roomDto = new RoomDto();
            roomDto.setCommunityId(reqJson.getString("communityId"));
            roomDto.setFloorNum(roomNums[0]);
            roomDto.setUnitNum(roomNums[1]);
            roomDto.setRoomNum(roomNums[2]);
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            Assert.listOnlyOne(roomDtos, "找不到房屋信息！");
        }
        // todo 房屋名称 刷入 房屋ID
        freshPayerObjIdByRoomNum(reqJson);

        FeeDto feeDto = BeanConvertUtil.covertBean(reqJson, FeeDto.class);

        // todo 处理 多个房屋
        morePayerObjIds(reqJson, feeDto);

        List<ApiFeeDataVo> fees = new ArrayList<>();
        // 根据业主ID查询费用信息
        if (reqJson.containsKey("ownerId") && !StringUtil.isEmpty(reqJson.getString("ownerId"))) {
            OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
            ownerRoomRelDto.setRoomId(reqJson.getString("payerObjId"));
            ownerRoomRelDto.setOwnerId(reqJson.getString("ownerId"));
            List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
            // 如果业主与房屋无关联关系，返回空结果
            if (ListUtil.isNull(ownerRoomRelDtos)) {
                ApiFeeVo apiFeeVo = new ApiFeeVo();
                apiFeeVo.setTotal(0);
                apiFeeVo.setRecords((int) Math.ceil((double) 0 / (double) reqJson.getInteger("row")));
                apiFeeVo.setFees(fees);
                ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeVo), HttpStatus.OK);
                cmdDataFlowContext.setResponseEntity(responseEntity);
            }
        }
        
        // 查询费用总数
        int count = feeInnerServiceSMOImpl.queryFeesCount(feeDto);
        if (count > 0) {
            // 查询费用明细数据
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            //todo 计算费用
            computeFeePrice(feeDtos);
            List<ApiFeeDataVo> apiFeeDataVos = BeanConvertUtil.covertBeanList(feeDtos, ApiFeeDataVo.class);
            for (ApiFeeDataVo apiFeeDataVo : apiFeeDataVos) {
                //获取付费对象类型
                String payerObjType = apiFeeDataVo.getPayerObjType();
                // 如果是车辆费用，设置车辆类型编码
                if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(payerObjType)) {
                    apiFeeDataVo.setCarTypeCd("1001");
                }
                fees.add(apiFeeDataVo);
            }
            // 刷新费用属性信息
            freshFeeAttrs(fees, feeDtos);
        } else {
            fees = new ArrayList<>();
        }
        
        // 封装返回结果
        ApiFeeVo apiFeeVo = new ApiFeeVo();
        apiFeeVo.setTotal(count);
        apiFeeVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiFeeVo.setFees(fees);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 处理多个付费对象ID
     * 将逗号分隔的付费对象ID字符串转换为数组并设置到费用DTO中
     *
     * @param reqJson 请求JSON对象
     * @param feeDto 费用数据传输对象
     */
    private void morePayerObjIds(JSONObject reqJson, FeeDto feeDto) {
        if (!reqJson.containsKey("payerObjIds") || StringUtil.isEmpty(reqJson.getString("payerObjIds"))) {
            return;
        }
        String payerObjIds = reqJson.getString("payerObjIds");
        feeDto.setPayerObjIds(payerObjIds.split(","));
    }

    /**
     * 根据房屋编号刷新付费对象ID
     * 通过房屋编号（格式：楼栋号-单元号-房间号）查询对应的房屋ID并设置到请求参数中
     *
     * @param reqJson 请求JSON对象，包含小区ID和房屋编号
     */
    private void freshPayerObjIdByRoomNum(JSONObject reqJson) {
        if (!reqJson.containsKey("roomNum") || StringUtil.isEmpty(reqJson.getString("roomNum"))) {
            return;
        }
        // 解析房屋编号：楼栋号-单元号-房间号
        String[] roomNums = reqJson.getString("roomNum").split("-", 3);
        if (roomNums == null || roomNums.length != 3) {
            throw new IllegalArgumentException("房屋编号格式错误！");
        }
        String floorNum = roomNums[0];
        String unitNum = roomNums[1];
        String roomNum = roomNums[2];
        
        // 根据楼栋号查询楼栋信息
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorNum(floorNum);
        floorDto.setCommunityId(reqJson.getString("communityId"));
        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
        if (ListUtil.isNull(floorDtos)) {
            return;
        }
        
        // 遍历楼栋，查询单元信息
        for (FloorDto floor : floorDtos) {
            UnitDto unitDto = new UnitDto();
            unitDto.setFloorId(floor.getFloorId());
            unitDto.setUnitNum(unitNum);
            List<UnitDto> unitDtos = unitInnerServiceSMOImpl.queryUnits(unitDto);
            if (ListUtil.isNull(unitDtos)) {
                continue;
            }
            
            // 遍历单元，查询房间信息
            for (UnitDto unit : unitDtos) {
                RoomDto roomDto = new RoomDto();
                roomDto.setUnitId(unit.getUnitId());
                roomDto.setRoomNum(roomNum);
                roomDto.setCommunityId(reqJson.getString("communityId"));
                List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
                Assert.listOnlyOne(roomDtos, "查询不到房屋！");
                // 设置付费对象ID为房间ID
                reqJson.put("payerObjId", roomDtos.get(0).getRoomId());
            }
        }
    }

    /**
     * 刷新费用属性信息
     * 将费用DTO中的属性信息设置到API返回VO中
     *
     * @param fees API费用数据VO列表
     * @param feeDtos 费用数据传输对象列表
     */
    private void freshFeeAttrs(List<ApiFeeDataVo> fees, List<FeeDto> feeDtos) {
        for (ApiFeeDataVo apiFeeDataVo : fees) {
            for (FeeDto feeDto : feeDtos) {
                if (apiFeeDataVo.getFeeId().equals(feeDto.getFeeId())) {
                    apiFeeDataVo.setFeeAttrs(feeDto.getFeeAttrDtos());
                }
            }
        }
    }

    /**
     * 计算费用价格
     * 对费用列表进行价格计算，包括总价、欠费金额、到期时间等
     *
     * @param feeDtos 费用数据传输对象列表
     */
    private void computeFeePrice(List<FeeDto> feeDtos) {
        if (ListUtil.isNull(feeDtos)) {
            return;
        }
        
        // 获取总费用价格配置，先取小区配置，没有则取全局配置
        String val = CommunitySettingFactory.getValue(feeDtos.get(0).getCommunityId(), TOTAL_FEE_PRICE);
        if (StringUtil.isEmpty(val)) {
            val = MappingCache.getValue(DOMAIN_COMMON, TOTAL_FEE_PRICE);
        }
        
        // 获取实收款开关配置
        String received_amount_switch = CommunitySettingFactory.getValue(feeDtos.get(0).getCommunityId(), RECEIVED_AMOUNT_SWITCH);
        if (StringUtil.isEmpty(received_amount_switch)) {
            received_amount_switch = MappingCache.getValue(DOMAIN_COMMON, RECEIVED_AMOUNT_SWITCH);
        }
        
        // 获取线下支付开关配置
        String offlinePayFeeSwitch = CommunitySettingFactory.getValue(feeDtos.get(0).getCommunityId(), OFFLINE_PAY_FEE_SWITCH);
        if (StringUtil.isEmpty(offlinePayFeeSwitch)) {
            offlinePayFeeSwitch = MappingCache.getValue(DOMAIN_COMMON, OFFLINE_PAY_FEE_SWITCH);
        }
        
        // 遍历计算每个费用的价格信息
        for (FeeDto feeDto : feeDtos) {
            try {
                // 轮数 * 周期 * 30 + 开始时间 = 目标 到期时间
                Map<String, Object> targetEndDateAndOweMonth = computeFeeSMOImpl.getTargetEndDateAndOweMonth(feeDto);
                Date targetEndDate = (Date) targetEndDateAndOweMonth.get("targetEndDate");
                double oweMonth = (double) targetEndDateAndOweMonth.get("oweMonth");
                feeDto.setCycle(feeDto.getPaymentCycle());
                //todo 这里考虑 账单模式的场景
                if (FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())) {
                    feeDto.setCycle(oweMonth + "");
                }
                feeDto.setDeadlineTime(targetEndDate);
                //todo 算费
                doComputeFeePrice(feeDto, oweMonth);

                feeDto.setVal(val);
                //关闭 线下收银功能
                if (StringUtil.isEmpty(received_amount_switch)) {
                    feeDto.setReceivedAmountSwitch("1");//默认启用实收款输入框
                } else {
                    feeDto.setReceivedAmountSwitch(received_amount_switch);
                }
                feeDto.setOfflinePayFeeSwitch(offlinePayFeeSwitch);
            } catch (Exception e) {
                logger.error("查询费用信息 ，费用信息错误", e);
            }
            //去掉多余0
            feeDto.setSquarePrice(Double.parseDouble(feeDto.getSquarePrice()) + "");
            feeDto.setAdditionalAmount(Double.parseDouble(feeDto.getAdditionalAmount()) + "");
        }
    }

    /**
     * 执行费用价格计算
     * 根据费用计算公式和欠费月数计算具体费用金额
     *
     * @param feeDto 费用数据传输对象
     * @param oweMonth 欠费月数
     */
    private void doComputeFeePrice(FeeDto feeDto, double oweMonth) {
        String computingFormula = feeDto.getComputingFormula();
        // 获取费用价格信息
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        //BigDecimal feeTotalPrice = new BigDecimal(Double.parseDouble(feePriceAll.get("feeTotalPrice").toString()));
        // 计算总费用价格，考虑小数位数和精度
        feeDto.setFeeTotalPrice(MoneyUtil.computePriceScale(Double.parseDouble(feePriceAll.get("feeTotalPrice").toString()),
                feeDto.getScale(),
                Integer.parseInt(feeDto.getDecimalPlace())));
        // 计算欠费金额
        BigDecimal curFeePrice = new BigDecimal(feeDto.getFeePrice());
        curFeePrice = curFeePrice.multiply(new BigDecimal(oweMonth));
        feeDto.setAmountOwed(MoneyUtil.computePriceScale(curFeePrice.doubleValue(), feeDto.getScale(), Integer.parseInt(feeDto.getDecimalPlace())) + "");
        
        //动态费用处理
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())
                && feeDto.getDeadlineTime() == null) {
            feeDto.setDeadlineTime(DateUtil.getCurrentDate());
        }
        //考虑租金递增
        computeFeeSMOImpl.dealRentRate(feeDto);
    }
}