package com.java110.api.SYSclient.proxy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.java110.api.SYSclient.SysClientProperties;
import com.java110.api.smo.IApiServiceSMO;
import com.java110.core.client.RestTemplate;
import com.java110.core.context.Environment;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.constant.CommonConstant;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.DateUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 入站代理处理器 — 将上位机转发的业务请求透传给原项目 API 接口
 *
 * 职责:
 * 1. 接收上位机 INBOUND_REQUEST（method + path + headers + body）
 * 2. 构建 API 管线所需的标准请求头
 * 3. 验证 X-Host-Key 后注入系统管理员身份（levelCd=00），确保上位机有权拉取所有社区数据
 * 4. 上位机验证通过后，直接调用微服务，跳过 ApiServiceSMOImpl 的权限检查
 * 5. 将结果封装为 INBOUND_RESPONSE 返回
 *
 * @author SYS-client
 */
@Component
public class InboundProxyHandler {

    private static final Logger logger = LoggerFactory.getLogger(InboundProxyHandler.class);

    /** 上位机认证请求头 */
    private static final String HEADER_HOST_KEY = "X-Host-Key";

    @Autowired
    @Qualifier("apiServiceSMOImpl")
    private IApiServiceSMO apiServiceSMOImpl;

    @Autowired
    private SysClientProperties sysClientProperties;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMO;

    /** 缓存的管理员 userId，避免每次请求都查询数据库 */
    private volatile String cachedAdminUserId;

    /**
     * 处理入站请求，返回可直接放入 INBOUND_RESPONSE payload 的结果
     *
     * @param method   HTTP 方法 (GET/POST/PUT/DELETE)
     * @param path     请求路径，如 /api/user.listUser
     * @param headers  上位机透传的请求头（含 app-id, user-id 等）
     * @param body     请求体 JSON 字符串
     * @return INBOUND_RESPONSE 的 payload
     */
    public Map<String, Object> handle(String method, String path,
                                       Map<String, String> headers, String body) {
        try {
            String serviceCode = extractServiceCode(path);
            if (serviceCode == null || serviceCode.isEmpty()) {
                return errorResult(400, "无法从路径提取 serviceCode: " + path);
            }

            Map<String, String> apiHeaders = buildApiHeaders(method, serviceCode, headers, body);

            ResponseEntity<String> responseEntity;
            String hostKey = headers != null ? headers.get(HEADER_HOST_KEY) : null;
            if (hostKey != null && hostKey.equals(sysClientProperties.getAuthKey())) {
                responseEntity = invokeMicroservice(method, serviceCode, apiHeaders, body);
            } else {
                responseEntity = apiServiceSMOImpl.service(body, apiHeaders);
            }

            return toResultMap(responseEntity);

        } catch (Exception e) {
            logger.error("[入站代理] 处理请求失败: path={}, error={}", path, e.getMessage(), e);
            return errorResult(500, "入站代理内部异常: " + e.getMessage());
        }
    }

    /**
     * 直接调用微服务，跳过 ApiServiceSMOImpl 的权限检查
     * 上位机验证通过后，直接转发到微服务，绕过服务配置表的权限限制
     */
    private ResponseEntity<String> invokeMicroservice(String method, String serviceCode,
                                                       Map<String, String> apiHeaders, String body) {
        RestTemplate restTemplate;
        if (Environment.isStartBootWay()) {
            restTemplate = ApplicationContextFactory.getBean("outRestTemplate", RestTemplate.class);
        } else {
            restTemplate = ApplicationContextFactory.getBean("restTemplate", RestTemplate.class);
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        for (String key : apiHeaders.keySet()) {
            httpHeaders.add(key, apiHeaders.get(key));
        }

        String requestUrl = "/cmd/" + serviceCode;
        if (Environment.isStartBootWay()) {
            requestUrl = Environment.BOOT_PATH + requestUrl;
        }

        if ("GET".equalsIgnoreCase(method) && body != null && !body.isEmpty()) {
            requestUrl += mapToUrlParam(JSON.parseObject(body));
        }

        HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);

        try {
            if ("GET".equalsIgnoreCase(method)) {
                return restTemplate.exchange(requestUrl, HttpMethod.GET, httpEntity, String.class);
            } else if ("PUT".equalsIgnoreCase(method)) {
                return restTemplate.exchange(requestUrl, HttpMethod.PUT, httpEntity, String.class);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                return restTemplate.exchange(requestUrl, HttpMethod.DELETE, httpEntity, String.class);
            } else {
                return restTemplate.exchange(requestUrl, HttpMethod.POST, httpEntity, String.class);
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        }
    }

    /**
     * 从请求路径中提取 serviceCode
     * /api/user.listUser       → user.listUser
     * /app/user.listUser       → user.listUser
     * /api/htapi/wechat/callback → htapi/wechat/callback  (回调请求保留特征码)
     */
    private String extractServiceCode(String path) {
        if (path == null) return null;
        if (path.contains("/api/htapi/")) {
            return path.substring(path.indexOf("/api/htapi/") + 1);
        }
        if (path.startsWith("/api/")) {
            return path.substring(5);
        }
        if (path.startsWith("/app/")) {
            return path.substring(5);
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * 构建 API 管线所需的标准请求头
     * 若请求来自上位机（X-Host-Key 匹配），注入系统管理员身份
     */
    private Map<String, String> buildApiHeaders(String method, String serviceCode,
                                                 Map<String, String> inboundHeaders,
                                                 String body) {
        Map<String, String> apiHeaders = new HashMap<>();

        if (inboundHeaders != null) {
            apiHeaders.putAll(inboundHeaders);
        }

        String hostKey = inboundHeaders != null ? inboundHeaders.get(HEADER_HOST_KEY) : null;
        if (hostKey != null && hostKey.equals(sysClientProperties.getAuthKey())) {
            injectAdminCredentials(apiHeaders);
            apiHeaders.remove(HEADER_HOST_KEY);
            apiHeaders.remove("X-Host-Auth");
        }

        if (!apiHeaders.containsKey(CommonConstant.APP_ID)) {
            apiHeaders.put(CommonConstant.APP_ID, CommonConstant.HC_WEB_APP_ID);
        }

        if (!apiHeaders.containsKey(CommonConstant.HTTP_USER_ID)) {
            apiHeaders.put(CommonConstant.HTTP_USER_ID,
                    apiHeaders.getOrDefault(CommonConstant.USER_ID, "-1"));
        }
        if (!apiHeaders.containsKey(CommonConstant.USER_ID)) {
            apiHeaders.put(CommonConstant.USER_ID,
                    apiHeaders.getOrDefault(CommonConstant.HTTP_USER_ID, "-1"));
        }
        if (!apiHeaders.containsKey(CommonConstant.HTTP_TRANSACTION_ID)) {
            apiHeaders.put(CommonConstant.HTTP_TRANSACTION_ID, GenerateCodeFactory.getUUID());
        }
        if (!apiHeaders.containsKey(CommonConstant.HTTP_REQ_TIME)) {
            apiHeaders.put(CommonConstant.HTTP_REQ_TIME, DateUtil.getNowDefault());
        }
        apiHeaders.put(CommonConstant.HTTP_SERVICE, serviceCode);
        apiHeaders.put(CommonConstant.HTTP_METHOD, method);

        String requestUrl = "http://127.0.0.1:8008/" + serviceCode;
        if ("GET".equalsIgnoreCase(method) && body != null && !body.isEmpty()) {
            requestUrl += mapToUrlParam(JSON.parseObject(body));
        }
        apiHeaders.put("REQUEST_URL", requestUrl);

        if (body != null && !body.isEmpty()) {
            try {
                JSONObject bodyObj = JSON.parseObject(body);
                String userId = apiHeaders.get(CommonConstant.USER_ID);
                if (userId != null && !"-1".equals(userId) && !bodyObj.containsKey("userId")) {
                    bodyObj.put("userId", userId);
                }
                if (userId != null && !"-1".equals(userId) && !bodyObj.containsKey("loginUserId")) {
                    bodyObj.put("loginUserId", userId);
                }
            } catch (Exception e) {
                logger.debug("[入站代理] 解析body失败: {}", e.getMessage());
            }
        }

        return apiHeaders;
    }

    /**
     * 注入系统管理员身份凭证到 API 请求头
     */
    private void injectAdminCredentials(Map<String, String> apiHeaders) {
        String adminUserId = getAdminUserId();
        if (adminUserId != null) {
            apiHeaders.put(CommonConstant.HTTP_USER_ID, adminUserId);
            apiHeaders.put(CommonConstant.USER_ID, adminUserId);
            logger.debug("[入站代理] 已注入管理员 userId={}", adminUserId);
        }
    }

    /**
     * 获取系统管理员 userId（levelCd=00），带缓存
     */
    private String getAdminUserId() {
        if (cachedAdminUserId != null) {
            return cachedAdminUserId;
        }
        try {
            UserDto query = new UserDto();
            query.setLevelCd(UserDto.LEVEL_CD_ADMIN);
            List<UserDto> admins = userV1InnerServiceSMO.getSystemUsers(query);
            if (admins != null && !admins.isEmpty()) {
                cachedAdminUserId = admins.get(0).getUserId();
                logger.info("[入站代理] 已缓存系统管理员 userId={}", cachedAdminUserId);
                return cachedAdminUserId;
            }
            List<UserDto> users = userV1InnerServiceSMO.queryUsers(query);
            if (users != null && !users.isEmpty()) {
                cachedAdminUserId = users.get(0).getUserId();
                logger.info("[入站代理] 已缓存管理员 userId={} (queryUsers fallback)", cachedAdminUserId);
                return cachedAdminUserId;
            }
        } catch (Exception e) {
            logger.warn("[入站代理] 查询系统管理员失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 将 JSON 参数拼接到 URL 查询字符串
     */
    private String mapToUrlParam(JSONObject param) {
        if (param == null || param.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        for (String key : param.keySet()) {
            Object val = param.get(key);
            if (val != null) {
                sb.append(key).append("=").append(val).append("&");
            }
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.length() == 1 ? "" : sb.toString();
    }

    /**
     * 将 ResponseEntity 转换为 InboundProxyHandler 标准响应 Map
     */
    private Map<String, Object> toResultMap(ResponseEntity<String> responseEntity) {
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", responseEntity.getStatusCodeValue());
        result.put("body", responseEntity.getBody());
        if (responseEntity.getHeaders() != null) {
            Map<String, String> respHeaders = new HashMap<>();
            responseEntity.getHeaders().forEach((k, v) -> {
                if (v != null && !v.isEmpty()) {
                    respHeaders.put(k, v.get(0));
                }
            });
            result.put("headers", respHeaders);
        }
        return result;
    }

    /**
     * 构造错误响应
     */
    private Map<String, Object> errorResult(int statusCode, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", statusCode);
        result.put("body", "{\"msg\":\"" + message + "\"}");
        return result;
    }
}