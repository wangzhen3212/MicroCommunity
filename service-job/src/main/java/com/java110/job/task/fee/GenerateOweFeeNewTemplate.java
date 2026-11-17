package com.java110.job.task.fee;

import com.java110.dto.task.TaskDto;
import com.java110.job.quartz.TaskSystemQuartz;
import org.springframework.stereotype.Component;

/**
 * @ClassName GenerateOwnerBillTemplate
 * @Description TODO  房屋费用账单生成
 * @Author wuxw
 * @Date 2020/6/4 8:33
 * @Version 1.0
 * add by wuxw 2020/6/4
 **/
@Component
public class GenerateOweFeeNewTemplate extends TaskSystemQuartz {

    @Override
    protected void process(TaskDto taskDto) throws Exception {
    }
}
