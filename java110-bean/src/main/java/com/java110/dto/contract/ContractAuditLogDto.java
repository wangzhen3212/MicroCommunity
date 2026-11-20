package com.java110.dto.contract;

import com.java110.dto.PageDto;

import java.io.Serializable;
import java.util.Date;

/**
 * 合同审批记录数据层封装（对应 contract_audit_log，一行 = 一次审核动作）
 */
public class ContractAuditLogDto extends PageDto implements Serializable {

    /**
     * 单据类型：1 合同 / 2 变更单
     */
    public static final String BIZ_TYPE_CONTRACT = "1";
    public static final String BIZ_TYPE_CHANGE = "2";

    /**
     * 审批环节：1 法务校对 / 2 领导审批
     */
    public static final String STEP_LEGAL = "1";
    public static final String STEP_LEADER = "2";

    /**
     * 审批结果：1 通过 / 2 驳回
     */
    public static final String RESULT_PASS = "1";
    public static final String RESULT_REJECT = "2";

    private String logId;
    private String bizType;
    private String bizId;
    private String step;
    private String result;
    private String opinion;
    private String auditorId;
    private String auditorName;
    private Date auditTime;
    private String storeId;
    private String statusCd = "0";
    private Date createTime;

    /**
     * 批量查询用：单据ID集合（不落库）
     */
    private String[] bizIds;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
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

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public String getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(String auditorId) {
        this.auditorId = auditorId;
    }

    public String getAuditorName() {
        return auditorName;
    }

    public void setAuditorName(String auditorName) {
        this.auditorName = auditorName;
    }

    public Date getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(Date auditTime) {
        this.auditTime = auditTime;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
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
}
