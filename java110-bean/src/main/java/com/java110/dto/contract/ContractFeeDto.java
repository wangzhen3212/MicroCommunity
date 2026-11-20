package com.java110.dto.contract;

import com.java110.dto.PageDto;

import java.io.Serializable;
import java.util.Date;

/**
 * 合同费用构成数据层封装（费用项快照 + 试算结果，一行 = 合同的一个费用项）
 */
public class ContractFeeDto extends PageDto implements Serializable {

    /**
     * 是否计入合同金额
     */
    public static final String INCLUDE_IN_AMOUNT_YES = "1";
    public static final String INCLUDE_IN_AMOUNT_NO = "0";

    private String cfId;
    private String contractId;
    private String[] contractIds;
    private String feeSeq;
    private String configId;
    private String feeTypeCd;
    private String feeTypeName;
    private String feeName;
    private String computingFormula;
    private String computingFormulaName;
    private String unitPrice;
    private String additionalAmount;
    private String cycle;
    private String months;
    private String coefficient;
    private String manualAmount;
    private String baseAmount;
    private String amount;
    private String includeInAmount;
    private String computeRemark;
    private String remark;
    private String storeId;
    private String communityId;

    private Date createTime;

    private String statusCd = "0";

    public String getCfId() {
        return cfId;
    }

    public void setCfId(String cfId) {
        this.cfId = cfId;
    }

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

    public String getFeeSeq() {
        return feeSeq;
    }

    public void setFeeSeq(String feeSeq) {
        this.feeSeq = feeSeq;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getFeeTypeCd() {
        return feeTypeCd;
    }

    public void setFeeTypeCd(String feeTypeCd) {
        this.feeTypeCd = feeTypeCd;
    }

    public String getFeeTypeName() {
        return feeTypeName;
    }

    public void setFeeTypeName(String feeTypeName) {
        this.feeTypeName = feeTypeName;
    }

    public String getFeeName() {
        return feeName;
    }

    public void setFeeName(String feeName) {
        this.feeName = feeName;
    }

    public String getComputingFormula() {
        return computingFormula;
    }

    public void setComputingFormula(String computingFormula) {
        this.computingFormula = computingFormula;
    }

    public String getComputingFormulaName() {
        return computingFormulaName;
    }

    public void setComputingFormulaName(String computingFormulaName) {
        this.computingFormulaName = computingFormulaName;
    }

    public String getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getAdditionalAmount() {
        return additionalAmount;
    }

    public void setAdditionalAmount(String additionalAmount) {
        this.additionalAmount = additionalAmount;
    }

    public String getCycle() {
        return cycle;
    }

    public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    public String getMonths() {
        return months;
    }

    public void setMonths(String months) {
        this.months = months;
    }

    public String getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(String coefficient) {
        this.coefficient = coefficient;
    }

    public String getManualAmount() {
        return manualAmount;
    }

    public void setManualAmount(String manualAmount) {
        this.manualAmount = manualAmount;
    }

    public String getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(String baseAmount) {
        this.baseAmount = baseAmount;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getIncludeInAmount() {
        return includeInAmount;
    }

    public void setIncludeInAmount(String includeInAmount) {
        this.includeInAmount = includeInAmount;
    }

    public String getComputeRemark() {
        return computeRemark;
    }

    public void setComputeRemark(String computeRemark) {
        this.computeRemark = computeRemark;
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

    public String getCommunityId() {
        return communityId;
    }

    public void setCommunityId(String communityId) {
        this.communityId = communityId;
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
