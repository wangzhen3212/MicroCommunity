package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractAuditDto;
import com.java110.intf.store.IContractAuditInnerServiceSMO;
import com.java110.store.bmo.contract.IContractAuditBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询单据审批链（详情抽屉 / 工作台右栏 / 审核中心共用，返回最新审批实例与两级环节记录）
 */
@Java110Cmd(serviceCode = "contract.queryContractAuditFlow")
public class QueryContractAuditFlowCmd extends Cmd {

    @Autowired
    private IContractAuditInnerServiceSMO contractAuditInnerServiceSMOImpl;

    @Autowired
    private IContractAuditBMO contractAuditBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "bizId", "请求报文中未包含业务ID");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String auditType = StringUtil.isEmpty(reqJson.getString("auditType"))
                ? ContractAuditDto.AUDIT_TYPE_CONTRACT : reqJson.getString("auditType");

        ContractAuditDto auditDto = new ContractAuditDto();
        auditDto.setBizId(reqJson.getString("bizId"));
        auditDto.setAuditType(auditType);
        auditDto.setStoreId(storeId);
        auditDto.setStatusCd("0");
        auditDto.setPage(-1);
        List<ContractAuditDto> auditDtos = contractAuditInnerServiceSMOImpl.queryContractAudits(auditDto);

        Map<String, Object> data = new HashMap<>();
        if (auditDtos == null || auditDtos.isEmpty()) {
            data.put("audit", null);
            data.put("records", new ArrayList<>());
            context.setResponseEntity(ResultVo.createResponseEntity(data));
            return;
        }
        List<Map> wrapped = contractAuditBMOImpl.wrapAuditRecords(auditDtos.subList(0, 1));
        data.put("audit", wrapped.get(0).get("audit"));
        data.put("records", wrapped.get(0).get("records"));
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
