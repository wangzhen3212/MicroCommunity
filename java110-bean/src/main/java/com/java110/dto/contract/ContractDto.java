package com.java110.dto.contract;

import com.java110.dto.PageDto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @ClassName FloorDto
 * @Description 合同管理数据层封装
 * @Author wuxw
 * @Date 2019/4/24 8:52
 * @Version 1.0
 * add by wuxw 2019/4/24
 **/
public class ContractDto extends PageDto implements Serializable {

    /**
     * 33	已驳回
     * 44	合同终止
     */
    public static final String STATE_FAIL = "33";//33 44
    public static final String STATE_AUDIT_FINISH = "22";//审核完成
    public static final String STATE_AUDIT_DOING = "55";//33 44

    public static final String STATE_COMPLAINT = "44"; //合同终止

    public static final String STATE_DRAFT = "10"; //草稿
    public static final String STATE_AUDIT = "11"; //法务校对
    public static final String STATE_AUDIT_LEADER = "56"; //领导审批

    /**
     * 合同类型分类：1 房屋出租 / 2 采购 / 3 维保
     */
    public static final String CATEGORY_RENT = "1";
    public static final String CATEGORY_PURCHASE = "2";
    public static final String CATEGORY_MAINTAIN = "3";

    private String aLink;
    private String aContacts;
    private String amount;
    private String storeId;

    private String operator;
    private String signingTime;
    private String bContacts;
    private String partyA;
    private String bLink;
    private String partyB;
    private String contractId;
    private String[] contractIds;
    private String objId;
    private String contractName;
    private String contractNameLike;

    private String startTime;
    private String endTime;

    private String queryStartTime;
    private String queryEndTime;
    private String state;
    private String[] states;
    private String[] noStates;
    private String stateDesc;
    private String contractCode;

    private String contractCodeLike;
    private String objType;
    private String operatorLink;
    private String contractParentId;
    //
    //obj_person_name
    //obj_person_id
    private String objName;
    private String objPersonName;
    private String objPersonId;


    private Date createTime;

    private String statusCd = "0";
    private String stateName;


    private String parentContractCode;
    private String parentContractCodeLike;

    private String parentContractName;


    private String currentUserId;
    private String processInstanceId;
    private String taskId;
    private String auditCode;
    private String auditMessage;
    private String staffId;
    private String staffName;
    private String staffTel;
    private String startUserId;
    private String nextUserId;

    private String communityId;
    private String[] communityIds;
    private String audit;

    private String hasAudit;

    private String hasEnd;

    private String partyBLike;

    /**
     * 甲方主体ID
     */
    private String partyaId;
    /**
     * 合同模板ID
     */
    private String templateId;
    /**
     * 合同模板名称
     */
    private String templateName;

    /**
     * 付款方式
     */
    private String payMode;
    /**
     * 合同类型分类 1房屋出租 2采购 3维保
     */
    private String category;
    private String[] categories;
    /**
     * 到期区间查询
     */
    private String endTimeBegin;
    private String endTimeEnd;
    /**
     * 是否存在变更 1 存在
     */
    private String changed;
    /**
     * 合同类型名称
     */
    private String typeNameLike;
    /**
     * 履约付款状态过滤（3 全部未付清，含逾期）
     */
    private String payState;

    /**
     * 关键字：合同编号 / 名称 / 签约方
     */
    private String searchText;

    private String ownerName;
    private String roomNum;
    private String shopNum;

    private List<ContractAttrDto> attrs;

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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStoreId() {

        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getSigningTime() {
        return signingTime;
    }

    public void setSigningTime(String signingTime) {
        this.signingTime = signingTime;
    }

    public String getbContacts() {
        return bContacts;
    }

    public void setbContacts(String bContacts) {
        this.bContacts = bContacts;
    }

    public String getPartyA() {
        return partyA;
    }

    public void setPartyA(String partyA) {
        this.partyA = partyA;
    }

    public String getbLink() {
        return bLink;
    }

    public void setbLink(String bLink) {
        this.bLink = bLink;
    }

    public String getPartyB() {
        return partyB;
    }

    public void setPartyB(String partyB) {
        this.partyB = partyB;
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

    public String getObjId() {
        return objId;
    }

    public void setObjId(String objId) {
        this.objId = objId;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getContractNameLike() {
        return contractNameLike;
    }

    public void setContractNameLike(String contractNameLike) {
        this.contractNameLike = contractNameLike;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
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

    public String[] getNoStates() {
        return noStates;
    }

    public void setNoStates(String[] noStates) {
        this.noStates = noStates;
    }

    public String getStateDesc() {
        return stateDesc;
    }

    public void setStateDesc(String stateDesc) {
        this.stateDesc = stateDesc;
    }

    public String getContractCode() {
        return contractCode;
    }

    public void setContractCode(String contractCode) {
        this.contractCode = contractCode;
    }

    public String getObjType() {
        return objType;
    }

    public void setObjType(String objType) {
        this.objType = objType;
    }

    public String getOperatorLink() {
        return operatorLink;
    }

    public void setOperatorLink(String operatorLink) {
        this.operatorLink = operatorLink;
    }

    public String getContractParentId() {
        return contractParentId;
    }

    public void setContractParentId(String contractParentId) {
        this.contractParentId = contractParentId;
    }

    public String getObjName() {
        return objName;
    }

    public void setObjName(String objName) {
        this.objName = objName;
    }

    public String getObjPersonName() {
        return objPersonName;
    }

    public void setObjPersonName(String objPersonName) {
        this.objPersonName = objPersonName;
    }

    public String getObjPersonId() {
        return objPersonId;
    }

    public void setObjPersonId(String objPersonId) {
        this.objPersonId = objPersonId;
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

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getStateName() {

        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getParentContractCode() {
        return parentContractCode;
    }

    public void setParentContractCode(String parentContractCode) {
        this.parentContractCode = parentContractCode;
    }

    public String getParentContractName() {
        return parentContractName;
    }

    public void setParentContractName(String parentContractName) {
        this.parentContractName = parentContractName;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getAuditCode() {
        return auditCode;
    }

    public void setAuditCode(String auditCode) {
        this.auditCode = auditCode;
    }

    public String getAuditMessage() {
        return auditMessage;
    }

    public void setAuditMessage(String auditMessage) {
        this.auditMessage = auditMessage;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffTel() {
        return staffTel;
    }

    public void setStaffTel(String staffTel) {
        this.staffTel = staffTel;
    }

    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    public String getCommunityId() {
        return communityId;
    }

    public void setCommunityId(String communityId) {
        this.communityId = communityId;
    }

    public String[] getCommunityIds() {
        return communityIds;
    }

    public void setCommunityIds(String[] communityIds) {
        this.communityIds = communityIds;
    }

    public List<ContractAttrDto> getAttrs() {
        return attrs;
    }

    public void setAttrs(List<ContractAttrDto> attrs) {
        this.attrs = attrs;
    }

    public String getNextUserId() {
        return nextUserId;
    }

    public void setNextUserId(String nextUserId) {
        this.nextUserId = nextUserId;
    }

    public String getHasAudit() {
        return hasAudit;
    }

    public void setHasAudit(String hasAudit) {
        this.hasAudit = hasAudit;
    }

    public String getHasEnd() {
        return hasEnd;
    }

    public void setHasEnd(String hasEnd) {
        this.hasEnd = hasEnd;
    }

    public String getAudit() {
        return audit;
    }

    public void setAudit(String audit) {
        this.audit = audit;
    }

    public String getContractCodeLike() {
        return contractCodeLike;
    }

    public void setContractCodeLike(String contractCodeLike) {
        this.contractCodeLike = contractCodeLike;
    }

    public String getQueryStartTime() {
        return queryStartTime;
    }

    public void setQueryStartTime(String queryStartTime) {
        this.queryStartTime = queryStartTime;
    }

    public String getQueryEndTime() {
        return queryEndTime;
    }

    public void setQueryEndTime(String queryEndTime) {
        this.queryEndTime = queryEndTime;
    }

    public String getPartyBLike() {
        return partyBLike;
    }

    public void setPartyBLike(String partyBLike) {
        this.partyBLike = partyBLike;
    }

    public String getParentContractCodeLike() {
        return parentContractCodeLike;
    }

    public void setParentContractCodeLike(String parentContractCodeLike) {
        this.parentContractCodeLike = parentContractCodeLike;
    }

    public String getPartyaId() {
        return partyaId;
    }

    public void setPartyaId(String partyaId) {
        this.partyaId = partyaId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getPayMode() {
        return payMode;
    }

    public void setPayMode(String payMode) {
        this.payMode = payMode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public String getEndTimeBegin() {
        return endTimeBegin;
    }

    public void setEndTimeBegin(String endTimeBegin) {
        this.endTimeBegin = endTimeBegin;
    }

    public String getEndTimeEnd() {
        return endTimeEnd;
    }

    public void setEndTimeEnd(String endTimeEnd) {
        this.endTimeEnd = endTimeEnd;
    }

    public String getChanged() {
        return changed;
    }

    public void setChanged(String changed) {
        this.changed = changed;
    }

    public String getTypeNameLike() {
        return typeNameLike;
    }

    public void setTypeNameLike(String typeNameLike) {
        this.typeNameLike = typeNameLike;
    }

    public String getPayState() {
        return payState;
    }

    public void setPayState(String payState) {
        this.payState = payState;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getRoomNum() {
        return roomNum;
    }

    public void setRoomNum(String roomNum) {
        this.roomNum = roomNum;
    }

    public String getShopNum() {
        return shopNum;
    }

    public void setShopNum(String shopNum) {
        this.shopNum = shopNum;
    }
}
