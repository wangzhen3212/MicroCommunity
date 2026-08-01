package com.java110.api.SYSclient.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.dev.ICacheV1InnerServiceSMO;
import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ID 冲突更新器
 *
 * 收到上位机 REGISTER_ACK(CONFLICT) 后，由 SysWebSocketClient.onClose() 触发调用。
 *
 * 处理流程（UPDATE 方式，不会破坏外键关联）:
 * 1. 解析冲突列表，提取冲突的 communityId
 * 2. 使用 GenerateCodeFactory 生成新 ID
 * 3. 先全量备份数据库(DbBackupService)作为保底，再通过 CommunityIdReplacer 全表 UPDATE community_id
 * 4. 调用 ICacheV1InnerServiceSMO.startFlush() 刷新缓存使新ID立即生效
 * 5. 返回成功/失败，调用方随后全量重新注册（sendRegister 重新查询 DB）
 *
 * 注意: 上位机只通知社区ID冲突，不涉及商户ID修改。
 *       CommunityIdReplacer 使用 UPDATE 方式，s_community.store_id 保持不变。
 *
 * @author SYS-client
 */
@Service
public class LocalIdUpdater {

    private static final Logger logger = LoggerFactory.getLogger(LocalIdUpdater.class);

    @Autowired
    private ICacheV1InnerServiceSMO cacheV1InnerServiceSMO;

    @Autowired
    private DbBackupService dbBackupService;

    @Autowired
    private CommunityIdReplacer communityIdReplacer;

    /**
     * 处理 CONFLICT，生成新 ID 并通过 UPDATE 替换数据库中的旧 ID
     *
     * @param conflictsPayload REGISTER_ACK 中的 conflicts 数组（JSONArray 或 List&lt;Map&gt;）
     * @return true=全部处理成功，false=存在失败（不应重连）
     */
    public boolean resolveConflicts(Object conflictsPayload) {
        if (conflictsPayload == null) {
            logger.warn("[ID更新] conflicts 为空，跳过");
            return true;
        }

        List<Map<String, String>> conflicts = parseConflicts(conflictsPayload);
        if (conflicts.isEmpty()) {
            logger.warn("[ID更新] 冲突列表为空，跳过");
            return true;
        }

        for (Map<String, String> conflict : conflicts) {
            String localCommunityId = conflict.get("localCommunityId");
            if (localCommunityId == null || localCommunityId.isEmpty()) continue;

            try {
                String newCommunityId = GenerateCodeFactory.getCommunityId();
                logger.info("[ID更新] 小区ID冲突，UPDATE: {} → {}", localCommunityId, newCommunityId);

                // 保底措施：执行全表 community_id 替换前，先将整库备份到日志目录(LOG_PATH)，确保异常时可回滚
                dbBackupService.backup("replaceCommunityId_" + localCommunityId);

                if (communityIdReplacer.replace(localCommunityId, newCommunityId) < 1) {
                    logger.error("[ID更新] UPDATE 小区ID失败: old={}, new={}", localCommunityId, newCommunityId);
                    return false;
                }

            } catch (Exception e) {
                logger.error("[ID更新] 处理小区冲突失败 communityId={}: {}", localCommunityId, e.getMessage(), e);
                return false;
            }
        }

        logger.info("[ID更新] ID冲突处理完成，刷新缓存...");

        try {
            cacheV1InnerServiceSMO.startFlush();
            logger.info("[ID更新] 缓存刷新完成");
        } catch (Exception e) {
            logger.warn("[ID更新] 缓存刷新失败: {}", e.getMessage());
        }

        return true;
    }

    /**
     * 解析冲突列表（支持 JSONArray 和 List&lt;Map&gt; 两种格式）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseConflicts(Object payload) {
        List<Map<String, String>> conflicts = new ArrayList<>();
        if (payload instanceof JSONArray) {
            JSONArray arr = (JSONArray) payload;
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Map<String, String> item = new HashMap<>();
                for (String key : obj.keySet()) {
                    item.put(key, obj.getString(key));
                }
                conflicts.add(item);
            }
        } else if (payload instanceof List) {
            for (Object o : (List<?>) payload) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    Map<String, String> item = new HashMap<>();
                    for (Map.Entry<String, Object> e : m.entrySet()) {
                        item.put(e.getKey(), e.getValue() != null ? e.getValue().toString() : null);
                    }
                    conflicts.add(item);
                }
            }
        }
        return conflicts;
    }
}
