package com.java110.dev.cmd.task;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.task.TaskTemplateSpecDto;
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
 * 查询任务模板规格列表命令类
 * 处理服务编码为"task.listTaskTemplateSpec"的请求
 * 
 * @author Java110
 */
@Java110Cmd(serviceCode = "task.listTaskTemplateSpec")
public class ListTaskTemplateSpecCmd extends Cmd {

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
     * 查询任务模板规格列表并返回分页结果
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为任务模板规格DTO对象
        TaskTemplateSpecDto taskTemplateSpecDto = BeanConvertUtil.covertBean(reqJson, TaskTemplateSpecDto.class);

        // 查询任务模板规格总数
        int count = taskInnerServiceSMOImpl.queryTaskTemplateSpecCount(taskTemplateSpecDto);

        // 定义任务模板规格列表
        List<TaskTemplateSpecDto> taskTemplateSpecDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 如果存在数据，则查询具体的数据列表
            taskTemplateSpecDtos = taskInnerServiceSMOImpl.queryTaskTemplateSpec(taskTemplateSpecDto);
        } else {
            // 如果没有数据，返回空列表
            taskTemplateSpecDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数/每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, taskTemplateSpecDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}