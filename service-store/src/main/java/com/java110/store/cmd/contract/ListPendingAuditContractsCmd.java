package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractAuditLogDto;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.store.bmo.contract.IContractAuditBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询合同待审核列表（state 11 法务校对 / 56 领导审批），并带出最近一次审批记录
 */
@Java110Cmd(serviceCode = "contract.listPendingAuditContracts")
public class ListPendingAuditContractsCmd extends Cmd {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractAuditBMO contractAuditBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String communityId = reqJson.getString("communityId");

        ContractDto contractDto = BeanConvertUtil.covertBean(reqJson, ContractDto.class);
        contractDto.setStoreId(storeId);
        contractDto.setStates(new String[]{ContractDto.STATE_AUDIT, ContractDto.STATE_AUDIT_LEADER});

        int count = contractInnerServiceSMOImpl.queryContractsCount(contractDto);
        List<Map<String, Object>> data = new ArrayList<>();
        if (count < 1) {
            context.setResponseEntity(ResultVo.createResponseEntity(
                    (int) Math.ceil((double) count / (double) contractDto.getRow()), count, data));
            return;
        }

        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
        String[] contractIds = new String[contractDtos.size()];
        for (int index = 0; index < contractDtos.size(); index++) {
            contractIds[index] = contractDtos.get(index).getContractId();
        }
        Map<String, Map<String, Object>> latestAuditLogs = contractAuditBMOImpl.queryLatestAuditLogs(
                ContractAuditLogDto.BIZ_TYPE_CONTRACT, contractIds, storeId, communityId);

        for (ContractDto contract : contractDtos) {
            data.add(wrapPendingContract(contract, latestAuditLogs.get(contract.getContractId())));
        }

        context.setResponseEntity(ResultVo.createResponseEntity(
                (int) Math.ceil((double) count / (double) contractDto.getRow()), count, data));
    }

    /**
     * 列表行：合同基本信息 + 最近一次审批记录（列表直接展示，无需再查询审批链）
     */
    private Map<String, Object> wrapPendingContract(ContractDto contract, Map<String, Object> lastAuditLog) {
        Map<String, Object> row = new HashMap<>();
        row.put("contractId", contract.getContractId());
        row.put("contractCode", contract.getContractCode());
        row.put("contractName", contract.getContractName());
        row.put("partyA", contract.getPartyA());
        row.put("partyB", contract.getPartyB());
        row.put("amount", contract.getAmount());
        row.put("state", contract.getState());
        row.put("stateName", contract.getStateName());
        Map<String, Object> lastAudit = lastAuditLog == null ? new HashMap<>() : lastAuditLog;
        row.put("lastAuditLog", lastAudit);
        return row;
    }
}
