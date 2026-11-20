package com.java110.store.bmo.contract.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractAuditLogDto;
import com.java110.dto.contract.ContractChangePlanDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.privilege.HasPrivilegeDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IContractAuditInnerServiceSMO;
import com.java110.intf.store.IContractChangePlanInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.contract.ContractAuditLogPo;
import com.java110.po.contract.ContractChangePlanPo;
import com.java110.po.contract.ContractPo;
import com.java110.store.bmo.contract.IContractAuditBMO;
import com.java110.store.bmo.contract.IUpdateContractBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同两级审批实现（状态机 + 按钮权限 + 审批记录留痕，不接入 Activiti）
 */
@Service("contractAuditBMOImpl")
public class ContractAuditBMOImpl implements IContractAuditBMO {

    /**
     * 按钮权限：合同-法务校对 / 合同-领导审批
     */
    private static final String PRIVILEGE_LEGAL = "502026111600000009";
    private static final String PRIVILEGE_LEADER = "502026111600000010";

    private static final String PREFIX_LOG_ID = "94";

    private static final int OPINION_MAX_LENGTH = 200;

    @Autowired
    private IContractAuditInnerServiceSMO contractAuditInnerServiceSMOImpl;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractChangePlanInnerServiceSMO contractChangePlanInnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IUpdateContractBMO updateContractBMOImpl;

    @Override
    public void audit(JSONObject reqJson, String storeId, String userId) {
        String bizType = StringUtil.isEmpty(reqJson.getString("bizType"))
                ? ContractAuditLogDto.BIZ_TYPE_CONTRACT : reqJson.getString("bizType");
        String bizId = reqJson.getString("bizId");
        String result = reqJson.getString("result");
        String opinion = reqJson.getString("opinion");

        boolean pass = ContractAuditLogDto.RESULT_PASS.equals(result);
        if (!pass && !ContractAuditLogDto.RESULT_REJECT.equals(result)) {
            throw new CmdException("审批结果不正确");
        }
        if (!pass) {
            if (StringUtil.isEmpty(opinion)) {
                throw new CmdException("驳回时必须填写审批意见");
            }
            if (opinion.length() > OPINION_MAX_LENGTH) {
                throw new CmdException("审批意见不能超过" + OPINION_MAX_LENGTH + "字");
            }
        }

        String state = queryBizState(bizType, bizId, storeId);
        if (StringUtil.isEmpty(state)) {
            throw new CmdException("未查询到待审核单据");
        }

        String step;
        String privilegeId;
        if (ContractDto.STATE_AUDIT.equals(state)) {
            step = ContractAuditLogDto.STEP_LEGAL;
            privilegeId = PRIVILEGE_LEGAL;
        } else if (ContractDto.STATE_AUDIT_LEADER.equals(state)) {
            step = ContractAuditLogDto.STEP_LEADER;
            privilegeId = PRIVILEGE_LEADER;
        } else {
            throw new CmdException("该单据不处于待审核状态");
        }

        checkAuditPrivilege(privilegeId, userId, stepNameOf(step));

        saveAuditLog(bizType, bizId, step, result, opinion, userId, storeId);

        if (!pass) {
            updateBizState(bizType, bizId, storeId, ContractDto.STATE_FAIL);
            return;
        }

        if (ContractAuditLogDto.STEP_LEGAL.equals(step)) {
            updateBizState(bizType, bizId, storeId, ContractDto.STATE_AUDIT_LEADER);
            return;
        }

        // 领导审批通过：变更单先回写合同正文，再置为审核完成
        if (ContractAuditLogDto.BIZ_TYPE_CHANGE.equals(bizType)) {
            updateContractBMOImpl.applyChangePlan(bizId, storeId);
        }
        updateBizState(bizType, bizId, storeId, ContractDto.STATE_AUDIT_FINISH);
    }

    @Override
    public Map<String, Object> queryAuditFlow(String bizType, String bizId, String storeId) {
        String state = queryBizState(bizType, bizId, storeId);
        String currentStep = currentStepOf(state);

        ContractAuditLogDto logDto = new ContractAuditLogDto();
        logDto.setBizType(bizType);
        logDto.setBizId(bizId);
        logDto.setStoreId(storeId);
        logDto.setStatusCd("0");
        logDto.setPage(-1);
        List<ContractAuditLogDto> logs = contractAuditInnerServiceSMOImpl.queryContractAuditLogs(logDto);
        List<Map<String, Object>> records = new ArrayList<>();
        if (logs != null) {
            for (ContractAuditLogDto log : logs) {
                records.add(wrapAuditLog(log));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("state", state);
        data.put("stateName", stateNameOf(state));
        data.put("currentStep", currentStep);
        data.put("currentStepName", stepNameOf(currentStep));
        data.put("records", records);
        return data;
    }

    @Override
    public Map<String, Map<String, Object>> queryLatestAuditLogs(String bizType, String[] bizIds, String storeId) {
        Map<String, Map<String, Object>> latestLogs = new HashMap<>();
        if (bizIds == null || bizIds.length < 1) {
            return latestLogs;
        }
        ContractAuditLogDto logDto = new ContractAuditLogDto();
        logDto.setBizType(bizType);
        logDto.setBizIds(bizIds);
        logDto.setStoreId(storeId);
        logDto.setStatusCd("0");
        logDto.setPage(-1);
        List<ContractAuditLogDto> logs = contractAuditInnerServiceSMOImpl.queryContractAuditLogs(logDto);
        if (logs == null) {
            return latestLogs;
        }
        for (ContractAuditLogDto log : logs) {
            latestLogs.put(log.getBizId(), wrapAuditLog(log));
        }
        return latestLogs;
    }

    /**
     * 审批记录输出结构：环节、结果含中文名，前端直接展示
     */
    private Map<String, Object> wrapAuditLog(ContractAuditLogDto log) {
        Map<String, Object> record = new HashMap<>();
        record.put("logId", log.getLogId());
        record.put("bizType", log.getBizType());
        record.put("bizId", log.getBizId());
        record.put("step", log.getStep());
        record.put("stepName", stepNameOf(log.getStep()));
        record.put("result", log.getResult());
        record.put("resultName", resultNameOf(log.getResult()));
        record.put("opinion", log.getOpinion());
        record.put("auditorId", log.getAuditorId());
        record.put("auditorName", log.getAuditorName());
        record.put("auditTime", log.getAuditTime() == null ? "" : DateUtil.getFormatTimeString(log.getAuditTime(), DateUtil.DATE_FORMATE_STRING_A));
        return record;
    }

    /**
     * 查询单据当前状态（合同 / 变更单），不存在返回空串
     */
    private String queryBizState(String bizType, String bizId, String storeId) {
        if (StringUtil.isEmpty(bizId)) {
            return "";
        }
        if (ContractAuditLogDto.BIZ_TYPE_CHANGE.equals(bizType)) {
            ContractChangePlanDto planDto = new ContractChangePlanDto();
            planDto.setPlanId(bizId);
            planDto.setStoreId(storeId);
            planDto.setStatusCd("0");
            List<ContractChangePlanDto> planDtos = contractChangePlanInnerServiceSMOImpl.queryContractChangePlans(planDto);
            if (planDtos == null || planDtos.isEmpty()) {
                return "";
            }
            return planDtos.get(0).getState();
        }
        ContractDto contractDto = new ContractDto();
        contractDto.setContractId(bizId);
        contractDto.setStoreId(storeId);
        contractDto.setStatusCd("0");
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
        if (contractDtos == null || contractDtos.isEmpty()) {
            return "";
        }
        return contractDtos.get(0).getState();
    }

    /**
     * 回写单据状态
     */
    private void updateBizState(String bizType, String bizId, String storeId, String state) {
        if (ContractAuditLogDto.BIZ_TYPE_CHANGE.equals(bizType)) {
            ContractChangePlanPo changePlanPo = new ContractChangePlanPo();
            changePlanPo.setPlanId(bizId);
            changePlanPo.setState(state);
            changePlanPo.setStatusCd("0");
            contractChangePlanInnerServiceSMOImpl.updateContractChangePlan(changePlanPo);
            return;
        }
        ContractPo contractPo = new ContractPo();
        contractPo.setContractId(bizId);
        contractPo.setState(state);
        contractPo.setStatusCd("0");
        contractInnerServiceSMOImpl.updateContract(contractPo);
    }

    /**
     * 审批记录留痕：环节、结果、意见、审核人、审核时间
     */
    private void saveAuditLog(String bizType, String bizId, String step, String result, String opinion,
                              String userId, String storeId) {
        ContractAuditLogPo logPo = new ContractAuditLogPo();
        logPo.setLogId(GenerateCodeFactory.getGeneratorId(PREFIX_LOG_ID));
        logPo.setBizType(bizType);
        logPo.setBizId(bizId);
        logPo.setStep(step);
        logPo.setResult(result);
        logPo.setOpinion(opinion);
        logPo.setAuditorId(userId);
        logPo.setAuditorName(queryUserName(userId));
        logPo.setAuditTime(DateUtil.getCurrentDate());
        logPo.setStoreId(storeId);
        logPo.setStatusCd("0");
        int flag = contractAuditInnerServiceSMOImpl.saveContractAuditLog(logPo);
        if (flag < 1) {
            throw new CmdException("保存审批记录失败");
        }
    }

    /**
     * 按钮权限校验：有权限的人才能审核
     */
    private void checkAuditPrivilege(String privilegeId, String userId, String stepName) {
        if (StringUtil.isEmpty(userId)) {
            throw new CmdException("未获取到登录用户信息，无法审核");
        }
        HasPrivilegeDto hasPrivilegeDto = new HasPrivilegeDto();
        hasPrivilegeDto.setpId(privilegeId);
        hasPrivilegeDto.setUserId(userId);
        List<HasPrivilegeDto> privilegeDtos = menuInnerServiceSMOImpl.hasPrivilege(hasPrivilegeDto);
        if (privilegeDtos == null || privilegeDtos.isEmpty()) {
            throw new CmdException("您没有【" + stepName + "】审核权限");
        }
    }

    private String queryUserName(String userId) {
        if (StringUtil.isEmpty(userId)) {
            return "";
        }
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        if (userDtos == null || userDtos.isEmpty()) {
            return "";
        }
        return userDtos.get(0).getName();
    }

    /**
     * 当前审批环节：11 法务校对 / 56 领导审批
     */
    private String currentStepOf(String state) {
        if (ContractDto.STATE_AUDIT.equals(state)) {
            return ContractAuditLogDto.STEP_LEGAL;
        }
        if (ContractDto.STATE_AUDIT_LEADER.equals(state)) {
            return ContractAuditLogDto.STEP_LEADER;
        }
        return "";
    }

    private String stepNameOf(String step) {
        if (ContractAuditLogDto.STEP_LEGAL.equals(step)) {
            return "法务校对";
        }
        if (ContractAuditLogDto.STEP_LEADER.equals(step)) {
            return "领导审批";
        }
        return "";
    }

    private String resultNameOf(String result) {
        if (ContractAuditLogDto.RESULT_PASS.equals(result)) {
            return "通过";
        }
        if (ContractAuditLogDto.RESULT_REJECT.equals(result)) {
            return "驳回";
        }
        return "";
    }

    private String stateNameOf(String state) {
        if (ContractDto.STATE_DRAFT.equals(state)) {
            return "草稿";
        }
        if (ContractDto.STATE_AUDIT.equals(state)) {
            return "法务校对";
        }
        if (ContractDto.STATE_AUDIT_LEADER.equals(state)) {
            return "领导审批";
        }
        if (ContractDto.STATE_AUDIT_FINISH.equals(state)) {
            return "已生效";
        }
        if (ContractDto.STATE_FAIL.equals(state)) {
            return "已驳回";
        }
        if (ContractDto.STATE_COMPLAINT.equals(state)) {
            return "已终止";
        }
        return "";
    }
}
