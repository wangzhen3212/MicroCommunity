package com.java110.api.SYSclient.websocket;

import com.alibaba.fastjson.JSONObject;
import com.java110.api.SYSclient.SysClientProperties;
import com.java110.api.SYSclient.model.MessageType;
import com.java110.api.SYSclient.model.WsMessage;
import com.java110.api.SYSclient.proxy.InboundProxyHandler;
import com.java110.api.SYSclient.service.LocalIdUpdater;
import com.java110.core.event.CacheFlushCompleteEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 客户机 WebSocket 长连接客户端（骨架阶段）
 *
 * 职责:
 * 1. 检测 sys.client.enabled 配置，仅在开启时启动连接
 * 2. 主动向上位机发起 WebSocket 连接
 * 3. 连接成功后发送 REGISTER 注册消息
 * 4. 维持心跳检测（默认30秒间隔）
 * 5. 两段式重连策略：上位机未上线→固定10分钟重试；瞬断→指数退避(1s~60s)
 * 6. 处理上位机 INBOUND_REQUEST 入站代理请求
 *
 * 数据统一规范: 客户机不主动上报，所有数据由上位机通过KEY+INBOUND_REQUEST拉取。
 *
 * @author SYS-client
 */
@ClientEndpoint
@Component
public class SysWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(SysWebSocketClient.class);

    @Autowired
    private SysClientProperties sysClientProperties;

    @Autowired
    private InboundProxyHandler inboundProxyHandler;

    @Autowired
    private LocalIdUpdater localIdUpdater;

    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMO;

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMO;

    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMO;

    /** WebSocket 会话（连接保持期间有效） */
    private Session session;

    /** 是否已成功注册 */
    private volatile boolean registered = false;

    /** KEY 是否无效（INVALID_KEY应答后不再重连） */
    private volatile boolean invalidKey = false;

    /** CONFLICT 应答的原始负载（供 onClose 中使用 LocalIdUpdater） */
    private volatile Object conflictPayload;

    /** 是否曾成功建立过 WebSocket 连接。用于区分"上位机从未上线"和"临时断线"两种重连策略 */
    private volatile boolean wasEverOpened = false;

    /** 重连计数器（成功后归零） */
    private int reconnectCount = 0;

    /** 心跳定时器句柄 */
    private ScheduledFuture<?> heartbeatFuture;

    /** 单线程调度器，用于心跳和重连 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sys-client-scheduler");
        t.setDaemon(true);
        return t;
    });

    // ==================== 生命周期 ====================

    /**
     * 应用启动完成后，检查配置是否有效
     * 连接逻辑由 CacheFlushCompleteEvent 触发
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!sysClientProperties.isEnabled()) {
            logger.info("[客户机] sys.client.enabled=false，跳过启动");
            return;
        }
        if (!sysClientProperties.isValid()) {
            logger.error("[客户机] 配置不完整: serverUrl={}, authKey={}",
                    sysClientProperties.getServerUrl(),
                    sysClientProperties.getAuthKey() == null ? "null" : "***");
            return;
        }
        logger.info("[客户机] 配置有效，等待缓存加载完成事件...");
    }

    /**
     * 监听缓存刷新完成事件，收到事件后连接上位机
     */
    @EventListener(CacheFlushCompleteEvent.class)
    public void onCacheFlushComplete(CacheFlushCompleteEvent event) {
        if (!sysClientProperties.isEnabled()) {
            return;
        }
        logger.info("[客户机] 收到缓存刷新完成事件，准备连接上位机: {}", sysClientProperties.getServerUrl());
        connectToHost();
    }

    /**
     * 发起 WebSocket 连接
     */
    private void connectToHost() {
        try {
            String serverUrl = sysClientProperties.getServerUrl();
            logger.info("[客户机] 正在连接上位机: {}", serverUrl);
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(serverUrl));
        } catch (Exception e) {
            logger.error("[客户机] 连接上位机失败: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.reconnectCount = 0;
        this.wasEverOpened = true;
        logger.info("[客户机] WebSocket 连接已建立, sessionId={}", session.getId());
        sendRegister();
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.warn("[客户机] WebSocket 连接关闭: code={}, reason={}",
                closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
        this.session = null;
        this.registered = false;
        stopHeartbeat();

        // INVALID_KEY: 不自动重连，等待人工介入
        if (invalidKey) {
            logger.error("[客户机] KEY 无效，不再自动重连。请修正配置后重启服务");
            return;
        }

        // CONFLICT: 调用 LocalIdUpdater 解决ID冲突后重连
        if (conflictPayload != null) {
            logger.info("[客户机] 开始处理ID冲突...");
            try {
                boolean resolved = localIdUpdater.resolveConflicts(conflictPayload);
                this.conflictPayload = null;
                if (resolved) {
                    logger.info("[客户机] ID冲突已解决，缓存已刷新，将全量重新注册");
                } else {
                    logger.error("[客户机] ID冲突处理失败，暂不重连");
                    return;
                }
            } catch (Exception e) {
                logger.error("[客户机] ID冲突处理异常: {}", e.getMessage(), e);
                return;
            }
        }

        scheduleReconnect();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("[客户机] WebSocket 连接异常: {}", error.getMessage());
    }

    @PreDestroy
    public void onDestroy() {
        logger.info("[客户机] 服务关闭，清理资源");
        stopHeartbeat();
        scheduler.shutdownNow();
        closeQuietly();
    }

    // ==================== 消息发送 ====================

    /**
     * 发送 REGISTER 注册消息
     * 通过 Feign 查询实际小区/商户/微信APPID列表，动态填充负载
     */
    private void sendRegister() {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("authKey", sysClientProperties.getAuthKey());

            // 查询所有小区
            List<Map<String, Object>> communities = new ArrayList<>();
            try {
                List<CommunityDto> communityList = communityV1InnerServiceSMO.queryCommunitys(new CommunityDto());
                if (communityList != null) {
                    for (CommunityDto c : communityList) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("localCommunityId", c.getCommunityId());
                        item.put("communityName", c.getName());
                        item.put("localStoreId", c.getStoreId() != null ? c.getStoreId() : "");
                        communities.add(item);
                    }
                }
            } catch (Exception e) {
                logger.warn("[客户机] 查询小区列表失败，将上报空列表: {}", e.getMessage());
            }
            payload.put("localCommunities", communities);

            // 查询所有商户
            List<Map<String, Object>> stores = new ArrayList<>();
            try {
                List<StoreDto> storeList = storeV1InnerServiceSMO.queryStores(new StoreDto());
                if (storeList != null) {
                    for (StoreDto s : storeList) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("localStoreId", s.getStoreId());
                        item.put("storeName", s.getName());
                        stores.add(item);
                    }
                }
            } catch (Exception e) {
                logger.warn("[客户机] 查询商户列表失败，将上报空列表: {}", e.getMessage());
            }
            payload.put("localStores", stores);

            // 查询所有微信APPID
            List<String> wechatAppIds = new ArrayList<>();
            try {
                List<SmallWeChatDto> wechatList = smallWeChatInnerServiceSMO.querySmallWeChats(new SmallWeChatDto());
                if (wechatList != null) {
                    for (SmallWeChatDto w : wechatList) {
                        if (w.getAppId() != null && !w.getAppId().isEmpty()) {
                            wechatAppIds.add(w.getAppId());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("[客户机] 查询微信APPID列表失败，将上报空列表: {}", e.getMessage());
            }
            payload.put("wechatAppIds", wechatAppIds);

            WsMessage msg = new WsMessage(UUID.randomUUID().toString(), MessageType.REGISTER, payload);
            String jsonStr = msg.toJsonString();
            logger.info("[客户机] 发送注册消息: key=***, 小区数={}, 商户数={}, 微信APPID数={}, msgId={}",
                    communities.size(), stores.size(), wechatAppIds.size(), msg.getMsgId());
            this.session.getBasicRemote().sendText(jsonStr);
        } catch (Exception e) {
            logger.error("[客户机] 发送注册消息失败: {}", e.getMessage());
        }
    }

    /**
     * 发送 HEARTBEAT 心跳消息
     */
    private void sendHeartbeat() {
        try {
            if (session != null && session.isOpen()) {
                WsMessage msg = new WsMessage(UUID.randomUUID().toString(), MessageType.HEARTBEAT, null);
                this.session.getBasicRemote().sendText(msg.toJsonString());
            }
        } catch (Exception e) {
            logger.warn("[客户机] 发送心跳失败: {}", e.getMessage());
        }
    }

    // ==================== 消息接收 ====================

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            WsMessage msg = WsMessage.fromJson(message);

            switch (msg.getType()) {
                case MessageType.REGISTER_ACK:
                    handleRegisterAck(msg);
                    break;
                case MessageType.INBOUND_REQUEST:
                    handleInboundRequest(msg);
                    break;
                case MessageType.ACK:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
        }
    }

    /**
     * 处理 REGISTER_ACK 注册应答
     */
    private void handleRegisterAck(WsMessage msg) {
        Map<String, Object> payload = msg.getPayload();
        String status = payload != null ? (String) payload.get("status") : null;

        if ("OK".equals(status)) {
            this.registered = true;
            this.invalidKey = false;
            this.conflictPayload = null;
            logger.info("[客户机] 注册成功，启动心跳（数据由上位机通过KEY拉取）");
            startHeartbeat();
        } else if ("CONFLICT".equals(status)) {
            logger.warn("[客户机] 注册冲突: {}", JSONObject.toJSONString(payload));
            this.conflictPayload = payload.get("conflicts");
            closeQuietly();
        } else if ("INVALID_KEY".equals(status)) {
            logger.error("[客户机] KEY 无效，拒绝注册。请检查配置 sys.client.auth-key");
            this.invalidKey = true;
            this.conflictPayload = null;
            closeQuietly();
        } else {
            logger.warn("[客户机] 未知注册应答状态: {}", status);
        }
    }

    // ==================== 心跳管理 ====================

    private void startHeartbeat() {
        stopHeartbeat();
        int interval = sysClientProperties.getHeartbeatInterval();
        logger.info("[客户机] 启动心跳，间隔={}秒", interval);
        heartbeatFuture = scheduler.scheduleWithFixedDelay(
                this::sendHeartbeat, interval, interval, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
    }

    // ==================== 重连逻辑 ====================

    /**
     * 两段式重连策略:
     * - 上位机从未上线(wasEverOpened=false): 固定长间隔重试，默认600秒(10分钟)
     * - 曾连接成功后断线(wasEverOpened=true): 指数退避 1s→2s→4s→...→60s(最大)
     */
    private void scheduleReconnect() {
        if (!sysClientProperties.isEnabled()) {
            return;
        }

        long delay;
        reconnectCount++;

        if (wasEverOpened) {
            // 瞬断恢复场景：使用指数退避快速重连
            int maxInterval = sysClientProperties.getReconnectMaxInterval();
            delay = Math.min((long) Math.pow(2, reconnectCount - 1), maxInterval);
            logger.info("[客户机] 计划第{}次重连（瞬断恢复），延迟{}秒", reconnectCount, delay);
        } else {
            // 上位机未上线场景：使用固定长间隔，避免频繁无效连接
            delay = sysClientProperties.getHostRetryInterval();
            logger.info("[客户机] 上位机未上线，第{}次重试，延迟{}秒（{}分钟）",
                    reconnectCount, delay, delay / 60);
        }

        scheduler.schedule(this::connectToHost, delay, TimeUnit.SECONDS);
    }

    // ==================== 辅助方法 ====================

    private void closeQuietly() {
        try {
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "客户机主动关闭"));
            }
        } catch (IOException e) {
            logger.warn("[客户机] 关闭会话时异常: {}", e.getMessage());
        }
    }

    // ==================== 入站代理（节点二） ====================

    /**
     * 处理上位机转发的入站业务请求
     */
    private void handleInboundRequest(WsMessage msg) {
        Map<String, Object> payload = msg.getPayload();
        if (payload == null) {
            return;
        }
        String method = (String) payload.get("method");
        String path = (String) payload.get("path");
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) payload.get("headers");

        logger.info("[上位机管理消息] INBOUND_REQUEST method={}, path={}, headers={}", method, path, headers);

        Map<String, Object> result = inboundProxyHandler.handle(method, path, headers, null);

        try {
            WsMessage resp = new WsMessage(msg.getMsgId(), MessageType.INBOUND_RESPONSE, result);
            this.session.getBasicRemote().sendText(resp.toJsonString());
        } catch (Exception e) {
        }
    }

}
