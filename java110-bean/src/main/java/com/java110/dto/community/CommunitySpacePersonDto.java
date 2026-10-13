package com.java110.dto.community;

import com.java110.dto.PageDto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @ClassName FloorDto
 * @Description 小区场地预约数据层封装
 * @Author wuxw
 * @Date 2019/4/24 8:52
 * @Version 1.0
 * add by wuxw 2019/4/24
 **/
public class CommunitySpacePersonDto extends PageDto implements Serializable {

    public static final String STATE_S = "S"; //预约成功
    public static final String STATE_F = "F";//预约失败
    public static final String STATE_W = "W";//待审核
    public static final String STATE_WP = "WP";//待支付
    public static final String STATE_CL = "CL";//取消预约
    public static final String STATE_C = "C";//已完成

    private String receivableAmount;
    private String payWay;
    private String payWayName;
    private String remark;
    private String receivedAmount;
    private String personName;
    private String spaceId;
    private String venueId;
    private String venueName;

    private String spaceName;
    private String cspId;
    private String appointmentTime;
    private String personId;
    private String state;
    private String stateName;
    private String personTel;
    private String communityId;

    private String orderId;

    private String payTime;
    private String cancelTime;
    private String cancelReason;

    private String[] states;
    private String[] stateNotIn;

    private String keyword;
    private String beginDate;
    private String endDate;

    private String pending;
    private String active;
    private String finished;
    private String canceled;
    private String receivableTotal;
    private String receivedTotal;

    private List<CommunitySpacePersonTimeDto> times;


    private Date createTime;


    private String statusCd = "0";


    public String getReceivableAmount() {
        return receivableAmount;
    }

    public void setReceivableAmount(String receivableAmount) {
        this.receivableAmount = receivableAmount;
    }

    public String getPayWay() {
        return payWay;
    }

    public void setPayWay(String payWay) {
        this.payWay = payWay;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(String receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getCspId() {
        return cspId;
    }

    public void setCspId(String cspId) {
        this.cspId = cspId;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPersonTel() {
        return personTel;
    }

    public void setPersonTel(String personTel) {
        this.personTel = personTel;
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

    public String getPayWayName() {
        return payWayName;
    }

    public void setPayWayName(String payWayName) {
        this.payWayName = payWayName;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public List<CommunitySpacePersonTimeDto> getTimes() {
        return times;
    }

    public void setTimes(List<CommunitySpacePersonTimeDto> times) {
        this.times = times;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getPayTime() {
        return payTime;
    }

    public void setPayTime(String payTime) {
        this.payTime = payTime;
    }

    public String getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(String cancelTime) {
        this.cancelTime = cancelTime;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String[] getStates() {
        return states;
    }

    public void setStates(String[] states) {
        this.states = states;
    }

    public String[] getStateNotIn() {
        return stateNotIn;
    }

    public void setStateNotIn(String[] stateNotIn) {
        this.stateNotIn = stateNotIn;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(String beginDate) {
        this.beginDate = beginDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getPending() {
        return pending;
    }

    public void setPending(String pending) {
        this.pending = pending;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getFinished() {
        return finished;
    }

    public void setFinished(String finished) {
        this.finished = finished;
    }

    public String getCanceled() {
        return canceled;
    }

    public void setCanceled(String canceled) {
        this.canceled = canceled;
    }

    public String getReceivableTotal() {
        return receivableTotal;
    }

    public void setReceivableTotal(String receivableTotal) {
        this.receivableTotal = receivableTotal;
    }

    public String getReceivedTotal() {
        return receivedTotal;
    }

    public void setReceivedTotal(String receivedTotal) {
        this.receivedTotal = receivedTotal;
    }
}
