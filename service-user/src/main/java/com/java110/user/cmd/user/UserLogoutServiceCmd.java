/**
 * 用户退出登录服务命令类
 * 
 * 该类处理用户退出登录的请求，主要负责验证请求参数和执行业务逻辑
 * 通过删除用户的token信息来实现用户退出登录功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 用户退出登录服务命令实现类
 * 服务码：user.service.logout
 */
@Java110Cmd(serviceCode = "user.service.logout")
public class UserLogoutServiceCmd extends Cmd {
    
    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必需的token参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含token节点
        Assert.jsonObjectHaveKey(reqJson, "token", "请求报文中未包含token节点请检查");
    }

    /**
     * 执行退出登录业务逻辑的方法
     * 
     * 从上下文中获取token信息，调用认证工厂删除token，
     * 并根据操作结果设置相应的响应实体
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;
        
        // 从命令上下文中获取当前用户的token ID
        String token = CmdContextUtils.getTokenId(context);
        
        try {
            // 调用认证工厂删除token信息，实现退出登录
            AuthenticationFactory.deleteToken(token);
            
            // 设置退出登录成功的响应
            responseEntity = new ResponseEntity<String>("退出登录成功", HttpStatus.OK);
        } catch (Exception e) {
            // 处理token删除过程中可能出现的异常
            // 包括无效签名/声明等认证相关异常
            responseEntity = new ResponseEntity<String>("退出登录失败，请联系管理员", HttpStatus.UNAUTHORIZED);
        }
        
        // 将响应实体设置到上下文中，供后续处理使用
        context.setResponseEntity(responseEntity);
    }
}