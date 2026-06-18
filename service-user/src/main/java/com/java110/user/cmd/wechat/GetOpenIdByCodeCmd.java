package com.java110.user.cmd.wechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.po.user.UserAttrPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.List;

/**
 * 根据小程序授权码获取微信用户OpenID的命令类
 * 主要功能：通过微信小程序的code获取用户的openid，并根据用户登录状态决定是否将openid保存到用户属性中
 * 注意：此接口专为商城场景设计，支持已登录和未登录用户的openid获取
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode wechat.getOpenIdByCode
 */
@Java110Cmd(serviceCode = "wechat.getOpenIdByCode")
public class GetOpenIdByCodeCmd extends Cmd {
    
    /** 日志记录器 */
    private final static Logger logger = LoggerFactory.getLogger(GetOpenIdByCodeCmd.class);
    
    /** 用于发送HTTP请求的RestTemplate */
    @Autowired
    private RestTemplate outRestTemplate;

    /** 小程序信息服务接口 */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /** 用户属性服务接口 */
    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的code和appId参数
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含code参数
        Assert.hasKeyAndValue(reqJson, "code", "未包含code");
        // 验证请求参数中必须包含appId参数
        Assert.hasKeyAndValue(reqJson, "appId", "未包含小程序ID");
    }

    /**
     * 命令执行方法
     * 根据小程序code获取微信openid，并根据用户登录状态决定是否保存到用户属性中
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");

        String appId = "";
        String appSecret = "";
        
        // 根据appId类型获取对应的微信配置信息
        if ("MALL".equals(reqJson.getString("appId"))) {
            // 如果是商城小程序，从缓存中获取配置信息
            appId = MappingCache.getValue(MappingConstant.MALL_WECHAT_DOMAIN, "wechatAppId");
            appSecret = MappingCache.getValue(MappingConstant.MALL_WECHAT_DOMAIN, "wechatAppSecret");
        } else {
            // 如果是其他小程序，从数据库中查询配置信息
            SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
            smallWeChatDto.setAppId(reqJson.getString("appId"));
            List<SmallWeChatDto> smallWeChatDtos = smallWechatV1InnerServiceSMOImpl.querySmallWechats(smallWeChatDto);

            // 验证是否配置了小程序信息
            if (smallWeChatDtos == null || smallWeChatDtos.size() < 1) {
                throw new IllegalArgumentException("未配置小程序信息");
            }

            appId = smallWeChatDtos.get(0).getAppId();
            appSecret = smallWeChatDtos.get(0).getAppSecret();
        }

        // 调用微信接口获取openid
        String code = reqJson.getString("code");
        String urlString = "https://api.weixin.qq.com/sns/jscode2session?appid={appId}&secret={secret}&js_code={code}&grant_type={grantType}";
        String response = outRestTemplate.getForObject(
                urlString, String.class,
                appId,
                appSecret,
                code,
                "authorization_code");

        logger.debug("微信返回报文：" + response);

        // 解析微信接口返回的JSON数据
        JSONObject responseObj = JSONObject.parseObject(response);

        // 检查微信接口返回是否包含错误码
        if (responseObj.containsKey("errcode") && !"0".equals(responseObj.getString("errcode"))) {
            throw new IllegalArgumentException("微信验证失败，可能是code失效" + responseObj);
        }

        // 从响应中提取openid
        String openId = responseObj.getString("openid");

        // 如果用户未登录或用户ID异常，直接返回openid
        if (StringUtil.isEmpty(userId) || userId.startsWith("-")) {
            context.setResponseEntity(ResultVo.createResponseEntity(openId));
            return;
        }

        // 查询用户是否已存在商城openid属性
        UserAttrDto userAttrDto = new UserAttrDto();
        userAttrDto.setUserId(userId);
        userAttrDto.setSpecCd(UserAttrDto.SPEC_MALL_OPEN_ID);
        List<UserAttrDto> userAttrDtos = userAttrV1InnerServiceSMOImpl.queryUserAttrs(userAttrDto);
        
        if (userAttrDtos == null || userAttrDtos.size() < 1) {
            // 如果不存在，创建新的用户属性记录
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setAttrId(GenerateCodeFactory.getAttrId());
            userAttrPo.setUserId(userId);
            userAttrPo.setSpecCd(UserAttrDto.SPEC_MALL_OPEN_ID);
            userAttrPo.setValue(openId);
            userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
        } else {
            // 如果已存在，更新现有的用户属性记录
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setAttrId(userAttrDtos.get(0).getAttrId());
            userAttrPo.setValue(openId);
            userAttrV1InnerServiceSMOImpl.updateUserAttr(userAttrPo);
        }
        
        // 返回openid给前端
        context.setResponseEntity(ResultVo.createResponseEntity(openId));
    }
}