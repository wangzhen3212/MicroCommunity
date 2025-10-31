package com.java110.dev.cmd.service;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
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
 * 更新服务信息命令类
 * 负责处理服务信息的更新操作
 * 
 * @Java110Cmd 注解声明该类为命令处理器，serviceCode指定处理的服务编码
 */
@Java110Cmd(serviceCode = "service.updateService")
public class UpdateServiceCmd extends Cmd {

    // 注入服务内部服务接口
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 对请求参数进行校验，确保必填参数不为空
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "serviceId", "服务ID不能为空");
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
     * 执行服务信息的更新操作
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为ServiceDto对象
        ServiceDto serviceDto = BeanConvertUtil.covertBean(reqJson, ServiceDto.class);

        // 调用服务更新接口
        int count = serviceInnerServiceSMOImpl.updateService(serviceDto);

        // 检查更新结果
        if (count < 1) {
            // 更新失败，抛出异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "修改数据失败");
        }

        // 构建成功响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}