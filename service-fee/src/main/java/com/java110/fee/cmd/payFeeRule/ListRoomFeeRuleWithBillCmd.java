package com.java110.fee.cmd.payFeeRule;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.Java110CmdDoc;
import com.java110.doc.annotation.Java110ExampleDoc;
import com.java110.doc.annotation.Java110ParamDoc;
import com.java110.doc.annotation.Java110ParamsDoc;
import com.java110.doc.annotation.Java110ResponseDoc;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.payFee.PayFeeRuleDto;
import com.java110.intf.fee.IPayFeeRuleV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 房屋计费设置与账单聚合查询
 *
 * 一次返回某个房屋的全部计费设置（pay_fee_rule），每条计费设置内挂未缴清、已取消的账单（pay_fee），
 * 账单与计费设置的关联统一走 pay_fee.rule_id（pay_fee_rule_bill 当前不落库）。
 */
@Java110CmdDoc(title = "查询房屋计费设置与账单",
        description = "按房屋返回计费设置列表，每条计费设置返回未缴清、已取消的账单及汇总金额",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/payFeeRule.listRoomFeeRuleWithBill",
        resource = "feeDoc",
        author = "吴学文",
        serviceCode = "payFeeRule.listRoomFeeRuleWithBill",
        seq = 1
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "payerObjId", length = 30, remark = "房屋ID"),
        @Java110ParamDoc(name = "payerObjType", length = 12, remark = "付费对象类型，默认3333（房屋）"),
        @Java110ParamDoc(name = "state", length = 12, remark = "计费设置状态，2008001 计费中 2009001 已终止，空为全部"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Array", remark = "计费设置列表"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/payFeeRule.listRoomFeeRuleWithBill?communityId=2022121921870161&payerObjId=752022082030880010&payerObjType=3333",
        resBody = "{\"code\":0,\"msg\":\"成功\",\"records\":1,\"total\":1,\"data\":[{\"ruleId\":\"111026102800000000000001\",\"feeName\":\"住宅物业费\",\"state\":\"2008001\",\"unpaidCount\":1,\"oweAmount\":\"224.00\",\"risk\":true,\"bills\":[]}]}"
)
@Java110Cmd(serviceCode = "payFeeRule.listRoomFeeRuleWithBill")
public class ListRoomFeeRuleWithBillCmd extends Cmd {

    private static final String STATUS_CD_VALID = "0";

    private static final String STATUS_CD_DELETED = "1";

    private static final String BILL_STATE_UNPAID = "unpaid";

    private static final String BILL_STATE_CANCELLED = "cancelled";

    private static final String SYNTHETIC_RULE_ID = "-1";

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private IPayFeeRuleV1InnerServiceSMO payFeeRuleV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "payerObjId", "未包含房屋ID");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String communityId = reqJson.getString("communityId");
        String payerObjId = reqJson.getString("payerObjId");
        String payerObjType = reqJson.getString("payerObjType");
        if (StringUtil.isEmpty(payerObjType)) {
            payerObjType = FeeDto.PAYER_OBJ_TYPE_ROOM;
        }

        List<PayFeeRuleDto> rules = queryRules(communityId, payerObjId, payerObjType);

        // 计费设置与账单按 rule_id 分组，规则全量查询后再按状态过滤，避免账单被归入「未纳入」分组
        Map<String, List<PayFeeDto>> ruleBills = new HashMap<>();
        Map<String, PayFeeRuleDto> ruleMap = new HashMap<>();
        for (PayFeeRuleDto rule : rules) {
            ruleMap.put(rule.getRuleId(), rule);
            ruleBills.put(rule.getRuleId(), new ArrayList<>());
        }

        List<PayFeeDto> otherBills = new ArrayList<>();
        collectBills(communityId, payerObjId, payerObjType, STATUS_CD_VALID, ruleMap, ruleBills, otherBills);
        collectBills(communityId, payerObjId, payerObjType, STATUS_CD_DELETED, ruleMap, ruleBills, otherBills);

        List<PayFeeRuleDto> result = new ArrayList<>();
        String state = reqJson.getString("state");
        for (PayFeeRuleDto rule : rules) {
            computeSummary(rule, ruleBills.get(rule.getRuleId()), rule.getFeeName());
            if (!StringUtil.isEmpty(state) && !state.equals(rule.getState())) {
                continue;
            }
            result.add(rule);
        }

        if (StringUtil.isEmpty(state) && !otherBills.isEmpty()) {
            PayFeeRuleDto other = new PayFeeRuleDto();
            other.setRuleId(SYNTHETIC_RULE_ID);
            other.setCommunityId(communityId);
            other.setPayerObjId(payerObjId);
            other.setPayerObjType(payerObjType);
            other.setPayerObjName(reqJson.getString("payerObjName"));
            other.setStatusCd(STATUS_CD_VALID);
            other.setSyntheticOther(true);
            computeSummary(other, otherBills, null);
            result.add(other);
        }

        context.setResponseEntity(ResultVo.createResponseEntity(result.size(), result.size(), result));
    }

    private List<PayFeeRuleDto> queryRules(String communityId, String payerObjId, String payerObjType) {
        PayFeeRuleDto ruleDto = new PayFeeRuleDto();
        ruleDto.setCommunityId(communityId);
        ruleDto.setPayerObjId(payerObjId);
        ruleDto.setPayerObjType(payerObjType);
        ruleDto.setStatusCd(STATUS_CD_VALID);
        ruleDto.setPage(-1);
        List<PayFeeRuleDto> rules = payFeeRuleV1InnerServiceSMOImpl.queryPayFeeRules(ruleDto);
        if (rules == null) {
            return new ArrayList<>();
        }
        return rules;
    }

    private void collectBills(String communityId, String payerObjId, String payerObjType, String statusCd,
                              Map<String, PayFeeRuleDto> ruleMap, Map<String, List<PayFeeDto>> ruleBills,
                              List<PayFeeDto> otherBills) {
        PayFeeDto feeDto = new PayFeeDto();
        feeDto.setCommunityId(communityId);
        feeDto.setPayerObjId(payerObjId);
        feeDto.setPayerObjType(payerObjType);
        feeDto.setStatusCd(statusCd);
        feeDto.setPage(-1);
        List<PayFeeDto> fees = payFeeV1InnerServiceSMOImpl.queryPayFees(feeDto);
        if (fees == null || fees.isEmpty()) {
            return;
        }
        for (PayFeeDto fee : fees) {
            // 账单查询以状态作为条件，需回填状态
            fee.setStatusCd(statusCd);
            String ruleId = fee.getRuleId();
            if (StringUtil.isEmpty(ruleId) || !ruleMap.containsKey(ruleId)) {
                otherBills.add(fee);
                continue;
            }
            ruleBills.get(ruleId).add(fee);
        }
    }

    private void computeSummary(PayFeeRuleDto rule, List<PayFeeDto> ruleBills, String feeName) {
        List<PayFeeDto> bills = ruleBills == null ? new ArrayList<>() : ruleBills;
        List<PayFeeDto> pendingBills = new ArrayList<>();
        BigDecimal receivable = ZERO;
        BigDecimal received = ZERO;
        BigDecimal owe = ZERO;
        int billCount = 0;
        int unpaidCount = 0;
        int paidCount = 0;
        int cancelledCount = 0;

        for (PayFeeDto bill : bills) {
            bill.setFeeName(feeName);
            bill.setYearMonth(resolveYearMonth(bill));
            if (STATUS_CD_DELETED.equals(bill.getStatusCd())) {
                cancelledCount++;
                bill.setBillState(BILL_STATE_CANCELLED);
                bill.setReceivedAmount(formatAmount(ZERO));
                pendingBills.add(bill);
                continue;
            }
            billCount++;
            BigDecimal billReceivable = toDecimal(bill.getReceivableAmount());
            BigDecimal billOwe = toDecimal(bill.getOweAmount());
            if (billOwe.compareTo(ZERO) < 0) {
                billOwe = ZERO;
            }
            BigDecimal billReceived = billReceivable.subtract(billOwe);
            if (billReceived.compareTo(ZERO) < 0) {
                billReceived = ZERO;
            }
            receivable = receivable.add(billReceivable);
            received = received.add(billReceived);
            owe = owe.add(billOwe);
            bill.setReceivedAmount(formatAmount(billReceived));
            if (billOwe.compareTo(ZERO) > 0) {
                unpaidCount++;
                bill.setBillState(BILL_STATE_UNPAID);
                pendingBills.add(bill);
            } else {
                paidCount++;
            }
        }

        pendingBills.sort((bill1, bill2) -> {
            String feeTime1 = StringUtil.isEmpty(bill1.getFeeTime()) ? "" : bill1.getFeeTime();
            String feeTime2 = StringUtil.isEmpty(bill2.getFeeTime()) ? "" : bill2.getFeeTime();
            return feeTime2.compareTo(feeTime1);
        });

        rule.setBills(pendingBills);
        rule.setBillCount(billCount);
        rule.setUnpaidCount(unpaidCount);
        rule.setPaidCount(paidCount);
        rule.setCancelledCount(cancelledCount);
        rule.setReceivableAmount(formatAmount(receivable));
        rule.setReceivedAmount(formatAmount(received));
        rule.setOweAmount(formatAmount(owe));
        rule.setSettled(FeeDto.STATE_FINISH.equals(rule.getState()) && unpaidCount == 0);
        rule.setRisk(FeeDto.STATE_DOING.equals(rule.getState()) && cancelledCount > 0);
    }

    private String resolveYearMonth(PayFeeDto bill) {
        String feeTime = bill.getFeeTime();
        if (!StringUtil.isEmpty(feeTime) && feeTime.length() >= 7) {
            return feeTime.substring(0, 7);
        }
        String startTime = bill.getStartTime();
        if (!StringUtil.isEmpty(startTime) && startTime.length() >= 7) {
            return startTime.substring(0, 7);
        }
        return "";
    }

    private BigDecimal toDecimal(String amount) {
        if (StringUtil.isEmpty(amount)) {
            return ZERO;
        }
        try {
            return new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            return ZERO;
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
