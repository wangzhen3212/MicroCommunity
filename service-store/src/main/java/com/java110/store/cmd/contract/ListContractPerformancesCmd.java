package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractInnerServiceSMO;
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
 * 合同履约汇总列表（采购/维保合同按付款节点跟踪，含已付/待付/逾期与下一节点）
 */
@Java110Cmd(serviceCode = "contract.listContractPerformances")
public class ListContractPerformancesCmd extends Cmd {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);

        Map<String, Object> info = buildQueryInfo(reqJson);
        info.put("storeId", storeId);
        info.put("communityId", reqJson.getString("communityId"));

        int count = contractInnerServiceSMOImpl.queryContractPerformancesCount(info);

        List<Map> performances = new ArrayList<>();
        if (count > 0) {
            performances = contractInnerServiceSMOImpl.queryContractPerformances(info);
        }

        Map<String, Object> summary = querySummary(info);

        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")),
                count, performances, summary);
        context.setResponseEntity(ResultVo.createResponseEntity(resultVo));
    }

    /**
     * 汇总卡：履约中合同数、已付金额、待付金额、逾期节点数（不参与分页）
     */
    private Map<String, Object> querySummary(Map<String, Object> info) {
        Map<String, Object> summaryInfo = new HashMap<>(info);
        summaryInfo.remove("page");
        summaryInfo.put("page", -1);
        List<Map> allPerformances = contractInnerServiceSMOImpl.queryContractPerformances(summaryInfo);

        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        int overdueCount = 0;
        int performingCount = 0;
        if (allPerformances != null) {
            for (Map performance : allPerformances) {
                if (performance.get("planCount") != null
                        && Integer.parseInt(performance.get("planCount").toString()) > 0) {
                    performingCount++;
                }
                paidAmount = paidAmount.add(toDecimal(performance.get("paidAmount")));
                unpaidAmount = unpaidAmount.add(toDecimal(performance.get("unpaidAmount")));
                if (performance.get("overdueCount") != null) {
                    overdueCount += Integer.parseInt(performance.get("overdueCount").toString());
                }
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("performingCount", performingCount);
        summary.put("paidAmount", paidAmount);
        summary.put("unpaidAmount", unpaidAmount);
        summary.put("overdueCount", overdueCount);
        return summary;
    }

    private Map<String, Object> buildQueryInfo(JSONObject reqJson) {
        Map<String, Object> info = new HashMap<>();
        if (!StringUtil.isEmpty(reqJson.getString("category"))) {
            info.put("category", reqJson.getString("category"));
        } else { // 履约页默认只展示采购与维保合同，房屋出租走收费管理
            info.put("categories", new String[]{ContractDto.CATEGORY_PURCHASE, ContractDto.CATEGORY_MAINTAIN});
        }
        if (!StringUtil.isEmpty(reqJson.getString("payState"))) {
            info.put("payState", reqJson.getString("payState"));
        }
        if (!StringUtil.isEmpty(reqJson.getString("searchText"))) {
            info.put("searchText", reqJson.getString("searchText"));
        }
        if (!StringUtil.isEmpty(reqJson.getString("contractId"))) {
            info.put("contractId", reqJson.getString("contractId"));
        }
        info.put("page", reqJson.getInteger("page"));
        info.put("row", reqJson.getInteger("row"));
        return info;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null || StringUtil.isEmpty(value.toString())) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }
}
