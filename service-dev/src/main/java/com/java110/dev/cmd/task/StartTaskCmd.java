package com.java110.dev.cmd.task;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.task.TaskDto;
import com.java110.intf.job.ITaskInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 启动任务命令类
 * 处理任务启动请求，验证参数并执行任务启动操作
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode为"task.startTask"
 */
@Java110Cmd(serviceCode = "task.startTask")
public class StartTaskCmd extends Cmd {

    /**
     * 任务内部服务接口，用于操作任务数据
     */
    @Autowired
    private ITaskInnerServiceSMO taskInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的taskId字段
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求JSON中必须包含taskId字段
        Assert.hasKeyAndValue(reqJson, "taskId", "taskId不能为空");
    }

    /**
     * 命令执行方法
     * 根据taskId查询任务信息并启动对应的任务
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建任务数据传输对象并设置任务ID
        TaskDto taskDto = new TaskDto();
        taskDto.setTaskId(reqJson.getString("taskId"));
        
        // 根据任务ID查询任务信息
        List<TaskDto> taskDtos = taskInnerServiceSMOImpl.queryTasks(taskDto);
        ResultVo resultVo = null;
        
        // 检查任务是否存在
        if (taskDtos == null || taskDtos.size() < 1) {
            // 任务不存在，返回错误信息
            resultVo = new ResultVo(ResultVo.ORDER_ERROR, "传入任务ID错误");
            ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 启动任务，获取执行结果状态
        int state = taskInnerServiceSMOImpl.startTask(taskDtos.get(0));

        // 根据启动结果设置返回信息
        if (state > 0) {
            // 启动成功
            resultVo = new ResultVo(ResultVo.CODE_OK, "启动成功");
        } else {
            // 启动失败
            resultVo = new ResultVo(ResultVo.ORDER_ERROR, "启动失败");
        }
        
        // 设置响应实体并返回结果
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}