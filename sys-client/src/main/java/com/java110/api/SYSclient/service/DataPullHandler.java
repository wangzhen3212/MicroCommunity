package com.java110.api.SYSclient.service;

import com.java110.api.SYSclient.proxy.InboundProxyHandler;
import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 数据拉取处理器 —— 仅处理 INBOUND_REQUEST 中的普通 API 请求
 *
 * 安全约束:
 * 1. 统计数据拉取只能通过 DATA_PULL_REQ 消息类型访问（由 SysWebSocketClient.handleDataPullRequest 处理）
 * 2. INBOUND_REQUEST 管线不暴露统计接口，防止外部直接 HTTP 访问
 * 3. 不提供本地缓存，拉取频率由上位机严格控制
 *
 * @author SYS-client
 */
@Service
public class DataPullHandler {

    private static final Logger logger = LoggerFactory.getLogger(DataPullHandler.class);

    @Autowired
    private InboundProxyHandler inboundProxyHandler;

    /**
     * 处理上位机转发的普通入站请求
     *
     * @param method  HTTP 方法
     * @param path    API 路径
     * @param headers 请求头
     * @param body    请求体 JSON
     * @return 响应结果 Map{statusCode, body, headers}
     */
    public Map<String, Object> handle(String method, String path,
                                       Map<String, String> headers, String body) {
        logger.debug("[数据拉取] 处理入站请求: method={}, path={}", method, path);
        return inboundProxyHandler.handle(method, path, headers, body);
    }
}
