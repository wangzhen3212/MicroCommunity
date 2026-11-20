package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractCollectionPlanDto;
import com.java110.dto.contract.ContractFeeDto;
import com.java110.intf.store.IContractCollectionPlanInnerServiceSMO;
import com.java110.intf.store.IContractFeeInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单合同收款计划期次明细（含履约进度统计、费用构成）
 */
@Java110Cmd(serviceCode = "contract.listContractCollectionPlans")
public class ListContractCollectionPlansCmd extends Cmd {

    @Autowired
    private IContractCollectionPlanInnerServiceSMO contractCollectionPlanInnerServiceSMOImpl;

    @Autowired
    private IContractFeeInnerServiceSMO contractFeeInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "contractId", "请求报文中未包含合同ID");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String contractId = reqJson.getString("contractId");

        ContractCollectionPlanDto planDto = new ContractCollectionPlanDto();
        planDto.setContractId(contractId);
        planDto.setStatusCd("0");
        List<ContractCollectionPlanDto> plans = contractCollectionPlanInnerServiceSMOImpl.queryContractCollectionPlans(planDto);

        ContractFeeDto feeDto = new ContractFeeDto();
        feeDto.setContractId(contractId);
        feeDto.setStatusCd("0");
        List<ContractFeeDto> fees = contractFeeInnerServiceSMOImpl.queryContractFees(feeDto);

        Map<String, Object> info = new HashMap<>();
        info.put("contractId", contractId);
        info.put("storeId", storeId);
        List<Map> statistics = contractCollectionPlanInnerServiceSMOImpl.queryContractCollectionPlanStatistics(info);
        Map<String, Object> summary = new HashMap<>();
        if (statistics != null && !statistics.isEmpty()) {
            summary = statistics.get(0);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("plans", plans == null ? new ArrayList<>() : plans);
        data.put("fees", fees == null ? new ArrayList<>() : fees);
        data.put("feeSummary", buildFeeSummary(fees));
        data.put("statistics", summary);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 费用构成合计：费用总额 / 计入合同金额合计
     */
    private Map<String, Object> buildFeeSummary(List<ContractFeeDto> fees) {
        BigDecimal feeTotal = BigDecimal.ZERO;
        BigDecimal includeTotal = BigDecimal.ZERO;
        if (fees != null) {
            for (ContractFeeDto fee : fees) {
                BigDecimal amount = parseAmount(fee.getAmount());
                feeTotal = feeTotal.add(amount);
                if (ContractFeeDto.INCLUDE_IN_AMOUNT_YES.equals(fee.getIncludeInAmount())) {
                    includeTotal = includeTotal.add(amount);
                }
            }
        }
        Map<String, Object> feeSummary = new HashMap<>();
        feeSummary.put("feeTotal", feeTotal.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        feeSummary.put("includeTotal", includeTotal.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        return feeSummary;
    }

    private BigDecimal parseAmount(String amount) {
        if (StringUtil.isEmpty(amount)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
