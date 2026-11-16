package com.java110.dto.contract;

import com.java110.dto.PageDto;

import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName FloorDto
 * @Description 合同收款计划（履约节点）数据层封装
 **/
public class ContractCollectionPlanDto extends PageDto implements Serializable {

    /**
     * 付款状态 0 未到期 1 已付 2 逾期
     */
    public static final String PAY_STATE_DOING = "0";
    public static final String PAY_STATE_PAID = "1";
    public static final String PAY_STATE_OVERDUE = "2";

    private String contractId;
    private String[] contractIds;
    private String feeName;
    private String planName;
    private String planId;
    private String remark;
    private String storeId;
    private String feeId;
    private String planSeq;
    private String planTime;
    private String planAmount;
    private String ratio;
    private String payState;
    private String[] payStates;
    private String payTime;
    private String triggerDesc;
    private String feeType;
    private String planTimeBegin;
    private String planTimeEnd;

    private Date createTime;

    private String statusCd = "0";


    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String[] getContractIds() {
        return contractIds;
    }

    public void setContractIds(String[] contractIds) {
        this.contractIds = contractIds;
    }

    public String getFeeName() {
        return feeName;
    }

    public void setFeeName(String feeName) {
        this.feeName = feeName;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getFeeId() {
        return feeId;
    }

    public void setFeeId(String feeId) {
        this.feeId = feeId;
    }

    public String getPlanSeq() {
        return planSeq;
    }

    public void setPlanSeq(String planSeq) {
        this.planSeq = planSeq;
    }

    public String getPlanTime() {
        return planTime;
    }

    public void setPlanTime(String planTime) {
        this.planTime = planTime;
    }

    public String getPlanAmount() {
        return planAmount;
    }

    public void setPlanAmount(String planAmount) {
        this.planAmount = planAmount;
    }

    public String getRatio() {
        return ratio;
    }

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }

    public String getPayState() {
        return payState;
    }

    public void setPayState(String payState) {
        this.payState = payState;
    }

    public String[] getPayStates() {
        return payStates;
    }

    public void setPayStates(String[] payStates) {
        this.payStates = payStates;
    }

    public String getPayTime() {
        return payTime;
    }

    public void setPayTime(String payTime) {
        this.payTime = payTime;
    }

    public String getTriggerDesc() {
        return triggerDesc;
    }

    public void setTriggerDesc(String triggerDesc) {
        this.triggerDesc = triggerDesc;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public String getPlanTimeBegin() {
        return planTimeBegin;
    }

    public void setPlanTimeBegin(String planTimeBegin) {
        this.planTimeBegin = planTimeBegin;
    }

    public String getPlanTimeEnd() {
        return planTimeEnd;
    }

    public void setPlanTimeEnd(String planTimeEnd) {
        this.planTimeEnd = planTimeEnd;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getStatusCd() {
        return statusCd;
    }

    public void setStatusCd(String statusCd) {
        this.statusCd = statusCd;
    }
}
