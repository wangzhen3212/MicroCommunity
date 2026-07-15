package com.java110.api.SYSclient.service;

import com.java110.core.log.LoggerFactory;
import com.java110.db.dao.IQueryServiceDAO;
import com.java110.utils.factory.ApplicationContextFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
            String sql = "SELECT r.room_id, r.state FROM building_room r WHERE r.community_id = ? AND r.status_cd = '0'";
            List<Map<String, Object>> results = getQueryServiceDAO().executeSql(sql, new Object[]{communityId});

            Set<String> uniqueRoomIds = new HashSet<>();
            Map<String, Integer> stateCountMap = new HashMap<>();

            if (results != null) {
                for (Map<String, Object> row : results) {
                    String roomId = (String) row.get("room_id");
                    String state = (String) row.get("state");
                    if (roomId != null && !uniqueRoomIds.contains(roomId)) {
                        uniqueRoomIds.add(roomId);
                        stateCountMap.merge(state != null ? state : "", 1, Integer::sum);
                    }
                }
            }

            stats.put("total", uniqueRoomIds.size());

            Map<String, String> stateNameMap = getStateCache().getStateMap("building_room", "state");

            List<Map<String, Object>> stateCounts = new ArrayList<>();
            int sum = 0;
            for (Map.Entry<String, Integer> entry : stateCountMap.entrySet()) {
                String state = entry.getKey();
                int count = entry.getValue();
                sum += count;

                Map<String, Object> item = new HashMap<>();
                item.put("state", state);
                item.put("stateName", stateNameMap.getOrDefault(state, state));
                item.put("count", count);
                stateCounts.add(item);
            }

            stats.put("stateCounts", stateCounts);
            stats.put("calculatedTotal", sum);

            logger.debug("[SQL统计] 房屋统计完成: communityId={}, total={}", communityId, uniqueRoomIds.size());
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
            String sql = "SELECT w.work_id, w.state FROM work_pool w WHERE w.community_id = ? AND w.status_cd = '0'";
            List<Map<String, Object>> results = getQueryServiceDAO().executeSql(sql, new Object[]{communityId});

            Set<String> uniqueWorkIds = new HashSet<>();
            Map<String, Integer> stateCountMap = new HashMap<>();

            if (results != null) {
                for (Map<String, Object> row : results) {
                    String workId = (String) row.get("work_id");
                    String state = (String) row.get("state");
                    if (workId != null && !uniqueWorkIds.contains(workId)) {
                        uniqueWorkIds.add(workId);
                        stateCountMap.merge(state != null ? state : "", 1, Integer::sum);
                    }
                }
            }

            stats.put("total", uniqueWorkIds.size());

            Map<String, String> stateNameMap = getStateCache().getStateMap("work_pool", "state");

            List<Map<String, Object>> stateCounts = new ArrayList<>();
            int sum = 0;
            for (Map.Entry<String, Integer> entry : stateCountMap.entrySet()) {
                String state = entry.getKey();
                int count = entry.getValue();
                sum += count;

                Map<String, Object> item = new HashMap<>();
                item.put("state", state);
                item.put("stateName", stateNameMap.getOrDefault(state, state));
                item.put("count", count);
                stateCounts.add(item);
            }

            stats.put("stateCounts", stateCounts);
            stats.put("calculatedTotal", sum);

            logger.debug("[SQL统计] 工单统计完成: communityId={}, total={}", communityId, uniqueWorkIds.size());
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
            String sql = "SELECT c.complaint_id, c.state FROM complaint c WHERE c.community_id = ? AND c.status_cd = '0'";
            List<Map<String, Object>> results = getQueryServiceDAO().executeSql(sql, new Object[]{communityId});

            Set<String> uniqueComplaintIds = new HashSet<>();
            Map<String, Integer> stateCountMap = new HashMap<>();

            if (results != null) {
                for (Map<String, Object> row : results) {
                    String complaintId = (String) row.get("complaint_id");
                    String state = (String) row.get("state");
                    if (complaintId != null && !uniqueComplaintIds.contains(complaintId)) {
                        uniqueComplaintIds.add(complaintId);
                        stateCountMap.merge(state != null ? state : "", 1, Integer::sum);
                    }
                }
            }

            stats.put("total", uniqueComplaintIds.size());

            Map<String, String> stateNameMap = getStateCache().getStateMap("complaint", "state");

            List<Map<String, Object>> stateCounts = new ArrayList<>();
            int sum = 0;
            for (Map.Entry<String, Integer> entry : stateCountMap.entrySet()) {
                String state = entry.getKey();
                int count = entry.getValue();
                sum += count;

                Map<String, Object> item = new HashMap<>();
                item.put("state", state);
                item.put("stateName", stateNameMap.getOrDefault(state, state));
                item.put("count", count);
                stateCounts.add(item);
            }

            stats.put("stateCounts", stateCounts);
            stats.put("calculatedTotal", sum);

            logger.debug("[SQL统计] 投诉统计完成: communityId={}, total={}", communityId, uniqueComplaintIds.size());
        } catch (Exception e) {
            logger.warn("[SQL统计] 投诉统计查询异常: communityId={}, error={}", communityId, e.getMessage());
            stats.put("total", 0);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    public Map<String, Object> pullFeeStats(String communityId) {
        Map<String, Object> stats = new HashMap<>();

        if (communityId == null || communityId.isEmpty()) {
            logger.warn("[SQL统计] 费用统计 communityId 为空");
            return stats;
        }

        try {
            String sql = "SELECT b.receivable, b.receipts, b.cur_receivable, b.cur_bill, b.bill_time " +
                    "FROM bill b WHERE b.community_id = ? AND b.cur_bill = 'T' LIMIT 1";
            List<Map<String, Object>> results = getQueryServiceDAO().executeSql(sql, new Object[]{communityId});

            if (results != null && !results.isEmpty()) {
                Map<String, Object> bill = results.get(0);

                String receivable = toString(bill.get("receivable"));
                String receipts = toString(bill.get("receipts"));
                String curReceivable = toString(bill.get("cur_receivable"));

                stats.put("currentBillName", bill.get("cur_bill"));
                stats.put("currentBillTime", bill.get("bill_time"));

                BigDecimal receivableBig = toBigDecimal(receivable);
                BigDecimal receiptsBig = toBigDecimal(receipts);
                BigDecimal curReceivableBig = toBigDecimal(curReceivable);

                stats.put("currentReceivable", curReceivableBig.toPlainString());
                stats.put("currentReceipts", receiptsBig.toPlainString());

                BigDecimal rate = BigDecimal.ZERO;
                if (curReceivableBig.compareTo(BigDecimal.ZERO) > 0) {
                    rate = receiptsBig.multiply(new BigDecimal("100")).divide(curReceivableBig, 2, BigDecimal.ROUND_HALF_UP);
                }
                stats.put("collectionRate", rate.toPlainString());

                BigDecimal oweFee = curReceivableBig.subtract(receiptsBig);
                stats.put("currentOweFee", oweFee.toPlainString());

                stats.put("totalReceivable", receivableBig.toPlainString());
                stats.put("totalReceipts", receiptsBig.toPlainString());
            }
        } catch (Exception e) {
            logger.warn("[SQL统计] 费用统计查询异常: communityId={}, error={}", communityId, e.getMessage());
        }

        logger.debug("[SQL统计] 费用统计完成: communityId={}", communityId);
        return stats;
    }

    public Map<String, Object> pullCameraStats(String communityId) {
        Map<String, Object> stats = new HashMap<>();
        List<Map<String, Object>> cameras = new ArrayList<>();

        if (communityId == null || communityId.isEmpty()) {
            logger.warn("[SQL统计] 摄像头统计 communityId 为空");
            stats.put("total", 0);
            stats.put("cameras", cameras);
            return stats;
        }

        try {
            String sql = "SELECT m.machine_id, m.machine_code, m.machine_name, m.machine_ip, " +
                    "m.location_obj_id, m.location_type_cd, m.state FROM machine m WHERE m.community_id = ? AND m.status_cd = '0' AND m.machine_type_cd = '8000'";
            List<Map<String, Object>> results = getQueryServiceDAO().executeSql(sql, new Object[]{communityId});

            if (results != null) {
                for (Map<String, Object> row : results) {
                    Map<String, Object> camera = new HashMap<>();
                    camera.put("machineId", row.get("machine_id"));
                    camera.put("machineCode", row.get("machine_code"));
                    camera.put("machineName", row.get("machine_name"));
                    camera.put("machineIp", row.get("machine_ip"));
                    camera.put("locationObjId", row.get("location_obj_id"));
                    camera.put("locationTypeCd", row.get("location_type_cd"));
                    camera.put("state", row.get("state"));
                    cameras.add(camera);
                }
            }
        } catch (Exception e) {
            logger.warn("[SQL统计] 摄像头统计查询异常: communityId={}, error={}", communityId, e.getMessage());
        }

        stats.put("total", cameras.size());
        stats.put("cameras", cameras);
        return stats;
    }

    private String toString(Object value) {
        return value == null ? "" : value.toString();
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}