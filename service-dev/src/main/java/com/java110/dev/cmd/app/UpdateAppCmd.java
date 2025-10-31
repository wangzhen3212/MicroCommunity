package com.java110.dev.cmd.app;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.app.AppDto;
import com.java110.intf.community.IAppInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 更新应用信息命令类
 * 
 * 该类负责处理应用信息的更新操作，包括参数验证和业务逻辑执行
 * 使用@Java110Cmd注解标识服务代码为"app.updateApp"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "app.updateApp")
public class UpdateAppCmd extends Cmd {

    /**
     * 应用内部服务接口，用于执行应用相关的数据库操作
     */
    @Autowired
    private IAppInnerServiceSMO appInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：
     * - appId: 应用ID，不能为空
     * - name: 应用名称，必填项
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含appId字段
        Assert.hasKeyAndValue(reqJson, "appId", "应用Id不能为空");
        // 验证请求参数中必须包含name字段
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写应用名称");
    }

    /**
     * 执行命令方法
     * 
     * 将请求参数转换为AppDto对象，调用服务层更新应用信息
     * 根据更新结果返回相应的响应
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为AppDto对象
        AppDto appDto = BeanConvertUtil.covertBean(reqJson, AppDto.class);
        
        // 调用服务层更新应用信息，返回受影响的行数
        int count = appInnerServiceSMOImpl.updateApp(appDto);
        
        // 检查更新是否成功，如果受影响行数小于1，则认为更新失败
        if (count < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "修改数据失败");
        }
        
        // 创建成功的HTTP响应，返回空内容和200状态码
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}