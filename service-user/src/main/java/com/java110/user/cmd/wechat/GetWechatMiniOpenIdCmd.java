package com.java110.user.cmd.wechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.List;

/**
 * 微信小程序OpenID获取命令类
 * 该命令用于通过微信小程序的授权码(code)获取用户的OpenID
 * 支持从不同配置源获取小程序配置信息，包括商城配置和社区配置
 * 
 * @serviceCode wechat.getWechatMiniOpenId
 */
@Java110Cmd(serviceCode = "wechat.getWechatMiniOpenId")
public class GetWechatMiniOpenIdCmd extends Cmd {
    private final static Logger logger = LoggerFactory.getLogger(GetWechatMiniOpenIdCmd.class);

    @Autowired
    private RestTemplate outRestTemplate; // 用于发送HTTP请求的RestTemplate

    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl; // 小程序配置信息服务

    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl; // 用户属性服务

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的code和appId字段
     *
     * @param event   CmdEvent事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException    命令异常
     * @throws ParseException  解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含code字段
        Assert.hasKeyAndValue(reqJson, "code", "未包含code");
        // 验证请求参数中必须包含appId字段
        Assert.hasKeyAndValue(reqJson, "appId", "未包含小程序ID");
    }

    /**
     * 命令执行方法
     * 根据小程序配置信息，调用微信API获取用户的OpenID
     *
     * @param event   CmdEvent事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException    命令异常
     * @throws ParseException  解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        String appId = "";     // 小程序AppID
        String appSecret = ""; // 小程序AppSecret

        // 判断是否为商城小程序
        if ("MALL".equals(reqJson.getString("appId"))) {
            // 从商城配置中获取微信小程序的AppID和AppSecret
            appId = MappingCache.getValue(MappingConstant.MALL_WECHAT_DOMAIN, "wechatAppId");
            appSecret = MappingCache.getValue(MappingConstant.MALL_WECHAT_DOMAIN, "wechatAppSecret");
        } else {
            // 创建小程序查询对象
            SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
            smallWeChatDto.setAppId(reqJson.getString("appId"));
            
            // 如果请求中包含社区ID，则设置查询条件
            String communityId = reqJson.getString("communityId");
            if (!StringUtil.isEmpty(communityId)) {
                smallWeChatDto.setObjId(communityId);
            }
            
            // 查询小程序配置信息
            List<SmallWeChatDto> smallWeChatDtos = smallWechatV1InnerServiceSMOImpl.querySmallWechats(smallWeChatDto);

            // 根据查询结果决定使用哪种配置
            if (ListUtil.isNull(smallWeChatDtos)) {
                // 如果未查询到特定小程序配置，使用全局默认配置
                appId = MappingCache.getValue(WechatConstant.WECHAT_DOMAIN, "appId");
                appSecret = MappingCache.getValue(WechatConstant.WECHAT_DOMAIN, "appSecret");
            } else {
                // 使用查询到的第一个小程序配置
                appId = smallWeChatDtos.get(0).getAppId();
                appSecret = smallWeChatDtos.get(0).getAppSecret();
            }
        }

        ResponseEntity<String> responseEntity;
        String code = reqJson.getString("code"); // 获取前端传来的授权码
        
        // 构建微信API请求URL
        String urlString = "https://api.weixin.qq.com/sns/jscode2session?appid={appId}&secret={secret}&js_code={code}&grant_type={grantType}";
        
        // 调用微信API获取session信息
        String response = outRestTemplate.getForObject(
                urlString, String.class,
                appId,
                appSecret,
                code,
                "authorization_code");

        logger.debug("微信返回报文：" + response);

        // 解析微信API返回的JSON响应
        JSONObject responseObj = JSONObject.parseObject(response);

        // 检查微信API返回是否包含错误码
        if (responseObj.containsKey("errcode") && !"0".equals(responseObj.getString("errcode"))) {
            throw new IllegalArgumentException("微信验证失败，可能是code失效" + responseObj);
        }

        // 从响应中提取OpenID
        String openId = responseObj.getString("openid");
        // String unionId = responseObj.getString("unionid"); // 如果需要获取UnionID可以取消注释
        
        // 将session信息缓存到Redis中，有效期与token相同
        CommonCache.setValue("code2Session"+openId, responseObj.toJSONString(), CommonCache.TOKEN_EXPIRE_TIME);

        // 将OpenID返回给前端
        context.setResponseEntity(ResultVo.createResponseEntity(openId));
    }
}