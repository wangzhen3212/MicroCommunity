package com.java110.po.contract;

import java.io.Serializable;
import java.util.Date;

/**
 * 合同审批实例持久化对象
 */
public class ContractAuditPo implements Serializable {

    private String auditId;
    private String auditCode;
    private String auditType;
    private String bizId;
    private String currentStep;
    private String state;
    private String submitStaffId;
    private String submitStaffName;
    private Date submitTime;
    private Date finishTime;
    private String storeId;
    private String statusCd = "0";

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

    public String getStatusCd() {
        return statusCd;
    }

    public void setStatusCd(String statusCd) {
        this.statusCd = statusCd;
    }
}
