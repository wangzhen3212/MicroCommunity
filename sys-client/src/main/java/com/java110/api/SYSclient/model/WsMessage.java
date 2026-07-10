package com.java110.api.SYSclient.model;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一 WebSocket 消息结构体
 *
 * 客户机与上位机之间所有消息均使用此格式，
 * msgId 用于请求-响应匹配（如 INBOUND_REQUEST 与 INBOUND_RESPONSE 通过 msgId 对应）
 *
 * JSON 格式:
 * {
 *   "msgId": "uuid-v4",
 *   "type": "REGISTER",
 *   "payload": { ... },
 *   "timestamp": 1700000000000
 * }
 *
 * @author SYS-client
 */
public class WsMessage {

    /** 消息唯一标识 (UUID v4)，用于请求-响应匹配 */
    private String msgId;

    /** 消息类型，参见 MessageType 常量 */
    private String type;

    /** 消息负载，键值对承载具体业务数据 */
    private Map<String, Object> payload;

    /** 消息发送时间戳 (毫秒) */
    private long timestamp;

    public WsMessage() {
        this.timestamp = System.currentTimeMillis();
        this.payload = new HashMap<>();
    }

    public WsMessage(String msgId, String type, Map<String, Object> payload) {
        this.msgId = msgId;
        this.type = type;
        this.payload = payload != null ? payload : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /** 转换为 WebSocket 传输的 JSON 字符串 */
    public String toJsonString() {
        Map<String, Object> map = new HashMap<>();
        map.put("msgId", this.msgId);
        map.put("type", this.type);
        map.put("payload", this.payload);
        map.put("timestamp", this.timestamp);
        return JSONObject.toJSONString(map);
    }

    /** 从 JSON 字符串解析为 WsMessage */
    public static WsMessage fromJson(String jsonStr) {
        JSONObject json = JSONObject.parseObject(jsonStr);
        WsMessage msg = new WsMessage();
        msg.msgId = json.getString("msgId");
        msg.type = json.getString("type");
        msg.timestamp = json.getLongValue("timestamp");
        JSONObject payloadJson = json.getJSONObject("payload");
        if (payloadJson != null) {
            msg.payload = new HashMap<>(payloadJson.getInnerMap());
        }
        return msg;
    }

    // ======== getter / setter ========

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
