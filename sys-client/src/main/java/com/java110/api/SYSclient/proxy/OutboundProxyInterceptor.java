package com.java110.api.SYSclient.proxy;

import com.java110.api.SYSclient.SysClientProperties;
import com.java110.core.log.LoggerFactory;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutboundProxyInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(OutboundProxyInterceptor.class);

    private static final List<String> LOCAL_HOSTS = Arrays.asList(
            "127.0.0.1", "localhost", "0.0.0.0", "192.168.", "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.", "172.27.", "172.28.",
            "172.29.", "172.30.", "172.31.", "0:0:0:0:0:0:0:1"
    );

    private static final String IOT_DOMAIN = "IOT";
    private static final String IOT_URL_KEY = "IOT_URL";
    private static final String DEFAULT_IOT_URL = "https://things.homecommunity.cn";

    private static final String OSS_DOMAIN = "OSS";
    private static final String OSS_ENDPOINT_KEY = "endpoint";

    private static final long HOST_CACHE_TTL_MS = 60000;

    /** 是否已注册到 outRestTemplate（由 SysWebSocketClient 在连接上位机后控制） */
    private volatile boolean registered = false;

    private volatile String cachedIotHost;
    private volatile long lastIotHostRefreshTime;

    private volatile String cachedOssHost;
    private volatile long lastOssHostRefreshTime;

    @Resource
    private SysClientProperties sysClientProperties;

    /**
     * 由 SysWebSocketClient 在注册成功（REGISTER_ACK/OK）后调用，将本拦截器注入 outRestTemplate
     */
    public void register(RestTemplate restTemplate) {
        if (restTemplate == null) {
            return;
        }
        boolean alreadyPresent = false;
        for (ClientHttpRequestInterceptor interceptor : restTemplate.getInterceptors()) {
            if (interceptor == this) {
                alreadyPresent = true;
                break;
            }
        }
        if (!alreadyPresent) {
            restTemplate.getInterceptors().add(0, this);
        }
        this.registered = true;
        logger.info("[出站代理] 已注册到 outRestTemplate（上位机连接就绪）");
    }

    /**
     * 由 SysWebSocketClient 在 WebSocket 断开后调用，将本拦截器从 outRestTemplate 移除
     */
    public void unregister(RestTemplate restTemplate) {
        if (restTemplate == null) {
            return;
        }
        this.registered = false;
        restTemplate.getInterceptors().remove(this);
        logger.warn("[出站代理] 已从 outRestTemplate 注销（上位机连接断开）");
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        URI originalUri = request.getURI();
        String originalUrl = originalUri.toString();

        // 未注册到上位机时直连（不代理也不输出日志）
        if (!registered) {
            return execution.execute(request, body);
        }

        if (!sysClientProperties.isEnabled()) {
            return execution.execute(request, body);
        }
        if (!sysClientProperties.isProxyEnabled()) {
            return execution.execute(request, body);
        }

        String proxyUrl = sysClientProperties.getProxyUrl();
        if (proxyUrl == null || proxyUrl.trim().isEmpty()) {
            return execution.execute(request, body);
        }

        String host = extractHost(originalUrl);

        if (!shouldProxy(originalUrl, host)) {
            return execution.execute(request, body);
        }

        try {
            String encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8.name());
            if (proxyUrl.endsWith("/")) {
                proxyUrl = proxyUrl.substring(0, proxyUrl.length() - 1);
            }
            String newUrl = proxyUrl + "?__url__=" + encodedUrl;

            logger.info("[出站代理] 拦截请求: {} {} → {}, bodyLength={}", 
                    request.getMethod(), originalUrl, newUrl, body != null ? body.length : 0);

            HttpRequest proxyRequest = new ProxyHttpRequest(request, URI.create(newUrl));
            ClientHttpResponse proxyResponse = execution.execute(proxyRequest, body);

            byte[] responseBodyBytes = readResponseBodyBytes(proxyResponse);
            String responseBody = new String(responseBodyBytes, StandardCharsets.UTF_8);

            logger.info("[出站代理] 收到响应: {} {} → status={}, bodyLength={}, body={}", 
                    request.getMethod(), originalUrl, 
                    proxyResponse.getStatusCode().value(), 
                    responseBodyBytes.length, 
                    responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

            return new CachedClientHttpResponse(proxyResponse, responseBodyBytes);
        } catch (Exception e) {
            logger.error("[出站代理] 拦截处理异常: {} → {}, error: {}",
                    request.getMethod(), originalUrl, e.getMessage());
            return execution.execute(request, body);
        }
    }

    private boolean shouldProxy(String url, String host) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        url = url.toLowerCase();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        if (url.contains("__url__=")) {
            return false;
        }

        if (host == null) {
            return false;
        }

        for (String localHost : LOCAL_HOSTS) {
            if (host.startsWith(localHost) || host.equals(localHost)) {
                return false;
            }
        }

        String iotHost = getIotHost();
        if (iotHost != null && (host.startsWith(iotHost) || host.equals(iotHost))) {
            return false;
        }

        String ossHost = getOssHost();
        if (ossHost != null && (host.startsWith(ossHost) || host.equals(ossHost))) {
            return false;
        }

        String whitelist = sysClientProperties.getProxyWhitelistHosts();
        if (whitelist != null && !whitelist.trim().isEmpty()) {
            boolean inWhitelist = false;
            for (String allowedHost : whitelist.split(",")) {
                String trimmed = allowedHost.trim().toLowerCase();
                if (trimmed.isEmpty()) {
                    continue;
                }
                // 精确匹配: api.weixin.qq.com == api.weixin.qq.com
                // 子域名通配: mp.weixin.qq.com 的 .endsWith(".weixin.qq.com") 匹配 weixin.qq.com
                if (host.equals(trimmed) || host.endsWith("." + trimmed)) {
                    inWhitelist = true;
                    break;
                }
            }
            if (!inWhitelist) {
                return false;
            }
        }

        String blacklist = sysClientProperties.getProxyBlacklistHosts();
        if (blacklist != null && !blacklist.trim().isEmpty()) {
            for (String blockedHost : blacklist.split(",")) {
                String trimmed = blockedHost.trim().toLowerCase();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (host.equals(trimmed) || host.endsWith("." + trimmed)) {
                    return false;
                }
            }
        }

        // 未命中任何排除规则 → 走代理
        return true;
    }

    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String getIotHost() {
        long now = System.currentTimeMillis();
        if (cachedIotHost != null && now - lastIotHostRefreshTime < HOST_CACHE_TTL_MS) {
            return cachedIotHost;
        }
        try {
            String iotUrl = MappingCache.getValue(IOT_DOMAIN, IOT_URL_KEY);
            if (StringUtil.isEmpty(iotUrl)) {
                iotUrl = DEFAULT_IOT_URL;
            }
            String host = extractHost(iotUrl);
            cachedIotHost = host;
            lastIotHostRefreshTime = now;
            return host;
        } catch (Exception e) {
            return cachedIotHost;
        }
    }

    private String getOssHost() {
        long now = System.currentTimeMillis();
        if (cachedOssHost != null && now - lastOssHostRefreshTime < HOST_CACHE_TTL_MS) {
            return cachedOssHost;
        }
        try {
            String ossEndpoint = MappingCache.getValue(OSS_DOMAIN, OSS_ENDPOINT_KEY);
            if (StringUtil.isEmpty(ossEndpoint)) {
                return null;
            }
            String host = extractHost(ossEndpoint);
            cachedOssHost = host;
            lastOssHostRefreshTime = now;
            return host;
        } catch (Exception e) {
            return cachedOssHost;
        }
    }

    private static class ProxyHttpRequest implements HttpRequest {

        private final HttpRequest original;
        private final URI newUri;

        public ProxyHttpRequest(HttpRequest original, URI newUri) {
            this.original = original;
            this.newUri = newUri;
        }

        @Override
        public URI getURI() {
            return newUri;
        }

        @Override
        public String getMethodValue() {
            return original.getMethodValue();
        }

        @Override
        public org.springframework.http.HttpMethod getMethod() {
            return original.getMethod();
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return original.getHeaders();
        }
    }

    private byte[] readResponseBodyBytes(ClientHttpResponse response) throws IOException {
        InputStream is = response.getBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    private static class CachedClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse original;
        private final byte[] bodyBytes;

        public CachedClientHttpResponse(ClientHttpResponse original, byte[] bodyBytes) {
            this.original = original;
            this.bodyBytes = bodyBytes;
        }

        @Override
        public HttpStatus getStatusCode() throws IOException {
            return original.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return original.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return original.getStatusText();
        }

        @Override
        public void close() {
            original.close();
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return original.getHeaders();
        }

        @Override
        public InputStream getBody() throws IOException {
            return new ByteArrayInputStream(bodyBytes);
        }
    }
}
