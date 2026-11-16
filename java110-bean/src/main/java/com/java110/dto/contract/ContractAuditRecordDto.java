package com.java110.dto.contract;

import com.java110.dto.PageDto;

import java.io.Serializable;
import java.util.Date;

/**
 * 合同审批环节记录数据层封装
 */
public class ContractAuditRecordDto extends PageDto implements Serializable {

    /**
     * 环节结果
     */
    public static final String RESULT_DOING = "0";
    public static final String RESULT_PASS = "1";
    public static final String RESULT_REJECT = "2";

    private String recordId;
    private String auditId;
    private String[] auditIds;
    private String step;
    private String stepName;
    private String auditorId;
    private String auditorName;
    private String auditorTel;
    private String result;
    private String opinion;
    private Date auditTime;
    private String seq;
    private String statusCd = "0";
    private Date createTime;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String[] getAuditIds() {
        return auditIds;
    }

    public void setAuditIds(String[] auditIds) {
        this.auditIds = auditIds;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
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

    public String getAuditorTel() {
        return auditorTel;
    }

    public void setAuditorTel(String auditorTel) {
        this.auditorTel = auditorTel;
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

    public Date getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(Date auditTime) {
        this.auditTime = auditTime;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
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
