package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractAuditConfigDto;
import com.java110.dto.contract.ContractAuditDto;
import com.java110.store.bmo.contract.IContractAuditBMO;
import com.java110.utils.exception.CmdException;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询审核人配置（法务校对 / 领导审核）
 */
@Java110Cmd(serviceCode = "contract.queryContractAuditorConfig")
public class QueryContractAuditorConfigCmd extends Cmd {

    @Autowired
    private IContractAuditBMO contractAuditBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        List<ContractAuditConfigDto> configs = contractAuditBMOImpl.queryAuditorConfigs(storeId);
        List<ContractAuditConfigDto> legalAuditors = new ArrayList<>();
        List<ContractAuditConfigDto> leaderAuditors = new ArrayList<>();
        if (configs != null) {
            for (ContractAuditConfigDto config : configs) {
                if (ContractAuditDto.STEP_LEGAL.equals(config.getStep())) {
                    legalAuditors.add(config);
                    continue;
                }
                if (ContractAuditDto.STEP_LEADER.equals(config.getStep())) {
                    leaderAuditors.add(config);
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("configs", configs == null ? new ArrayList<>() : configs);
        data.put("legalAuditors", legalAuditors);
        data.put("leaderAuditors", leaderAuditors);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
