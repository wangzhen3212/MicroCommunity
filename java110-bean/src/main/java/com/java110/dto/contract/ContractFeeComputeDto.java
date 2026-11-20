package com.java110.dto.contract;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 合同费用试算返回体
 */
public class ContractFeeComputeDto implements Serializable {

    private String payerObjType;
    private String feeName;
    private String feeTypeCd;
    private String computingFormula;
    private String computingFormulaName;
    private String paymentCycle;
    private int months;
    private int cycle;
    private String unitPrice;
    private String additionalAmount;
    private String baseAmount;
    private String amount;
    private String remark;
    private List<ContractFeeComputeDetail> details = new ArrayList<>();

    public String getPayerObjType() {
        return payerObjType;
    }

    public void setPayerObjType(String payerObjType) {
        this.payerObjType = payerObjType;
    }

    public String getFeeName() {
        return feeName;
    }

    public void setFeeName(String feeName) {
        this.feeName = feeName;
    }

    public String getFeeTypeCd() {
        return feeTypeCd;
    }

    public void setFeeTypeCd(String feeTypeCd) {
        this.feeTypeCd = feeTypeCd;
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

    public String getPaymentCycle() {
        return paymentCycle;
    }

    public void setPaymentCycle(String paymentCycle) {
        this.paymentCycle = paymentCycle;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public int getCycle() {
        return cycle;
    }

    public void setCycle(int cycle) {
        this.cycle = cycle;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<ContractFeeComputeDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ContractFeeComputeDetail> details) {
        this.details = details;
    }

    /**
     * 单个标的试算明细
     */
    public static class ContractFeeComputeDetail implements Serializable {

        private String payerObjType;
        private String objId;
        private String objName;
        private String builtUpArea;
        private String areaValue;
        private String rentValue;
        private String amount;
        private String remark;

        public String getPayerObjType() {
            return payerObjType;
        }

        public void setPayerObjType(String payerObjType) {
            this.payerObjType = payerObjType;
        }

        public String getObjId() {
            return objId;
        }

        public void setObjId(String objId) {
            this.objId = objId;
        }

        public String getObjName() {
            return objName;
        }

        public void setObjName(String objName) {
            this.objName = objName;
        }

        public String getBuiltUpArea() {
            return builtUpArea;
        }

        public void setBuiltUpArea(String builtUpArea) {
            this.builtUpArea = builtUpArea;
        }

        public String getAreaValue() {
            return areaValue;
        }

        public void setAreaValue(String areaValue) {
            this.areaValue = areaValue;
        }

        public String getRentValue() {
            return rentValue;
        }

        public void setRentValue(String rentValue) {
            this.rentValue = rentValue;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
