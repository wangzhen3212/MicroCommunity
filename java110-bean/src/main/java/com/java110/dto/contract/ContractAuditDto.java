package com.java110.dto.contract;

import com.java110.dto.PageDto;

import java.io.Serializable;
import java.util.Date;

/**
 * 合同两级审批实例数据层封装（替代 Activiti 流程实例）
 */
public class ContractAuditDto extends PageDto implements Serializable {

    /**
     * 单据类型
     */
    public static final String AUDIT_TYPE_CONTRACT = "1";
    public static final String AUDIT_TYPE_CHANGE = "2";

    /**
     * 实例状态
     */
    public static final String STATE_DOING = "0";
    public static final String STATE_PASS = "1";
    public static final String STATE_REJECT = "2";
    public static final String STATE_CANCEL = "3";

    /**
     * 环节
     */
    public static final String STEP_LEGAL = "1";
    public static final String STEP_LEADER = "2";
    public static final String STEP_LEGAL_NAME = "法务校对";
    public static final String STEP_LEADER_NAME = "领导审核";

    private String auditId;
    private String auditCode;
    private String auditCodeLike;
    private String auditType;
    private String bizId;
    private String[] bizIds;
    private String currentStep;
    private String state;
    private String[] states;
    private String stateName;
    private String submitStaffId;
    private String submitStaffName;
    private Date submitTime;
    private Date finishTime;
    private String storeId;

    /**
     * 查询辅助字段：当前待办审核人、当前环节待办结果
     */
    private String auditorId;
    private String result;
    private String step;

    /**
     * 查询辅助字段：关键字（合同编号/名称/审批单号）
     */
    private String searchText;

    private String statusCd = "0";
    private Date createTime;

    private int recordCount;
    private String bizCode;
    private String bizName;
    private String bizAmount;
    private String bizPartyB;
    private String bizState;
    private String bizStateName;
    private String bizContractType;

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getAuditCode() {
        return auditCode;
    }

    public void setAuditCode(String auditCode) {
        this.auditCode = auditCode;
    }

    public String getAuditCodeLike() {
        return auditCodeLike;
    }

    public void setAuditCodeLike(String auditCodeLike) {
        this.auditCodeLike = auditCodeLike;
    }

    public String getAuditType() {
        return auditType;
    }

    public void setAuditType(String auditType) {
        this.auditType = auditType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String[] getBizIds() {
        return bizIds;
    }

    public void setBizIds(String[] bizIds) {
        this.bizIds = bizIds;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String[] getStates() {
        return states;
    }

    public void setStates(String[] states) {
        this.states = states;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getSubmitStaffId() {
        return submitStaffId;
    }

    public void setSubmitStaffId(String submitStaffId) {
        this.submitStaffId = submitStaffId;
    }

    public String getSubmitStaffName() {
        return submitStaffName;
    }

    public void setSubmitStaffName(String submitStaffName) {
        this.submitStaffName = submitStaffName;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(String auditorId) {
        this.auditorId = auditorId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getStatusCd() {
        return statusCd;
    }

    public void setStatusCd(String statusCd) {
        this.statusCd = statusCd;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public String getBizName() {
        return bizName;
    }

    public void setBizName(String bizName) {
        this.bizName = bizName;
    }

    public String getBizAmount() {
        return bizAmount;
    }

    public void setBizAmount(String bizAmount) {
        this.bizAmount = bizAmount;
    }

    public String getBizPartyB() {
        return bizPartyB;
    }

    public void setBizPartyB(String bizPartyB) {
        this.bizPartyB = bizPartyB;
    }

    public String getBizState() {
        return bizState;
    }

    public void setBizState(String bizState) {
        this.bizState = bizState;
    }

    public String getBizStateName() {
        return bizStateName;
    }

    public void setBizStateName(String bizStateName) {
        this.bizStateName = bizStateName;
    }

    public String getBizContractType() {
        return bizContractType;
    }

    public void setBizContractType(String bizContractType) {
        this.bizContractType = bizContractType;
    }
}
