package com.java110.api.SYSclient.model;

/**
 * WebSocket 消息类型常量 —— 客户机 ↔ 上位机 通信协议
 *
 * 上行(客户机→上位机): REGISTER, HEARTBEAT, INBOUND_RESPONSE
 * 下行(上位机→客户机): REGISTER_ACK, ACK, INBOUND_REQUEST
 *
 * @author SYS-client
 */
public final class MessageType {

    private MessageType() {}

    /** 客户机→上位机: 注册请求，上报 KEY + 全部小区/商户/微信APPID列表 */
    public static final String REGISTER = "REGISTER";

    /** 上位机→客户机: 注册应答 (OK / CONFLICT / INVALID_KEY) */
    public static final String REGISTER_ACK = "REGISTER_ACK";

    /** 客户机→上位机: 心跳 (默认30s间隔) */
    public static final String HEARTBEAT = "HEARTBEAT";

    /** 上位机→客户机: 心跳应答 */
    public static final String ACK = "ACK";

    /** 上位机→客户机: 入站业务请求（移动端全流量 / 外部回调透传 / 数据拉取） */
    public static final String INBOUND_REQUEST = "INBOUND_REQUEST";

    /** 客户机→上位机: 入站请求响应（api接口处理结果透传） */
    public static final String INBOUND_RESPONSE = "INBOUND_RESPONSE";

    /** 上位机→客户机: 数据拉取请求（已废弃，改用 INBOUND_REQUEST，保留用于向后兼容） */
    public static final String DATA_PULL_REQ = "DATA_PULL_REQ";

    /** 客户机→上位机: 数据拉取响应（已废弃，保留用于向后兼容） */
    public static final String DATA_PULL_ACK = "DATA_PULL_ACK";

    /** 上位机→客户机: 公网回调推送（微信/支付宝支付回调等） */
    public static final String CALLBACK_IN = "CALLBACK_IN";

    /** 上位机→客户机: 配置下发（预留） */
    public static final String CONFIG_PUSH = "CONFIG_PUSH";
}