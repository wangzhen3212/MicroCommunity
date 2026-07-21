package com.java110.job.quartz;

import com.java110.dto.task.TaskDto;
import com.java110.utils.factory.ApplicationContextFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * @author
 */

public class TaskSystemJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(TaskSystemJob.class);

    public static String JOB_DATA_CONFIG_NAME = "taskSystemTaskName";
    public static String JOB_DATA_TASK_ID = "taskSystemTaskId";
    public static String JOB_DATA_TASK_ATTR = "taskSystemTaskAttr";
    public static String JOB_DATA_TASK = "taskSystemTask";
    public static String JOB_GROUP_NAME = "taskSystemJobGroup"; // 任务的 分组名称


    private TaskSystemQuartz taskSystemQuartz;

    protected void executeInternal(JobExecutionContext context) {
        TaskDto taskDto = null;
        try {
            taskDto = (TaskDto) (context.getJobDetail().getJobDataMap()
                    .get(JOB_DATA_TASK));
            String classBean = taskDto.getTaskTemplateDto().getClassBean();
            taskSystemQuartz = (TaskSystemQuartz) ApplicationContextFactory.getBean(classBean);
            taskSystemQuartz.startTask(taskDto);
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException ex) {
            String taskName = taskDto != null ? taskDto.getTaskName() : "unknown";
            String classBean = taskDto != null && taskDto.getTaskTemplateDto() != null
                    ? taskDto.getTaskTemplateDto().getClassBean() : "unknown";
            logger.warn("定时任务[{}]对应的Bean[{}]不存在，已跳过执行。请在数据库中清理该任务或实现对应类。",
                    taskName, classBean);
        } catch (Throwable ex) {
            logger.error("执行任务失败：", ex);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        this.executeInternal(jobExecutionContext);
    }
}
