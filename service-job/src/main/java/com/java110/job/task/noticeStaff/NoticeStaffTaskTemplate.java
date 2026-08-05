package com.java110.job.task.noticeStaff;

import com.java110.dto.community.CommunityDto;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.inspection.InspectionTaskDetailDto;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.dto.notice.NoticeStaffDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.storeStaffLeader.StoreStaffLeaderDto;
import com.java110.dto.task.TaskDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.*;
import com.java110.intf.order.IPrivilegeInnerServiceSMO;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.intf.store.ISmallWechatAttrInnerServiceSMO;
import com.java110.intf.store.IStoreStaffLeaderV1InnerServiceSMO;
import com.java110.intf.user.IStaffAppAuthInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.job.msgNotify.IMsgNotify;
import com.java110.job.msgNotify.MsgNotifyFactory;
import com.java110.job.quartz.TaskSystemQuartz;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知员工工单处理
 */
@Component
public class NoticeStaffTaskTemplate extends TaskSystemQuartz {

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IInspectionPlanInnerServiceSMO inspectionPlanInnerServiceSMOImpl;

    @Autowired
    private IInspectionPlanStaffInnerServiceSMO inspectionPlanStaffInnerServiceSMOImpl;

    @Autowired
    private IInspectionRoutePointRelInnerServiceSMO inspectionRoutePointRelInnerServiceSMOImpl;


    @Autowired
    private IInspectionTaskInnerServiceSMO inspectionTaskInnerServiceSMOImpl;

    @Autowired
    private IInspectionTaskDetailInnerServiceSMO inspectionTaskDetailInnerServiceSMOImpl;

    @Autowired
    private IPrivilegeInnerServiceSMO privilegeInnerServiceSMO;

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    @Autowired
    private IStoreStaffLeaderV1InnerServiceSMO storeStaffLeaderV1InnerServiceSMOImpl;


    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMO;


    @Override
    protected void process(TaskDto taskDto) throws Exception {
        logger.debug("开始执行微信模板信息推送" + taskDto.toString());

        // 获取小区
        List<CommunityDto> communityDtos = getAllCommunity();

        for (CommunityDto communityDto : communityDtos) {
            try {
                //todo 巡检 补推（GeneratorInspectionTaskTemplate 提前推送失败的补偿）
                publishStaffInspectionTaskAdvance(taskDto, communityDto);
            } catch (Exception e) {
                logger.error("巡检 补推失败", e);
            }
            try {
                //todo 巡检情况通知
                notifyTodayInspection(communityDto);
            } catch (Exception e) {
                logger.error("巡检情况通知", e);
            }

            try {
                //todo 报修快超时通知
                notifyRepairTimeout(communityDto);
            } catch (Exception e) {
                logger.error("巡检情况通知", e);
            }
        }
    }

    /**
     * 报修超时通知
     *
     * @param communityDto
     */
    private void notifyRepairTimeout(CommunityDto communityDto) {

        RepairDto repairDto = new RepairDto();
        repairDto.setCommunityId(communityDto.getCommunityId());
        repairDto.setStatess(new String[]{RepairDto.STATE_WAIT, RepairDto.STATE_TAKING});
        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
        if (ListUtil.isNull(repairDtos)) {
            return;
        }

        RepairUserDto repairUserDto;
        List<RepairUserDto> repairUserDtos;
        IMsgNotify msgNotify = MsgNotifyFactory.getMsgNotify(MsgNotifyFactory.NOTIFY_WAY_WECHAT);
        for (RepairDto tRepairDto : repairDtos) {
            if (!NoticeTaskUtil.isRepairAboutTimeout(tRepairDto)) {
                continue;
            }
            repairUserDto = new RepairUserDto();
            repairUserDto.setRepairId(tRepairDto.getRepairId());
            repairUserDto.setState(RepairUserDto.STATE_DOING);
            repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
            if (ListUtil.isNull(repairUserDtos)) {
                continue;
            }
            try {
                msgNotify.sendStaffMsg(
                        new NoticeStaffDto(communityDto.getCommunityId(),
                                repairUserDtos.get(0).getStaffId(),
                                tRepairDto.getRepairName() + "提交报修单快超时了",
                                "系统管理员"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //todo 提示已经超时的工单
        for (RepairDto tRepairDto : repairDtos) {
            if (!NoticeTaskUtil.isRepairTimeout(tRepairDto)) {
                continue;
            }
            repairUserDto = new RepairUserDto();
            repairUserDto.setRepairId(tRepairDto.getRepairId());
            repairUserDto.setState(RepairUserDto.STATE_DOING);
            repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
            if (ListUtil.isNull(repairUserDtos)) {
                continue;
            }
            try {
                msgNotify.sendStaffMsg(
                        new NoticeStaffDto(communityDto.getCommunityId(),
                                repairUserDtos.get(0).getStaffId(),
                                tRepairDto.getRepairName() + "提交报修单已超时",
                                "系统管理员"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            StoreStaffLeaderDto storeStaffLeaderDto = new StoreStaffLeaderDto();
            storeStaffLeaderDto.setStaffId(repairUserDtos.get(0).getStaffId());
            List<StoreStaffLeaderDto> storeStaffLeaderDtos = storeStaffLeaderV1InnerServiceSMOImpl.queryStoreStaffLeaders(storeStaffLeaderDto);
            if(ListUtil.isNull(storeStaffLeaderDtos)){
                continue;
            }
            for(StoreStaffLeaderDto leaderDto : storeStaffLeaderDtos){
                try {
                    msgNotify.sendStaffMsg(
                            new NoticeStaffDto(communityDto.getCommunityId(),
                                    leaderDto.getLeaderId(),
                                    tRepairDto.getRepairName() + "提交报修单已超时",
                                    "系统管理员"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 今日巡检情况通知
     *
     * @param communityDto
     */
    private void notifyTodayInspection(CommunityDto communityDto) {

        if (!NoticeTaskUtil.isNotifyTodayInspection(communityDto)) {
            return;
        }

        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        communityMemberDto.setCommunityId(communityDto.getCommunityId());
        communityMemberDto.setAuditStatusCd(CommunityMemberDto.AUDIT_STATUS_NORMAL);
        communityMemberDto.setMemberTypeCd(CommunityMemberDto.MEMBER_TYPE_PROPERTY);
        List<CommunityMemberDto> communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);
        Assert.listOnlyOne(communityMemberDtos, "小区没有物业公司");

        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource("/todayInspectionNotifyStaff");
        basePrivilegeDto.setStoreId(communityMemberDtos.get(0).getMemberId());
        basePrivilegeDto.setCommunityId(communityDto.getCommunityId());
        List<UserDto> userDtos = privilegeInnerServiceSMO.queryPrivilegeUsers(basePrivilegeDto);
        if (ListUtil.isNull(userDtos)) {
            return;
        }

        IMsgNotify msgNotify = MsgNotifyFactory.getMsgNotify(MsgNotifyFactory.NOTIFY_WAY_WECHAT);
        String wechatUrl = MappingCache.getValue(MappingConstant.URL_DOMAIN, "STAFF_WECHAT_URL");
        if (wechatUrl.endsWith("/")) {
            wechatUrl += "#/pages/inspection/inspectionTodayReport";
        } else {
            wechatUrl += "/#/pages/inspection/inspectionTodayReport";
        }
        for (UserDto userDto : userDtos) {
            //根据 userId 查询到openId
            try {
                msgNotify.sendStaffMsg(
                        new NoticeStaffDto(communityDto.getCommunityId(),
                                userDto.getUserId(),
                                communityDto.getName() + "今日巡检情况",
                                "系统管理员",
                                wechatUrl));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 巡检补推：补偿 GeneratorInspectionTaskTemplate 提前推送失败的情况
     * 查询 sendFlag=N 且当前时间在提前窗口内的任务，进行补推
     */
    private void publishStaffInspectionTaskAdvance(TaskDto taskDto, CommunityDto communityDto) {

        InspectionTaskDetailDto inspectionTaskDetailDto = new InspectionTaskDetailDto();
        inspectionTaskDetailDto.setCommunityId(communityDto.getCommunityId());
        inspectionTaskDetailDto.setState(InspectionTaskDto.STATE_NO_START);
        inspectionTaskDetailDto.setSendFlag(InspectionTaskDetailDto.SEND_FLAG_N);
        inspectionTaskDetailDto.setQueryTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B));
        List<InspectionTaskDetailDto> inspectionTaskDetailDtos = inspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetails(inspectionTaskDetailDto);

        // 按 taskId 聚合
        Map<String, List<InspectionTaskDetailDto>> taskGroup = new HashMap<>();
        for (InspectionTaskDetailDto detail : inspectionTaskDetailDtos) {
            taskGroup.computeIfAbsent(detail.getTaskId(), k -> new ArrayList<>()).add(detail);
        }

        Date now = DateUtil.getCurrentDate();
        String wechatUrl = MappingCache.getValue(MappingConstant.URL_DOMAIN, "STAFF_WECHAT_URL");
        if (wechatUrl.endsWith("#/")) {
            wechatUrl += "pages/inspection/inspection";
        } else if (wechatUrl.endsWith("/")) {
            wechatUrl += "#/pages/inspection/inspection";
        } else {
            wechatUrl += "/#/pages/inspection/inspection";
        }

        for (Map.Entry<String, List<InspectionTaskDetailDto>> entry : taskGroup.entrySet()) {
            List<InspectionTaskDetailDto> details = entry.getValue();
            if (details.isEmpty()) {
                continue;
            }

            InspectionTaskDetailDto firstDetail = details.get(0);

            // 检查是否已到提前推送时间：now >= plan_ins_time - beforeTime 且 now < plan_ins_time
            int beforeTime;
            try {
                beforeTime = Integer.parseInt(firstDetail.getBeforeTime() == null ? "30" : firstDetail.getBeforeTime());
            } catch (NumberFormatException e) {
                beforeTime = 30;
            }

            Date planInsTime;
            try {
                planInsTime = DateUtil.getDateFromString(firstDetail.getPlanInsTime(), DateUtil.DATE_FORMATE_STRING_A);
            } catch (ParseException e) {
                logger.error("解析计划开始时间失败: " + firstDetail.getPlanInsTime(), e);
                continue;
            }

            Calendar advanceTime = Calendar.getInstance();
            advanceTime.setTime(planInsTime);
            advanceTime.add(Calendar.MINUTE, -beforeTime);

            // 还没到提前推送时间 或 已经过了任务开始时间
            if (now.before(advanceTime.getTime()) || !now.before(planInsTime)) {
                continue;
            }

            // 发送提前提醒（按任务聚合，一条消息）
            IMsgNotify msgNotify = MsgNotifyFactory.getMsgNotify(MsgNotifyFactory.NOTIFY_WAY_WECHAT);
            try {
                ResultVo resultVo = msgNotify.sendStaffMsg(
                        new NoticeStaffDto(communityDto.getCommunityId(),
                                firstDetail.getPlanUserId(),
                                "巡检任务已开始，请尽快进行巡检",
                                "系统管理员",
                                wechatUrl)
                );
                if (resultVo != null && resultVo.getCode() == ResultVo.CODE_OK) {
                    // 推送成功，更新 sendFlag 为 A
                    for (InspectionTaskDetailDto detail : details) {
                        InspectionTaskDetailDto updateDto = new InspectionTaskDetailDto();
                        updateDto.setTaskDetailId(detail.getTaskDetailId());
                        updateDto.setSendFlag(InspectionTaskDetailDto.SEND_FLAG_A);
                        updateDto.setStatusCd("0");
                        inspectionTaskDetailInnerServiceSMOImpl.updateInspectionTaskDetail(updateDto);
                    }
                } else {
                    logger.error("补推失败(taskId=" + firstDetail.getTaskId() + "), sendFlag保持N, 等待下次重试");
                }
            } catch (Exception e) {
                logger.error("补推失败: taskId=" + firstDetail.getTaskId(), e);
            }
        }
    }

}
