-- 场地预约二次改版（二）：PC 端核销页（菜单入口 + 核销留痕 + 扫码解析/撤销/统计接口）
-- 说明：
-- 1. community_space_confirm_order 新增 verify_method、verifier_id、verifier_name 与索引，存量行 verify_method='1'
-- 2. t_dict 补 verify_method 字典（1 扫码盒子核销 / 2 手动核销）
-- 3. 服务注册：扫码解析、撤销核销、核销统计
-- 4. 菜单/权限：「场地核销」+ 授权（复制「场地管理」权限的权限组授权）
-- 幂等：ALTER 语句通过 information_schema 判断；其余先 DELETE 再 INSERT

-- ---------------------------------------------------------------------------
-- 1. 核销留痕字段与索引
-- ---------------------------------------------------------------------------
SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_confirm_order'
                  AND COLUMN_NAME = 'verify_method') = 0,
              'ALTER TABLE `community_space_confirm_order` ADD COLUMN `verify_method` varchar(2) DEFAULT ''1'' COMMENT ''核销方式 1 扫码盒子核销 2 手动核销'' AFTER `pay_time`',
              'SELECT ''verify_method exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_confirm_order'
                  AND COLUMN_NAME = 'verifier_id') = 0,
              'ALTER TABLE `community_space_confirm_order` ADD COLUMN `verifier_id` varchar(30) DEFAULT NULL COMMENT ''核销人用户ID'' AFTER `verify_method`',
              'SELECT ''verifier_id exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_confirm_order'
                  AND COLUMN_NAME = 'verifier_name') = 0,
              'ALTER TABLE `community_space_confirm_order` ADD COLUMN `verifier_name` varchar(64) DEFAULT NULL COMMENT ''核销人姓名'' AFTER `verifier_id`',
              'SELECT ''verifier_name exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_confirm_order'
                  AND INDEX_NAME = 'idx_csco_community_time') = 0,
              'ALTER TABLE `community_space_confirm_order` ADD INDEX `idx_csco_community_time` (`community_id`, `create_time`)',
              'SELECT ''idx exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_confirm_order'
                  AND INDEX_NAME = 'idx_csco_csp') = 0,
              'ALTER TABLE `community_space_confirm_order` ADD INDEX `idx_csco_csp` (`csp_id`)',
              'SELECT ''idx exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_space_confirm_order'
                  AND INDEX_NAME = 'idx_csco_time') = 0,
              'ALTER TABLE `community_space_confirm_order` ADD INDEX `idx_csco_time` (`time_id`)',
              'SELECT ''idx exists'' AS msg');
PREPARE hc_stmt FROM @sql;
EXECUTE hc_stmt;
DEALLOCATE PREPARE hc_stmt;

-- 存量核销单按扫码盒子核销记录（核销人留空，页面显示 -）
UPDATE `community_space_confirm_order` SET `verify_method` = '1' WHERE `verify_method` IS NULL OR `verify_method` = '';

-- ---------------------------------------------------------------------------
-- 2. 字典：核销方式
-- ---------------------------------------------------------------------------
DELETE FROM `t_dict` WHERE `id` IN (626361, 626362);
INSERT INTO `t_dict` (`id`, `status_cd`, `name`, `description`, `create_time`, `table_name`, `table_columns`) VALUES
  (626361, '1', '扫码盒子核销', '扫码盒子核销', NOW(), 'community_space_confirm_order', 'verify_method'),
  (626362, '2', '手动核销', '手动核销', NOW(), 'community_space_confirm_order', 'verify_method');

-- ---------------------------------------------------------------------------
-- 3. 服务注册（PC app 8000418004）
-- ---------------------------------------------------------------------------
DELETE FROM `c_service` WHERE `service_code` IN (
  'communitySpace.getCommunitySpaceConfirmOrder',
  'communitySpace.deleteCommunitySpaceConfirmOrder',
  'communitySpace.listCommunitySpaceConfirmOrderSummary');
DELETE FROM `c_route` WHERE `service_id` IN ('982026101316310002', '982026101316310003', '982026101316310004');

INSERT INTO `c_service` VALUES ('2112', '982026101316310002', 'communitySpace.getCommunitySpaceConfirmOrder', 'API', '扫码解析预约核销', '1', null, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_service` VALUES ('2113', '982026101316310003', 'communitySpace.deleteCommunitySpaceConfirmOrder', 'API', '撤销预约核销', '1', null, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_service` VALUES ('2114', '982026101316310004', 'communitySpace.listCommunitySpaceConfirmOrderSummary', 'API', '预约核销统计', '1', null, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', NOW(), '0');

INSERT INTO `c_route` VALUES ('3098', '8000418004', '982026101316310002', 'Q', '1000', 'S', NOW(), '0');
INSERT INTO `c_route` VALUES ('3099', '8000418004', '982026101316310003', 'D', '1000', 'S', NOW(), '0');
INSERT INTO `c_route` VALUES ('3100', '8000418004', '982026101316310004', 'Q', '1000', 'S', NOW(), '0');

-- ---------------------------------------------------------------------------
-- 4. 菜单 / 权限 / 授权：「场地核销」（房产组 800201904006，seq=15）
-- ---------------------------------------------------------------------------
DELETE FROM `m_menu` WHERE `m_id` = '702026101316310001';
DELETE FROM `p_privilege` WHERE `p_id` = '502026101316310001';
DELETE FROM `p_privilege_rel` WHERE `p_id` = '502026101316310001';
DELETE FROM `p_privilege_user` WHERE `p_id` = '502026101316310001';

INSERT INTO `m_menu` VALUES ('702026101316310001', '场地核销', '800201904006', '/#/pages/property/communitySpaceConfirm', '15', '502026101316310001', '场地预约二维码核销', NOW(), '0', 'Y');
INSERT INTO `p_privilege` VALUES ('502026101316310001', '场地核销', '场地预约二维码核销', NOW(), '0', '800900000003', '/#/pages/property/communitySpaceConfirm', '702026101316310001');

-- 授权：复制「场地管理」权限（502022093035580003）的权限组授权
INSERT INTO `p_privilege_rel` (`p_id`, `pg_id`, `create_time`, `status_cd`)
SELECT DISTINCT '502026101316310001', r.pg_id, NOW(), '0'
  FROM `p_privilege_rel` r
 WHERE r.p_id = '502022093035580003' AND r.status_cd = '0';
