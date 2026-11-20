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
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 合同审批通过 / 驳回（法务校对、领导审批两级状态流转 + 审批记录留痕）
 */
@Java110Cmd(serviceCode = "contract.auditContract")
public class AuditContractCmd extends Cmd {

    @Autowired
    private IContractAuditBMO contractAuditBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "bizId", "请求报文中未包含业务ID");
        Assert.hasKeyAndValue(reqJson, "result", "请求报文中未包含审批结果");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        if (StringUtil.isEmpty(reqJson.getString("bizType"))) {
            reqJson.put("bizType", "1");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String userId = CmdContextUtils.getUserId(context);
        contractAuditBMOImpl.audit(reqJson, storeId, reqJson.getString("communityId"), userId);
        context.setResponseEntity(ResultVo.success());
    }
}
