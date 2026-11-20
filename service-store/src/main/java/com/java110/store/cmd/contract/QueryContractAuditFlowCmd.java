package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractAuditLogDto;
import com.java110.store.bmo.contract.IContractAuditBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 查询单据审批情况（合同状态 + 审批记录时间线，详情抽屉 / 工作台 / 待审核页共用）
 */
@Java110Cmd(serviceCode = "contract.queryContractAuditFlow")
public class QueryContractAuditFlowCmd extends Cmd {

    @Autowired
    private IContractAuditBMO contractAuditBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "bizId", "请求报文中未包含业务ID");
        if (StringUtil.isEmpty(reqJson.getString("bizType"))) {
            reqJson.put("bizType", ContractAuditLogDto.BIZ_TYPE_CONTRACT);
        }
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        Map<String, Object> data = contractAuditBMOImpl.queryAuditFlow(reqJson.getString("bizType"),
                reqJson.getString("bizId"), storeId);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
