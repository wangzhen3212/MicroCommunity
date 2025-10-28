package com.java110.acct.cmd.alipay;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;

import java.text.ParseException;

/**
 * 支付宝获取用户OpenID命令类
 * 该类用于通过支付宝授权码获取用户的OpenID信息
 * 主要功能包括参数验证和调用支付宝API获取用户信息
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode alipay.getOpenIdFromAliPay
 */
@Java110Cmd(serviceCode = "alipay.getOpenIdFromAliPay")
public class GetOpenIdFromAliPayCmd extends Cmd {
    
    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含authCode字段
        Assert.hasKeyAndValue(reqJson, "authCode", "未包含authCode");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 执行命令方法
     * 通过支付宝API获取用户OpenID信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当获取用户信息失败时抛出异常
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求参数中获取小区ID
        String communityId = reqJson.getString("communityId");
        String openId = "";
        
        // 创建支付宝客户端实例，配置相关参数
        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                CommunitySettingFactory.getValue(communityId, "APP_ID"),          // 从配置中获取应用ID
                CommunitySettingFactory.getRemark(communityId, "APP_PRIVATE_KEY"), // 从配置中获取应用私钥
                "json",                                                           // 数据格式
                "UTF-8",                                                          // 字符编码
                CommunitySettingFactory.getRemark(communityId, "ALIPAY_PUBLIC_KEY"), // 从配置中获取支付宝公钥
                "RSA2");                                                          // 签名算法类型
        
        // 创建获取访问令牌请求
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        // 设置授权码参数
        request.setCode(reqJson.getString("authCode"));
        // 设置授权类型为授权码模式
        request.setGrantType("authorization_code");
        
        try {
            // 执行获取访问令牌请求
            AlipaySystemOauthTokenResponse oauthTokenResponse = alipayClient.execute(request);
            
            // 创建获取用户信息请求
            AlipayUserInfoShareRequest requestUserInfo = new AlipayUserInfoShareRequest();
            // 使用访问令牌执行获取用户信息请求
            AlipayUserInfoShareResponse userinfoShareResponse = alipayClient.execute(requestUserInfo, oauthTokenResponse.getAccessToken());
            // 从用户信息响应中获取用户ID（OpenID）
            openId = userinfoShareResponse.getUserId();
        } catch (Exception e) {
            // 处理异常情况，打印异常堆栈信息
            e.printStackTrace();
            // 抛出命令异常，包含错误信息
            throw new CmdException("获取用户信息失败"+e);
        }

        // 将获取到的OpenID设置到响应中
        context.setResponseEntity(ResultVo.createResponseEntity(openId));
    }
}