package com.java110.store.bmo.contract.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractAuditConfigDto;
import com.java110.dto.contract.ContractAuditDto;
import com.java110.dto.contract.ContractAuditRecordDto;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractAuditInnerServiceSMO;
import com.java110.intf.store.IContractChangePlanInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.po.contract.ContractAuditPo;
import com.java110.po.contract.ContractAuditRecordPo;
import com.java110.po.contract.ContractChangePlanPo;
import com.java110.po.contract.ContractPo;
import com.java110.store.bmo.contract.IContractAuditBMO;
import com.java110.store.bmo.contract.IUpdateContractBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同两级审批实现（法务校对 -> 领导审核，不接入 Activiti）
 */
@Service("contractAuditBMOImpl")
public class ContractAuditBMOImpl implements IContractAuditBMO {

    private static Logger logger = LoggerFactory.getLogger(ContractAuditBMOImpl.class);

    private static final String PREFIX_AUDIT_ID = "92";
    private static final String PREFIX_RECORD_ID = "93";

    @Autowired
    private IContractAuditInnerServiceSMO contractAuditInnerServiceSMOImpl;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractChangePlanInnerServiceSMO contractChangePlanInnerServiceSMOImpl;

    @Autowired
    private IUpdateContractBMO updateContractBMOImpl;

    @Override
    public ContractAuditDto submitAudit(JSONObject reqJson, String storeId) {
        String auditType = reqJson.getString("auditType");
        String bizId = reqJson.getString("bizId");
        Assert.hasLength(auditType, "请求报文中未包含审批类型");
        Assert.hasLength(bizId, "请求报文中未包含业务ID");

        if (queryDoingAudit(auditType, bizId) != null) {
            throw new CmdException("该单据正在审批中，请勿重复提交");
        }

        List<ContractAuditConfigDto> configs = queryAuditorConfigs(storeId);
        ContractAuditConfigDto legalConfig = pickAuditor(configs, ContractAuditDto.STEP_LEGAL, reqJson.getString("legalAuditorId"));
        ContractAuditConfigDto leaderConfig = pickAuditor(configs, ContractAuditDto.STEP_LEADER, reqJson.getString("leaderAuditorId"));
        if (legalConfig == null) {
            throw new CmdException("未配置法务校对审核人，请先在审核人设置中配置");
        }
        if (leaderConfig == null) {
            throw new CmdException("未配置领导审核审核人，请先在审核人设置中配置");
        }

        ContractAuditPo auditPo = new ContractAuditPo();
        auditPo.setAuditId(GenerateCodeFactory.getGeneratorId(PREFIX_AUDIT_ID));
        auditPo.setAuditCode(generateAuditCode());
        auditPo.setAuditType(auditType);
        auditPo.setBizId(bizId);
        auditPo.setCurrentStep(ContractAuditDto.STEP_LEGAL);
        auditPo.setState(ContractAuditDto.STATE_DOING);
        auditPo.setSubmitStaffId(reqJson.getString("submitStaffId"));
        auditPo.setSubmitStaffName(reqJson.getString("submitStaffName"));
        auditPo.setSubmitTime(DateUtil.getCurrentDate());
        auditPo.setStoreId(storeId);
        int flag = contractAuditInnerServiceSMOImpl.saveContractAudit(auditPo);
        if (flag < 1) {
            throw new CmdException("提交审批失败");
        }

        saveRecord(auditPo.getAuditId(), ContractAuditDto.STEP_LEGAL, ContractAuditDto.STEP_LEGAL_NAME, legalConfig, "1");
        saveRecord(auditPo.getAuditId(), ContractAuditDto.STEP_LEADER, ContractAuditDto.STEP_LEADER_NAME, leaderConfig, "2");

        updateBizState(auditType, bizId, storeId, ContractDto.STATE_AUDIT);

        ContractAuditDto auditDto = new ContractAuditDto();
        auditDto.setAuditId(auditPo.getAuditId());
        auditDto.setAuditCode(auditPo.getAuditCode());
        auditDto.setAuditType(auditType);
        auditDto.setBizId(bizId);
        auditDto.setCurrentStep(ContractAuditDto.STEP_LEGAL);
        auditDto.setState(ContractAuditDto.STATE_DOING);
        return auditDto;
    }

    @Override
    public void audit(JSONObject reqJson, String storeId, String userId) {
        String auditId = reqJson.getString("auditId");
        String result = reqJson.getString("result");
        String opinion = reqJson.getString("opinion");
        boolean pass = ContractAuditRecordDto.RESULT_PASS.equals(result);
        if (!pass && !ContractAuditRecordDto.RESULT_REJECT.equals(result)) {
            throw new CmdException("审批结果不正确");
        }
        if (!pass) {
            if (StringUtil.isEmpty(opinion)) {
                throw new CmdException("驳回时必须填写审批意见");
            }
            if (opinion.length() > 200) {
                throw new CmdException("审批意见不能超过200字");
            }
        }

        ContractAuditDto auditDto = new ContractAuditDto();
        auditDto.setAuditId(auditId);
        auditDto.setStoreId(storeId);
        List<ContractAuditDto> auditDtos = contractAuditInnerServiceSMOImpl.queryContractAudits(auditDto);
        Assert.listOnlyOne(auditDtos, "未查询到审批实例");
        ContractAuditDto audit = auditDtos.get(0);
        if (!ContractAuditDto.STATE_DOING.equals(audit.getState())) {
            throw new CmdException("该审批已结束，无法再次审批");
        }

        ContractAuditRecordDto recordDto = new ContractAuditRecordDto();
        recordDto.setAuditId(auditId);
        recordDto.setStep(audit.getCurrentStep());
        recordDto.setStatusCd("0");
        List<ContractAuditRecordDto> recordDtos = contractAuditInnerServiceSMOImpl.queryContractAuditRecords(recordDto);
        Assert.listOnlyOne(recordDtos, "未查询到当前环节审批记录");
        ContractAuditRecordDto record = recordDtos.get(0);
        if (!ContractAuditRecordDto.RESULT_DOING.equals(record.getResult())) {
            throw new CmdException("当前环节已处理");
        }
        if (StringUtil.isEmpty(userId) || !userId.equals(record.getAuditorId())) {
            throw new CmdException("您不是当前环节的审核人，无权审批");
        }

        ContractAuditRecordPo recordPo = new ContractAuditRecordPo();
        recordPo.setRecordId(record.getRecordId());
        recordPo.setResult(result);
        recordPo.setOpinion(opinion);
        recordPo.setAuditTime(DateUtil.getCurrentDate());
        recordPo.setStatusCd("0");
        contractAuditInnerServiceSMOImpl.updateContractAuditRecord(recordPo);

        if (!pass) {
            finishAudit(audit, ContractAuditDto.STATE_REJECT);
            updateBizState(audit.getAuditType(), audit.getBizId(), storeId, ContractDto.STATE_FAIL);
            return;
        }

        if (ContractAuditDto.STEP_LEGAL.equals(audit.getCurrentStep())) {
            ContractAuditPo updatePo = new ContractAuditPo();
            updatePo.setAuditId(audit.getAuditId());
            updatePo.setCurrentStep(ContractAuditDto.STEP_LEADER);
            updatePo.setStatusCd("0");
            contractAuditInnerServiceSMOImpl.updateContractAudit(updatePo);
            return;
        }

        finishAudit(audit, ContractAuditDto.STATE_PASS);
        // 变更单：两级审批全部通过后回写合同并留痕
        if (ContractAuditDto.AUDIT_TYPE_CHANGE.equals(audit.getAuditType())) {
            updateContractBMOImpl.applyChangePlan(audit.getBizId(), storeId);
        }
        updateBizState(audit.getAuditType(), audit.getBizId(), storeId, ContractDto.STATE_AUDIT_FINISH);
    }

    @Override
    public void cancelAudit(String auditId, String storeId, String userId) {
        ContractAuditDto auditDto = new ContractAuditDto();
        auditDto.setAuditId(auditId);
        auditDto.setStoreId(storeId);
        List<ContractAuditDto> auditDtos = contractAuditInnerServiceSMOImpl.queryContractAudits(auditDto);
        Assert.listOnlyOne(auditDtos, "未查询到审批实例");
        ContractAuditDto audit = auditDtos.get(0);
        if (!ContractAuditDto.STATE_DOING.equals(audit.getState())) {
            throw new CmdException("该审批已结束，无法撤回");
        }
        if (!ContractAuditDto.STEP_LEGAL.equals(audit.getCurrentStep())) {
            throw new CmdException("法务校对已处理，无法撤回");
        }
        if (StringUtil.isEmpty(userId) || !userId.equals(audit.getSubmitStaffId())) {
            throw new CmdException("只有提交人可以撤回审批");
        }

        ContractAuditRecordDto recordDto = new ContractAuditRecordDto();
        recordDto.setAuditId(auditId);
        recordDto.setStep(ContractAuditDto.STEP_LEGAL);
        recordDto.setStatusCd("0");
        List<ContractAuditRecordDto> recordDtos = contractAuditInnerServiceSMOImpl.queryContractAuditRecords(recordDto);
        if (recordDtos != null) {
            for (ContractAuditRecordDto record : recordDtos) {
                if (!ContractAuditRecordDto.RESULT_DOING.equals(record.getResult())) {
                    throw new CmdException("法务校对已处理，无法撤回");
                }
            }
        }

        finishAudit(audit, ContractAuditDto.STATE_CANCEL);
        updateBizState(audit.getAuditType(), audit.getBizId(), storeId, ContractDto.STATE_DRAFT);
    }

    @Override
    public List<ContractAuditConfigDto> queryAuditorConfigs(String storeId) {
        ContractAuditConfigDto configDto = new ContractAuditConfigDto();
        configDto.setStoreId(storeId);
        configDto.setStatusCd("0");
        return contractAuditInnerServiceSMOImpl.queryContractAuditConfigs(configDto);
    }

    @Override
    public ContractAuditDto queryDoingAudit(String auditType, String bizId) {
        ContractAuditDto auditDto = new ContractAuditDto();
        auditDto.setAuditType(auditType);
        auditDto.setBizId(bizId);
        auditDto.setState(ContractAuditDto.STATE_DOING);
        auditDto.setStatusCd("0");
        List<ContractAuditDto> auditDtos = contractAuditInnerServiceSMOImpl.queryContractAudits(auditDto);
        if (auditDtos == null || auditDtos.isEmpty()) {
            return null;
        }
        return auditDtos.get(0);
    }

    /**
     * 结束审批实例
     */
    private void finishAudit(ContractAuditDto audit, String state) {
        ContractAuditPo auditPo = new ContractAuditPo();
        auditPo.setAuditId(audit.getAuditId());
        auditPo.setState(state);
        auditPo.setFinishTime(DateUtil.getCurrentDate());
        auditPo.setStatusCd("0");
        contractAuditInnerServiceSMOImpl.updateContractAudit(auditPo);
    }

    /**
     * 回写单据状态（合同 state / 变更 state）
     */
    private void updateBizState(String auditType, String bizId, String storeId, String state) {
        if (ContractAuditDto.AUDIT_TYPE_CHANGE.equals(auditType)) {
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

    private void saveRecord(String auditId, String step, String stepName, ContractAuditConfigDto config, String seq) {
        ContractAuditRecordPo recordPo = new ContractAuditRecordPo();
        recordPo.setRecordId(GenerateCodeFactory.getGeneratorId(PREFIX_RECORD_ID));
        recordPo.setAuditId(auditId);
        recordPo.setStep(step);
        recordPo.setStepName(stepName);
        recordPo.setAuditorId(config.getStaffId());
        recordPo.setAuditorName(config.getStaffName());
        recordPo.setAuditorTel(config.getStaffTel());
        recordPo.setResult(ContractAuditRecordDto.RESULT_DOING);
        recordPo.setSeq(seq);
        recordPo.setStatusCd("0");
        int flag = contractAuditInnerServiceSMOImpl.saveContractAuditRecord(recordPo);
        if (flag < 1) {
            throw new CmdException("保存审批环节记录失败");
        }
    }

    /**
     * 选择环节审核人（优先使用提交人确认的审核人，否则取配置中的第一位）
     */
    private ContractAuditConfigDto pickAuditor(List<ContractAuditConfigDto> configs, String step, String staffId) {
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        ContractAuditConfigDto defaultConfig = null;
        for (ContractAuditConfigDto config : configs) {
            if (!step.equals(config.getStep())) {
                continue;
            }
            if (!StringUtil.isEmpty(staffId) && staffId.equals(config.getStaffId())) {
                return config;
            }
            if (defaultConfig == null) {
                defaultConfig = config;
            }
        }
        return defaultConfig;
    }

    /**
     * 生成审批单号 SP-{yyyy}-{4位序号}
     */
    private String generateAuditCode() {
        String year = DateUtil.getNow("yyyy");
        ContractAuditDto countDto = new ContractAuditDto();
        countDto.setAuditCodeLike("SP-" + year + "-");
        countDto.setStatusCd("");
        int count = contractAuditInnerServiceSMOImpl.queryContractAuditsCount(countDto);
        return "SP-" + year + "-" + String.format("%04d", count + 1);
    }

    /**
     * 查询审批环节记录（供 CMD 使用）
     */
    public List<ContractAuditRecordDto> queryRecords(String[] auditIds) {
        if (auditIds == null || auditIds.length < 1) {
            return new ArrayList<>();
        }
        ContractAuditRecordDto recordDto = new ContractAuditRecordDto();
        recordDto.setAuditIds(auditIds);
        recordDto.setStatusCd("0");
        return contractAuditInnerServiceSMOImpl.queryContractAuditRecords(recordDto);
    }

    @Override
    public List<Map> wrapAuditRecords(List<ContractAuditDto> auditDtos) {
        List<Map> result = new ArrayList<>();
        if (auditDtos == null || auditDtos.isEmpty()) {
            return result;
        }
        String[] auditIds = new String[auditDtos.size()];
        for (int i = 0; i < auditDtos.size(); i++) {
            auditIds[i] = auditDtos.get(i).getAuditId();
        }
        List<ContractAuditRecordDto> recordDtos = queryRecords(auditIds);
        for (ContractAuditDto auditDto : auditDtos) {
            Map<String, Object> item = new HashMap<>();
            item.put("audit", auditDto);
            List<ContractAuditRecordDto> records = new ArrayList<>();
            for (ContractAuditRecordDto record : recordDtos) {
                if (auditDto.getAuditId().equals(record.getAuditId())) {
                    records.add(record);
                }
            }
            item.put("records", records);
            result.add(item);
        }
        return result;
    }
}
