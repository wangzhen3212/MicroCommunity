package com.java110.api.SYSclient.service;

import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CommunityStatsHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommunityStatsHandler.class);

    @Autowired
    private DirectSqlStatsHandler directSqlStatsHandler;

    public Map<String, Object> handlePull(String communityId, List<String> pullTypes) {
        Map<String, Object> result = new HashMap<>();
        result.put("communityId", communityId);
        result.put("timestamp", System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();

        if (pullTypes == null || pullTypes.isEmpty()) {
            pullTypes = Arrays.asList("room", "workorder", "complaint");
        }

        for (String pullType : pullTypes) {
            try {
                switch (pullType.toLowerCase()) {
                    case "room":
                        data.put("room", directSqlStatsHandler.pullRoomStats(communityId));
                        break;
                    case "workorder":
                        data.put("workorder", directSqlStatsHandler.pullWorkorderStats(communityId));
                        break;
                    case "complaint":
                        data.put("complaint", directSqlStatsHandler.pullComplaintStats(communityId));
                        break;
                    case "fund":
                        data.put("fund", pullFundStats(communityId));
                        break;
                    case "public":
                        data.put("public", pullPublicProfitStats(communityId));
                        break;
                    default:
                        logger.warn("[统计拉取] 未知拉取类型: {}", pullType);
                }
            } catch (Exception e) {
                logger.error("[统计拉取] 拉取类型 {} 失败: communityId={}, error={}",
                        pullType, communityId, e.getMessage(), e);
                data.put(pullType, buildErrorResult(e.getMessage()));
            }
        }

        result.put("data", data);
        result.put("success", true);

        // 调试日志：输出每种类型的数据摘要
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String type = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typeMap = (Map<String, Object>) value;
                summary.append(type).append("={total=").append(typeMap.get("total"))
                        .append(", hasError=").append(typeMap.containsKey("error")).append("}, ");
            } else {
                summary.append(type).append("={raw=").append(value).append("}, ");
            }
        }
        logger.info("[统计拉取] communityId={}, pullTypes={}, 返回数据摘要: {}", 
                communityId, pullTypes, summary.toString());

        return result;
    }

    private Map<String, Object> pullFundStats(String communityId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("used", 0);
        stats.put("available", 0);
        stats.put("status", "reserved");
        stats.put("message", "维修基金统计接口预留，系统暂不支持");
        return stats;
    }

    private Map<String, Object> pullPublicProfitStats(String communityId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReceived", 0);
        stats.put("status", "reserved");
        stats.put("message", "公共收益统计接口预留，系统暂不支持");
        return stats;
    }

    private Map<String, Object> buildErrorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        return result;
    }
}