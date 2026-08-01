package com.java110.job.task.purchase;

import com.java110.dto.community.CommunityDto;
import com.java110.dto.inspection.InspectionTaskDetailDto;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.dto.task.TaskDto;
import com.java110.intf.community.IInspectionTaskDetailInnerServiceSMO;
import com.java110.intf.community.IInspectionTaskInnerServiceSMO;
import com.java110.job.quartz.TaskSystemQuartz;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 巡检签到状态修改定时任务
 *
 * @author fqz
 * @date 2021-06-11 9:12
 */
@Component
public class InspectionTaskDetailTemplate extends TaskSystemQuartz {

    private static Logger logger = LoggerFactory.getLogger(InspectionTaskDetailTemplate.class);

    /**
     * 超时状态码
     */
    private static final String STATE_TIMEOUT = "20200408";

    /**
     * 缺勤状态码（有部分打卡但未完成）
     */
    private static final String STATE_ABSENCE = "20200409";

    /**
     * 明细签到状态：未到
     */
    private static final String INSPECTION_STATE_NOT_ARRIVE = "70000";

    /**
     * 超时清理天数：超过该天数后软删除
     */
    private static final int EXPIRED_DAYS = 3;

    @Autowired
    private IInspectionTaskDetailInnerServiceSMO iInspectionTaskDetailInnerServiceSMOImpl;

    @Autowired
    private IInspectionTaskInnerServiceSMO iInspectionTaskInnerServiceSMOImpl;

    @Override
    protected void process(TaskDto taskDto) {
        logger.debug("开始执行巡检任务状态校准" + taskDto.toString());
        // 获取小区
        List<CommunityDto> communityDtos = getAllCommunity();
        for (CommunityDto communityDto : communityDtos) {
            try {
                // 1. 校准超时任务状态：未完成任务比对planEndTime，超时置为超时
                updateTimeoutTask(communityDto);
                // 2. 校准超时明细状态：未完成明细比对planEndTime，超时置为超时
                updateTimeoutTaskDetail(communityDto);
                // 3. 清理超时3天后的任务和巡检明细（软删除）
                cleanExpiredTask(communityDto);
            } catch (Exception e) {
                logger.error("巡检任务状态校准失败", e);
            }
        }
    }

    /**
     * 步骤1：查询所有未完成任务（未开始、巡检中），比对planEndTime与当前时间，超时置为超时状态
     *
     * @param communityDto 小区信息
     */
    private void updateTimeoutTask(CommunityDto communityDto) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();

        // 查询所有未完成的巡检任务（未开始 + 巡检中）
        InspectionTaskDto queryTaskDto = new InspectionTaskDto();
        queryTaskDto.setCommunityId(communityDto.getCommunityId());
        queryTaskDto.setStates(new String[]{InspectionTaskDto.STATE_NO_START, InspectionTaskDto.STATE_DOING});
        List<InspectionTaskDto> tasks = iInspectionTaskInnerServiceSMOImpl.queryInspectionTasks(queryTaskDto);

        if (ListUtil.isNull(tasks)) {
            return;
        }

        for (InspectionTaskDto task : tasks) {
            String planEndTimeStr = task.getPlanEndTime();
            if (StringUtil.isEmpty(planEndTimeStr)) {
                continue;
            }
            Date planEndTime = sdf.parse(planEndTimeStr);
            // 计划结束时间早于当前时间，说明已超时
            if (now.after(planEndTime)) {
                InspectionTaskDto updateTask = new InspectionTaskDto();
                updateTask.setTaskId(task.getTaskId());
                updateTask.setState(STATE_TIMEOUT);
                iInspectionTaskInnerServiceSMOImpl.updateInspectionTask(updateTask);
            }
        }
    }

    /**
     * 步骤2：查询所有未完成的巡检明细，比对关联任务的planEndTime与当前时间，超时置为超时状态
     *
     * @param communityDto 小区信息
     */
    private void updateTimeoutTaskDetail(CommunityDto communityDto) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();

        // 查询未打卡的巡检明细（inspection_time IS NULL AND inspection_state IS NULL）
        InspectionTaskDetailDto queryDetail = new InspectionTaskDetailDto();
        queryDetail.setCommunityId(communityDto.getCommunityId());
        queryDetail.setInspectionTimeFlag("1");
        List<InspectionTaskDetailDto> details = iInspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetails(queryDetail);

        if (ListUtil.isNull(details)) {
            return;
        }

        for (InspectionTaskDetailDto detail : details) {
            // 已是超时状态的明细跳过，避免重复更新
            if (STATE_TIMEOUT.equals(detail.getState())) {
                continue;
            }
            String planEndTimeStr = detail.getPlanEndTime();
            if (StringUtil.isEmpty(planEndTimeStr)) {
                continue;
            }
            Date planEndTime = sdf.parse(planEndTimeStr);
            // 计划结束时间早于当前时间，说明已超时
            if (now.after(planEndTime)) {
                detail.setState(STATE_TIMEOUT);
                iInspectionTaskDetailInnerServiceSMOImpl.updateInspectionTaskDetail(detail);
            }
        }
    }

    /**
     * 步骤3：清理超时3天后的任务和巡检明细（软删除，status_cd置为1）
     *
     * @param communityDto 小区信息
     */
    private void cleanExpiredTask(CommunityDto communityDto) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance(Locale.CHINA);
        cal.add(Calendar.DAY_OF_MONTH, -EXPIRED_DAYS);
        Date expiredDate = cal.getTime();

        // 查询所有超时任务，在Java中过滤超过3天的
        InspectionTaskDto queryTask = new InspectionTaskDto();
        queryTask.setCommunityId(communityDto.getCommunityId());
        queryTask.setState(STATE_TIMEOUT);
        List<InspectionTaskDto> timeoutTasks = iInspectionTaskInnerServiceSMOImpl.queryInspectionTasks(queryTask);

        if (ListUtil.isNull(timeoutTasks)) {
            return;
        }

        for (InspectionTaskDto task : timeoutTasks) {
            String planEndTimeStr = task.getPlanEndTime();
            if (StringUtil.isEmpty(planEndTimeStr)) {
                continue;
            }
            Date planEndTime = sdf.parse(planEndTimeStr);
            // 超时超过3天的，软删除任务和明细
            if (planEndTime.before(expiredDate)) {
                // 查询该任务下是否有打卡记录，区分缺勤和超时
                InspectionTaskDetailDto checkDetail = new InspectionTaskDetailDto();
                checkDetail.setTaskId(task.getTaskId());
                checkDetail.setInspectionTimeFlag("2"); // 已打卡明细
                List<InspectionTaskDetailDto> checkedDetails = iInspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetails(checkDetail);

                String finalState = ListUtil.isNull(checkedDetails) ? STATE_TIMEOUT : STATE_ABSENCE;

                // 更新任务状态并软删除
                InspectionTaskDto updateTask = new InspectionTaskDto();
                updateTask.setTaskId(task.getTaskId());
                updateTask.setState(finalState);
                updateTask.setStatusCd("1");
                iInspectionTaskInnerServiceSMOImpl.updateInspectionTask(updateTask);

                // 查询该任务下的所有明细，未打卡的标记为未到，全部软删除
                InspectionTaskDetailDto queryDetail = new InspectionTaskDetailDto();
                queryDetail.setTaskId(task.getTaskId());
                queryDetail.setCommunityId(communityDto.getCommunityId());
                List<InspectionTaskDetailDto> details = iInspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetails(queryDetail);
                if (!ListUtil.isNull(details)) {
                    for (InspectionTaskDetailDto detail : details) {
                        // 未打卡明细标记为未到
                        if (StringUtil.isEmpty(detail.getInspectionTime())) {
                            detail.setInspectionState(INSPECTION_STATE_NOT_ARRIVE);
                        }
                        detail.setStatusCd("1");
                        iInspectionTaskDetailInnerServiceSMOImpl.updateInspectionTaskDetail(detail);
                    }
                }
            }
        }
    }
}
