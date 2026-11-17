package com.java110.job.adapt.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.system.Business;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IPayFeeRuleV1InnerServiceSMO;
import com.java110.intf.fee.IRuleGeneratorPayFeeBillV1InnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.job.adapt.DatabusAdaptImpl;
import com.java110.po.payFee.PayFeeRulePo;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 欠费催缴databus
 */
@Component(value = "roomCreatePayFeeAdapt")
public class RoomCreatePayFeeAdapt extends DatabusAdaptImpl {
    private static Logger logger = LoggerFactory.getLogger(RoomCreatePayFeeAdapt.class);

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IPayFeeRuleV1InnerServiceSMO payFeeRuleV1InnerServiceSMOImpl;

    @Autowired
    private IRuleGeneratorPayFeeBillV1InnerServiceSMO ruleGeneratorPayFeeBillV1InnerServiceSMOImpl;

    @Override
    public void execute(Business business, List<Business> businesses) throws ParseException {
        JSONObject data = business.getData();

        JSONArray roomIds = data.getJSONArray("roomIds");

        if (roomIds == null || roomIds.isEmpty()) {
            return;
        }

        //todo 逐房屋处理：一条失败只跳过该房屋，不影响后续房屋
        for (int roomIndex = 0; roomIndex < roomIds.size(); roomIndex++) {
            String roomId = roomIds.getString(roomIndex);
            try {
                doCreateRoomPayFee(data, roomId);
            } catch (Exception e) {
                //todo 失败的房屋不重试，靠日志定位原因（房屋失效/无业主/费用项配置/金额计算等）
                logger.error("房屋批量创建费用失败 batchId={} communityId={} configId={} roomId={} idx={}/{}",
                        data.getString("batchId"), data.getString("communityId"),
                        data.getString("configId"), roomId, roomIndex + 1, roomIds.size(), e);
            }
        }
    }

    private void doCreateRoomPayFee(JSONObject data, String roomId) {
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(data.getString("communityId"));
        roomDto.setRoomId(roomId);
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        //todo 房屋不存在（如单元被失效后重建），只记日志跳过该房屋，不影响其他房屋
        if (ListUtil.isNull(roomDtos)) {
            logger.error("房屋批量创建费用失败，未查询到房屋 batchId={} communityId={} configId={} roomId={}",
                    data.getString("batchId"), data.getString("communityId"),
                    data.getString("configId"), roomId);
            return;
        }
        RoomDto room = roomDtos.get(0);

        //todo 房屋刷入业主信息（与"设置费用"链路一致）
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(data.getString("communityId"));
        ownerDto.setRoomId(roomId);
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnersByRoom(ownerDto);
        if (ListUtil.isNull(ownerDtos)) {
            throw new IllegalArgumentException("根据房屋未能查到业主，不能创建费用！roomId=" + roomId);
        }
        room.setOwnerId(ownerDtos.get(0).getOwnerId());
        room.setOwnerName(ownerDtos.get(0).getName());

        //todo 查询费用项配置
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(data.getString("communityId"));
        feeConfigDto.setConfigId(data.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        Assert.listOnlyOne(feeConfigDtos, "当前费用项ID不存在或存在多条" + data.getString("configId"));

        //todo 封装费用规则（与"设置费用"链路一致，走规则->账单模式）
        List<PayFeeRulePo> payFeeRulePos = new ArrayList<>();
        payFeeRulePos.add(createPayFeeRulePo(room, data, feeConfigDtos.get(0)));
        doSavePayFeeRules(payFeeRulePos);

    }

    /**
     * 保存费用规则并生成费用账单
     *
     * @param payFeeRulePos
     */
    private void doSavePayFeeRules(List<PayFeeRulePo> payFeeRulePos) {
        //todo 写费用规则表 pay_fee_rule
        payFeeRuleV1InnerServiceSMOImpl.savePayFeeRules(payFeeRulePos);

        //todo 走账单模式生成 pay_fee（内部按 paymentCycle/paymentCd/deductFrom
        //     计算 start_time/end_time/fee_time/金额，与"设置费用"链路完全一致）
        ruleGeneratorPayFeeBillV1InnerServiceSMOImpl.rulesGeneratePayFees(payFeeRulePos);
    }

    private PayFeeRulePo createPayFeeRulePo(RoomDto roomDto, JSONObject paramInJson, FeeConfigDto feeConfigDto) {

        String time = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A);
        if (paramInJson.containsKey("feeEndDate")) {
            time = paramInJson.getString("feeEndDate");
        } else if (paramInJson.containsKey("startTime")) {
            time = paramInJson.getString("startTime");
        }

        PayFeeRulePo payFeeRulePo = new PayFeeRulePo();
        payFeeRulePo.setRuleId(GenerateCodeFactory.getGeneratorId("11"));
        payFeeRulePo.setConfigId(paramInJson.getString("configId"));
        payFeeRulePo.setCommunityId(paramInJson.getString("communityId"));
        payFeeRulePo.setFeeTypeCd(paramInJson.getString("feeTypeCd"));
        payFeeRulePo.setFeeFlag(paramInJson.getString("feeFlag"));
        payFeeRulePo.setAmount("-1");
        if (paramInJson.containsKey("amount") && !StringUtil.isEmpty(paramInJson.getString("amount"))) {
            payFeeRulePo.setAmount(paramInJson.getString("amount"));
        }
        payFeeRulePo.setStartTime(time);
        payFeeRulePo.setEndTime(time);
        payFeeRulePo.setMaxTime(paramInJson.getString("endTime"));
        payFeeRulePo.setBatchId(paramInJson.getString("batchId"));
        payFeeRulePo.setUserId(paramInJson.getString("userId"));
        payFeeRulePo.setIncomeObjId(paramInJson.getString("storeId"));
        payFeeRulePo.setState(FeeDto.STATE_DOING);
        payFeeRulePo.setDeductFrom(feeConfigDto.getDeductFrom());
        payFeeRulePo.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_ROOM);
        payFeeRulePo.setPayerObjId(roomDto.getRoomId());
        payFeeRulePo.setOwnerId(roomDto.getOwnerId());
        payFeeRulePo.setOwnerName(roomDto.getOwnerName());
        payFeeRulePo.setPayerObjName(StringUtil.isEmpty(roomDto.getRoomName())
                ? roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum()
                : roomDto.getRoomName());
        String curYearMonth = DateUtil.getFormatTimeStringB(DateUtil.getDateFromStringB(payFeeRulePo.getEndTime()));
        // 后付费
        if (FeeConfigDto.PAYMENT_CD_AFTER.equals(feeConfigDto.getPaymentCd())) {
            int ruleCycle = Integer.parseInt(feeConfigDto.getPaymentCycle());
            Date endTime = DateUtil.getNextMonthDateB(payFeeRulePo.getEndTime(), ruleCycle);
            curYearMonth = DateUtil.getFormatTimeStringB(endTime);
        }
        payFeeRulePo.setCurYearMonth(curYearMonth);
        return payFeeRulePo;
    }

}
