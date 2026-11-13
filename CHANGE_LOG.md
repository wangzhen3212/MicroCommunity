# 变更日志

## 2026-07-31 — 新增：账单手工生成功能

### DDL：新增两张表

```sql
-- 账单手工生成申请主表
CREATE TABLE `manual_bill_generation` (
  `application_id` varchar(30) NOT NULL COMMENT '申请ID',
  `community_id` varchar(30) NOT NULL COMMENT '小区ID',
  `application_name` varchar(200) DEFAULT NULL COMMENT '申请名称',
  `bill_month` varchar(7) DEFAULT NULL COMMENT '目标账单月份，格式YYYY-MM',
  `config_id` varchar(30) DEFAULT NULL COMMENT '费用项ID',
  `fee_name` varchar(100) DEFAULT NULL COMMENT '费用项名称（冗余）',
  `state` varchar(10) DEFAULT NULL COMMENT '状态：1001待审核/1002已通过/1003已驳回',
  `audit_user_id` varchar(30) DEFAULT NULL COMMENT '审核人ID',
  `audit_user_name` varchar(100) DEFAULT NULL COMMENT '审核人姓名',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注/驳回原因',
  `total_count` int(11) DEFAULT NULL COMMENT '总明细数',
  `success_count` int(11) DEFAULT NULL COMMENT '成功生成数',
  `error_count` int(11) DEFAULT NULL COMMENT '生成失败数',
  `remark` varchar(500) DEFAULT NULL COMMENT '申请备注',
  `create_user_id` varchar(30) DEFAULT NULL COMMENT '创建人ID',
  `create_user_name` varchar(100) DEFAULT NULL COMMENT '创建人姓名',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `status_cd` varchar(2) DEFAULT '0' COMMENT '数据状态，0有效1删除',
  PRIMARY KEY (`application_id`),
  KEY `idx_community_id` (`community_id`),
  KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单手工生成申请主表';

-- 账单手工生成明细表
CREATE TABLE `manual_bill_generation_detail` (
  `detail_id` varchar(30) NOT NULL COMMENT '明细ID',
  `application_id` varchar(30) DEFAULT NULL COMMENT '关联申请ID',
  `community_id` varchar(30) DEFAULT NULL COMMENT '小区ID',
  `config_id` varchar(30) DEFAULT NULL COMMENT '费用项ID',
  `fee_name` varchar(100) DEFAULT NULL COMMENT '费用项名称（冗余）',
  `rule_id` varchar(30) DEFAULT NULL COMMENT '对应pay_fee_rule.rule_id',
  `room_id` varchar(30) DEFAULT NULL COMMENT '房屋ID',
  `room_name` varchar(200) DEFAULT NULL COMMENT '房屋名称（冗余）',
  `owner_id` varchar(30) DEFAULT NULL COMMENT '业主ID',
  `owner_name` varchar(100) DEFAULT NULL COMMENT '业主姓名（冗余）',
  `fee_type_cd` varchar(30) DEFAULT NULL COMMENT '费用类型',
  `fee_flag` varchar(10) DEFAULT NULL COMMENT '费用标识（固定1003006周期性）',
  `cur_year_month` varchar(30) DEFAULT NULL COMMENT '当前账期/账单日（冗余自pay_fee_rule.cur_year_month）',
  `prepayment_period` int(11) DEFAULT NULL COMMENT '生成期数',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '预计单期金额',
  `state` varchar(10) DEFAULT NULL COMMENT '明细状态：1001待生成/1002已生成/1003生成失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '生成失败原因',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `status_cd` varchar(2) DEFAULT '0' COMMENT '数据状态，0有效1删除',
  PRIMARY KEY (`detail_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单手工生成明细表';

ALTER TABLE `pay_fee_rule`
  ADD COLUMN `owner_id` varchar(30) DEFAULT NULL COMMENT '业主ID',
  ADD COLUMN `owner_name` varchar(50) DEFAULT NULL COMMENT '业主姓名',
  ADD COLUMN `payer_obj_name` varchar(64) NOT NULL DEFAULT '' COMMENT '缴费对象名称';
```

## 2026-08-06 — 修复：manual_bill_generation_detail 列宽不足

`fee_type_cd` 原 `varchar(10)` 无法容纳 12 位费用类型码（如 `888800010001`），`cur_year_month` 原 `varchar(7)` 无法容纳完整账期日期（如 `2026-09-22`），导致提交申请报 `Data too long`。两列同时扩宽至 `varchar(30)`，与 `pay_fee_detail.fee_type_cd`、`pay_fee_rule.cur_year_month` 保持一致。

```sql
ALTER TABLE `manual_bill_generation_detail`
  MODIFY COLUMN `fee_type_cd` varchar(30) DEFAULT NULL COMMENT '费用类型',
  MODIFY COLUMN `cur_year_month` varchar(30) DEFAULT NULL COMMENT '当前账期/账单日（冗余自 pay_fee_rule.cur_year_month）';
  
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`) VALUES ('2084', '982026080614340001', 'unit.queryUnitsHasOwner', 'API', '查询有业主的单元', '1', NULL, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', '2026-08-06 09:50:28', '0');
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`) VALUES ('2083', '982026073100000006', 'fee.deleteManualBillApplication', 'API', '删除手工账单申请', '1', NULL, 'CMD', 'http://fee-service', 'POST', '60', '3', '8000418002', '2026-08-06 09:32:14', '0');
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`) VALUES ('2082', '982026073100000005', 'fee.auditManualBillApplication', 'API', '审核手工账单生成申请', '1', NULL, 'CMD', 'http://fee-service', 'POST', '60', '3', '8000418002', '2026-08-06 09:32:14', '0');
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`) VALUES ('2078', '982026073100000001', 'fee.searchManualBillTargets', 'API', '查询手工账单生成目标', '1', NULL, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-08-06 09:32:13', '0');
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`) VALUES ('2079', '982026073100000002', 'fee.saveManualBillApplication', 'API', '提交手工账单生成申请', '1', NULL, 'CMD', 'http://fee-service', 'POST', '60', '3', '8000418002', '2026-08-06 09:32:13', '0');
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`) VALUES ('2080', '982026073100000003', 'fee.listManualBillApplications', 'API', '查询手工账单申请列表', '1', NULL, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-08-06 09:32:13', '0');
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`) VALUES ('2081', '982026073100000004', 'fee.getManualBillApplication', 'API', '查询手工账单申请详情', '1', NULL, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-08-06 09:32:13', '0');
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`) VALUES ('3070', '8000418004', '982026080614340001', 'Q', '1000', 'S', '2026-08-06 09:50:43', '0');
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`) VALUES ('3065', '8000418004', '982026073100000002', 'D', '1000', 'S', '2026-08-06 09:32:22', '0');
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`) VALUES ('3069', '8000418004', '982026073100000006', 'D', '1000', 'S', '2026-08-06 09:32:22', '0');
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`) VALUES ('3067', '8000418004', '982026073100000004', 'Q', '1000', 'S', '2026-08-06 09:32:22', '0');
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`) VALUES ('3068', '8000418004', '982026073100000005', 'D', '1000', 'S', '2026-08-06 09:32:22', '0');
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`) VALUES ('3064', '8000418004', '982026073100000001', 'Q', '1000', 'S', '2026-08-06 09:32:22', '0');
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`) VALUES ('3066', '8000418004', '982026073100000003', 'Q', '1000', 'S', '2026-08-06 09:32:22', '0');


```

## 2026-08-11 车辆加入联系人功能
```sql
ALTER TABLE `owner_car`
  ADD COLUMN `pa_id` varchar(30) NOT NULL DEFAULT '-1' COMMENT '停车场ID' AFTER `ps_id`,
  ADD COLUMN `contact` varchar(64) DEFAULT '' COMMENT '联系人' AFTER `pa_id`,
  ADD COLUMN `tel` varchar(20) DEFAULT '' COMMENT '联系电话' AFTER `contact`,
  MODIFY COLUMN `ps_id` varchar(30) DEFAULT '-1' COMMENT '车位ID（-1或无为空车位车辆）';

-- 索引（按停车场维度查重/查询）
ALTER TABLE `owner_car` ADD INDEX `idx_oc_pa_id` (`pa_id`) USING BTREE;
INSERT INTO `t_dict` ( `status_cd`, `name`, `description`, `create_time`, `table_name`, `table_columns`) 
VALUES ( '11', '预付款扣', '预付款扣', '2024-05-31 16:17:51', 'pay_fee_detail', 'prime_rate');

```

## 2026-08-27 车位修改，支持停车区
```sql
DROP TABLE IF EXISTS `parking_area_section`;
CREATE TABLE `parking_area_section` (
  `section_id`  varchar(30) NOT NULL COMMENT '区域ID',
  `pa_id`       varchar(30) NOT NULL COMMENT '停车场ID',
  `community_id` varchar(30) NOT NULL COMMENT '小区ID',
  `section_num` varchar(12) NOT NULL COMMENT '区域编号',
  `section_name` varchar(64) DEFAULT NULL COMMENT '区域名称',
  `remark`      varchar(300) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status_cd`   varchar(2) NOT NULL DEFAULT '0' COMMENT '数据状态 0在用 1失效',
  PRIMARY KEY (`section_id`),
  KEY `idx_pa_id` (`pa_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- 车位表扩展字段（均非必填）
-- ----------------------------
ALTER TABLE `parking_space`
  ADD COLUMN `owner_id`   varchar(30) DEFAULT NULL COMMENT '业主ID' AFTER `pa_id`,
  ADD COLUMN `owner_name` varchar(64) DEFAULT NULL COMMENT '业主名称' AFTER `owner_id`,
  ADD COLUMN `car_id`     varchar(30) DEFAULT NULL COMMENT '车辆ID' AFTER `owner_name`,
  ADD COLUMN `car_num`    varchar(12) DEFAULT NULL COMMENT '车牌号' AFTER `car_id`,
  ADD COLUMN `start_time` datetime DEFAULT NULL COMMENT '出租开始时间' AFTER `car_num`,
  ADD COLUMN `end_time`   datetime DEFAULT NULL COMMENT '出租结束时间' AFTER `start_time`,
  ADD COLUMN `section_id` varchar(30) DEFAULT NULL COMMENT '停车区域ID' AFTER `pa_id`;

-- ----------------------------
-- c_service 服务注册（停车区域 + 停车场-区域树接口）
-- ----------------------------
INSERT INTO `c_service` VALUES ('2085', '982026082700000001', 'parkingArea.saveParkingAreaSection', 'API', '添加停车区域', '1', NULL, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', '2026-08-27 09:34:00', '0');
INSERT INTO `c_service` VALUES ('2086', '982026082700000002', 'parkingArea.updateParkingAreaSection', 'API', '修改停车区域', '1', NULL, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', '2026-08-27 09:34:00', '0');
INSERT INTO `c_service` VALUES ('2087', '982026082700000003', 'parkingArea.deleteParkingAreaSection', 'API', '删除停车区域', '1', NULL, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', '2026-08-27 09:34:00', '0');
INSERT INTO `c_service` VALUES ('2088', '982026082700000004', 'parkingArea.listParkingAreaSections', 'API', '查询停车区域', '1', NULL, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', '2026-08-27 09:34:00', '0');
INSERT INTO `c_service` VALUES ('2089', '982026082700000005', 'parkingArea.queryParkingAreaAndSections', 'API', '查询停车场及停车区域', '1', NULL, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', '2026-08-27 09:34:00', '0');
INSERT INTO `c_service` VALUES ('2090', '982026082700000006', 'parkingSpace.releaseParkingSpace', 'API', '释放车位', '1', NULL, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', '2026-08-28 09:00:00', '0');
INSERT INTO `c_service` VALUES ('2091', '982026082700000007', 'parkingSpace.updateParkingSpaceCar', 'API', '修改车位车辆', '1', NULL, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', '2026-08-28 09:00:00', '0');
-- ----------------------------
-- c_route 路由配置（appId 8000418004 管理端）
-- ----------------------------
INSERT INTO `c_route` VALUES ('3071', '8000418004', '982026082700000001', 'D', '1000', 'S', '2026-08-27 09:34:00', '0');
INSERT INTO `c_route` VALUES ('3072', '8000418004', '982026082700000002', 'D', '1000', 'S', '2026-08-27 09:34:00', '0');
INSERT INTO `c_route` VALUES ('3073', '8000418004', '982026082700000003', 'D', '1000', 'S', '2026-08-27 09:34:00', '0');
INSERT INTO `c_route` VALUES ('3074', '8000418004', '982026082700000004', 'Q', '1000', 'S', '2026-08-27 09:34:00', '0');
INSERT INTO `c_route` VALUES ('3075', '8000418004', '982026082700000005', 'Q', '1000', 'S', '2026-08-27 09:34:00', '0');


-- ----------------------------
-- c_route 路由配置（appId 8000418004 管理端）
-- ----------------------------
INSERT INTO `c_route` VALUES ('3076', '8000418004', '982026082700000006', 'D', '1000', 'S', '2026-08-28 09:00:00', '0');
INSERT INTO `c_route` VALUES ('3077', '8000418004', '982026082700000007', 'D', '1000', 'S', '2026-08-28 09:00:00', '0');
ALTER TABLE `owner_car` DROP COLUMN `car_brand`, DROP COLUMN `car_color`, DROP COLUMN `ps_id`;
ALTER TABLE `business_owner_car` DROP COLUMN `car_brand`, DROP COLUMN `car_color`, DROP COLUMN `ps_id`;
INSERT INTO `c_service` VALUES ('2092', '982026082700000008', 'owner.queryCarsWithoutParkingSpace', 'API', '查询无车位车辆', '1', NULL, 'CMD', 'http://user-service', 'GET', '60', '3', '8000418002', '2026-08-28 09:00:00', '0');
INSERT INTO `c_route` VALUES ('3078', '8000418004', '982026082700000008', 'Q', '1000', 'S', '2026-08-28 09:00:00', '0');
INSERT INTO `c_service` (service_id, service_code, business_type_cd, name, seq, is_instance, url, method, timeout, retry_count, provide_app_id, create_time, status_cd)
VALUES ('9820260827000001', 'cache.queryCache', 'API', '查询缓存列表', '1', 'CMD', 'http://dev-service', 'GET', '60', '3', '8000418002', NOW(), '0');

INSERT INTO `c_service` (service_id, service_code, business_type_cd, name, seq, is_instance, url, method, timeout, retry_count, provide_app_id, create_time, status_cd)
VALUES ('9820260827000002', 'cache.flushCache', 'API', '刷新缓存', '1', 'CMD', 'http://dev-service', 'POST', '60', '3', '8000418002', NOW(), '0');

-- 2. c_route 注册（app 8000418002=PC端，S=同步）
INSERT INTO `c_route` (app_id, service_id, order_type_cd, invoke_limit_times, invoke_model, create_time, status_cd)
VALUES ('8000418004', '9820260827000001', 'Q', NULL, 'S', NOW(), '0');
INSERT INTO `c_route` (app_id, service_id, order_type_cd, invoke_limit_times, invoke_model, create_time, status_cd)
VALUES ('8000418004', '9820260827000002', 'Q', NULL, 'S', NOW(), '0');

-- 3. 前端 URL 变更后同步 p_privilege.resource
UPDATE p_privilege SET resource = '/app/cache.queryCache' WHERE p_id = '502021111540930062';  -- 缓存查询
UPDATE p_privilege SET resource = '/app/cache.flushCache' WHERE p_id = '502021111510430063';  -- 刷新缓存
```

## 2026-08-27 车辆费用修改为车位费用
```sql
UPDATE `m_menu`
SET `name` = '车位收费',
    `url` = '/#/pages/property/parkingSpaceCreateFee'
WHERE `m_id` = '702020013091330005';

-- 2. m_menu_user：菜单名与 url 同步修改（两条）
UPDATE `m_menu_user`
SET `name` = '车位收费',
    `url` = '/#/pages/property/parkingSpaceCreateFee'
WHERE `mu_id` IN ('102026061600146693', '102026061699016707');

-- 3. m_menu_org：url 统一改为新地址（菜单名已是"车位收费"）
UPDATE `m_menu_org`
SET `url` = '/#/pages/property/parkingSpaceCreateFee'
WHERE `m_id` = '702020013091330005';

-- 4. sys_document：帮助文档地址与标题同步修改
UPDATE `sys_document`
SET `doc_code` = '/pages/property/parkingSpaceCreateFee',
    `doc_title` = '车位收费'
WHERE `doc_id` = '8620210101192317775';

```

## 2026-08-28 车位收费页面体验对齐房屋收费
```sql
-- 1. c_service 注册新接口（查询有主车位的停车场及停车区域，SQL exists 过滤无 S/H 车位的停车场/区域）
INSERT INTO `c_service` VALUES ('2095', '982026082700000009', 'parkingArea.queryParkingAreaAndSectionsHasOwner', 'API', '查询有主车位的停车场及停车区域', '1', NULL, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', '2026-08-28 09:00:00', '0');
INSERT INTO `c_route` VALUES ('3081', '8000418004', '982026082700000009', 'Q', '1000', 'S', '2026-08-28 09:00:00', '0');
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2096', '982026082800000001', 'reportFeeMonthStatistics.queryReportFeeDetailParkingSpace', 'API', '查询车位费用明细', '1', NULL, 'CMD', 'http://report-service', 'GET', '60', '3', '8000418002', '2026-08-28 10:00:00', '0');

INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3082', '8000418004', '982026082800000001', 'Q', '1000', 'S', '2026-08-28 10:00:00', '0');
```

## 2026-08-31 移除系统中 FTP 相关内容
```sql
-- 1. 修正 file_rel / business_file_rel 表 save_way 字段注释（ftp → s3）
ALTER TABLE `file_rel` MODIFY COLUMN `save_way` varchar(12) NOT NULL COMMENT '存放方式，s3 table 具体查看t_dict表';
ALTER TABLE `business_file_rel` MODIFY COLUMN `save_way` varchar(12) NOT NULL COMMENT '存放方式，s3 table 具体查看t_dict表';

-- 2. 删除 FTP 遗留六表（无任何业务引用，存量库中若无数据可一并 DROP）
DROP TABLE IF EXISTS `hc_ftp_tasklog_detail`;
DROP TABLE IF EXISTS `hc_ftp_tasklog`;
DROP TABLE IF EXISTS `hc_ftp_task_attr`;
DROP TABLE IF EXISTS `hc_ftp_task`;
DROP TABLE IF EXISTS `hc_ftp_log`;
DROP TABLE IF EXISTS `hc_ftp_item_spec`;

-- 3. t_dict 中 save_way 字典项 ftp → s3
UPDATE `t_dict` SET `status_cd` = 's3', `name` = 's3对象存储', `description` = 's3对象存储' WHERE `id` = '73';

-- 4. 修正 c_mapping_domain FILE_DOMAIN 存储配置说明（去除 ftp 描述）
UPDATE `c_mapping_domain` SET `description` = '系统存储配置，IMG_PATH 配置，如果是S3 时 则配置Nginx代理S3 静态文件访问地址，如果是阿里云OSS 配置 ，配置阿里云OSS图片访问地址，可以登录OSS在后台查看，OSS_SWITCH 为文件存储方式 OSS 为S3对象存储（MinIO） COS 为腾讯云COS存储 OSS 为阿里云OSS存储 需要阿里云购买OSS' WHERE `domain` = 'FILE_DOMAIN';

-- 5. 定时任务菜单 URL 去除 FTP 遗留命名
UPDATE `m_menu_org` SET `url` = '/jobIndex/' WHERE `m_id` = '702019090293430003';

## 2026-09-03 优惠申请（电话申请）改造：支持多费用勾选 + 批量缴费自动打折

### 1. 新增申请-费用关联表 apply_room_discount_fee（TT.sql 已同步）

```sql
DROP TABLE IF EXISTS `apply_room_discount_fee`;
CREATE TABLE `apply_room_discount_fee` (
  `ard_fee_id` varchar(30) NOT NULL COMMENT '主键ID',
  `ard_id` varchar(30) NOT NULL COMMENT '优惠申请ID',
  `fee_id` varchar(30) NOT NULL COMMENT '费用ID',
  `fee_name` varchar(128) DEFAULT NULL COMMENT '费用名称（冗余存储，取自 fee_config.fee_name）',
  `community_id` varchar(30) NOT NULL COMMENT '小区ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status_cd` varchar(2) NOT NULL DEFAULT '0' COMMENT '数据状态0正常1失效',
  PRIMARY KEY (`ard_fee_id`),
  KEY `idx_ard_id` (`ard_id`),
  KEY `idx_fee_id` (`fee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

### 2. 主表 apply_room_discount 删除 fee_id 列（存量库升级需按顺序执行 ①②）

```sql
-- ① 迁移历史数据（存在 fee_id 的旧申请写入子表，fee_name 联查 fee_config 回填）
INSERT INTO apply_room_discount_fee(ard_fee_id, ard_id, fee_id, fee_name, community_id, create_time, status_cd)
SELECT CONCAT('F', REPLACE(UUID(), '-', ''), FLOOR(RAND()*10)), t.ard_id, t.fee_id, fc.fee_name, t.community_id, now(), '0'
FROM apply_room_discount t
LEFT JOIN pay_fee pf ON pf.fee_id = t.fee_id AND pf.status_cd = '0'
LEFT JOIN fee_config fc ON fc.config_id = pf.config_id AND fc.status_cd = '0'
WHERE t.fee_id IS NOT NULL AND t.fee_id != '';

-- ② 删除主表字段
ALTER TABLE `apply_room_discount` DROP COLUMN `fee_id`;
```

> 说明：新库（TT.sql 重建）中 apply_room_discount 已不含 fee_id 列，仅存量库需要执行上述迁移；申请时间由前端默认传值（startTime=当前时间、endTime=2050-01-01 23:59:59，长期有效），批量缴费与单费用缴费命中口径统一为 state='4' and in_use='0' and end_time >= 当前时间 and feeId 命中子表。

```

## 2026-09-08 — 发票抬头统一由商城管理（物业端下线 owner_invoice）


-- 2) invoice_apply 增加抬头快照列(列表/详情展示用, 数据来自商城抬头同步)
ALTER TABLE `invoice_apply`
  ADD COLUMN `invoice_name` varchar(64) DEFAULT NULL COMMENT '发票名头(抬头快照)' AFTER `invoice_type`,
  ADD COLUMN `invoice_num` varchar(64) DEFAULT NULL COMMENT '纳税人识别号(抬头快照)' AFTER `invoice_name`,
  ADD COLUMN `invoice_address` varchar(64) DEFAULT NULL COMMENT '地址(抬头快照)' AFTER `invoice_num`;
```

## 2026-09-08 — 开票页选房交互与发票种类改造（右侧弹出选房/业主直取/普票专票）


```sql
ALTER TABLE `invoice_apply`
  ADD COLUMN `invoice_kind` varchar(12) NOT NULL DEFAULT '普票' COMMENT '发票种类(普票/专票)' AFTER `invoice_type`;

  -- ① 服务定义 c_service
INSERT INTO `c_service` VALUES ('2098', '982026090800000002', 'invoice.queryInvoiceHeadings', 'API', '查询发票抬头(商城)', '1', null, 'CMD', 'http://acct-service', 'GET', '60', '3', '8000418002', '2026-09-08 15:00:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004)
INSERT INTO `c_route` VALUES ('3084', '8000418004', '982026090800000002', 'Q', '1000', 'S', '2026-09-08 15:00:00', '0');
```

## 2026-09-09 — 撤销发票相关冗余接口注册（保留 queryInvoiceHeadings）

```sql
-- 撤销 5 个冗余接口注册（目标库执行，执行后刷新 AppRouteCache 或重启 service-api）
-- c_route 主键为 id（3083/3085/3086/3087/3088），按 service_id 精确匹配删除更安全
DELETE FROM `c_route` WHERE service_id IN ('982026090800000001','982026090800000003','982026090800000004','982026090800000005','982026090800000006');
DELETE FROM `c_service` WHERE service_id IN ('982026090800000001','982026090800000003','982026090800000004','982026090800000005','982026090800000006');
```

## 2026-09-08 — 业主端开票页 App 专用接口注册（c_service / c_route）

```sql
-- ① c_service 服务注册
INSERT INTO `c_service` VALUES ('2103', '982026090800000007', 'fee.queryAppUnInvoiceFeeDetail', 'API', '查询未开票缴费明细(业主端)', '1', NULL, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-09-08 18:30:00', '0');
INSERT INTO `c_service` VALUES ('2104', '982026090800000008', 'invoice.queryAppInvoiceApply', 'API', '查询已开票记录(业主端)', '1', NULL, 'CMD', 'http://acct-service', 'GET', '60', '3', '8000418002', '2026-09-08 18:30:00', '0');

-- ② c_route 路由配置（业主端 app 992020061452450002，与 owner.queryAppOwnerCars 等 App 命令同 app）
INSERT INTO `c_route` VALUES ('3089', '992020061452450002', '982026090800000007', 'Q', '1000', 'S', '2026-09-08 18:30:00', '0');
INSERT INTO `c_route` VALUES ('3090', '992020061452450002', '982026090800000008', 'Q', '1000', 'S', '2026-09-08 18:30:00', '0');
```

> 注：执行后需刷新 AppRouteCache 或重启 service-api；提交开票仍复用 `invoice.saveInvoiceApply`（已注册），无需新增。

## 2026-09-08 — 业主端开票支持多选（取消单条限制）

```sql
INSERT INTO `c_service` VALUES ('2105', '982026090800000009', 'invoice.queryAppInvoiceHeader', 'API', '查询发票抬头(业主端)', '1', NULL, 'CMD', 'http://acct-service', 'GET', '60', '3', '8000418002', '2026-09-08 19:00:00', '0');
INSERT INTO `c_route` VALUES ('3091', '992020061452450002', '982026090800000009', 'Q', '1000', 'S', '2026-09-08 19:00:00', '0');
```


## 2026-09-09 — 车位列表查询条件调整 & 车位申请/审核字段调整

```sql
-- 车位申请表增加停车场ID（申请时选择停车场，审核通过不再强制选择车位）
ALTER TABLE `parking_space_apply`
    ADD COLUMN `pa_id` varchar(30) DEFAULT NULL COMMENT '停车场ID' AFTER `ps_id`;

-- 彻底移除汽车品牌/颜色字段（存量库执行，执行前请备份）
ALTER TABLE `parking_space_apply`
    DROP COLUMN `car_brand`,
    DROP COLUMN `car_color`;
```

## 2026-09-15 — 商城为开票/审核权威源：物业端零状态、只提交 + 只读展示

### 数据库变更（物业库 TT）

```sql
-- ① 服务注册：商城开票状态只读同步 + 提交失败重新提交
INSERT INTO `c_service` VALUES ('2106', '982026090800000010', 'invoice.syncInvoiceApplyState', 'API', '同步商城开票状态', '1', null, 'CMD', 'http://acct-service', 'POST', '60', '3', '8000418002', '2026-09-15 12:29:38', '0');
INSERT INTO `c_service` VALUES ('2107', '982026090800000011', 'invoice.reSubmitInvoiceApply', 'API', '重新提交开票申请', '1', null, 'CMD', 'http://acct-service', 'POST', '60', '3', '8000418002', '2026-09-15 12:29:38', '0');
INSERT INTO `c_route` VALUES ('3092', '8000418004', '982026090800000010', 'D', '1000', 'S', '2026-09-15 12:29:38', '0');
INSERT INTO `c_route` VALUES ('3093', '8000418004', '982026090800000011', 'D', '1000', 'S', '2026-09-15 12:29:38', '0');

-- ② 服务注销：审核/上传发票/登记核销/查询发票事件（TT.sql 中已注释下线；存量库执行下列删除）
DELETE FROM `c_route` WHERE service_id IN ('982023101251070001','982023101245240001','982023101272310011','982023101340300041');
DELETE FROM `c_service` WHERE service_id IN ('982023101251070001','982023101245240001','982023101272310011','982023101340300041');

-- ③ 存量状态清洗 + 字典调整
UPDATE `invoice_apply` SET `state` = 'S' WHERE `state` IN ('W', 'U', 'G', 'C');
UPDATE `t_dict` SET `name` = '提交失败', `description` = '提交失败' WHERE `id` = '626307';
INSERT INTO `t_dict` (`id`,`status_cd`,`name`,`description`,`create_time`,`table_name`,`table_columns`)
SELECT '626311', 'S', '已提交', '已提交', '2026-09-15 12:29:38', 'invoice_apply', 'state' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_dict` WHERE `status_cd`='S' AND `table_name`='invoice_apply' AND `table_columns`='state');

-- ④ 定时兜底同步任务（service-job InvoiceStateSyncJob）
INSERT INTO `task_template` VALUES ('1036', '商城开票状态同步', '兜底同步商城开票状态并回写缴费明细开票状态', '2026-09-15 12:29:38', '0', 'invoiceStateSyncJob');
INSERT INTO `task` VALUES ('522026091512293800', '-1', '商城开票状态同步', '1036', '0 */5 * * * ?', '002', '0', '2026-09-15 12:29:38');
```


## 2026-09-15 — 物业开票单归并：单接口提交（抬头随开票 upsert）· ext_apply_id 关联 · 查询即取商城状态/PDF

### 数据库变更

**商城库（hc_mall）**：`docs/sql/invoice/invoice_order_ext_apply_id.sql`

```sql
ALTER TABLE `invoice_order`
  ADD COLUMN `ext_apply_id` VARCHAR(32) DEFAULT NULL COMMENT '外部(物业)发票申请ID' AFTER `source`,
  ADD KEY `idx_ext_apply` (`ext_apply_id`);

-- 存量 WEB 单可选回填: remark 格式为 applyId|业主名称|业务类目
UPDATE `invoice_order` SET `ext_apply_id` = SUBSTRING_INDEX(`remark`, '|', 1)
 WHERE `source` = 'WEB' AND (`ext_apply_id` IS NULL OR `ext_apply_id` = '') AND `remark` LIKE '%|%';
```

**物业库（TT）**：`docs/sql/invoice/unregister_invoice_sync_state_service.sql`（`docs/db/TT.sql` 已注释下线基线）

```sql
DELETE FROM `c_route` WHERE `service_id` = '982026090800000010';
DELETE FROM `c_service` WHERE `service_id` = '982026090800000010';
```

## 2026-09-15 — 开票中心改造：申请单落房屋（room_id/room_name）· 页面左右布局（房屋树/发票记录）· 商城抬头不再写入开票单备注

```sql
ALTER TABLE `invoice_apply`
  ADD COLUMN `room_id` varchar(30) DEFAULT NULL COMMENT '房屋ID(选房开票时记录)' AFTER `community_id`,
  ADD COLUMN `room_name` varchar(64) DEFAULT NULL COMMENT '房屋编号(楼栋-单元-房号)' AFTER `room_id`,
  ADD KEY `idx_room_id` (`room_id`);
```


## 2026-09-15 — 发票红冲（物业端发起，商城执行）：物业端零新增表 · 红冲数据以商城为权威源 · 开票页/房屋详情页双入口


### 数据库变更

```sql
-- ① 服务定义 c_service
INSERT INTO `c_service` VALUES ('2111', '982026090800000012', 'invoice.saveInvoiceRed', 'API', '发起发票红冲', '1', null, 'CMD', 'http://acct-service', 'POST', '60', '3', '8000418002', '2026-09-15 18:00:00', '0');
INSERT INTO `c_service` VALUES ('2112', '982026090800000013', 'invoice.listInvoiceReds', 'API', '查询发票红冲', '1', null, 'CMD', 'http://acct-service', 'GET', '60', '3', '8000418002', '2026-09-15 18:00:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004, D=POST 受理单, Q=GET 查询单)
INSERT INTO `c_route` VALUES ('3098', '8000418004', '982026090800000012', 'D', '1000', 'S', '2026-09-15 18:00:00', '0');
INSERT INTO `c_route` VALUES ('3099', '8000418004', '982026090800000013', 'Q', '1000', 'S', '2026-09-15 18:00:00', '0');
```

## 2026-10-13 — 场地预约模块优化（场地管理 / 场地预约 / 预约订单）：日期语义统一 · 时段占用可控 · 收款/核销状态流转

> 迁移脚本：`docs/sql/community/20261013_community_space_reservation_optimize.sql`（已同步合入 `docs/db/TT.sql`）

### 数据库变更

```sql
-- ① community_space_person：预约日期语义规范 + 索引 + 收款/取消留痕
ALTER TABLE `community_space_person`
    MODIFY COLUMN `appointment_time` varchar(10) NOT NULL COMMENT '预约日期 YYYY-MM-DD',
    ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '收款时间' AFTER `pay_way`,
    ADD COLUMN `cancel_time` datetime DEFAULT NULL COMMENT '取消时间' AFTER `pay_time`,
    ADD COLUMN `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因' AFTER `cancel_time`,
    ADD INDEX `idx_csp_space_date` (`space_id`, `appointment_time`),
    ADD INDEX `idx_csp_state` (`state`);

-- ② community_space_person_time：新增冗余日期列 + 索引
ALTER TABLE `community_space_person_time`
    ADD COLUMN `appointment_date` varchar(10) NOT NULL DEFAULT '' COMMENT '预约日期 YYYY-MM-DD' AFTER `hours`,
    ADD INDEX `idx_cspt_space_date_hours` (`space_id`, `appointment_date`, `hours`);

-- ③ 存量数据清洗
UPDATE `community_space_person` SET `appointment_time` = LEFT(`appointment_time`, 10) WHERE CHAR_LENGTH(`appointment_time`) > 10;
UPDATE `community_space_person_time` cspt JOIN `community_space_person` csp ON cspt.csp_id = csp.csp_id
   SET cspt.appointment_date = csp.appointment_time
 WHERE cspt.appointment_date = '' OR cspt.appointment_date IS NULL;

-- ④ community_space_confirm_order：核销单收款留痕
ALTER TABLE `community_space_confirm_order`
    ADD COLUMN `received_amount` decimal(10,2) DEFAULT '0.00' COMMENT '实收金额' AFTER `space_id`,
    ADD COLUMN `pay_way` varchar(12) DEFAULT NULL COMMENT '支付方式 1 现金 2 微信 3 支付宝' AFTER `received_amount`,
    ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '收款时间' AFTER `pay_way`;

-- ⑤ 字典：删除语义冲突的 626109(F 待支付)，补齐订单/时段状态与收款方式（id 626353~626360）
DELETE FROM `t_dict` WHERE `table_name` = 'community_space_person' AND `table_columns` = 'state' AND `status_cd` = 'F' AND `name` = '待支付';
INSERT INTO `t_dict` (`id`,`status_cd`,`name`,`description`,`create_time`,`table_name`,`table_columns`) VALUES
  ('626353', 'WP', '待支付', '待支付', NOW(), 'community_space_person', 'state'),
  ('626354', 'C',  '已完成', '已完成', NOW(), 'community_space_person', 'state'),
  ('626355', 'W',  '待核销', '待核销', NOW(), 'community_space_person_time', 'state'),
  ('626356', 'C',  '已核销', '已核销', NOW(), 'community_space_person_time', 'state'),
  ('626357', 'CL', '已取消', '已取消', NOW(), 'community_space_person_time', 'state'),
  ('626358', '1',  '现金',   '现金',   NOW(), 'community_space_confirm_order', 'pay_way'),
  ('626359', '2',  '微信',   '微信',   NOW(), 'community_space_confirm_order', 'pay_way'),
  ('626360', '3',  '支付宝', '支付宝', NOW(), 'community_space_confirm_order', 'pay_way');

-- ⑥ 服务注册（c_service 2110 / c_route 3096）
INSERT INTO `c_service` VALUES ('2110', '982026101315180001', 'communitySpace.listCommunitySpacePersonSummary', 'API', '场地预约订单汇总', '1', null, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_route` VALUES ('3096', '8000418004', '982026101315180001', 'Q', '1000', 'S', NOW(), '0');
```

## 2026-10-13 — 场地预约二次改版：开放时间配置独立页（场馆维度）· 场地预约页交互修正 · 预约订单页筛选表格合并 · PC 核销页按原型实现

> 迁移脚本：`docs/sql/community/20261013_community_venue_open_time.sql`、`docs/sql/community/20261013_community_space_confirm_optimize.sql`（均已同步合入 `docs/db/TT.sql`）

### 数据库变更

```sql
-- ① community_space_open_time：开放时间维度由场地(space_id)升级为场馆(venue_id) + 星期(week_day)
ALTER TABLE `community_space_open_time`
  ADD COLUMN `venue_id` varchar(30) NOT NULL DEFAULT '' COMMENT '场馆ID' AFTER `space_id`,
  ADD COLUMN `week_day` varchar(2) NOT NULL DEFAULT '0' COMMENT '星期 0 全周通用 1~7 周一~周日' AFTER `venue_id`,
  MODIFY COLUMN `space_id` varchar(30) NOT NULL DEFAULT '' COMMENT '场地ID（场馆维度下为空）',
  ADD INDEX `idx_csot_venue_week_hours` (`venue_id`, `week_day`, `hours`);
-- 存量数据：按 community_space.venue_id 回填 → 同场馆任一场地该小时为 Y 则场馆开放 → 为存量场馆按 start_time/end_time 补 24 行
-- week_day='0' → 逻辑删除场地维度行（空间维度配置不再参与判定）

-- ② community_space_confirm_order：核销留痕（核销方式/核销人）+ 索引
ALTER TABLE `community_space_confirm_order`
  ADD COLUMN `verify_method` varchar(2) DEFAULT '1' COMMENT '核销方式 1 扫码盒子核销 2 手动核销' AFTER `pay_time`,
  ADD COLUMN `verifier_id` varchar(30) DEFAULT NULL COMMENT '核销人用户ID' AFTER `verify_method`,
  ADD COLUMN `verifier_name` varchar(64) DEFAULT NULL COMMENT '核销人姓名' AFTER `verifier_id`,
  ADD INDEX `idx_csco_community_time` (`community_id`, `create_time`),
  ADD INDEX `idx_csco_csp` (`csp_id`),
  ADD INDEX `idx_csco_time` (`time_id`);
UPDATE `community_space_confirm_order` SET `verify_method` = '1' WHERE `verify_method` IS NULL OR `verify_method` = '';

-- ③ 字典：核销方式（id 626361~626362）
INSERT INTO `t_dict` (`id`,`status_cd`,`name`,`description`,`create_time`,`table_name`,`table_columns`) VALUES
  ('626361', '1', '扫码盒子核销', '扫码盒子核销', NOW(), 'community_space_confirm_order', 'verify_method'),
  ('626362', '2', '手动核销', '手动核销', NOW(), 'community_space_confirm_order', 'verify_method');

-- ④ 服务注册：场馆开放时间批量保存 + 扫码解析/撤销核销/核销统计（c_service 2111~2114 / c_route 3097~3100）
INSERT INTO `c_service` VALUES ('2114', '982026101316310001', 'communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch', 'API', '批量保存场馆开放时间', '1', null, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_service` VALUES ('2115', '982026101316310002', 'communitySpace.getCommunitySpaceConfirmOrder', 'API', '扫码解析预约核销', '1', null, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_service` VALUES ('2116', '982026101316310003', 'communitySpace.deleteCommunitySpaceConfirmOrder', 'API', '撤销预约核销', '1', null, 'CMD', 'http://community-service', 'POST', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_service` VALUES ('2117', '982026101316310004', 'communitySpace.listCommunitySpaceConfirmOrderSummary', 'API', '预约核销统计', '1', null, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_route` VALUES ('3101', '8000418004', '982026101316310001', 'D', '1000', 'S', NOW(), '0');
INSERT INTO `c_route` VALUES ('3102', '8000418004', '982026101316310002', 'Q', '1000', 'S', NOW(), '0');
INSERT INTO `c_route` VALUES ('3103', '8000418004', '982026101316310003', 'D', '1000', 'S', NOW(), '0');
INSERT INTO `c_route` VALUES ('3104', '8000418004', '982026101316310004', 'Q', '1000', 'S', NOW(), '0');

-- ⑤ 菜单/权限/授权（房产组 800201904006）：开放时间配置 seq=14、场地核销 seq=15
INSERT INTO `m_menu` VALUES ('702026101316310002', '开放时间配置', '800201904006', '/#/pages/property/communityVenueOpenTime', '14', '502026101316310002', '场馆维度开放时间配置', NOW(), '0', 'Y');
INSERT INTO `m_menu` VALUES ('702026101316310001', '场地核销', '800201904006', '/#/pages/property/communitySpaceConfirm', '15', '502026101316310001', '场地预约二维码核销', NOW(), '0', 'Y');
INSERT INTO `p_privilege` VALUES ('502026101316310002', '开放时间配置', '场馆维度开放时间配置', NOW(), '0', '800900000003', '/#/pages/property/communityVenueOpenTime', '702026101316310002');
INSERT INTO `p_privilege` VALUES ('502026101316310001', '场地核销', '场地预约二维码核销', NOW(), '0', '800900000003', '/#/pages/property/communitySpaceConfirm', '702026101316310001');
-- 授权：两个新权限均按「场地管理」(502022093035580003) 的权限组授权复制（queryMenus 需授权行才可见）
INSERT INTO `p_privilege_rel` (`p_id`, `pg_id`, `create_time`, `status_cd`)
SELECT DISTINCT '502026101316310002', r.pg_id, NOW(), '0' FROM `p_privilege_rel` r WHERE r.p_id = '502022093035580003' AND r.status_cd = '0';
INSERT INTO `p_privilege_rel` (`p_id`, `pg_id`, `create_time`, `status_cd`)
SELECT DISTINCT '502026101316310001', r.pg_id, NOW(), '0' FROM `p_privilege_rel` r WHERE r.p_id = '502022093035580003' AND r.status_cd = '0';
```

## 2026-10-19 — 缴费订单/缴费明细导出对齐页面：拆分导出适配器 · 列名与列顺序修正


```sql
-- 字典：下载中心文件类型（id 626363，已同步合入 docs/db/TT.sql）
INSERT INTO `t_dict` VALUES ('626363', 'reportPayFeeOrder', '缴费订单表', '缴费订单表', '2026-10-19 09:00:00', 'user_download_file', 'file_type');
```

## 2026-10-19 — 补打收据：类型下拉缺失修复 + 「全部X」按业主名下对象过滤

```sql
-- 字典：补打收据对象类型补充「商铺」（id 626364，已同步合入 docs/db/TT.sql）
-- 未执行时前端内置兜底选项仍可选出商铺
INSERT INTO `t_dict` VALUES ('626364', '4444', '商铺', '商铺', '2026-10-19 16:42:20', 'fee_receipt', 'obj_type');
```

- 前端 `components/fee/simplifyFeeReceipt.vue`：创建即加载 `fee_receipt/obj_type` 字典并内置兜底项（房屋/商铺/车位/合同）、新增商铺二级下拉、合同改按 `objPersonId` 查询、「全部X」按 `payObjId`（即业主）过滤、未选业主/对象时不发请求、`_getFeeObjName` 支持 4444。
- 父页面 `views/simplify/simplifyAcceptanceList.vue`：多房业主改打开房间树选房（原 `$refs.searchRoom` 不存在导致 roomId 为空）。
- 后端 `GetFeeReceiptBMOImpl.freshRoomInfo`：商铺/合同不再按车位刷新对象名。

## 2026-10-19 — 补打收据：业主无房屋时误提示「请选择对象」· 车位对象口径修正

无 DDL/字典变更。

- 前端 `components/fee/simplifyFeeReceipt.vue`：进入 Tab 由 `_initFeeReceiptObjType` 按「房屋→车位→商铺→合同→全部」自动定位对象类型，全程不弹提示；`当前X` 无具体对象时自动降级为「全部X」按业主查询（去掉「请选择对象」）；车位下拉改用 `ownerParkingSpaces`（`psId` + 车位编号 `区域-区域-车位号`）；`carId/ownerCars` 改为 `psId/ownerParkingSpaces`；`_getFeeObjName(6666)` 与类型下拉文案改为「车位」；类型切换时该类型无数据提示「该业主名下没有X」并自动切「全部X」。
- 前端 `api/fee/simplifyFeeReceiptApi.js`：新增 `listOwnerParkingSpaces`（`parkingSpace.queryParkingSpaces`，按业主过滤），移除无引用的 `queryOwnerCars`。
- 前端 `views/fee/roomCreateFeeLang.js`：`simplifyFeeReceipt` 三语新增 `parkingSpace/selectParkingSpace/noParkingSpace/noRoom`。
- 后端 `java110-db/mapper/community/ParkingSpaceServiceDaoImplMapper.xml`：`getParkingSpaceInfo` 与 `queryParkingSpacesCount` 同口径新增 `ownerId` 条件（`t.owner_id = #{ownerId}`，车位表自身即有 `owner_id`，与 `building_shop_owner.owner_id` 口径一致）。

## 2026-10-20 — 报表（/pages/report/dataReport）停车费按车位口径统计

```sql
-- 服务注册：收费情况/月实收明细/月欠费明细（停车费，GET，c_service 2115~2117 / c_route 3101~3103，已同步合入 docs/db/TT.sql）
INSERT INTO `c_service` VALUES ('2118', '982026102012120001', 'dataReport.queryParkingFeeStatistics', 'API', '查询收费情况(停车费)', '1', null, 'CMD', 'http://report-service', 'GET', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_service` VALUES ('2119', '982026102012120002', 'dataReport.queryParkingMonthReceivedDetail', 'API', '查询月实收明细(停车费)', '1', null, 'CMD', 'http://report-service', 'GET', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_service` VALUES ('2120', '982026102012120003', 'dataReport.queryParkingMonthOweDetail', 'API', '查询月欠费明细(停车费)', '1', null, 'CMD', 'http://report-service', 'GET', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_route` VALUES ('3105', '8000418004', '982026102012120001', 'Q', '1000', 'S', NOW(), '0');
INSERT INTO `c_route` VALUES ('3106', '8000418004', '982026102012120002', 'Q', '1000', 'S', NOW(), '0');
INSERT INTO `c_route` VALUES ('3107', '8000418004', '982026102012120003', 'Q', '1000', 'S', NOW(), '0');

-- 字典：下载中心文件类型（id 626365~626367，已同步合入 docs/db/TT.sql）
INSERT INTO `t_dict` VALUES ('626365', 'dataReportParkingFeeStatistics', '收费情况(停车费)', '收费情况(停车费)', NOW(), 'user_download_file', 'file_type');
INSERT INTO `t_dict` VALUES ('626366', 'dataMonthReceivedParkingStatistics', '月实收明细(停车费)', '月实收明细(停车费)', NOW(), 'user_download_file', 'file_type');
INSERT INTO `t_dict` VALUES ('626367', 'dataMonthOweParkingStatistics', '月欠费明细(停车费)', '月欠费明细(停车费)', NOW(), 'user_download_file', 'file_type');
```

- 后端 `java110-db/mapper/report/ReportFeeStatisticsServiceDaoImplMapper.xml`：新增 7 条车位口径 SQL（`payer_obj_type='6666'`、`parking_space.ps_id = pay_fee.payer_obj_id`、`parking_area.pa_id = parking_space.pa_id`）：`getParkingAreaFeeStatistics`、`getParkingMonthReceivedDetailCount/Info/Amount`、`getParkingMonthOweDetailCount/Info/Amount`，既有房屋口径节点零改动。
- 后端 `QueryStatisticsDto`：新增 `paId/paIds/todayStartDate/todayEndDate/monthEndDate` 字段。
- 后端 `IReportFeeStatisticsServiceDao`、`ReportFeeStatisticsServiceDaoImpl`、`IReportFeeStatisticsInnerServiceSMO`、`ReportFeeStatisticsInnerServiceSMOImpl`（明细 Info 做 `(page-1)*row`）、`IFeeStatistics`、`FeeStatisticsImpl`：新增上述 7 个方法。
- 后端新增 CMD：`QueryParkingFeeStatisticsCmd`（按停车场循环聚合）、`QueryParkingMonthReceivedDetailCmd`、`QueryParkingMonthOweDetailCmd`（`ResultVo` + `sumTotal`）。
- 后端新增导出适配：`DataReportParkingFeeStatisticsAdapt`（`dataReportParkingFeeStatistics`，列头 停车场/车位数/收费车位数）、`DataMonthReceivedParkingStatisticsAdapt`（`dataMonthReceivedParkingStatistics`，列头「车位」）、`DataMonthOweParkingStatisticsAdapt`（`dataMonthOweParkingStatistics`，列头「车位」）。
- 前端 `api/report/dataReportApi.js`：新增 `queryParkingAreas`（`parkingArea.listParkingAreas`）、`queryParkingFeeStatistics`、`queryParkingMonthReceivedDetail`、`queryParkingMonthOweDetail`。
- 前端 `components/report/`：三个 Tab 改为容器（仅保留左侧费用大类列表 + 动态组件，`feeTypeCd === '888800010008'` 时渲染车位组件，`:key="feeTypeCd"` 强制重建），右侧内容拆为 `*Room.vue`（原逻辑平移）与 `*Parking.vue`（停车场维度、停车场下拉、「车位」列、新导出 pagePath）。
- 前端 `views/report/dataReportLang.js`：三语新增 `parkingArea/parkingSpaceCount/feeParkingSpaceCount/parkingSpace/selectParkingArea/parkingRemainingCount`。

## 2026-10-20 — 报表口径统一：实收金额/收款方式统计不限支付对象类型（可对账）

无 DDL/字典/服务注册变更。

- 后端 `java110-db/mapper/report/ReportFeeStatisticsServiceDaoImplMapper.xml`：新增 `getReceivedFeeAll`（与 `getReceivedFee` 同源，唯一区别不限制 `pf.payer_obj_type`，`endDate` 用 `<=`）；`getReceivedFeeByPrimeRate` 去掉 `payer_obj_type='3333'`、`pay_fee` 由 `left join` 改 `inner join`、时间边界统一 `<= #{endDate}`，不做「其他」兜底分组。`getReceivedFee` 本身未改动（月度费用汇总、费用汇总导出、小区费用汇总数值不变）。
- 后端 `IReportFeeStatisticsServiceDao`、`ReportFeeStatisticsServiceDaoImpl`、`IReportFeeStatisticsInnerServiceSMO`、`ReportFeeStatisticsInnerServiceSMOImpl`、`IFeeStatistics`、`FeeStatisticsImpl`：新增 `getReceivedFeeAll`。
- 后端 `QueryFeeDataReportCmd`：顶部「实收金额」改调 `getReceivedFeeAll`（含房屋/车位/车辆/商铺/合同收款），与收款方式统计各方式之和逐分相等；`QueryReceivedWayStatisticsCmd` 仅补注释，逻辑不变。
- 前端 `views/report/dataReportLang.js`：三语新增 `receivedFeeTip/monthPaidAmountTip`。
- 前端 `views/report/dataReportList.vue`：「实收金额」卡片加口径 tooltip；`components/report/DataReportFeeStatisticsRoom.vue`、`...Parking.vue`：「当月已收金额」列头加 tooltip（本月1日至月末；停车费仅统计车位收款）。

## 2026-10-23 — 商铺删除 / 退铺（shop.deleteBuildingShop、shop.deleteBuildingShopOwner）报错修复

无 DDL/字典/服务注册变更（仅代码，可选脏数据修复 SQL 见下）。

```sql
-- 可选：修复历史 status_cd 为 NULL 的商铺/租赁脏数据（先查后改）
SELECT shop_id, status_cd FROM building_shop WHERE status_cd IS NULL;
UPDATE building_shop SET status_cd = '0' WHERE status_cd IS NULL;
UPDATE building_shop_owner SET status_cd = '0' WHERE status_cd IS NULL;
```

- 后端 `java110-db/mapper/community/BuildingShopOwnerServiceDaoImplMapper.xml`：`deleteBuildingShopOwner` 改为「单条 `shopOwnerId` / 批量 `shopId(+communityId)`」双入参逻辑删除（`SET status_cd='1'`，`<where>` 动态条件 + `status_cd='0'`）；`updateBuildingShopOwner` 由 `<set>` 改 `<trim prefix="SET" suffixOverrides=",">` 并补 `community_id`、`status_cd` 条件赋值，修复「update 语句没有 set」。
- 后端 `java110-db/mapper/community/BuildingShopServiceDaoImplMapper.xml`：`updateBuildingShopInfo` 的 `status_cd` 改为条件更新（`statusCd` 非空才写），其余字段同步改 `<set> + 逗号后缀` 写法，`shopId` 存在时追加恒真项 `t.shop_id = t.shop_id` 兜底，避免 `status_cd` 被置 NULL/脏值导致商铺从列表消失。
- 后端 `service-community/dao/impl/BuildingShopOwnerV1ServiceDaoImpl`：`deleteBuildingShopOwnerInfo` 改为幂等（`shopOwnerId`/`shopId`/`communityId` 全空抛 `DAOException`，影响 0 行仅 warn 并返回 0）；`updateBuildingShopOwnerInfo` 前置校验 `shopOwnerId` 必填且至少一个可更新字段。
- 后端 `service-community/smo/impl/BuildingShopOwnerInnerServiceSMOImpl`：`deleteBuildingShopOwner` 改走 `deleteBuildingShopOwnerInfo`（专用删除 mapper），不再 `setStatusCd("1")` 后复用动态 update。
- 后端 `service-community/cmd/shop/DeleteBuildingShopCmd`：批量逻辑删除租赁关系后校验影响行数，失败文案明确。
- 后端 `service-community/cmd/shop/DeleteBuildingShopOwnerCmd`：`validate` 改为 `shopOwnerId` 或 `(shopId + ownerId)` 至少一组（提示「shopOwnerId 或 (shopId + ownerId) 不能为空」）；退铺后按该商铺剩余有效租赁记录回写 `building_shop.state`（无剩余置 `2008` 空置，仍有记录取剩余首条 `state`），且不传 `statusCd` 以免污染商铺状态。

## 2026-11-05 — 车位结构图（/pages/property/carStructure）改造：左树右卡片 + 欠费感知

新增服务注册 `car.listParkingSpaceStructures`（`c_service` 2126 + `c_route` 3108，物业端 app `8000418004`，GET 对应 `Q`）；无 DDL/字典/菜单变更。脚本：`docs/sql/car/20261105_parking_space_structure_service.sql`。

```sql
-- ① 服务定义 c_service
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2126', '982026110515300001', 'car.listParkingSpaceStructures', 'API', '查询车位结构图数据', '1', null, 'CMD', 'http://report-service', 'GET', '60', '3', '8000418002', '2026-11-05 15:30:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004, GET 用 Q)
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3108', '8000418004', '982026110515300001', 'Q', '1000', 'S', '2026-11-05 15:30:00', '0');
```

## 2026-11-09 — 事务回退日志列值错位（`Incorrect datetime value ... for column 'end_time'`）修复

无 DDL/字典/服务注册变更（仅代码，可选脏数据清理 SQL 见下）。

```sql
-- 可选：清理错位写入的 business_pay_fee（中间表，按 b_id 精确清理后再由业务重新生成）
SELECT b_id, o_id, action, action_obj, log_text FROM c_order_item WHERE action_obj = 'pay_fee' ORDER BY create_time DESC LIMIT 10;
DELETE FROM business_pay_fee WHERE b_id = '202026110907400396' AND operate = 'ADD';
```

- 新增 `java110-db/src/main/java/com/java110/db/SqlInsertParser.java`：按 INSERT 的列段与 VALUES 段逐列对位，占位符（`?`）按参数顺序取值、字面量列（`'0'`、`'-1'`、`now()` 等）跳过；支持 `<foreach>` 多行 VALUES；结构无法安全对齐（列数与 token 数不等、函数内含占位符、`insert ... select`）时返回 `null`。
- 后端 `java110-db/.../Java110MybatisInterceptor.java`：INSERT 分支改为走 `SqlInsertParser`，不再用「列序号 = 参数序号」硬配对；`dealInsertSql` 支持多行 `afterValues`（有序 `JSONObject(true)`），解析失败仅 warn 并跳过写日志（宁缺勿错）；`replaceFirst` 正则替换改为 `buildSqlWithValues` 顺序重建（跳过单引号内 `?`，值含 `$`/`\`/`?` 不再异常）；`getParameterValue`/`dealReturnMap` 对字符串值做 `'` → `''` 转义。
- 后端 `service-order/.../AsynNotifySubServiceImpl.java`、`service-order/.../OIdServiceSMOImpl.java`：`business_*` 历史表/回退 SQL 的列顺序由 `JSONObject.keySet()` 改为 `TreeMap` 字典序，避免列顺序随 hash 变化。

## 2026-11-13 — Java 17 自定义公式金额为 0（内置 nashorn 引擎被移除）修复

无 DDL/字典/服务注册变更，仅代码与 Maven 依赖调整。

```sql
-- 本次无数据库变更
-- 说明：Java 15+（含 17）已按 JEP 372 移除内置 nashorn 引擎，
--      javax.script 取不到 JavaScript 引擎导致公式求值 NPE 后被 catch 吞掉，应收金额恒为 0。
--      改为依赖 org.openjdk.nashorn:nashorn-core:15.4，由 java110-utils 统一提供求值入口。
```

- 新增 `java110-utils/src/main/java/com/java110/utils/util/FormulaUtil.java`：公式求值统一入口。`getEngine()` 按 `nashorn`/`JavaScript`/`javascript` 顺序获取引擎并用 `ThreadLocal` 线程内复用；`normalize()` 去换行与首尾空格；`evalAmount(expression[, formulaText])` 求值并按 `FeeConfigConstant.FEE_SCALE` 四舍五入，异常（含引擎为 null、非法公式）统一记录 error 日志（公式原文 + 替换后表达式 + 堆栈）后返回 0，不向上抛；`engineInfo()` 供启动自检与运维排查。
- 根 `pom.xml`：`dependencyManagement` 新增 `org.openjdk.nashorn:nashorn-core:15.4`；`java110-utils/pom.xml` 增加该依赖（compile 硬依赖，随 `springboot` 聚合包一起发布）。nashorn 传递依赖 `org.ow2.asm 7.3.1`（asm/asm-commons/asm-tree/asm-util/asm-analysis），项目内无 compile 作用域的 asm 竞争，故不做 asm 版本锁定。
- 后端 `service-fee/src/main/java/com/java110/fee/bill/formula/FeeFormulaCustomImpl.java`：房屋/车位/商铺三处 `ScriptEngineManager` 调用替换为 `FormulaUtil.evalAmount(...)`，保留「负数归 0」与 `remark` 拼公式逻辑，删除 `javax.script` import。
- 后端 `java110-core/src/main/java/com/java110/core/smo/impl/ComputeFeeSMOImpl.java`：车位、房屋（合同复用房屋方法）两处替换为 `FormulaUtil.evalAmount(...)`，保留「负数归 0」逻辑。
- 后端 `service-fee/src/main/java/com/java110/fee/bmo/importFee/impl/FeeSharingBMOImpl.java`：导入分摊公式改为 `FormulaUtil.evalAmount(...)`，`公摊用量` 与 `公摊公式` 记录口径不变（2 位小数四舍六入五成双），公式异常不再抛「公式计算异常」，改为返回 0 并记录 error 日志。
- 后端 `service-job/src/main/java/com/java110/job/adapt/fee/FloorShareReadingAdapt.java`：删除未使用的 `javax.script` import。
- 后端 `springboot/src/main/java/com/java110/boot/BootApplicationStart.java`：启动后打印一次 `FormulaUtil.engineInfo()`，便于确认运行通道（正常为 `OpenJDK Nashorn 15.4`，缺失时为「未获取到公式引擎(nashorn 缺失)」）。

## 2026-11-13 — 业主名称/owner_id 变更同步 pay_fee、pay_fee_rule 业主信息

无 DDL/字典/服务注册变更（仅代码 + 存量校准 SQL）。方案：`.txcode/plan-code/20261113115753/20261113115753_方案.md`

```sql
-- 存量校准：业主改名后历史账单/规则的 owner_name 未同步，分批执行（示例 2000 行/批）
UPDATE pay_fee pf
  JOIN building_owner bo ON pf.owner_id = bo.owner_id AND bo.status_cd = '0'
SET pf.owner_name = bo.name
WHERE pf.status_cd = '0' AND (pf.owner_name IS NULL OR pf.owner_name <> bo.name)
LIMIT 2000;

UPDATE pay_fee_rule pfr
  JOIN building_owner bo ON pfr.owner_id = bo.owner_id AND bo.status_cd = '0'
SET pfr.owner_name = bo.name
WHERE pfr.status_cd = '0' AND (pfr.owner_name IS NULL OR pfr.owner_name <> bo.name)
LIMIT 2000;

-- 建议（可选，执行前请 DBA 确认索引现状，避免大表全扫）：按业主同步的过滤列补索引
-- ALTER TABLE pay_fee ADD INDEX i_pf_owner_id (owner_id);
-- ALTER TABLE pay_fee_rule ADD INDEX i_pfr_owner_id (owner_id);
```

- 后端 `java110-bean/.../dto/fee/PayFeeDto.java`、`java110-bean/.../dto/payFee/PayFeeRuleDto.java`：新增 `oldOwnerId`（变更前业主ID），用于「旧 owner_id → 新 owner_id」定位。
- 后端 `java110-interface/intf/fee/IPayFeeV1InnerServiceSMO`、`IPayFeeRuleV1InnerServiceSMO`：新增 `updatePayFeeOwner`（`/payFeeV1Api`）、`updatePayFeeRuleOwner`（`/payFeeRuleV1Api`），返回同步影响行数。
- 后端 `service-fee/dao/{IPayFeeV1ServiceDao,IPayFeeRuleV1ServiceDao}` + `dao/impl/*Impl`：新增 `updatePayFeeOwnerInfo` / `updatePayFeeRuleOwnerInfo`，委托 `sqlSessionTemplate.update`。
- 后端 `java110-db/mapper/fee/PayFeeV1ServiceDaoImplMapper.xml`、`PayFeeRuleV1ServiceDaoImplMapper.xml`：新增 `updatePayFeeOwnerInfo` / `updatePayFeeRuleOwnerInfo`，按 `community_id + owner_id` 批量改 `owner_id`/`owner_name`；`ownerName` 条件过滤 `(owner_name is null or owner_name <> 新名称)` 保证分批可收敛，`limit #{row}` 控制单批行数。
- 后端 `service-fee/smo/impl/PayFeeV1InnerServiceSMOImpl`、`PayFeeRuleV1InnerServiceSMOImpl`：新增 `updatePayFeeOwner` / `updatePayFeeRuleOwner`，校验入参（`communityId`、`oldOwnerId` 缺省回退 `ownerId`）后按 2000 行/批循环更新，累计返回影响行数；无可更新字段直接返回 0。
- 新增 `service-user/.../bmo/owner/IOwnerFeeSyncBMO.java`、`.../bmo/owner/impl/OwnerFeeSyncBMOImpl.java`：比对 `name`/`ownerId` 有变化才通过 Feign 调 fee 服务两个同步接口；家庭成员（`memberId != ownerId`，与业主共用 `owner_id`）跳过；同步异常记录 error 日志并抛 `CmdException`，使业主修改事务回滚。
- 后端 `service-user/.../cmd/owner/EditOwnerCmd`：`doCmd` 先按 `memberId + communityId` 查原业主（`Assert.listOnlyOne`），`updateOwner` 成功后触发 `IOwnerFeeSyncBMO.syncOwnerToFee`。
- 后端 `service-user/.../cmd/owner/SaveHandoverCmd`：传 `ownerId` 走 `updateOwner` 的分支先查原业主，更新成功后同样触发同步（新建业主分支不涉及历史账单，不触发）。
- 未回改的业务快照表：`fee_receipt`、`account_receipt`、`pay_fee_order`、`pay_fee_detail_month`、`owe_fee_callable` 保留单据当时业主名；`DeleteOwnerCmd` 删旧建新（新 `owner_id`）不会自动关联老账单，需用上方存量校准 SQL 或手工指定 `oldOwnerId` 处理。




## 2026-11-13 — 房屋设置费用页「预生成账单」（周期性勾选连续期次 / 一次性一键出账）

无 DDL、无字典、无菜单变更；新增 2 个 serviceCode 注册。方案：`.txcode/plan-code/20261113125042/20261113125042_方案.md`

```sql
-- 服务注册（独立脚本：docs/sql/fee/20261113_pre_generate_fee_service.sql，执行后刷新 AppRouteCache 或重启 service-api）
DELETE FROM `c_service` WHERE `service_code` IN ('fee.queryPreGenerateFeePeriods','fee.preGenerateFee');
INSERT INTO `c_service` VALUES ('2127','982026111309000001','fee.queryPreGenerateFeePeriods','API','查询预生成账单期次','1',null,'CMD','http://fee-service','GET','60','3','8000418002','2026-11-13 09:00:00','0');
INSERT INTO `c_service` VALUES ('2128','982026111309000002','fee.preGenerateFee','API','预生成费用账单','1',null,'CMD','http://fee-service','POST','60','3','8000418002','2026-11-13 09:00:00','0');
INSERT INTO `c_route` VALUES ('3109','8000418004','982026111309000001','Q','1000','S','2026-11-13 09:00:00','0');
INSERT INTO `c_route` VALUES ('3110','8000418004','982026111309000002','D','1000','S','2026-11-13 09:00:00','0');
```

- 新增 `java110-bean/.../dto/fee/PreGenerateFeePeriodDto.java`：期次 DTO（`seq`/`startTime`/`endTime`）。
- 后端 `service-fee/.../bill/ICycleConvertOnceFee`、`CycleConvertOnceFeeImpl`：新增 `preGeneratePayFee(rule, periods)`（免账单日校验，按指定期次逐期复用 `doGeneratorPayFee`/`saveFeeAndAttrs` 出账，推进 `end_time`/`cur_year_month`，达 `max_time` 置 `2009001`）与 `generateOncePayFeeByRule(rule)`（一次性出 1 张账单后规则置结束，已有 `rule_id` 账单则拒绝）。
- 新增 `service-fee/.../bmo/fee/IPreGenerateFeeBMO.java`、`.../bmo/fee/impl/PreGenerateFeeBMOImpl.java`：`queryPeriods` 自 `pay_fee_rule.end_time` 起按 `payment_cycle` 递推最多 12 期（上限取 `min(rule.max_time, 费用项 end_time)`，末期截断），不查 `pay_fee`；`validatePeriods` 强校验「不掐头、不跳期、期次落在重算集合内」，并按 `rule_id + start_time` 兜底拦重复出账；`preGenerate` 用重算期次覆盖前端账期后再出账。
- 新增 `service-fee/.../cmd/fee/QueryPreGenerateFeePeriodsCmd.java`（`fee.queryPreGenerateFeePeriods`，GET）、`PreGenerateFeeCmd.java`（`fee.preGenerateFee`，POST，`@Java110Transactional`），出参统一 `ResultVo`。
- 前端新增 `pc-web/src/api/fee/preGenerateFeeApi.js`、`pc-web/src/components/fee/preGenerateFee.vue`（期次多选弹窗，勾选第 i 期自动补齐第 1~i 期、取消第 i 期自动取消 i 之后期次，提交前再校验连续）。
- 前端 `pc-web/src/components/fee/feeDetailFeeRule.vue`（「设置费用」tab）：规则行操作列新增「预生成」按钮（仅规则 `state=2008001` 有效、权限 `502020082314267912` 显示）；一次性费用走 `$confirm` 直接出账，周期性费用先查期次再弹窗勾选，`periods` 为空提示「暂无可预生成期次」。
- 前端 `pc-web/src/views/fee/roomCreateFeeLang.js`：`simplifyRoomFee` 三语新增 `preGenerate`/`preGenerateConfirmTip`/`preGenerateTitle`/`period`/`periodStart`/`periodEnd`/`selectPeriodTip`/`continuousTip`/`continuousRequired`/`noAvailablePeriod`/`preGenerateSuccess`/`preGenerateFailed`。

## 2026-11-13 — 房产菜单补录「关注用户」（公众号关注用户同步）

无 DDL、无字典变更；补录 2 个 serviceCode 注册 + 1 条数据总线 + 菜单/权限。方案：`.txcode/plan-code/20261113132427/20261113132427_方案.md`

```sql
-- 运行时配置补录（独立脚本：docs/sql/user/20261113_wechat_subscribe_register.sql，执行后刷新 AppRouteCache 或重启 service-api，重启 service-job 使数据总线生效）
DELETE FROM `c_service` WHERE `service_code` IN ('wechat.listWechatSubscribe','wechat.synchronizeWechatSubscribe');
INSERT INTO `c_service` VALUES ('2129','982026111309000003','wechat.listWechatSubscribe','API','查询公众号关注用户','1',null,'CMD','http://user-service','GET','60','3','8000418002',NOW(),'0');
INSERT INTO `c_service` VALUES ('2130','982026111309000004','wechat.synchronizeWechatSubscribe','API','同步公众号关注用户','1',null,'CMD','http://user-service','POST','60','3','8000418002',NOW(),'0');
INSERT INTO `c_route` VALUES ('3111','8000418004','982026111309000003','Q','1000','S',NOW(),'0');
INSERT INTO `c_route` VALUES ('3112','8000418004','982026111309000004','D','1000','S',NOW(),'0');
INSERT INTO `c_business_databus` (`databus_id`,`databus_name`,`business_type_cd`,`bean_name`,`seq`,`state`,`create_time`,`status_cd`)
VALUES ('122026111300000001','获取公众号关注用户','synchronizeWechatSubscribe','synchronizeWechatSubscribeAdapt','92','1001',NOW(),'0');
INSERT INTO `m_menu` VALUES ('702026111300000001','关注用户','800201904006','/#/pages/community/wechatSubscribe','19','502026111300000001','公众号关注用户同步与查看',NOW(),'0','Y');
INSERT INTO `p_privilege` VALUES ('502026111300000001','关注用户','同步并查看公众号关注用户',NOW(),'0','800900000003','/#/pages/community/wechatSubscribe','702026111300000001');
```

- 后端/前端业务代码无改动：`ListWechatSubscribeCmd`(`wechat.listWechatSubscribe`)、`SynchronizeWechatSubscribeCmd`(`wechat.synchronizeWechatSubscribe`)、`SynchronizeWechatSubscribeAdapt`(`@Component("synchronizeWechatSubscribeAdapt")`)、`pc-web` 页面/组件/API/路由均已存在。
- 新增 `docs/sql/user/20261113_wechat_subscribe_register.sql`：幂等补录 `c_service`（2129/2130）、`c_route`（3111/3112，物业端 `8000418004`，GET→`Q`、POST→`D`）、`c_business_databus`（`business_type_cd=synchronizeWechatSubscribe` → `bean_name=synchronizeWechatSubscribeAdapt`，seq 92）、`m_menu`（房产组 `800201904006`，seq 19）、`p_privilege`（domain `800900000003`）与 `p_privilege_rel`（授权物业端全部有效权限组，`rel_id` 自增）。
- 前端 `pc-web/src/i18n/menuLang.js`：`menu` 字典三语新增菜单名键「关注用户」（en `Follow Users` / zh-TW `關注用戶`）。
- 前置校验（未改代码，仅说明）：同步接口要求小区已配置 `WECHAT_TYPE_PUBLIC` 公众号（`/pages/community/communityWechat` 录入 appId/appSecret 且公众号配置 IP 白名单），否则提示「未配置公众号」；菜单可见性还依赖 `m_menu_group_community` 中房产组已授权小区。
