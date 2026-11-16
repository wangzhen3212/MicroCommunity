package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractAuditDto;
import com.java110.store.bmo.contract.IContractAuditBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 提交合同/变更两级审批（法务校对 -> 领导审核）
 */
@Java110Cmd(serviceCode = "contract.submitContractAudit")
public class SubmitContractAuditCmd extends Cmd {

    @Autowired
    private IContractAuditBMO contractAuditBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "bizId", "请求报文中未包含业务ID");
        if (StringUtil.isEmpty(reqJson.getString("auditType"))) {
            reqJson.put("auditType", ContractAuditDto.AUDIT_TYPE_CONTRACT);
        }
        if (StringUtil.isEmpty(reqJson.getString("submitStaffId"))) {
            reqJson.put("submitStaffId", CmdContextUtils.getUserId(context));
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        ContractAuditDto auditDto = contractAuditBMOImpl.submitAudit(reqJson, storeId);

        Map<String, Object> data = new HashMap<>();
        data.put("auditId", auditDto.getAuditId());
        data.put("auditCode", auditDto.getAuditCode());
        data.put("currentStep", auditDto.getCurrentStep());
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
