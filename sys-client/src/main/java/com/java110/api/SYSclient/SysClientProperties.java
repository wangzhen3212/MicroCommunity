package com.java110.api.SYSclient;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 客户机配置属性 —— 读取环境变量/配置文件
 *
 * 最小化配置原则：仅需 上位机地址 + KEY 两项即可启动客户机功能。
 * 上位机功能通过 sys.client.enabled 控制开关，关闭时客户机代码不执行任何操作。
 *
 * 配置方式:
 *   application.yml:
 *     sys:
 *       client:
 *         enabled: true
 *         server-url: ws://上位机公网地址:8080/ws/host
 *         auth-key: 上位机管理员分配的KEY
 *         heartbeat-interval: 30
 *         proxy-url: http://上位机公网地址:8080/proxy/out
 *
 *   环境变量（替代方式）:
 *     SYS_CLIENT_ENABLED=true
 *     SYS_CLIENT_SERVER_URL=ws://上位机公网地址:8080/ws/host
 *     SYS_CLIENT_AUTH_KEY=上位机管理员分配的KEY
 *     SYS_CLIENT_PROXY_URL=http://上位机公网地址:8080/proxy/out
 *
 * @author SYS-client
 */
@Component
@ConfigurationProperties(prefix = "sys.client")
public class SysClientProperties {

    /**
     * 上位机功能是否开启
     * 默认 false —— 关闭时客户机代码零影响，原项目完全独立运行
     */
    private boolean enabled = false;

    /** 上位机 WebSocket 服务端地址（含端口和路径），如 ws://10.0.0.1:8080/ws/host */
    private String serverUrl;

    /** 上位机管理员预分配的认证 KEY，用于客户机注册时身份验证 */
    private String authKey;

    /** 心跳发送间隔（秒），默认30秒 */
    private int heartbeatInterval = 30;

    /** 断线重连最大间隔（秒），默认60秒。仅在曾成功连接后的瞬断恢复场景使用 */
    private int reconnectMaxInterval = 60;

    /** 上位机未上线时的注册重试间隔（秒），默认600秒（10分钟）。从未成功连接过时使用此固定间隔 */
    private int hostRetryInterval = 600;

    /** 上位机 HTTP 代理地址，如 http://10.0.0.1:8080/proxy/out */
    private String proxyUrl;

    /** 是否启用出站HTTP代理（独立开关），默认启用 */
    private boolean proxyEnabled = true;

    /** 代理白名单主机列表（逗号分隔），为空则不限制。白名单模式下仅白名单内的主机走代理 */
    private String proxyWhitelistHosts;

    /** 代理黑名单主机列表（逗号分隔），为空则不限制。黑名单模式下跳过黑名单内的主机 */
    private String proxyBlacklistHosts;

    /** 代理请求超时时间（秒），默认30秒 */
    private int proxyTimeout = 30;

    // ======== getter / setter ========

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public int getReconnectMaxInterval() {
        return reconnectMaxInterval;
    }

    public void setReconnectMaxInterval(int reconnectMaxInterval) {
        this.reconnectMaxInterval = reconnectMaxInterval;
    }

    public int getHostRetryInterval() {
        return hostRetryInterval;
    }

    public void setHostRetryInterval(int hostRetryInterval) {
        this.hostRetryInterval = hostRetryInterval;
    }

    public String getProxyUrl() { return proxyUrl; }
    public void setProxyUrl(String proxyUrl) { this.proxyUrl = proxyUrl; }

    public boolean isProxyEnabled() { return proxyEnabled; }
    public void setProxyEnabled(boolean proxyEnabled) { this.proxyEnabled = proxyEnabled; }

    public String getProxyWhitelistHosts() { return proxyWhitelistHosts; }
    public void setProxyWhitelistHosts(String proxyWhitelistHosts) { this.proxyWhitelistHosts = proxyWhitelistHosts; }

    public String getProxyBlacklistHosts() { return proxyBlacklistHosts; }
    public void setProxyBlacklistHosts(String proxyBlacklistHosts) { this.proxyBlacklistHosts = proxyBlacklistHosts; }

    public int getProxyTimeout() { return proxyTimeout; }
    public void setProxyTimeout(int proxyTimeout) { this.proxyTimeout = proxyTimeout; }

    /**
     * 校验最小必要配置是否完整
     * @return true=配置完整可启动，false=缺少必要配置
     */
    public boolean isValid() {
        return enabled
                && serverUrl != null && !serverUrl.trim().isEmpty()
                && authKey != null && !authKey.trim().isEmpty();
    }
}