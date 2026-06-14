/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.dev.cmd.task;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.task.TaskAttrDto;
import com.java110.intf.job.ITaskAttrInnerServiceSMO;
import com.java110.intf.job.ITaskInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.task.TaskDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询任务列表
 * 服务编码：task.listTasks
 * 请求路径：/app/task.ListTask
 * add by 吴学文 at 2022-08-05 10:23:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "task.listTasks")
public class ListTasksCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListTasksCmd.class);

    /**
     * 任务内部服务接口
     */
    @Autowired
    private ITaskInnerServiceSMO taskInnerServiceSMOImpl;

    /**
     * 任务属性内部服务接口
     */
    @Autowired
    private ITaskAttrInnerServiceSMO taskAttrInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法 - 查询任务列表
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为TaskDto对象
        TaskDto taskDto = BeanConvertUtil.covertBean(reqJson, TaskDto.class);
        
        // 查询任务总数
        int count = taskInnerServiceSMOImpl.queryTasksCount(taskDto);
        List<TaskDto> taskDtos = null;
        
        if (count > 0) {
            // 查询任务列表
            taskDtos = taskInnerServiceSMOImpl.queryTasks(taskDto);
            // 刷新任务属性信息
            freshTaskAttr(taskDtos);
        } else {
            taskDtos = new ArrayList<>();
        }
        
        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, taskDtos);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 查询任务属性信息并设置到任务对象中
     * @param taskDtos 任务数据列表
     */
    private void freshTaskAttr(List<TaskDto> taskDtos) {
        for (TaskDto taskDto : taskDtos) {
            TaskAttrDto taskAttrDto = new TaskAttrDto();
            taskAttrDto.setTaskId(taskDto.getTaskId());
            // 查询任务属性列表
            List<TaskAttrDto> taskAttrDtos = taskAttrInnerServiceSMOImpl.queryTaskAttrs(taskAttrDto);
            // 设置任务属性到任务对象
            taskDto.setTaskAttr(taskAttrDtos);
        }
    }
}