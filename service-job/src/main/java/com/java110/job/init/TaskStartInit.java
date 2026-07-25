package com.java110.job.init;

import com.java110.dto.task.TaskDto;
import com.java110.dto.task.TaskAttrDto;
import com.java110.dto.task.TaskTemplateDto;
import com.java110.job.dao.ITaskServiceDao;
import com.java110.job.dao.ITaskAttrServiceDao;
import com.java110.job.quartz.TaskSystemJob;
import com.java110.utils.util.BeanConvertUtil;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaskStartInit implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TaskStartInit.class);

    private static final String prefixJobName = "task_";
    private static final String triggerNames = "taskToData_";

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ITaskServiceDao taskServiceDaoImpl;

    @Autowired
    private ITaskAttrServiceDao taskAttrServiceDaoImpl;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.warn("===== 系统启动，开始加载定时任务 =====");

        // 第一步：重置异常状态的触发器
        try {
            resetErrorTriggers();
        } catch (Throwable t) {
            logger.error("重置 ERROR 触发器时发生严重异常，继续尝试加载任务", t);
        }

        // 第二步：从数据库加载启用状态的任务并注册
        try {
            loadAndRegisterTasks();
        } catch (Throwable t) {
            logger.error("加载定时任务时发生严重异常", t);
        }

        logger.warn("===== 定时任务加载完成 =====");
    }

    /**
     * 遍历所有触发器，删除 ERROR 状态的
     */
    private void resetErrorTriggers() {
        logger.info("----- 开始检查 ERROR 状态触发器 -----");
        int errorCount = 0;
        int removedCount = 0;

        try {
            List<String> triggerGroups = scheduler.getTriggerGroupNames();
            logger.info("触发器组数量: " + (triggerGroups != null ? triggerGroups.size() : 0));

            if (triggerGroups == null || triggerGroups.isEmpty()) {
                logger.info("没有触发器组，跳过检查");
                return;
            }

            for (String group : triggerGroups) {
                logger.info("检查触发器组: " + group);
                java.util.Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(group));
                logger.info("  触发器数量: " + triggerKeys.size());

                for (TriggerKey triggerKey : triggerKeys) {
                    try {
                        Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);

                        if (state == Trigger.TriggerState.ERROR) {
                            errorCount++;
                            logger.warn("  [ERROR] " + triggerKey);

                            // 获取关联的 Job
                            Trigger trigger = scheduler.getTrigger(triggerKey);
                            if (trigger == null) {
                                logger.warn("  触发器不存在（可能已被删除），跳过");
                                continue;
                            }

                            JobKey jobKey = trigger.getJobKey();
                            logger.info("  关联Job: " + jobKey);

                            // 按顺序删除：暂停 → 取消调度 → 删除Job
                            scheduler.pauseTrigger(triggerKey);
                            logger.info("  已暂停触发器");

                            scheduler.unscheduleJob(triggerKey);
                            logger.info("  已取消调度");

                            scheduler.deleteJob(jobKey);
                            logger.info("  已删除Job");

                            removedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("  处理触发器 [" + triggerKey + "] 时异常: " + e.getMessage(), e);
                        // 继续处理下一个
                    }
                }
            }
        } catch (Exception e) {
            logger.error("扫描触发器时异常: " + e.getMessage(), e);
        }

        logger.warn("----- ERROR 触发器检查完成: 发现 " + errorCount + " 个, 已移除 " + removedCount + " 个 -----");
    }

    /**
     * 从数据库查询启用状态的任务并注册到 Quartz
     */
    private void loadAndRegisterTasks() {
        logger.info("----- 开始从数据库加载启用状态的任务 -----");

        Map<String, Object> info = new HashMap<>();
        info.put("state", "002");

        List<Map> taskInfos = taskServiceDaoImpl.getTaskInfo(info);

        logger.info("查询到 state=002 的任务数量: " + (taskInfos != null ? taskInfos.size() : 0));

        if (taskInfos == null || taskInfos.size() == 0) {
            logger.info("没有需要启动的定时任务");
            return;
        }

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (Map taskInfo : taskInfos) {
            TaskDto taskDto = BeanConvertUtil.covertBean(taskInfo, TaskDto.class);
            logger.info("处理任务: taskId=" + taskDto.getTaskId()
                    + ", taskName=" + taskDto.getTaskName()
                    + ", cron=" + taskDto.getTaskCron()
                    + ", templateId=" + taskDto.getTemplateId());

            try {
                String jobName = prefixJobName + taskDto.getTaskId();
                JobKey jobKey = new JobKey(jobName, TaskSystemJob.JOB_GROUP_NAME);

                if (scheduler.checkExists(jobKey)) {
                    logger.info("  任务已存在，跳过");
                    skipCount++;
                    continue;
                }

                registerTask(taskDto);
                successCount++;
                logger.info("  注册成功");

            } catch (Exception e) {
                failCount++;
                logger.error("  注册失败: " + e.getMessage(), e);
            }
        }

        logger.warn("----- 任务加载结果: 成功=" + successCount + ", 跳过=" + skipCount + ", 失败=" + failCount + " -----");
    }

    /**
     * 注册单个任务到 Quartz 调度器
     */
    private void registerTask(TaskDto taskDto) throws Exception {
        // 加载任务属性
        List<TaskAttrDto> attrDtos = BeanConvertUtil.covertBeanList(
                taskAttrServiceDaoImpl.getTaskAttrInfo(BeanConvertUtil.beanCovertMap(taskDto)),
                TaskAttrDto.class);

        // 加载任务模板
        Map<String, Object> templateInfo = new HashMap<>();
        templateInfo.put("templateId", taskDto.getTemplateId());
        List<TaskTemplateDto> taskTemplateDtos = BeanConvertUtil.covertBeanList(
                taskServiceDaoImpl.getTaskTemplateInfo(templateInfo),
                TaskTemplateDto.class);

        if (taskTemplateDtos == null || taskTemplateDtos.isEmpty()) {
            logger.warn("  警告: 任务模板不存在, templateId=" + taskDto.getTemplateId());
        } else {
            taskDto.setTaskTemplateDto(taskTemplateDtos.get(0));
        }
        taskDto.setTaskAttr(attrDtos);

        String cronExpression = taskDto.getTaskCron();
        if (cronExpression == null || cronExpression.isEmpty()) {
            cronExpression = "0 * * * * ?";
            logger.info("  使用默认cron: " + cronExpression);
        }

        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
        String triggerName = triggerNames + taskDto.getTaskId();
        String jobName = prefixJobName + taskDto.getTaskId();

        JobDetail jobDetail = JobBuilder.newJob(TaskSystemJob.class)
                .withIdentity(jobName, TaskSystemJob.JOB_GROUP_NAME)
                .withDescription("任务启动")
                .build();

        jobDetail.getJobDataMap().put(TaskSystemJob.JOB_DATA_CONFIG_NAME, taskDto.getTaskName());
        jobDetail.getJobDataMap().put(TaskSystemJob.JOB_DATA_TASK_ID, taskDto.getTaskId());
        jobDetail.getJobDataMap().put(TaskSystemJob.JOB_DATA_TASK, taskDto);
        jobDetail.getJobDataMap().put(TaskSystemJob.JOB_DATA_TASK_ATTR, taskDto);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerName, triggerName + "_group")
                .withSchedule(cronScheduleBuilder)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
}
