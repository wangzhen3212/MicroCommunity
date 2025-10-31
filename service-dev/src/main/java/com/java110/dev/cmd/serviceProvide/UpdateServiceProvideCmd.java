package com.java110.dev.cmd.serviceProvide;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.ServiceProvideDto;
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
 * 更新服务提供命令类
 * 负责处理服务提供信息的更新操作
 * 
 * @Java110Cmd 注解声明这是一个Java110框架的命令类，serviceCode指定服务编码
 */
@Java110Cmd(serviceCode = "serviceProvide.updateServiceProvide")
public class UpdateServiceProvideCmd extends Cmd {

    // 注入服务内部服务接口
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 对请求参数进行校验，确保必填字段不为空
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证提供ID不能为空
        Assert.hasKeyAndValue(reqJson, "id", "提供ID不能为空");
        // 验证服务名称不能为空
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写服务名称");
        // 验证服务编码不能为空
        Assert.hasKeyAndValue(reqJson, "serviceCode", "必填，请填写服务编码");
        // 验证参数不能为空
        Assert.hasKeyAndValue(reqJson, "params", "必填，请填写参数");
        // 验证查询模式不能为空
        Assert.hasKeyAndValue(reqJson, "queryModel", "必填，请选择是否显示菜单");
    }

    /**
     * 命令执行方法
     * 执行服务提供信息的更新操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当执行过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON数据转换为ServiceProvideDto对象
        ServiceProvideDto serviceProvideDto = BeanConvertUtil.covertBean(reqJson, ServiceProvideDto.class);

        // 调用服务更新方法，返回受影响的行数
        int count = serviceInnerServiceSMOImpl.updateServiceProvide(serviceProvideDto);
        
        // 检查更新是否成功，如果受影响行数小于1，则抛出异常
        if (count < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "修改数据失败");
        }

        // 创建成功的HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}