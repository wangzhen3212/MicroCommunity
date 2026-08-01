package com.java110.api.SYSclient.service;

import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 社区ID全表替换器（sys-client 自包含实现）
 *
 * 收到上位机社区ID冲突后，将旧社区ID在当前库所有含 community_id 列的基表中全量替换为新ID。
 * 不再跨模块走 Feign/Mapper，全部逻辑收敛在本模块内：
 * 1. 通过 INFORMATION_SCHEMA 动态发现含 community_id 列的基表(排除视图)
 * 2. 逐表 UPDATE community_id = 新ID WHERE community_id = 旧ID
 * 3. 单表失败仅记日志，不中断其余表替换
 *
 * 表名来自 INFORMATION_SCHEMA(非用户输入)，并用 ^[a-zA-Z0-9_]+$ 校验后以反引号拼接到 SQL，
 * 杜绝注入风险；community_id 值通过 JdbcTemplate 占位符参数化传递。
 *
 * @author SYS-client
 */
@Component
public class CommunityIdReplacer {

    private static final Logger logger = LoggerFactory.getLogger(CommunityIdReplacer.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CommunityIdReplacer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 将旧社区ID在所有含 community_id 列的基表中替换为新ID
     *
     * @param oldCommunityId 旧社区ID
     * @param newCommunityId 新社区ID
     * @return 受影响总行数(0 表示旧ID在库中已不存在)
     */
    public int replace(String oldCommunityId, String newCommunityId) {
        // 动态查询当前库中所有含 community_id 列的基表(排除视图)，避免硬编码表清单遗漏
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT DISTINCT c.TABLE_NAME AS t " +
                        "FROM INFORMATION_SCHEMA.COLUMNS c " +
                        "INNER JOIN INFORMATION_SCHEMA.TABLES tb " +
                        "  ON c.TABLE_NAME = tb.TABLE_NAME AND c.TABLE_SCHEMA = tb.TABLE_SCHEMA " +
                        "WHERE c.TABLE_SCHEMA = DATABASE() " +
                        "  AND c.COLUMN_NAME = 'community_id' " +
                        "  AND tb.TABLE_TYPE = 'BASE TABLE'");

        logger.info("[ID更新] 待替换 community_id 的表数量: {}, {} → {}", tables.size(), oldCommunityId, newCommunityId);

        int total = 0;
        for (Map<String, Object> row : tables) {
            String table = String.valueOf(row.get("t"));
            // 表名来自 INFORMATION_SCHEMA，再以白名单校验确保仅含合法标识符字符
            if (table == null || !table.matches("^[a-zA-Z0-9_]+$")) {
                logger.warn("[ID更新] 跳过非法表名: {}", table);
                continue;
            }
            try {
                int affected = jdbcTemplate.update(
                        "UPDATE `" + table + "` SET community_id = ? WHERE community_id = ?",
                        newCommunityId, oldCommunityId);
                if (affected > 0) {
                    logger.info("[ID更新] 表 {} 替换 {} 行", table, affected);
                }
                total += affected;
            } catch (Exception e) {
                // 单表失败不阻断整体替换(如表为视图/无权限等)，记录日志后继续
                logger.error("[ID更新] 替换表 {} 失败: {}", table, e.getMessage());
            }
        }

        logger.info("[ID更新] community_id 替换完成，总影响行数: {}", total);
        return total;
    }
}
