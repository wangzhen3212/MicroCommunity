package com.java110.core.factory;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Synchronized;
import com.java110.utils.cache.JWTCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.StringUtil;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

public class WechatFactory {

    private static Logger logger = LoggerFactory.getLogger(WechatFactory.class);

    private static final String password = "you are bad boy!";

    private static final String WECHAT = "WECHAT";

    private static final String SPLIT_STUB = "-";

    public static String formatText(String toUserName, String fromUserName, String content) {
        String str = "";
        str = String.format("<xml><ToUserName><![CDATA[%1$s]]></ToUserName><FromUserName><![CDATA[%2$s]]></FromUserName><CreateTime>%3$s</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[%4$s]]></Content><FuncFlag>0</FuncFlag></xml>", new Object[]{
                fromUserName, toUserName, Long.valueOf((new Date()).getTime()), content
        });
        return str;
    }

    /**
     * 获取accessToken
     *
     * @param appId     公众号ID
     * @param appSecure
     * @return
     */
    @Java110Synchronized(value = "appId")
    public static String getAccessToken(String appId, String appSecure) {
        String accessToken = JWTCache.getValue(WECHAT + appId);
        if (StringUtil.isEmpty(accessToken)) {
            return refreshAccessToken(appId, appSecure);
        }
        return accessToken;
    }

    /**
     * 刷新 access_token（使用 stable_token 接口，防止多进程相互刷新导致旧 token 失效）
     *
     * @param appId     应用ID
     * @param appSecure 应用秘钥
     * @return
     */
    private static String refreshAccessToken(String appId, String appSecure) {
        // 优先使用 DB 配置的 stable_token URL，如未配置则使用默认稳定版接口
        String url = MappingCache.getRemark(WechatConstant.WECHAT_DOMAIN, WechatConstant.STABLE_ACCESS_TOKEN_URL);
        if (StringUtil.isEmpty(url)) {
            url = WechatConstant.STABLE_ACCESS_TOKEN;
        }
        RestTemplate outRestTemplate = ApplicationContextFactory.getBean("outRestTemplate", RestTemplate.class);
        if (outRestTemplate == null) {
            throw new IllegalArgumentException("查询token 失败");
        }

        JSONObject paramIn = new JSONObject();
        paramIn.put("grant_type", "client_credential");
        paramIn.put("appid", appId);
        paramIn.put("secret", appSecure);

        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, paramIn.toJSONString(), String.class);
        logger.debug("获取stable_access_token 入参：" + paramIn.toJSONString() + " 返回参数" + responseEntity);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException("查询token 失败");
        }

        JSONObject responseObj = JSONObject.parseObject(responseEntity.getBody());

        if (responseObj.containsKey("access_token")) {
            String accessToken = responseObj.getString("access_token");
            int expiresIn = responseObj.getInteger("expires_in");
            JWTCache.setValue(WECHAT + appId, accessToken, expiresIn - 200);
            return accessToken;
        }

        // 微信返回错误，记录真实错误信息
        String errCode = responseObj.getString("errcode");
        String errMsg = responseObj.getString("errmsg");
        logger.error("获取微信access_token失败, appId={}, errcode={}, errmsg={}", appId, errCode, errMsg);
        throw new IllegalArgumentException("获取微信access_token失败, errcode=" + errCode + ", errmsg=" + errMsg);
    }

    /**
     * 清除缓存的 access_token（当调用方收到 40001 时可调用此方法，下次 getAccessToken 将重新获取）
     */
    public static void clearAccessTokenCache(String appId) {
        JWTCache.removeValue(WECHAT + appId);
        logger.info("已清除微信access_token缓存, appId={}", appId);
    }

    /**
     * 获取微信页面ID
     *
     * @param appId
     * @return
     */
    public static String getWId(String appId) {
        return AuthenticationFactory.encrypt(password, appId);
    }

    /**
     * 获取微信AppId
     *
     * @param wId
     * @return
     */
    public static String getAppId(String wId) {
        wId = wId.replace(" ", "+");
        return AuthenticationFactory.decrypt(password, wId);
    }


    public static String getPhoneNumberBeanS5(String decryptData, String key, String iv) {
        String resultMessage = decryptS5(decryptData, "UTF-8", key, iv);
        return resultMessage;
    }

    public static String decryptS5(String sSrc, String encodingFormat, String sKey, String ivParameter) {
        try {
            Base64 base64 = new Base64();
            byte[] raw = base64.decode(sKey);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            IvParameterSpec iv = new IvParameterSpec(base64.decode(ivParameter));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] myendicod = base64.decode(sSrc);
            byte[] original = cipher.doFinal(myendicod);
            String originalString = new String(original, encodingFormat);
            return originalString;
        } catch (Exception ex) {
            return null;
        }
    }

}