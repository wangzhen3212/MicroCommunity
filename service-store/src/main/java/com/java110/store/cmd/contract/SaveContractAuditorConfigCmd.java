package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractAuditDto;
import com.java110.intf.store.IContractAuditInnerServiceSMO;
import com.java110.po.contract.ContractAuditConfigPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 保存审核人配置（法务校对 / 领导审核，每个环节可配置多人备选）
 */
@Java110Cmd(serviceCode = "contract.saveContractAuditorConfig")
public class SaveContractAuditorConfigCmd extends Cmd {

    private static final String PREFIX_CONFIG_ID = "96";

    @Autowired
    private IContractAuditInnerServiceSMO contractAuditInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        if (!reqJson.containsKey("legalAuditors") && !reqJson.containsKey("leaderAuditors")) {
            throw new CmdException("请求报文中未包含审核人配置");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);

        JSONArray legalAuditors = reqJson.containsKey("legalAuditors")
                ? reqJson.getJSONArray("legalAuditors") : new JSONArray();
        JSONArray leaderAuditors = reqJson.containsKey("leaderAuditors")
                ? reqJson.getJSONArray("leaderAuditors") : new JSONArray();

        validateAuditors(legalAuditors, ContractAuditDto.STEP_LEGAL_NAME);
        validateAuditors(leaderAuditors, ContractAuditDto.STEP_LEADER_NAME);

        ContractAuditConfigPo deletePo = new ContractAuditConfigPo();
        deletePo.setStoreId(storeId);
        contractAuditInnerServiceSMOImpl.deleteContractAuditConfig(deletePo);

        saveAuditors(legalAuditors, ContractAuditDto.STEP_LEGAL, storeId);
        saveAuditors(leaderAuditors, ContractAuditDto.STEP_LEADER, storeId);

        context.setResponseEntity(ResultVo.success());
    }

    /**
     * 校验审核人配置：至少一人且必须包含员工ID
     */
    private void validateAuditors(JSONArray auditors, String stepName) {
        if (auditors == null || auditors.isEmpty()) {
            throw new CmdException("请配置" + stepName + "审核人");
        }
        for (int i = 0; i < auditors.size(); i++) {
            JSONObject auditor = auditors.getJSONObject(i);
            if (auditor == null || StringUtil.isEmpty(auditor.getString("staffId"))) {
                throw new CmdException(stepName + "审核人不能为空");
            }
        }
    }

    private void saveAuditors(JSONArray auditors, String step, String storeId) {
        for (int i = 0; i < auditors.size(); i++) {
            JSONObject auditor = auditors.getJSONObject(i);
            ContractAuditConfigPo configPo = new ContractAuditConfigPo();
            configPo.setConfigId(GenerateCodeFactory.getGeneratorId(PREFIX_CONFIG_ID));
            configPo.setStep(step);
            configPo.setStaffId(auditor.getString("staffId"));
            configPo.setStaffName(auditor.getString("staffName"));
            configPo.setStaffTel(auditor.getString("staffTel"));
            configPo.setStoreId(storeId);
            configPo.setStatusCd("0");
            int flag = contractAuditInnerServiceSMOImpl.saveContractAuditConfig(configPo);
            if (flag < 1) {
                throw new CmdException("保存审核人配置失败");
            }
        }
    }
}
