-- 场地预约二次改版（一）：开放时间由「场地维度」升级为「场馆维度」
-- 说明：
-- 1. community_space_open_time 新增 venue_id、week_day，业务键由 space_id 改为 venue_id
-- 2. 存量数据回填 venue_id 并按场馆聚合（同场馆任一场地为 Y 则该小时开放）
-- 3. 为存量场馆按 start_time/end_time 补齐 24 行 week_day='0' 配置
-- 4. 注册批量保存接口 communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch
-- 5. 新增「开放时间配置」PC 菜单与权限
-- 幂等：ALTER 语句通过 information_schema 判断后再执行；数据部分使用 NOT EXISTS / 限定条件，可重复执行

-- ---------------------------------------------------------------------------
-- 1. 表结构：新增 venue_id、week_day 与组合索引
-- ---------------------------------------------------------------------------
SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_open_time'
                  AND COLUMN_NAME = 'venue_id') = 0,
              'ALTER TABLE `community_space_open_time` ADD COLUMN `venue_id` varchar(30) NOT NULL DEFAULT '''' COMMENT ''场馆ID'' AFTER `space_id`',
              'SELECT ''venue_id exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_open_time'
                  AND COLUMN_NAME = 'week_day') = 0,
              'ALTER TABLE `community_space_open_time` ADD COLUMN `week_day` varchar(2) NOT NULL DEFAULT ''0'' COMMENT ''星期 0 全周通用 1~7 周一~周日'' AFTER `venue_id`',
              'SELECT ''week_day exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_open_time'
                  AND INDEX_NAME = 'idx_csot_venue_week_hours') = 0,
              'ALTER TABLE `community_space_open_time` ADD INDEX `idx_csot_venue_week_hours` (`venue_id`, `week_day`, `hours`)',
              'SELECT ''idx exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

-- space_id 保留兼容，场馆维度下写入空串
ALTER TABLE `community_space_open_time` MODIFY COLUMN `space_id` varchar(30) NOT NULL DEFAULT '' COMMENT '场地ID（场馆维度下为空）';

-- ---------------------------------------------------------------------------
-- 2. 存量数据回填 venue_id（按 community_space.venue_id）
-- ---------------------------------------------------------------------------
UPDATE `community_space_open_time` csot
  JOIN `community_space` cs ON csot.space_id = cs.space_id AND cs.status_cd = '0'
   SET csot.venue_id = cs.venue_id
 WHERE csot.status_cd = '0'
   AND csot.space_id <> ''
   AND (csot.venue_id = '' OR csot.venue_id IS NULL);

-- ---------------------------------------------------------------------------
-- 3. 为每个场馆生成 24 行 week_day='0' 配置（已存在则不重复插入）
-- ---------------------------------------------------------------------------
INSERT INTO `community_space_open_time` (time_id, space_id, venue_id, week_day, hours, is_open, community_id, status_cd, create_time)
SELECT CONCAT('9', v.venue_id, LPAD(h.hours, 2, '0')), '', v.venue_id, '0', h.hours, 'N', v.community_id, '0', NOW()
  FROM `community_venue` v
 CROSS JOIN (SELECT 0 AS hours UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
             UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
             UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11
             UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
             UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19
             UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23) h
 WHERE v.status_cd = '0'
   AND NOT EXISTS (SELECT 1 FROM `community_space_open_time` t
                    WHERE t.venue_id = v.venue_id AND t.week_day = '0' AND t.hours = h.hours AND t.status_cd = '0');

-- 3.1 按存量场地维度的开放配置聚合：同场馆任一场地某小时为 Y，则场馆该小时开放
UPDATE `community_space_open_time` t
  JOIN (SELECT cs.venue_id AS venueId, csot.hours AS hours
          FROM `community_space_open_time` csot
          JOIN `community_space` cs ON csot.space_id = cs.space_id AND cs.status_cd = '0'
         WHERE csot.status_cd = '0' AND csot.space_id <> '' AND csot.is_open = 'Y'
         GROUP BY cs.venue_id, csot.hours) s
    ON s.venueId = t.venue_id AND s.hours = t.hours
   SET t.is_open = 'Y'
 WHERE t.space_id = '' AND t.week_day = '0' AND t.status_cd = '0';

-- 3.2 无任何存量开放配置的场馆，按场馆开场/关场时间置为开放
UPDATE `community_space_open_time` t
  JOIN `community_venue` v ON t.venue_id = v.venue_id AND v.status_cd = '0'
  JOIN (SELECT venue_id, SUM(CASE WHEN is_open = 'Y' THEN 1 ELSE 0 END) AS openCount
          FROM `community_space_open_time`
         WHERE space_id = '' AND week_day = '0' AND status_cd = '0'
         GROUP BY venue_id) s ON s.venue_id = t.venue_id AND s.openCount = 0
   SET t.is_open = 'Y'
 WHERE t.space_id = '' AND t.week_day = '0' AND t.status_cd = '0'
   AND v.start_time IS NOT NULL AND v.start_time <> ''
   AND v.end_time IS NOT NULL AND v.end_time <> ''
   AND t.hours >= CAST(SUBSTRING_INDEX(v.start_time, ':', 1) AS UNSIGNED)
   AND t.hours <  CAST(SUBSTRING_INDEX(v.end_time, ':', 1) AS UNSIGNED);

-- 3.3 清理场地维度行（逻辑删除）
UPDATE `community_space_open_time` SET status_cd = '1' WHERE space_id <> '' AND status_cd = '0';

-- ---------------------------------------------------------------------------
-- 4. 服务注册：批量保存开放时间（PC app 8000418004）
-- ---------------------------------------------------------------------------
DELETE FROM `c_service` WHERE `service_code` = 'communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch';
DELETE FROM `c_route` WHERE `service_id` = '982026101316310001';

INSERT INTO `c_service` VALUES ('2111', '982026101316310001', 'communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch', 'API', '批量保存场馆开放时间', '1', null, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_route` VALUES ('3097', '8000418004', '982026101316310001', 'D', '1000', 'S', NOW(), '0');

-- ---------------------------------------------------------------------------
-- 5. 菜单与权限：「开放时间配置」（房产组 800201904006，seq=14）
-- ---------------------------------------------------------------------------
DELETE FROM `m_menu` WHERE `m_id` = '702026101316310002';
DELETE FROM `p_privilege` WHERE `p_id` = '502026101316310002';
DELETE FROM `p_privilege_rel` WHERE `p_id` = '502026101316310002';
DELETE FROM `p_privilege_user` WHERE `p_id` = '502026101316310002';

INSERT INTO `m_menu` VALUES ('702026101316310002', '开放时间配置', '800201904006', '/#/pages/property/communityVenueOpenTime', '14', '502026101316310002', '场馆维度开放时间配置', NOW(), '0', 'Y');
INSERT INTO `p_privilege` VALUES ('502026101316310002', '开放时间配置', '场馆维度开放时间配置', NOW(), '0', '800900000003', '/#/pages/property/communityVenueOpenTime', '702026101316310002');

-- 授权：复制「场地管理」权限（502022093035580003）的权限组授权（queryMenus 需授权行才可见）
INSERT INTO `p_privilege_rel` (`p_id`, `pg_id`, `create_time`, `status_cd`)
SELECT DISTINCT '502026101316310002', r.pg_id, NOW(), '0'
  FROM `p_privilege_rel` r
 WHERE r.p_id = '502022093035580003' AND r.status_cd = '0';
