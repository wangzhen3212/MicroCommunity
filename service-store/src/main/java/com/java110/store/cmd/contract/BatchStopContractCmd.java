package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractAuditDto;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractAuditInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.po.contract.ContractAuditPo;
import com.java110.po.contract.ContractPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量终止合同（逐条校验状态，审批中的合同同时结束在途审批实例）
 */
@Java110Cmd(serviceCode = "contract.batchStopContract")
public class BatchStopContractCmd extends Cmd {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractAuditInnerServiceSMO contractAuditInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        if (!reqJson.containsKey("contractIds")) {
            throw new CmdException("请求报文中未包含合同");
        }
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        JSONArray contractIds = reqJson.getJSONArray("contractIds");
        if (contractIds == null || contractIds.isEmpty()) {
            throw new CmdException("请求报文中未包含合同");
        }

        List<String> allowStates = Arrays.asList(ContractDto.STATE_DRAFT,
                ContractDto.STATE_AUDIT, ContractDto.STATE_AUDIT_FINISH);
        int successCount = 0;
        List<Map<String, Object>> failList = new ArrayList<>();
        for (int i = 0; i < contractIds.size(); i++) {
            String contractId = contractIds.getString(i);
            try {
                stopContract(contractId, storeId, allowStates);
                successCount++;
            } catch (Exception e) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("contractId", contractId);
                fail.put("msg", StringUtil.isEmpty(e.getMessage()) ? "终止失败" : e.getMessage());
                failList.add(fail);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("successCount", successCount);
        data.put("failCount", failList.size());
        data.put("failList", failList);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    private void stopContract(String contractId, String storeId, List<String> allowStates) {
        ContractDto contractDto = new ContractDto();
        contractDto.setContractId(contractId);
        contractDto.setStoreId(storeId);
        contractDto.setStatusCd("0");
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
        if (contractDtos == null || contractDtos.isEmpty()) {
            throw new CmdException("合同不存在");
        }
        ContractDto contract = contractDtos.get(0);
        if (!allowStates.contains(contract.getState())) {
            throw new CmdException("合同[已终止/已到期]不可再次终止");
        }

        ContractPo contractPo = new ContractPo();
        contractPo.setContractId(contractId);
        contractPo.setState(ContractDto.STATE_COMPLAINT);
        contractPo.setStatusCd("0");
        int flag = contractInnerServiceSMOImpl.updateContract(contractPo);
        if (flag < 1) {
            throw new CmdException("终止合同失败");
        }

        ContractAuditDto auditDto = new ContractAuditDto();
        auditDto.setBizId(contractId);
        auditDto.setAuditType(ContractAuditDto.AUDIT_TYPE_CONTRACT);
        auditDto.setState(ContractAuditDto.STATE_DOING);
        auditDto.setStatusCd("0");
        List<ContractAuditDto> auditDtos = contractAuditInnerServiceSMOImpl.queryContractAudits(auditDto);
        if (auditDtos == null || auditDtos.isEmpty()) {
            return;
        }
        ContractAuditPo auditPo = new ContractAuditPo();
        auditPo.setAuditId(auditDtos.get(0).getAuditId());
        auditPo.setState(ContractAuditDto.STATE_CANCEL);
        auditPo.setFinishTime(DateUtil.getCurrentDate());
        auditPo.setStatusCd("0");
        contractAuditInnerServiceSMOImpl.updateContractAudit(auditPo);
    }
}
