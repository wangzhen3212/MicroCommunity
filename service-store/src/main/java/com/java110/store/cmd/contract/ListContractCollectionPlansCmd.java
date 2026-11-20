package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractCollectionPlanDto;
import com.java110.intf.store.IContractCollectionPlanInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单合同收款计划期次明细（含履约进度统计）
 */
@Java110Cmd(serviceCode = "contract.listContractCollectionPlans")
public class ListContractCollectionPlansCmd extends Cmd {

    @Autowired
    private IContractCollectionPlanInnerServiceSMO contractCollectionPlanInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "contractId", "请求报文中未包含合同ID");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String contractId = reqJson.getString("contractId");
        String communityId = reqJson.getString("communityId");

        ContractCollectionPlanDto planDto = new ContractCollectionPlanDto();
        planDto.setContractId(contractId);
        planDto.setCommunityId(communityId);
        planDto.setStatusCd("0");
        List<ContractCollectionPlanDto> plans = contractCollectionPlanInnerServiceSMOImpl.queryContractCollectionPlans(planDto);

        Map<String, Object> info = new HashMap<>();
        info.put("contractId", contractId);
        info.put("storeId", storeId);
        info.put("communityId", communityId);
        List<Map> statistics = contractCollectionPlanInnerServiceSMOImpl.queryContractCollectionPlanStatistics(info);
        Map<String, Object> summary = new HashMap<>();
        if (statistics != null && !statistics.isEmpty()) {
            summary = statistics.get(0);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("plans", plans == null ? new ArrayList<>() : plans);
        data.put("statistics", summary);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
