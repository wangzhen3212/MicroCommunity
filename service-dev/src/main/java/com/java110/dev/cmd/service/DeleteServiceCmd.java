package com.java110.dev.cmd.service;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.ServiceDto;
import com.java110.intf.community.IServiceInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.constant.StatusConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 删除服务命令类
 * 负责处理服务删除相关的业务逻辑
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"service.deleteService"
 */
@Java110Cmd(serviceCode = "service.deleteService")
public class DeleteServiceCmd extends Cmd {

    /**
     * 服务内部服务接口，用于调用服务相关的数据操作
     */
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 在执行业务逻辑前对请求参数进行校验
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 可以在此处添加其他参数验证，目前注释掉
        // Assert.hasKeyAndValue(reqJson, "xxx", "xxx");

        // 验证请求中必须包含serviceId参数
        Assert.hasKeyAndValue(reqJson, "serviceId", "服务ID不能为空");
    }

    /**
     * 执行删除服务的核心业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为ServiceDto对象
        ServiceDto serviceDto = BeanConvertUtil.covertBean(reqJson, ServiceDto.class);

        // 设置服务状态为无效状态（逻辑删除）
        serviceDto.setStatusCd(StatusConstant.STATUS_CD_INVALID);

        // 调用服务接口执行删除操作
        int count = serviceInnerServiceSMOImpl.deleteService(serviceDto);

        // 检查删除操作是否成功
        if (count < 1) {
            // 如果删除失败，抛出异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "删除数据失败");
        }

        // 创建成功的HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}