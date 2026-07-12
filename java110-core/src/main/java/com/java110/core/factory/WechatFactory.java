package com.java110.core.factory;

import com.alibaba.fastjson.JSONObject;
import com.java110.utils.cache.JWTCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.lock.DistributedLock;
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

    private static final String WECHAT_TOKEN_LOCK = "WECHAT_TOKEN_LOCK:";

    private static final String SPLIT_STUB = "-";

    private static final int MIN_EXPIRES_IN = 300;

    private static final int LOCK_EXPIRE_TIME = 5000;

    private static final int MAX_RETRY_COUNT = 10;

    private static final int RETRY_INTERVAL_MS = 100;

    public static String formatText(String toUserName, String fromUserName, String content) {
        String str = "";
        str = String.format("<xml><ToUserName><![CDATA[%1$s]]></ToUserName><FromUserName><![CDATA[%2$s]]></FromUserName><CreateTime>%3$s</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[%4$s]]></Content><FuncFlag>0</FuncFlag></xml>", new Object[]{
                fromUserName, toUserName, Long.valueOf((new Date()).getTime()), content
        });
        return str;
    }

    public static void invalidateAccessToken(String appId) {
        JWTCache.removeValue(WECHAT + appId);
    }

    /**
     * 获取accessToken
     *
     * @param appId     公众号ID
     * @param appSecure 公众号秘钥
     * @return access_token
     */
    public static String getAccessToken(String appId, String appSecure) {
        String accessToken = JWTCache.getValue(WECHAT + appId);
        if (!StringUtil.isEmpty(accessToken)) {
            return accessToken;
        }

        String lockKey = WECHAT_TOKEN_LOCK + appId;
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            String requestId = DistributedLock.getLockUUID();
            try {
                if (DistributedLock.waitGetDistributedLock(lockKey, requestId, LOCK_EXPIRE_TIME)) {
                    accessToken = JWTCache.getValue(WECHAT + appId);
                    if (!StringUtil.isEmpty(accessToken)) {
                        return accessToken;
                    }
                    return refreshAccessToken(appId, appSecure);
                }
                retryCount++;
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalArgumentException("获取access_token时线程被中断", e);
            } finally {
                DistributedLock.releaseDistributedLock(lockKey, requestId);
            }
        }

        throw new IllegalArgumentException("获取access_token失败，重试" + MAX_RETRY_COUNT + "次后仍未获取到锁");
    }

    /**
     * 刷新 access_token
     *
     * @param appId     应用ID
     * @param appSecure 应用秘钥
     * @return access_token
     */
    private static String refreshAccessToken(String appId, String appSecure) {
        String getAccessToken = MappingCache.getRemark(WechatConstant.WECHAT_DOMAIN, WechatConstant.GET_ACCESS_TOKEN_URL);
        if (StringUtil.isEmpty(getAccessToken)) {
            getAccessToken = WechatConstant.GET_ACCESS_TOKEN;
        }
        String url = getAccessToken.replace("APPID", appId)
                .replace("SECRET", appSecure);
        RestTemplate outRestTemplate = ApplicationContextFactory.getBean("outRestTemplate", RestTemplate.class);
        if (outRestTemplate == null) {
            throw new IllegalArgumentException("获取access_token失败：outRestTemplate Bean不存在");
        }
        ResponseEntity<String> responseEntity = outRestTemplate.getForEntity(url, String.class);
        logger.debug("获取access_token appId: {}, HTTP状态: {}", appId, responseEntity.getStatusCode());

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException("获取access_token失败，HTTP状态码: " + responseEntity.getStatusCode());
        }

        if (responseEntity.getBody() == null) {
            throw new IllegalArgumentException("获取access_token失败，微信返回空响应");
        }

        JSONObject responseObj = JSONObject.parseObject(responseEntity.getBody());

        if (responseObj.containsKey("errcode") && responseObj.getIntValue("errcode") != 0) {
            throw new IllegalArgumentException("微信返回错误: errcode=" + responseObj.getIntValue("errcode")
                    + ", errmsg=" + responseObj.getString("errmsg"));
        }

        if (!responseObj.containsKey("access_token")) {
            throw new IllegalArgumentException("获取access_token失败，微信返回数据不包含access_token字段");
        }

        String accessToken = responseObj.getString("access_token");
        if (StringUtil.isEmpty(accessToken)) {
            throw new IllegalArgumentException("获取access_token失败，access_token为空");
        }

        int expiresIn = responseObj.getInteger("expires_in");
        if (expiresIn <= MIN_EXPIRES_IN) {
            throw new IllegalArgumentException("获取access_token失败，微信返回的expires_in无效: " + expiresIn);
        }

        JWTCache.setValue(WECHAT + appId, accessToken, expiresIn - 200);
        logger.debug("获取access_token成功 appId: {}, expires_in: {}", appId, expiresIn);
        return accessToken;
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