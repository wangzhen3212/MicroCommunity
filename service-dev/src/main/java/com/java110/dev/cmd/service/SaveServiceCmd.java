package com.java110.dev.cmd.service;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.service.ServiceDto;
import com.java110.intf.community.IServiceInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 保存服务命令类
 * 处理服务信息的保存操作
 * 
 * @Java110Cmd 注解定义服务编码为 "service.saveService"
 */
@Java110Cmd(serviceCode = "service.saveService")
public class SaveServiceCmd extends Cmd {

    // 服务内部服务接口，用于操作服务数据
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写服务名称");
        Assert.hasKeyAndValue(reqJson, "serviceCode", "必填，请填写服务编码如 service.saveService");
        Assert.hasKeyAndValue(reqJson, "businessTypeCd", "可填，请填写秘钥，如果填写了需要加密传输");
        Assert.hasKeyAndValue(reqJson, "seq", "必填，请填写序列");
        Assert.hasKeyAndValue(reqJson, "isInstance", "可填，请填写实例 Y 或N");
        Assert.hasKeyAndValue(reqJson, "method", "必填，请填写调用方式");
        Assert.hasKeyAndValue(reqJson, "timeout", "必填，请填写超时时间");
        Assert.hasKeyAndValue(reqJson, "retryCount", "必填，请填写重试次数");
        Assert.hasKeyAndValue(reqJson, "provideAppId", "必填，请填写提供服务");
    }

    /**
     * 命令执行方法
     * 执行服务信息的保存操作
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当保存操作失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON数据转换为ServiceDto对象
        ServiceDto serviceDto = BeanConvertUtil.covertBean(reqJson, ServiceDto.class);

        // 生成服务ID
        serviceDto.setServiceId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_service_id));

        // 调用服务保存接口
        int count = serviceInnerServiceSMOImpl.saveService(serviceDto);

        // 检查保存结果
        if (count < 1) {
            // 保存失败，抛出异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "保存数据失败");
        }

        // 创建成功响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}