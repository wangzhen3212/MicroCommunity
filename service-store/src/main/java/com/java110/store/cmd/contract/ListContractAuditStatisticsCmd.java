package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IContractAuditInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同审核汇总卡：待我审批 / 本月已通过 / 本月驳回
 */
@Java110Cmd(serviceCode = "contract.listContractAuditStatistics")
public class ListContractAuditStatisticsCmd extends Cmd {

    @Autowired
    private IContractAuditInnerServiceSMO contractAuditInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String userId = CmdContextUtils.getUserId(context);

        Map<String, Object> info = new HashMap<>();
        info.put("storeId", storeId);
        info.put("staffId", userId);
        List<Map> statistics = contractAuditInnerServiceSMOImpl.queryContractAuditStatistics(info);
        Map<String, Object> data = new HashMap<>();
        if (statistics != null && !statistics.isEmpty()) {
            data = statistics.get(0);
        }
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
