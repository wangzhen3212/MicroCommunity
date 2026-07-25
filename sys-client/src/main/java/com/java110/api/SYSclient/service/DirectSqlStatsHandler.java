package com.java110.api.SYSclient.service;

import com.java110.core.log.LoggerFactory;
import com.java110.db.dao.IQueryServiceDAO;
import com.java110.utils.factory.ApplicationContextFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DirectSqlStatsHandler {

    private static final Logger logger = LoggerFactory.getLogger(DirectSqlStatsHandler.class);

    private IQueryServiceDAO queryServiceDAO;
    private StateCache stateCache;

    private IQueryServiceDAO getQueryServiceDAO() {
        if (queryServiceDAO == null) {
            queryServiceDAO = ApplicationContextFactory.getBean("queryServiceDAOImpl", IQueryServiceDAO.class);
        }
        return queryServiceDAO;
    }

    private StateCache getStateCache() {
        if (stateCache == null) {
            stateCache = ApplicationContextFactory.getBean("stateCache", StateCache.class);
        }
        return stateCache;
    }

    public Map<String, Object> pullRoomStats(String communityId) {
        Map<String, Object> stats = new HashMap<>();

        if (communityId == null || communityId.isEmpty()) {
            logger.warn("[SQL统计] 房屋统计 communityId 为空");
            stats.put("total", 0);
            return stats;
        }

        try {
            String sql = "SELECT r.state, COUNT(*) AS cnt FROM building_room r " +
                    "WHERE r.community_id = ? AND r.status_cd = '0' GROUP BY r.state";
            List<Map<String, Object>> results = getQueryServiceDAO().executeSql(sql, new Object[]{communityId});

            int totalCount = buildStateCounts(stats, results, "building_room", "state");

            stats.put("total", totalCount);
            stats.put("calculatedTotal", totalCount);

            logger.info("[SQL统计] 房屋统计完成: communityId={}, total={}", communityId, totalCount);
        } catch (Exception e) {
            logger.warn("[SQL统计] 房屋统计查询异常: communityId={}, error={}", communityId, e.getMessage());
            stats.put("total", 0);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    public Map<String, Object> pullWorkorderStats(String communityId) {
        Map<String, Object> stats = new HashMap<>();

        if (communityId == null || communityId.isEmpty()) {
            logger.warn("[SQL统计] 工单统计 communityId 为空");
            stats.put("total", 0);
            return stats;
        }

        try {
            String sql = "SELECT r.state, COUNT(*) AS cnt FROM r_repair_pool r " +
                    "WHERE r.community_id = ? AND r.status_cd = '0' GROUP BY r.state";
            List<Map<String, Object>> results = getQueryServiceDAO().executeSql(sql, new Object[]{communityId});

            int totalCount = buildStateCounts(stats, results, "r_repair_pool", "state");

            stats.put("total", totalCount);
            stats.put("calculatedTotal", totalCount);

            logger.info("[SQL统计] 工单统计完成: communityId={}, total={}", communityId, totalCount);
        } catch (Exception e) {
            logger.warn("[SQL统计] 工单统计查询异常: communityId={}, error={}", communityId, e.getMessage());
            stats.put("total", 0);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    public Map<String, Object> pullComplaintStats(String communityId) {
        Map<String, Object> stats = new HashMap<>();

        if (communityId == null || communityId.isEmpty()) {
            logger.warn("[SQL统计] 投诉统计 communityId 为空");
            stats.put("total", 0);
            return stats;
        }

        try {
            String sql = "SELECT c.state, COUNT(*) AS cnt FROM complaint c " +
                    "WHERE c.community_id = ? AND c.status_cd = '0' GROUP BY c.state";
            List<Map<String, Object>> results = getQueryServiceDAO().executeSql(sql, new Object[]{communityId});

            int totalCount = buildStateCounts(stats, results, "complaint", "state");

            stats.put("total", totalCount);
            stats.put("calculatedTotal", totalCount);

            logger.info("[SQL统计] 投诉统计完成: communityId={}, total={}", communityId, totalCount);
        } catch (Exception e) {
            logger.warn("[SQL统计] 投诉统计查询异常: communityId={}, error={}", communityId, e.getMessage());
            stats.put("total", 0);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * 将 SQL GROUP BY 结果构建为 stateCounts 列表，返回总数
     */
    private int buildStateCounts(Map<String, Object> stats, List<Map<String, Object>> results,
                                  String tableName, String columnName) {
        Map<String, String> stateNameMap = getStateCache().getStateMap(tableName, columnName);
        List<Map<String, Object>> stateCounts = new ArrayList<>();
        int total = 0;

        if (results != null) {
            for (Map<String, Object> row : results) {
                String state = toString(row.get("state"));
                Object cntObj = row.get("cnt");
                int count = cntObj instanceof Number ? ((Number) cntObj).intValue() : 0;
                total += count;

                Map<String, Object> item = new HashMap<>();
                item.put("state", state);
                item.put("stateName", stateNameMap.getOrDefault(state, state));
                item.put("count", count);
                stateCounts.add(item);
            }
        }

        stats.put("stateCounts", stateCounts);
        return total;
    }

    private String toString(Object value) {
        return value == null ? "" : value.toString();
    }
}
