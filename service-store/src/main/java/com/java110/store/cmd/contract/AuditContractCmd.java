package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.store.bmo.contract.IContractAuditBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 合同审批通过 / 驳回（法务校对、领导审核两级，仅当前环节审核人可操作）
 */
@Java110Cmd(serviceCode = "contract.auditContract")
public class AuditContractCmd extends Cmd {

    @Autowired
    private IContractAuditBMO contractAuditBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "auditId", "请求报文中未包含审批实例ID");
        Assert.hasKeyAndValue(reqJson, "result", "请求报文中未包含审批结果");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String userId = CmdContextUtils.getUserId(context);
        contractAuditBMOImpl.audit(reqJson, storeId, userId);
        context.setResponseEntity(ResultVo.success());
    }
}
