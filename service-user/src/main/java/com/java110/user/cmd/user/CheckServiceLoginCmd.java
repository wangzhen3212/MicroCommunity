/**
 * 服务登录检查命令类
 * 
 * 该类用于验证服务登录请求中的token有效性，解析token中的用户信息
 * 如果token验证成功，返回用户ID信息；如果验证失败，返回认证失败信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.naming.AuthenticationException;
import java.text.ParseException;
import java.util.Map;

/**
 * 服务登录检查命令实现类
 * 通过@Java110Cmd注解声明服务编码为"check.service.login"
 */
@Java110Cmd(serviceCode = "check.service.login")
public class CheckServiceLoginCmd extends Cmd {
    
    /**
     * 参数验证方法
     * 
     * 该方法用于验证请求参数的有效性，当前实现为空方法
     * 可根据具体业务需求添加参数验证逻辑
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 当前方法为空实现，可根据业务需求添加参数验证逻辑
        // 例如：验证必填字段、字段格式、业务规则等
    }

    /**
     * 命令执行方法
     * 
     * 主要功能：
     * 1. 验证请求中是否包含token参数
     * 2. 使用AuthenticationFactory验证token的有效性
     * 3. 解析token中的用户声明信息
     * 4. 根据验证结果返回相应的响应实体
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当解析过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求报文中必须包含token节点
        Assert.jsonObjectHaveKey(reqJson,"token","请求报文中未包含token 节点请检查");
        
        ResponseEntity responseEntity = null;
        
        try {
            // 从请求JSON中获取token字符串
            String token = reqJson.getString("token");
            // 使用认证工厂验证token并解析声明信息
            Map<String, String> claims = AuthenticationFactory.verifyToken(token);
            
            // 检查解析出的声明信息是否为空
            if(claims == null || claims.isEmpty()){
                // 如果声明信息为空，抛出认证异常
                throw new AuthenticationException("认证失败，从token中解析到信息为空");
            }
            
            // 创建结果JSON对象
            JSONObject resultInfo = new JSONObject();
            // 从声明信息中获取用户ID并放入结果对象
            resultInfo.put("userId",claims.get("userId"));
            
            // 创建成功的HTTP响应实体，状态码为200 OK
            responseEntity = new ResponseEntity<String>(resultInfo.toJSONString(), HttpStatus.OK);
            
        } catch (Exception e) {
            // 捕获所有异常，包括token验证失败、签名无效、声明信息无效等情况
            // 创建认证失败的HTTP响应实体，状态码为401 UNAUTHORIZED
            responseEntity = new ResponseEntity<String>("认证失败，不是有效的token", HttpStatus.UNAUTHORIZED);
        }
        
        // 将响应实体设置到上下文中，供后续处理使用
        context.setResponseEntity(responseEntity);
    }
}