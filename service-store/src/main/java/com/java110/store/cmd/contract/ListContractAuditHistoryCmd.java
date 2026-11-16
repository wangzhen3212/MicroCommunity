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
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 已通过 / 已驳回 / 我发起 的审批列表（带两级审批意见）
 */
@Java110Cmd(serviceCode = "contract.listContractAuditHistory")
public class ListContractAuditHistoryCmd extends Cmd {

    @Autowired
    private IContractAuditInnerServiceSMO contractAuditInnerServiceSMOImpl;

    @Autowired
    private IContractAuditBMO contractAuditBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String userId = CmdContextUtils.getUserId(context);

        ContractAuditDto auditDto = BeanConvertUtil.covertBean(reqJson, ContractAuditDto.class);
        auditDto.setStoreId(storeId);
        auditDto.setStatusCd("0");
        // viewType=mine 查询我发起的，默认查询我审批过的
        if ("mine".equals(reqJson.getString("viewType"))) {
            auditDto.setSubmitStaffId(userId);
        } else {
            auditDto.setAuditorId(userId);
        }
        if (StringUtil.isEmpty(auditDto.getState())) {
            auditDto.setStates(new String[]{ContractAuditDto.STATE_PASS, ContractAuditDto.STATE_REJECT});
        }

        int count = contractAuditInnerServiceSMOImpl.queryContractAuditsCount(auditDto);
        List<ContractAuditDto> auditDtos = new ArrayList<>();
        if (count > 0) {
            auditDtos = contractAuditInnerServiceSMOImpl.queryContractAudits(auditDto);
        }
        List<Map> result = contractAuditBMOImpl.wrapAuditRecords(auditDtos);

        context.setResponseEntity(ResultVo.createResponseEntity(
                (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, result));
    }
}
