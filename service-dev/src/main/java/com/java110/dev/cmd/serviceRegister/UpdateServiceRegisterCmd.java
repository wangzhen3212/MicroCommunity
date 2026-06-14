package com.java110.dev.cmd.serviceRegister;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.RouteDto;
import com.java110.intf.community.IRouteInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 更新服务注册信息命令类
 * 
 * 负责处理服务注册信息的更新操作，包括参数验证和业务逻辑执行
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码
 */
@Java110Cmd(serviceCode = "serviceRegister.updateServiceRegister")
public class UpdateServiceRegisterCmd extends Cmd {
    
    /**
     * 路由服务接口，用于操作路由数据
     */
    @Autowired
    private IRouteInnerServiceSMO routeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "id", "绑定ID不能为空");
        Assert.hasKeyAndValue(reqJson, "appId", "必填，请填写应用ID");
        Assert.hasKeyAndValue(reqJson, "serviceId", "必填，请填写服务ID");
        Assert.hasKeyAndValue(reqJson, "orderTypeCd", "必填，请填写订单类型");
        Assert.hasKeyAndValue(reqJson, "invokeLimitTimes", "必填，请填写调用次数");
        Assert.hasKeyAndValue(reqJson, "invokeModel", "可填，请填写消息队列，订单在异步调用时使用");
    }

    /**
     * 命令执行方法
     * 
     * 执行服务注册信息的更新操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON数据转换为RouteDto对象
        RouteDto routeDto = BeanConvertUtil.covertBean(reqJson, RouteDto.class);

        // 调用服务更新路由信息
        int count = routeInnerServiceSMOImpl.updateRoute(routeDto);

        // 检查更新是否成功
        if (count < 1) {
            // 更新失败，抛出异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "修改数据失败");
        }

        // 构建成功响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}