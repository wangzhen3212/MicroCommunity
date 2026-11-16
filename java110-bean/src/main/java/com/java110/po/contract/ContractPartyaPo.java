package com.java110.po.contract;

import java.io.Serializable;

/**
 * 合同甲方主体持久化对象
 */
public class ContractPartyaPo implements Serializable {

    private String partyA;
    private String aLink;
    private String aContacts;
    private String partyaId;
    private String statusCd = "0";
    private String storeId;

    private String shortName;
    private String creditCode;
    private String legalPerson;
    private String bankName;
    private String bankAccount;
    private String contactPerson;
    private String contactTel;
    private String sealFlag;
    private String defaultFlag;

    public String getPartyA() {
        return partyA;
    }

    public void setPartyA(String partyA) {
        this.partyA = partyA;
    }

    public String getaLink() {
        return aLink;
    }

    public void setaLink(String aLink) {
        this.aLink = aLink;
    }

    public String getaContacts() {
        return aContacts;
    }

    public void setaContacts(String aContacts) {
        this.aContacts = aContacts;
    }

    public String getPartyaId() {
        return partyaId;
    }

    public void setPartyaId(String partyaId) {
        this.partyaId = partyaId;
    }

    public String getStatusCd() {
        return statusCd;
    }

    public void setStatusCd(String statusCd) {
        this.statusCd = statusCd;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getCreditCode() {
        return creditCode;
    }

    public void setCreditCode(String creditCode) {
        this.creditCode = creditCode;
    }

    public String getLegalPerson() {
        return legalPerson;
    }

    public void setLegalPerson(String legalPerson) {
        this.legalPerson = legalPerson;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactTel() {
        return contactTel;
    }

    public void setContactTel(String contactTel) {
        this.contactTel = contactTel;
    }

    public String getSealFlag() {
        return sealFlag;
    }

    public void setSealFlag(String sealFlag) {
        this.sealFlag = sealFlag;
    }

    public String getDefaultFlag() {
        return defaultFlag;
    }

    public void setDefaultFlag(String defaultFlag) {
        this.defaultFlag = defaultFlag;
    }
}
