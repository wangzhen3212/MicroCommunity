package com.java110.dev.cmd.task;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.task.TaskTemplateDto;
import com.java110.intf.job.ITaskInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询任务模板列表命令类
 * 处理服务码为"task.listTaskTemplate"的请求
 * 
 * @author Java110
 */
@Java110Cmd(serviceCode = "task.listTaskTemplate")
public class ListTaskTemplateCmd extends Cmd {

    // 注入任务内部服务接口
    @Autowired
    private ITaskInnerServiceSMO taskInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 主要验证分页参数
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 查询任务模板列表并返回结果
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为任务模板DTO对象
        TaskTemplateDto taskTemplateDto = BeanConvertUtil.covertBean(reqJson, TaskTemplateDto.class);

        // 查询任务模板总数
        int count = taskInnerServiceSMOImpl.queryTaskTemplateCount(taskTemplateDto);

        // 任务模板数据列表
        List<TaskTemplateDto> taskTemplateDtos = null;

        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            // 查询任务模板列表数据
            taskTemplateDtos = taskInnerServiceSMOImpl.queryTaskTemplate(taskTemplateDto);
        } else {
            // 如果没有数据，返回空列表
            taskTemplateDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, taskTemplateDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}