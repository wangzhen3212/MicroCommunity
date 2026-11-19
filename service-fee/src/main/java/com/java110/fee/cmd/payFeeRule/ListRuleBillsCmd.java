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
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 按计费设置（pay_fee_rule.ruleId）查询账单
 *
 * 账单与计费设置的关联统一走 pay_fee.rule_id，金额与状态口径对齐 ListRoomFeeRuleWithBillCmd#computeSummary，
 * 避免聚合接口与本接口两处口径不一致。
 */
@Java110CmdDoc(title = "查询计费设置下的账单",
        description = "按计费设置ID查询账单，支持全部、已缴、未缴、已取消视图",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/payFeeRule.listRuleBills",
        resource = "feeDoc",
        author = "吴学文",
        serviceCode = "payFeeRule.listRuleBills",
        seq = 2
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "ruleId", length = 30, remark = "计费设置ID"),
        @Java110ParamDoc(name = "billState", length = 12, remark = "账单视图 all 全部 paid 已缴 unpaid 未缴 cancelled 已取消，默认 all"),
        @Java110ParamDoc(name = "page", type = "int", length = 11, remark = "分页页数，-1 为全量"),
        @Java110ParamDoc(name = "row", type = "int", length = 11, remark = "分页行数"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Array", remark = "账单列表"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/payFeeRule.listRuleBills?communityId=2022121921870161&ruleId=111026102800000000000001&billState=unpaid&page=-1",
        resBody = "{\"code\":0,\"msg\":\"成功\",\"records\":1,\"total\":1,\"data\":[{\"feeId\":\"112026111900000000000001\",\"yearMonth\":\"2026-11\",\"receivableAmount\":\"224.00\",\"receivedAmount\":\"0.00\",\"oweAmount\":\"224.00\",\"billState\":\"unpaid\"}]}"
)
@Java110Cmd(serviceCode = "payFeeRule.listRuleBills")
public class ListRuleBillsCmd extends Cmd {

    private static final String STATUS_CD_VALID = "0";

    private static final String STATUS_CD_DELETED = "1";

    private static final String BILL_STATE_ALL = "all";

    private static final String BILL_STATE_PAID = "paid";

    private static final String BILL_STATE_UNPAID = "unpaid";

    private static final String BILL_STATE_CANCELLED = "cancelled";

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "ruleId", "未包含计费设置ID");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String communityId = reqJson.getString("communityId");
        String ruleId = reqJson.getString("ruleId");
        String billState = reqJson.getString("billState");
        if (StringUtil.isEmpty(billState)) {
            billState = BILL_STATE_ALL;
        }

        List<PayFeeDto> bills = new ArrayList<>();
        bills.addAll(queryBills(communityId, ruleId, STATUS_CD_VALID, reqJson));
        // 已取消账单仅在「全部」「已取消」视图下参与查询
        if (BILL_STATE_ALL.equals(billState) || BILL_STATE_CANCELLED.equals(billState)) {
            bills.addAll(queryBills(communityId, ruleId, STATUS_CD_DELETED, reqJson));
        }

        List<PayFeeDto> result = new ArrayList<>();
        for (PayFeeDto bill : bills) {
            computeBill(bill);
            if (!BILL_STATE_ALL.equals(billState) && !billState.equals(bill.getBillState())) {
                continue;
            }
            result.add(bill);
        }

        result.sort((bill1, bill2) -> {
            String feeTime1 = StringUtil.isEmpty(bill1.getFeeTime()) ? "" : bill1.getFeeTime();
            String feeTime2 = StringUtil.isEmpty(bill2.getFeeTime()) ? "" : bill2.getFeeTime();
            return feeTime2.compareTo(feeTime1);
        });

        context.setResponseEntity(ResultVo.createResponseEntity(result.size(), result.size(), result));
    }

    private List<PayFeeDto> queryBills(String communityId, String ruleId, String statusCd, JSONObject reqJson) {
        PayFeeDto feeDto = new PayFeeDto();
        feeDto.setCommunityId(communityId);
        feeDto.setRuleId(ruleId);
        feeDto.setStatusCd(statusCd);
        // 未同时传 page、row 时按全量查询（page=-1 时 SMO 不做 offset 转换）
        int page = reqJson.getIntValue("page");
        int row = reqJson.getIntValue("row");
        if (page > 0 && row > 0) {
            feeDto.setPage(page);
            feeDto.setRow(row);
        } else {
            feeDto.setPage(-1);
        }
        List<PayFeeDto> fees = payFeeV1InnerServiceSMOImpl.queryPayFees(feeDto);
        if (fees == null) {
            return new ArrayList<>();
        }
        for (PayFeeDto fee : fees) {
            // 账单查询以状态作为条件，需回填状态
            fee.setStatusCd(statusCd);
        }
        return fees;
    }

    private void computeBill(PayFeeDto bill) {
        bill.setYearMonth(resolveYearMonth(bill));
        if (STATUS_CD_DELETED.equals(bill.getStatusCd())) {
            bill.setBillState(BILL_STATE_CANCELLED);
            bill.setOweAmount(formatAmount(ZERO));
            bill.setReceivedAmount(formatAmount(ZERO));
            return;
        }
        BigDecimal billReceivable = toDecimal(bill.getReceivableAmount());
        BigDecimal billOwe = toDecimal(bill.getOweAmount());
        // 缴费结清后 pay_fee.state 会置为收费结束，历史数据 owe_amount 可能未同步归零
        if (FeeDto.STATE_FINISH.equals(bill.getState())) {
            billOwe = ZERO;
        }
        if (billOwe.compareTo(ZERO) < 0) {
            billOwe = ZERO;
        }
        BigDecimal billReceived = billReceivable.subtract(billOwe);
        if (billReceived.compareTo(ZERO) < 0) {
            billReceived = ZERO;
        }
        bill.setOweAmount(formatAmount(billOwe));
        bill.setReceivedAmount(formatAmount(billReceived));
        bill.setBillState(billOwe.compareTo(ZERO) > 0 ? BILL_STATE_UNPAID : BILL_STATE_PAID);
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
