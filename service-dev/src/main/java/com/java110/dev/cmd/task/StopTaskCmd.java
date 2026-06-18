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
 * 停止任务命令类
 * 负责处理停止定时任务的业务逻辑
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode为"task.stopTask"
 */
@Java110Cmd(serviceCode = "task.stopTask")
public class StopTaskCmd extends Cmd {
    
    /**
     * 任务内部服务接口
     * 用于操作任务相关的业务逻辑
     */
    @Autowired
    private ITaskInnerServiceSMO taskInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否合法
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含taskId字段
        Assert.hasKeyAndValue(reqJson, "taskId", "taskId不能为空");
    }

    /**
     * 命令执行方法
     * 执行停止任务的核心业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建任务数据传输对象并设置任务ID
        TaskDto taskDto = new TaskDto();
        taskDto.setTaskId(reqJson.getString("taskId"));
        
        // 根据任务ID查询任务信息
        List<TaskDto> taskDtos = taskInnerServiceSMOImpl.queryTasks(taskDto);
        
        ResultVo resultVo = null;
        
        // 验证任务是否存在
        if (taskDtos == null || taskDtos.size() < 1) {
            // 任务不存在，返回错误信息
            resultVo = new ResultVo(ResultVo.ORDER_ERROR, "传入任务ID错误");
            ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 执行停止任务操作
        int state = taskInnerServiceSMOImpl.stopTask(taskDtos.get(0));
        
        // 根据停止结果设置返回信息
        if(state > 0){
            resultVo = new ResultVo(ResultVo.CODE_OK, "停止成功");
        } else {
            resultVo = new ResultVo(ResultVo.ORDER_ERROR, "停止失败");
        }
        
        // 设置响应实体并返回结果
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}