/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.api.controller.app;


import com.java110.api.smo.wechatGateway.IWechatGatewaySMO;
import com.java110.core.base.controller.BaseController;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.util.StringUtil;
import com.java110.utils.util.XmlUtil;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @ClassName WechatController
 * @Description TODO 微信 对接类
 * @Author wuxw
 * @Date 2020/6/13 21:48
 * @Version 1.0
 * add by wuxw 2020/6/13
 **/
@RestController
@RequestMapping(path = "/app/wechat")
public class WechatGatewayController extends BaseController {

    private final static Logger logger = LoggerFactory.getLogger(WechatGatewayController.class);

    private final static String MSG_ILLEGAL_SIGN = "亲，非法访问，签名失败";
    private final static String MSG_ILLEGAL_PARAM = "亲，非法访问，参数错误";
    private final static String MSG_ILLEGAL_XML = "亲，非法访问，报文格式错误";
    private final static String MSG_EXPIRED = "亲，非法访问，请求已过期";

    /**
     * 时间戳有效期 5 分钟
     */
    private final static long TIMESTAMP_VALID_SECOND = 300L;

    private final static int MAX_ECHOSTR_LENGTH = 512;

    private final static Pattern APP_ID_PATTERN = Pattern.compile("^wx[0-9a-zA-Z]{14,18}$");

    @Autowired
    private IWechatGatewaySMO wechatGatewaySMOImpl;


    /**
     * 微信登录接口
     *
     * @param request
     */
    @RequestMapping(path = "/gateway/{appId}", method = RequestMethod.GET)
    public ResponseEntity<String> gateway(@PathVariable String appId, HttpServletRequest request) {

        if (!checkAppId(appId)) {
            logger.warn("微信 gateway 请求 appId 不合法，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_PARAM, HttpStatus.BAD_REQUEST);
        }

        String token = MappingCache.getValue(WechatConstant.WECHAT_DOMAIN, WechatConstant.TOKEN);
        if (StringUtil.isEmpty(token)) {
            logger.warn("微信 token 未配置，拒绝 gateway 请求，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_SIGN, HttpStatus.OK);
        }

        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");
        String echostr = request.getParameter("echostr");

        if (StringUtil.isEmpty(signature) || StringUtil.isEmpty(timestamp) || StringUtil.isEmpty(nonce)) {
            logger.warn("微信 gateway 请求缺少签名参数，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_PARAM, HttpStatus.OK);
        }

        if (!checkTimestamp(timestamp)) {
            logger.warn("微信 gateway 请求 timestamp 已过期，appId={}，timestamp={}", appId, timestamp);
            return new ResponseEntity<>(MSG_EXPIRED, HttpStatus.OK);
        }

        if (!checkSignature(token, signature, timestamp, nonce)) {
            logger.warn("微信 gateway 请求签名校验失败，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_SIGN, HttpStatus.OK);
        }

        return new ResponseEntity<>(escapeEchostr(echostr), HttpStatus.OK);
    }

    /**
     * 微信消息、事件接收接口
     *
     * @param request
     */
    @RequestMapping(path = "/gateway/{appId}", method = RequestMethod.POST)
    public ResponseEntity<String> gateway(@RequestBody String param,
                                          @PathVariable String appId,
                                          HttpServletRequest request) {

        if (!checkAppId(appId)) {
            logger.warn("微信 gateway 请求 appId 不合法，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_PARAM, HttpStatus.BAD_REQUEST);
        }

        if (!checkContentType(request.getContentType())) {
            logger.warn("微信 gateway 请求 Content-Type 不合法，appId={}，contentType={}", appId, request.getContentType());
            return new ResponseEntity<>(MSG_ILLEGAL_XML, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        String token = MappingCache.getValue(WechatConstant.WECHAT_DOMAIN, WechatConstant.TOKEN);
        if (StringUtil.isEmpty(token)) {
            logger.warn("微信 token 未配置，拒绝 gateway 请求，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_SIGN, HttpStatus.OK);
        }

        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");

        if (StringUtil.isEmpty(signature) || StringUtil.isEmpty(timestamp) || StringUtil.isEmpty(nonce)) {
            logger.warn("微信 gateway 请求缺少签名参数，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_PARAM, HttpStatus.OK);
        }

        if (!checkTimestamp(timestamp)) {
            logger.warn("微信 gateway 请求 timestamp 已过期，appId={}，timestamp={}", appId, timestamp);
            return new ResponseEntity<>(MSG_EXPIRED, HttpStatus.OK);
        }

        if (!checkSignature(token, signature, timestamp, nonce)) {
            logger.warn("微信 gateway 请求签名校验失败，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_SIGN, HttpStatus.OK);
        }

        if (StringUtil.isEmpty(param)) {
            return new ResponseEntity<>("未输入任何内容", HttpStatus.OK);
        }

        if (param.length() > XmlUtil.MAX_XML_LENGTH) {
            logger.warn("微信 gateway 请求报文超长，appId={}，length={}", appId, param.length());
            return new ResponseEntity<>(MSG_ILLEGAL_XML, HttpStatus.PAYLOAD_TOO_LARGE);
        }

        Element root;
        try {
            root = XmlUtil.parse(param);
        } catch (IllegalArgumentException e) {
            logger.warn("微信 gateway 报文解析失败，appId={}，reason={}", appId, e.getMessage());
            return new ResponseEntity<>(MSG_ILLEGAL_XML, HttpStatus.OK);
        }

        String fromUserName = root.elementText("FromUserName");
        String toUserName = root.elementText("ToUserName");
        String keyword = root.elementTextTrim("Content");
        String msgType = root.elementTextTrim("MsgType");
        String event = root.elementText("Event");
        String eventKey = root.elementText("EventKey");

        if (StringUtil.isEmpty(fromUserName) || StringUtil.isEmpty(toUserName)) {
            logger.warn("微信 gateway 报文缺少收发件人，appId={}", appId);
            return new ResponseEntity<>(MSG_ILLEGAL_XML, HttpStatus.OK);
        }

        try {
            return wechatGatewaySMOImpl.gateway(fromUserName, toUserName, keyword, msgType, event, eventKey, appId);
        } catch (Exception e) {
            logger.error("微信 gateway 处理失败，appId={}，msgType={}，event={}", appId, msgType, event, e);
            return new ResponseEntity<>("亲，处理失败，请稍后重试", HttpStatus.OK);
        }
    }

    /**
     * 校验 appId 格式，微信 appId 以 wx 开头
     *
     * @param appId 公众号 appId
     * @return true 合法
     */
    private boolean checkAppId(String appId) {
        return !StringUtil.isEmpty(appId) && APP_ID_PATTERN.matcher(appId).matches();
    }

    /**
     * 校验报文类型，只接受微信推送的 text/xml、application/xml
     *
     * @param contentType 请求头 Content-Type
     * @return true 合法
     */
    private boolean checkContentType(String contentType) {
        if (StringUtil.isEmpty(contentType)) {
            return false;
        }
        String lowerContentType = contentType.toLowerCase(Locale.ROOT);
        return lowerContentType.startsWith("text/xml") || lowerContentType.startsWith("application/xml");
    }

    /**
     * 校验时间戳有效期，防重放
     *
     * @param timestamp 微信推送时间戳（秒）
     * @return true 有效
     */
    private boolean checkTimestamp(String timestamp) {
        Long timestampValue;
        try {
            timestampValue = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        return Math.abs(System.currentTimeMillis() / 1000 - timestampValue) <= TIMESTAMP_VALID_SECOND;
    }

    /**
     * 校验微信签名，恒定时间比较避免时序侧信道
     *
     * @param token     公众号 token
     * @param signature 微信签名
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @return true 签名正确
     */
    private boolean checkSignature(String token, String signature, String timestamp, String nonce) {
        String[] ss = new String[]{token, timestamp, nonce};
        Arrays.sort(ss);
        StringBuilder sourceString = new StringBuilder();
        for (String s : ss) {
            sourceString.append(s);
        }
        String signature1 = AuthenticationFactory.SHA1Encode(sourceString.toString()).toLowerCase(Locale.ROOT);
        return MessageDigest.isEqual(signature1.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * echostr 输出转义并限制长度，避免反射型注入
     *
     * @param echostr 微信随机串
     * @return 转义后内容
     */
    private String escapeEchostr(String echostr) {
        if (StringUtil.isEmpty(echostr)) {
            return "";
        }
        String value = echostr.length() > MAX_ECHOSTR_LENGTH ? echostr.substring(0, MAX_ECHOSTR_LENGTH) : echostr;
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }



}
