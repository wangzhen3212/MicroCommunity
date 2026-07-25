package com.java110.api.SYSclient.service;

import com.java110.core.log.LoggerFactory;
import com.java110.db.dao.IQueryServiceDAO;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StateCache {

    private static final Logger logger = LoggerFactory.getLogger(StateCache.class);

    private final IQueryServiceDAO queryServiceDAO;

    private Map<String, Map<String, String>> cache = new HashMap<>();

    @Autowired
    public StateCache(IQueryServiceDAO queryServiceDAO) {
        this.queryServiceDAO = queryServiceDAO;
    }

    @PostConstruct
    public void init() {
        loadAllDicts();
    }

    private void loadAllDicts() {
        try {
            String sql = "SELECT t.name, t.table_name, t.table_columns, t.status_cd FROM t_dict t WHERE t.table_name IS NOT NULL AND t.table_columns IS NOT NULL";
            List<Map<String, Object>> results = queryServiceDAO.executeSql(sql, new Object[]{});

            if (results != null) {
                for (Map<String, Object> row : results) {
                    String tableName = (String) row.get("table_name");
                    String tableColumns = (String) row.get("table_columns");
                    String name = (String) row.get("name");

                    if (tableName == null || tableColumns == null) {
                        continue;
                    }

                    String key = tableName + ":" + tableColumns;
                    cache.computeIfAbsent(key, k -> new HashMap<>());

                    Map<String, String> columnMap = cache.get(key);
                    String statusCd = (String) row.get("status_cd");
                    if (statusCd != null) {
                        columnMap.put(statusCd, name);
                    }
                }
            }

            logger.info("[StateCache] 状态字典缓存加载完成，共 {} 个表字段映射", cache.size());
            for (Map.Entry<String, Map<String, String>> entry : cache.entrySet()) {
                logger.debug("[StateCache] {} = {}", entry.getKey(), entry.getValue().size());
            }
        } catch (Exception e) {
            logger.error("[StateCache] 加载状态字典缓存失败: {}", e.getMessage(), e);
        }
    }

    public Map<String, String> getStateMap(String tableName, String columnName) {
        String key = tableName + ":" + columnName;
        return cache.getOrDefault(key, new HashMap<>());
    }

    public String getStateName(String tableName, String columnName, String stateValue) {
        Map<String, String> stateMap = getStateMap(tableName, columnName);
        return stateMap.getOrDefault(stateValue, stateValue);
    }

    public void reload() {
        cache.clear();
        loadAllDicts();
    }
}