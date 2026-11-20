# 变更日志

## 2026-07-31 — 新增：账单手工生成功能

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
```

## 2026-09-03 优惠申请（电话申请）改造：支持多费用勾选 + 批量缴费自动打折

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

## 2026-09-08 — 发票抬头统一由商城管理（物业端下线 owner_invoice）

```sql
-- 1) 物业库下线 owner_invoice（存量库先迁移后执行）
-- RENAME TABLE `owner_invoice` TO `owner_invoice_bak`;
-- DROP TABLE IF EXISTS `owner_invoice`;

-- 2) invoice_apply 增加抬头快照列(列表/详情展示用, 数据来自商城抬头同步)
ALTER TABLE `invoice_apply`
  ADD COLUMN `invoice_name` varchar(64) DEFAULT NULL COMMENT '发票名头(抬头快照)' AFTER `invoice_type`,
  ADD COLUMN `invoice_num` varchar(64) DEFAULT NULL COMMENT '纳税人识别号(抬头快照)' AFTER `invoice_name`,
  ADD COLUMN `invoice_address` varchar(64) DEFAULT NULL COMMENT '地址(抬头快照)' AFTER `invoice_num`;

-- 3) 商城库（hc_mall）：owner_invoice 存量迁入 invoice_heading（先导入 owner_invoice 数据到 owner_invoice_tmp）
CREATE TABLE IF NOT EXISTS owner_invoice_tmp (
  oi_id           varchar(30) NOT NULL COMMENT '原表主键',
  owner_id        varchar(30) NOT NULL COMMENT '业主ID',
  owner_name      varchar(64) NOT NULL COMMENT '业主名称',
  invoice_type    varchar(12) NOT NULL COMMENT '发票类型 1001 个人 2002 企业',
  community_id    varchar(30) NOT NULL COMMENT '小区ID',
  invoice_name    varchar(64) NOT NULL COMMENT '发票名头',
  invoice_num     varchar(64) DEFAULT NULL COMMENT '纳税人识别号',
  invoice_address varchar(64) NOT NULL COMMENT '地址',
  invoice_link    varchar(20) DEFAULT NULL COMMENT '电话',
  invoice_account varchar(64) DEFAULT NULL COMMENT '开户行及账号',
  remark          varchar(200) DEFAULT NULL COMMENT '备注',
  create_time     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status_cd       varchar(2) NOT NULL DEFAULT '0',
  PRIMARY KEY (oi_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='owner_invoice迁移中转表';

INSERT INTO invoice_heading(heading_id, heading_type, heading_name, tax_no, phone, user_phone, email, is_default, address, bank_no, remark, status_cd, create_time)
SELECT
  CONCAT('HD', REPLACE(UUID(), '-', '')) AS heading_id,
  CASE t.invoice_type WHEN '1001' THEN '个人' WHEN '2002' THEN '公司' ELSE t.invoice_type END AS heading_type,
  t.invoice_name AS heading_name,
  t.invoice_num AS tax_no,
  IFNULL(NULLIF(t.invoice_link, ''), bo.link) AS phone,
  bo.link AS user_phone,
  NULL AS email,
  IF(rn.rn = 1, '1', '0') AS is_default,
  t.invoice_address AS address,
  t.invoice_account AS bank_no,
  t.remark AS remark,
  '0' AS status_cd,
  t.create_time AS create_time
FROM owner_invoice_tmp t
LEFT JOIN (SELECT member_id, link FROM building_owner WHERE link IS NOT NULL AND link != '') bo
  ON bo.member_id = t.owner_id
LEFT JOIN (
  SELECT oi_id,
         ROW_NUMBER() OVER (PARTITION BY IFNULL(bo2.link, '') , t2.invoice_type ORDER BY t2.create_time, t2.oi_id) AS rn
  FROM owner_invoice_tmp t2
  LEFT JOIN (SELECT member_id, link FROM building_owner WHERE link IS NOT NULL AND link != '') bo2
    ON bo2.member_id = t2.owner_id
) rn ON rn.oi_id = t.oi_id
WHERE t.status_cd = '0'
  AND (SELECT COUNT(1) FROM invoice_heading h WHERE h.user_phone = bo.link) = 0;

-- 无业主手机号的存量抬头需人工补录
SELECT t.oi_id, t.owner_id, t.owner_name, t.invoice_name, t.invoice_address, t.invoice_link
FROM owner_invoice_tmp t
LEFT JOIN building_owner bo ON bo.member_id = t.owner_id
WHERE (bo.link IS NULL OR bo.link = '') AND t.status_cd = '0';

-- DROP TABLE IF EXISTS owner_invoice_tmp;

-- 4) 物业库（TT）：下线发票抬头管理菜单/权限/服务，发票菜单更名「开票」
DELETE FROM `c_service` WHERE `service_code` IN (
  'invoice.deleteOwnerInvoice',
  'invoice.listOwnerInvoice',
  'invoice.saveOwnerInvoice',
  'invoice.updateOwnerInvoice'
);
DELETE FROM `p_privilege_rel` WHERE `p_id` = '502023100906390005';
DELETE FROM `p_privilege` WHERE `p_id` = '502023100906390005';
DELETE FROM `m_menu_user` WHERE `m_id` = '702023100962610004';
DELETE FROM `m_menu` WHERE `m_id` = '702023100962610004';
UPDATE `m_menu` SET `name` = '开票' WHERE `m_id` = '702023100951520002';
UPDATE `p_privilege` SET `name` = '开票' WHERE `p_id` = '502023100942720003';
UPDATE `m_menu_user` SET `name` = '开票' WHERE `m_id` = '702023100951520002';
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

## 2026-09-08 — 业主端开票页 App 专用接口注册（c_service / c_route）

```sql
-- ① c_service 服务注册
INSERT INTO `c_service` VALUES ('2103', '982026090800000007', 'fee.queryAppUnInvoiceFeeDetail', 'API', '查询未开票缴费明细(业主端)', '1', NULL, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-09-08 18:30:00', '0');
INSERT INTO `c_service` VALUES ('2104', '982026090800000008', 'invoice.queryAppInvoiceApply', 'API', '查询已开票记录(业主端)', '1', NULL, 'CMD', 'http://acct-service', 'GET', '60', '3', '8000418002', '2026-09-08 18:30:00', '0');

-- ② c_route 路由配置（业主端 app 992020061452450002，与 owner.queryAppOwnerCars 等 App 命令同 app）
INSERT INTO `c_route` VALUES ('3089', '992020061452450002', '982026090800000007', 'Q', '1000', 'S', '2026-09-08 18:30:00', '0');
INSERT INTO `c_route` VALUES ('3090', '992020061452450002', '982026090800000008', 'Q', '1000', 'S', '2026-09-08 18:30:00', '0');
```

## 2026-09-08 — 业主端开票支持多选（取消单条限制）

```sql
INSERT INTO `c_service` VALUES ('2105', '982026090800000009', 'invoice.queryAppInvoiceHeader', 'API', '查询发票抬头(业主端)', '1', NULL, 'CMD', 'http://acct-service', 'GET', '60', '3', '8000418002', '2026-09-08 19:00:00', '0');
INSERT INTO `c_route` VALUES ('3091', '992020061452450002', '982026090800000009', 'Q', '1000', 'S', '2026-09-08 19:00:00', '0');
```

## 2026-09-09 — 撤销发票相关冗余接口注册（保留 queryInvoiceHeadings）

```sql
DELETE FROM `c_route` WHERE service_id IN ('982026090800000001','982026090800000003','982026090800000004','982026090800000005','982026090800000006');
DELETE FROM `c_service` WHERE service_id IN ('982026090800000001','982026090800000003','982026090800000004','982026090800000005','982026090800000006');
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

-- ⑤ 上线前自检：商城开关与商城地址
SELECT `domain`, `key`, `value` FROM `c_mapping`
 WHERE `domain` = 'MALL' AND `key` = 'MALL_SWITCH' AND `status_cd` = '0';
SELECT `domain`, `key`, `value` FROM `c_mapping`
 WHERE `key` = 'MALL_URL' AND `status_cd` = '0';
```

## 2026-09-15 — 物业开票单归并：单接口提交（抬头随开票 upsert）· ext_apply_id 关联 · 查询即取商城状态/PDF

```sql
ALTER TABLE `invoice_order`
  ADD COLUMN `ext_apply_id` VARCHAR(32) DEFAULT NULL COMMENT '外部(物业)发票申请ID' AFTER `source`,
  ADD KEY `idx_ext_apply` (`ext_apply_id`);

-- 存量 WEB 单可选回填: remark 格式为 applyId|业主名称|业务类目
UPDATE `invoice_order` SET `ext_apply_id` = SUBSTRING_INDEX(`remark`, '|', 1)
 WHERE `source` = 'WEB' AND (`ext_apply_id` IS NULL OR `ext_apply_id` = '') AND `remark` LIKE '%|%';
DELETE FROM `c_route` WHERE `service_id` = '982026090800000010';
DELETE FROM `c_service` WHERE `service_id` = '982026090800000010';
```

## 2026-09-15 — 开票中心改造：申请单落房屋（room_id/room_name）· 页面左右布局（房屋树/发票记录）· 商城抬头不再写入开票单备注

```sql
ALTER TABLE `invoice_apply`
  ADD COLUMN `room_id` varchar(30) DEFAULT NULL COMMENT '房屋ID(选房开票时记录)' AFTER `community_id`,
  ADD COLUMN `room_name` varchar(64) DEFAULT NULL COMMENT '房屋编号(楼栋-单元-房号)' AFTER `room_id`,
  ADD KEY `idx_room_id` (`room_id`);

-- 存量回填（可选，人工核对后执行；仅开票项为物业缴费 item_type='2002' 的单据）
-- UPDATE `invoice_apply` a
-- JOIN (
--   SELECT i.`apply_id`, MAX(pf.`payer_obj_id`) AS `room_id`, MAX(pf.`payer_obj_name`) AS `room_name`
--   FROM `invoice_apply_item` i
--   JOIN `pay_fee_detail` pfd ON pfd.`detail_id` = i.`item_obj_id`
--   JOIN `pay_fee` pf ON pf.`fee_id` = pfd.`fee_id`
--   WHERE i.`item_type` = '2002' AND i.`status_cd` = '0'
--   GROUP BY i.`apply_id`
-- ) x ON x.`apply_id` = a.`apply_id`
-- SET a.`room_id` = x.`room_id`, a.`room_name` = x.`room_name`
-- WHERE (a.`room_id` IS NULL OR a.`room_id` = '');
```

## 2026-09-15 — 发票红冲（物业端发起，商城执行）：物业端零新增表 · 红冲数据以商城为权威源 · 开票页/房屋详情页双入口

```sql
-- ① 服务定义 c_service
INSERT INTO `c_service` VALUES ('2111', '982026090800000012', 'invoice.saveInvoiceRed', 'API', '发起发票红冲', '1', null, 'CMD', 'http://acct-service', 'POST', '60', '3', '8000418002', '2026-09-15 18:00:00', '0');
INSERT INTO `c_service` VALUES ('2112', '982026090800000013', 'invoice.listInvoiceReds', 'API', '查询发票红冲', '1', null, 'CMD', 'http://acct-service', 'GET', '60', '3', '8000418002', '2026-09-15 18:00:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004, D=POST 受理单, Q=GET 查询单)
INSERT INTO `c_route` VALUES ('3098', '8000418004', '982026090800000012', 'D', '1000', 'S', '2026-09-15 18:00:00', '0');
INSERT INTO `c_route` VALUES ('3099', '8000418004', '982026090800000013', 'Q', '1000', 'S', '2026-09-15 18:00:00', '0');
```

## 2026-10-13 — 场地预约模块优化（场地管理 / 场地预约 / 预约订单）：日期语义统一 · 时段占用可控 · 收款/核销状态流转

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

## 2026-10-23 — 商铺删除 / 退铺（shop.deleteBuildingShop、shop.deleteBuildingShopOwner）报错修复

```sql
-- 可选：修复历史 status_cd 为 NULL 的商铺/租赁脏数据（先查后改）
SELECT shop_id, status_cd FROM building_shop WHERE status_cd IS NULL;
UPDATE building_shop SET status_cd = '0' WHERE status_cd IS NULL;
UPDATE building_shop_owner SET status_cd = '0' WHERE status_cd IS NULL;
```

## 2026-11-05 — 车位结构图（/pages/property/carStructure）改造：左树右卡片 + 欠费感知

```sql
-- ① 服务定义 c_service
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2126', '982026110515300001', 'car.listParkingSpaceStructures', 'API', '查询车位结构图数据', '1', null, 'CMD', 'http://report-service', 'GET', '60', '3', '8000418002', '2026-11-05 15:30:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004, GET 用 Q)
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3108', '8000418004', '982026110515300001', 'Q', '1000', 'S', '2026-11-05 15:30:00', '0');
```

## 2026-11-09 — 事务回退日志列值错位（`Incorrect datetime value ... for column 'end_time'`）修复

```sql
-- 可选：清理错位写入的 business_pay_fee（中间表，按 b_id 精确清理后再由业务重新生成）
SELECT b_id, o_id, action, action_obj, log_text FROM c_order_item WHERE action_obj = 'pay_fee' ORDER BY create_time DESC LIMIT 10;
DELETE FROM business_pay_fee WHERE b_id = '202026110907400396' AND operate = 'ADD';
```

## 2026-11-13 — Java 17 自定义公式金额为 0（内置 nashorn 引擎被移除）修复

```sql
-- 本次无数据库变更
-- 说明：Java 15+（含 17）已按 JEP 372 移除内置 nashorn 引擎，
--      javax.script 取不到 JavaScript 引擎导致公式求值 NPE 后被 catch 吞掉，应收金额恒为 0。
--      改为依赖 org.openjdk.nashorn:nashorn-core:15.4，由 java110-utils 统一提供求值入口。
```

## 2026-11-13 — 业主名称/owner_id 变更同步 pay_fee、pay_fee_rule 业主信息

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

## 2026-11-13 — 房屋设置费用页「预生成账单」（周期性勾选连续期次 / 一次性一键出账）

```sql
-- 服务注册（执行后刷新 AppRouteCache 或重启 service-api）
DELETE FROM `c_service` WHERE `service_code` IN ('fee.queryPreGenerateFeePeriods','fee.preGenerateFee');
INSERT INTO `c_service` VALUES ('2127','982026111309000001','fee.queryPreGenerateFeePeriods','API','查询预生成账单期次','1',null,'CMD','http://fee-service','GET','60','3','8000418002','2026-11-13 09:00:00','0');
INSERT INTO `c_service` VALUES ('2128','982026111309000002','fee.preGenerateFee','API','预生成费用账单','1',null,'CMD','http://fee-service','POST','60','3','8000418002','2026-11-13 09:00:00','0');
INSERT INTO `c_route` VALUES ('3109','8000418004','982026111309000001','Q','1000','S','2026-11-13 09:00:00','0');
INSERT INTO `c_route` VALUES ('3110','8000418004','982026111309000002','D','1000','S','2026-11-13 09:00:00','0');
```

## 2026-11-13 — 房产菜单补录「关注用户」（公众号关注用户同步）

```sql
-- 运行时配置补录（执行后刷新 AppRouteCache 或重启 service-api，重启 service-job 使数据总线生效）
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

## 2026-11-16 — 合同模块菜单与原型对齐（菜单/权限 + 页面布局基线）

```sql
-- 菜单组 802020091723050020 对齐原型 7 项：合同甲方 / 合同模板 / 合同台账 / 合同履约 / 合同变更 / 到期提醒 / 合同审核
UPDATE `m_menu` SET seq='1' WHERE m_id='702021042214040302';
UPDATE `m_menu` SET name='合同模板', seq='2' WHERE m_id='702020091843810076';
UPDATE `m_menu` SET name='合同台账', seq='3' WHERE m_id='702020091785290021';
UPDATE `m_menu` SET name='合同履约', seq='4' WHERE m_id='702026111600000001';
UPDATE `m_menu` SET name='合同变更', seq='5' WHERE m_id='702021033017840295';
UPDATE `m_menu` SET name='到期提醒', seq='6' WHERE m_id='702021033149670076';
-- 下线「起草合同」（路由与页面保留，起草走台账「新建合同」）
UPDATE `m_menu` SET status_cd='1', is_show='N' WHERE m_id='702021033187310391';
UPDATE `p_privilege` SET status_cd='1' WHERE m_id='702021033187310391';
-- 新增「合同审核」菜单 + 菜单查看权限 + 审核人设置按钮权限归位
INSERT INTO `m_menu` (`m_id`,`name`,`g_id`,`url`,`seq`,`p_id`,`description`,`create_time`,`status_cd`,`is_show`)
SELECT '702026111600000002','合同审核','802020091723050020','/#/pages/admin/contractApplyAuditOrders','7','-1','待我审批/已通过/已驳回（合同与变更统一审核）',NOW(),'0','Y'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `m_menu` WHERE `m_id`='702026111600000002');
INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000008','合同审核','合同审核菜单查看',NOW(),'0','800900000003','/#/pages/admin/contractApplyAuditOrders','702026111600000002'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id`='502026111600000008');
UPDATE `p_privilege` SET m_id='702026111600000002' WHERE p_id='502026111600000003';
-- 权限名称/历史 resource 修正 + 收藏项名称同步
UPDATE `p_privilege` SET name='合同模板' WHERE p_id='502020091858500077';
UPDATE `p_privilege` SET name='合同台账' WHERE p_id='502020091779900022';
UPDATE `p_privilege` SET name='到期提醒', resource='/#/pages/admin/expirationContractManage' WHERE p_id='502021033136470077';
UPDATE `m_menu_user` SET name='合同台账' WHERE m_id='702020091785290021';
-- ① 合同主表扩展
ALTER TABLE `contract`
  ADD COLUMN `partya_id` varchar(30) DEFAULT NULL COMMENT '甲方主体ID' AFTER `party_a`,
  ADD COLUMN `template_id` varchar(30) DEFAULT NULL COMMENT '合同模板ID' AFTER `contract_type`,
  ADD COLUMN `pay_mode` varchar(100) DEFAULT NULL COMMENT '付款方式' AFTER `amount`;

-- ② 新增表：合同正文 / 两级审批 / 审核人配置 / 到期提醒日志
CREATE TABLE IF NOT EXISTS `contract_content` (
  `content_id` varchar(30) NOT NULL COMMENT '主键',
  `contract_id` varchar(30) NOT NULL COMMENT '合同ID',
  `template_id` varchar(30) DEFAULT NULL COMMENT '模板ID',
  `content` longtext COMMENT '合同正文HTML',
  `content_version` int DEFAULT '0' COMMENT '正文版本号，每次保存自增',
  `status_cd` char(1) NOT NULL DEFAULT '0' COMMENT '0在用 1失效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`content_id`),
  UNIQUE KEY `uk_contract_content_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同正文';

CREATE TABLE IF NOT EXISTS `contract_audit` (
  `audit_id` varchar(30) NOT NULL COMMENT '审批实例ID',
  `audit_code` varchar(30) DEFAULT NULL COMMENT '审批单号 SP-{yyyy}-{4位序号}',
  `audit_type` char(2) NOT NULL DEFAULT '1' COMMENT '1 合同 2 变更',
  `biz_id` varchar(30) NOT NULL COMMENT '业务ID（合同ID / 变更ID）',
  `current_step` char(2) NOT NULL DEFAULT '1' COMMENT '当前环节 1法务校对 2领导审核',
  `state` char(2) NOT NULL DEFAULT '0' COMMENT '0审批中 1通过 2驳回 3撤回',
  `submit_staff_id` varchar(30) DEFAULT NULL COMMENT '提交人',
  `submit_staff_name` varchar(100) DEFAULT NULL COMMENT '提交人名称',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `finish_time` datetime DEFAULT NULL COMMENT '结束时间',
  `store_id` varchar(30) DEFAULT NULL,
  `status_cd` char(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`audit_id`),
  KEY `idx_contract_audit_biz` (`biz_id`,`audit_type`,`state`),
  KEY `idx_contract_audit_code` (`audit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审批实例';

CREATE TABLE IF NOT EXISTS `contract_audit_record` (
  `record_id` varchar(30) NOT NULL,
  `audit_id` varchar(30) NOT NULL,
  `step` char(2) NOT NULL COMMENT '1法务校对 2领导审核',
  `step_name` varchar(50) DEFAULT NULL,
  `auditor_id` varchar(30) DEFAULT NULL,
  `auditor_name` varchar(100) DEFAULT NULL,
  `auditor_tel` varchar(30) DEFAULT NULL,
  `result` char(2) NOT NULL DEFAULT '0' COMMENT '0待办 1通过 2驳回',
  `opinion` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `audit_time` datetime DEFAULT NULL,
  `seq` int DEFAULT '1',
  `status_cd` char(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  KEY `idx_contract_audit_record_audit` (`audit_id`,`step`),
  KEY `idx_contract_audit_record_auditor` (`auditor_id`,`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审批环节记录';

CREATE TABLE IF NOT EXISTS `contract_audit_config` (
  `config_id` varchar(30) NOT NULL,
  `step` char(2) NOT NULL COMMENT '1法务校对 2领导审核',
  `staff_id` varchar(30) NOT NULL COMMENT '审核人（员工ID）',
  `staff_name` varchar(100) DEFAULT NULL,
  `staff_tel` varchar(30) DEFAULT NULL,
  `store_id` varchar(30) DEFAULT NULL,
  `status_cd` char(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  KEY `idx_contract_audit_config_store` (`store_id`,`step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审核人配置';

CREATE TABLE IF NOT EXISTS `contract_remind_log` (
  `log_id` varchar(30) NOT NULL,
  `contract_id` varchar(30) NOT NULL,
  `remind_type` varchar(10) DEFAULT NULL COMMENT '30/15/7/manual',
  `channel` varchar(20) DEFAULT NULL COMMENT 'inner 站内信 / sms 短信',
  `send_time` datetime DEFAULT NULL,
  `receive_tel` varchar(30) DEFAULT NULL,
  `content` varchar(500) DEFAULT NULL,
  `store_id` varchar(30) DEFAULT NULL,
  `status_cd` char(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_contract_remind_log_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同到期提醒日志';

-- ③ 既有业务表字段扩展
ALTER TABLE `contract_type`
  ADD COLUMN `category` varchar(4) DEFAULT '2' COMMENT '1房屋出租 2采购 3维保' AFTER `type_name`;

ALTER TABLE `contract_type_template`
  ADD COLUMN `template_name` varchar(100) DEFAULT NULL COMMENT '模板名称' AFTER `template_id`,
  ADD COLUMN `template_version` varchar(20) DEFAULT 'V1.0' COMMENT '模板版本' AFTER `template_name`,
  ADD COLUMN `template_desc` varchar(500) DEFAULT NULL COMMENT '模板描述' AFTER `template_version`,
  ADD COLUMN `use_count` int DEFAULT '0' COMMENT '使用次数' AFTER `template_desc`,
  ADD COLUMN `source_type` varchar(20) DEFAULT 'online' COMMENT '来源 online/word/paste' AFTER `use_count`;

ALTER TABLE `contract_partya`
  ADD COLUMN `short_name` varchar(100) DEFAULT NULL COMMENT '主体简称' AFTER `party_a`,
  ADD COLUMN `credit_code` varchar(50) DEFAULT NULL COMMENT '统一社会信用代码' AFTER `short_name`,
  ADD COLUMN `legal_person` varchar(50) DEFAULT NULL COMMENT '法定代表人' AFTER `credit_code`,
  ADD COLUMN `bank_name` varchar(200) DEFAULT NULL COMMENT '开户行' AFTER `a_contacts`,
  ADD COLUMN `bank_account` varchar(100) DEFAULT NULL COMMENT '银行账号' AFTER `bank_name`,
  ADD COLUMN `contact_person` varchar(50) DEFAULT NULL COMMENT '联系人' AFTER `bank_account`,
  ADD COLUMN `contact_tel` varchar(30) DEFAULT NULL COMMENT '联系电话' AFTER `contact_person`,
  ADD COLUMN `seal_flag` char(1) DEFAULT '0' COMMENT '是否已上传印章 0否 1是' AFTER `contact_tel`,
  ADD COLUMN `default_flag` char(1) DEFAULT '0' COMMENT '是否默认主体 0否 1是' AFTER `seal_flag`;

ALTER TABLE `contract_collection_plan`
  ADD COLUMN `plan_seq` int DEFAULT NULL COMMENT '期次' AFTER `plan_name`,
  ADD COLUMN `plan_time` datetime DEFAULT NULL COMMENT '计划收款日期' AFTER `plan_seq`,
  ADD COLUMN `plan_amount` decimal(12,2) DEFAULT '0.00' COMMENT '计划金额' AFTER `plan_time`,
  ADD COLUMN `ratio` decimal(6,4) DEFAULT '0.0000' COMMENT '比例' AFTER `plan_amount`,
  ADD COLUMN `pay_state` char(1) DEFAULT '0' COMMENT '0未到期 1已付 2逾期' AFTER `ratio`,
  ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '实付时间' AFTER `pay_state`,
  ADD COLUMN `trigger_desc` varchar(200) DEFAULT NULL COMMENT '履约节点描述' AFTER `pay_time`,
  ADD COLUMN `fee_type` varchar(20) DEFAULT NULL COMMENT '费用类型 rent/property/other/water' AFTER `trigger_desc`;

-- ④ 字典：合同状态新增「草稿」
INSERT INTO `t_dict` (`status_cd`, `name`, `description`, `create_time`, `table_name`, `table_columns`)
SELECT '10', '草稿', '草稿', NOW(), 'contract', 'state'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_dict` WHERE `status_cd` = '10' AND `table_name` = 'contract' AND `table_columns` = 'state');

-- ⑤ 存量回填：合同甲方主体、合同类型分类
UPDATE `contract` t
JOIN `contract_partya` p
  ON p.`party_a` = t.`party_a` AND p.`status_cd` = '0' AND p.`store_id` = t.`store_id`
SET t.`partya_id` = p.`partya_id`
WHERE (t.`partya_id` IS NULL OR t.`partya_id` = '');

UPDATE `contract_type` SET `category` = '1' WHERE `category` IS NULL AND (`type_name` LIKE '%租%');
UPDATE `contract_type` SET `category` = '3' WHERE `category` IS NULL AND (`type_name` LIKE '%维保%' OR `type_name` LIKE '%维修%');
UPDATE `contract_type` SET `category` = '2' WHERE `category` IS NULL;

-- ⑥ 存量在途审批迁移（Activiti -> 两级审批）：先 dry-run 清单
SELECT c.`contract_id`, c.`contract_code`, c.`contract_name`, c.`state`
FROM `contract` c
WHERE c.`state` = '11' AND c.`status_cd` = '0';

INSERT INTO `contract_audit`
  (`audit_id`, `audit_code`, `audit_type`, `biz_id`, `current_step`, `state`,
   `submit_staff_id`, `submit_staff_name`, `submit_time`, `store_id`, `status_cd`, `create_time`)
SELECT CONCAT('90', REPLACE(UUID(), '-', '')),
       CONCAT('SP-', DATE_FORMAT(NOW(), '%Y'), '-', LPAD(IFNULL(MAX(a.`id`), 0) + 1, 4, '0')),
       '1', c.`contract_id`, '1', '0',
       IFNULL(c.`start_user_id`, '-1'), NULL, NOW(), c.`store_id`, '0', NOW()
FROM `contract` c
LEFT JOIN `contract_audit` a ON a.`biz_id` = c.`contract_id` AND a.`audit_type` = '1' AND a.`state` = '0'
WHERE c.`state` = '11' AND c.`status_cd` = '0' AND a.`audit_id` IS NULL;

INSERT INTO `contract_audit_record`
  (`record_id`, `audit_id`, `step`, `step_name`, `auditor_id`, `auditor_name`, `auditor_tel`, `result`, `seq`, `status_cd`, `create_time`)
SELECT CONCAT('91', REPLACE(UUID(), '-', '')), a.`audit_id`, '1', '法务校对', cfg.`staff_id`, cfg.`staff_name`, cfg.`staff_tel`, '0', 1, '0', NOW()
FROM `contract_audit` a
LEFT JOIN `contract_audit_config` cfg ON cfg.`store_id` = a.`store_id` AND cfg.`step` = '1' AND cfg.`status_cd` = '0'
WHERE a.`audit_type` = '1' AND a.`state` = '0'
  AND NOT EXISTS (SELECT 1 FROM `contract_audit_record` r WHERE r.`audit_id` = a.`audit_id` AND r.`step` = '1');

INSERT INTO `contract_audit_record`
  (`record_id`, `audit_id`, `step`, `step_name`, `auditor_id`, `auditor_name`, `auditor_tel`, `result`, `seq`, `status_cd`, `create_time`)
SELECT CONCAT('91', REPLACE(UUID(), '-', '')), a.`audit_id`, '2', '领导审核', cfg.`staff_id`, cfg.`staff_name`, cfg.`staff_tel`, '0', 2, '0', NOW()
FROM `contract_audit` a
LEFT JOIN `contract_audit_config` cfg ON cfg.`store_id` = a.`store_id` AND cfg.`step` = '2' AND cfg.`status_cd` = '0'
WHERE a.`audit_type` = '1' AND a.`state` = '0'
  AND NOT EXISTS (SELECT 1 FROM `contract_audit_record` r WHERE r.`audit_id` = a.`audit_id` AND r.`step` = '2');

INSERT INTO `contract_audit_record`
  (`record_id`, `audit_id`, `step`, `step_name`, `auditor_id`, `auditor_name`, `auditor_tel`, `result`, `opinion`, `audit_time`, `seq`, `status_cd`, `create_time`)
SELECT CONCAT('91', REPLACE(UUID(), '-', '')),
       a.`audit_id`,
       CASE WHEN h.`NAME_` LIKE '%法务%' THEN '1' ELSE '2' END,
       h.`NAME_`,
       h.`ASSIGNEE_`,
       NULL,
       NULL,
       '1',
       NULL,
       h.`END_TIME_`,
       3,
       '0',
       NOW()
FROM `ACT_HI_TASKINST` h
JOIN `contract_audit` a ON a.`biz_id` = h.`PROC_INST_ID_` OR a.`biz_id` = h.`BUSINESS_KEY_`
WHERE h.`END_TIME_` IS NOT NULL
  AND a.`audit_type` = '1'
  AND (h.`NAME_` LIKE '%法务%' OR h.`NAME_` LIKE '%领导%' OR h.`NAME_` LIKE '%总经理%')
  AND NOT EXISTS (
    SELECT 1 FROM `contract_audit_record` r
    WHERE r.`audit_id` = a.`audit_id` AND r.`audit_time` = h.`END_TIME_`
  );

SELECT a.`audit_id`, a.`biz_id`, a.`current_step`, a.`state`, COUNT(r.`record_id`) recordCount
FROM `contract_audit` a
LEFT JOIN `contract_audit_record` r ON r.`audit_id` = a.`audit_id`
WHERE a.`audit_type` = '1' AND a.`state` = '0'
GROUP BY a.`audit_id`, a.`biz_id`, a.`current_step`, a.`state`;

-- 变更单迁移：同一批语句把 audit_type 置 2、biz_id 取 plan_id（源表 contract_change_plan）

-- ⑦ 服务注册（c_service 2131~2155 / c_route 3113~3137）
INSERT IGNORE INTO `c_service` (`id`,`service_id`,`service_code`,`business_type_cd`,`name`,`seq`,`messageQueueName`,`is_instance`,`url`,`method`,`timeout`,`retry_count`,`provide_app_id`,`create_time`,`status_cd`) VALUES
 ('2131','982026111600000001','contract.listContractStatistics','API','合同台账统计','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2132','982026111600000002','contract.generateContractCode','API','生成合同编号','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2133','982026111600000003','contract.saveContractDraft','API','保存合同草稿','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2134','982026111600000004','contract.previewContractContent','API','预览合同正文','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2135','982026111600000005','contract.queryContractContent','API','查询合同正文','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2136','982026111600000006','contract.listContractTemplates','API','合同模板列表','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2137','982026111600000007','contract.updateContractTemplate','API','更新合同模板','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2138','982026111600000008','contract.setDefaultContractPartya','API','设置默认甲方主体','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2139','982026111600000009','contract.submitContractAudit','API','提交合同审批','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2140','982026111600000010','contract.auditContract','API','合同审批通过/驳回','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2141','982026111600000011','contract.cancelContractAudit','API','撤回合同审批','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2142','982026111600000012','contract.listContractAuditTasks','API','待我审批','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2143','982026111600000013','contract.listContractAuditHistory','API','已办/我发起审批','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2144','982026111600000014','contract.listContractAuditStatistics','API','审批汇总卡','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2145','982026111600000015','contract.saveContractAuditorConfig','API','保存审核人配置','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2146','982026111600000016','contract.queryContractAuditorConfig','API','查询审核人配置','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2147','982026111600000017','contract.listContractPerformances','API','合同履约汇总','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2148','982026111600000018','contract.listContractCollectionPlans','API','收款计划期次明细','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2149','982026111600000019','contract.saveContractPayment','API','登记付款','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2150','982026111600000020','contract.listContractReminds','API','到期提醒清单','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2151','982026111600000021','contract.pushContractRemind','API','批量推送到期提醒','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2152','982026111600000022','contract.exportContracts','API','合同导出Excel','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0'),
 ('2153','982026111600000023','contract.batchSubmitContract','API','批量提交合同审批','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2154','982026111600000024','contract.batchStopContract','API','批量终止合同','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2155','982026111600000025','contract.queryContractAuditFlow','API','查询单据审批链','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0');

INSERT IGNORE INTO `c_route` (`id`,`app_id`,`service_id`,`order_type_cd`,`invoke_limit_times`,`invoke_model`,`create_time`,`status_cd`) VALUES
 ('3113','8000418004','982026111600000001','Q','1000','S',NOW(),'0'),
 ('3114','8000418004','982026111600000002','Q','1000','S',NOW(),'0'),
 ('3115','8000418004','982026111600000003','Q','1000','S',NOW(),'0'),
 ('3116','8000418004','982026111600000004','Q','1000','S',NOW(),'0'),
 ('3117','8000418004','982026111600000005','Q','1000','S',NOW(),'0'),
 ('3118','8000418004','982026111600000006','Q','1000','S',NOW(),'0'),
 ('3119','8000418004','982026111600000007','Q','1000','S',NOW(),'0'),
 ('3120','8000418004','982026111600000008','Q','1000','S',NOW(),'0'),
 ('3121','8000418004','982026111600000009','Q','1000','S',NOW(),'0'),
 ('3122','8000418004','982026111600000010','Q','1000','S',NOW(),'0'),
 ('3123','8000418004','982026111600000011','Q','1000','S',NOW(),'0'),
 ('3124','8000418004','982026111600000012','Q','1000','S',NOW(),'0'),
 ('3125','8000418004','982026111600000013','Q','1000','S',NOW(),'0'),
 ('3126','8000418004','982026111600000014','Q','1000','S',NOW(),'0'),
 ('3127','8000418004','982026111600000015','Q','1000','S',NOW(),'0'),
 ('3128','8000418004','982026111600000016','Q','1000','S',NOW(),'0'),
 ('3129','8000418004','982026111600000017','Q','1000','S',NOW(),'0'),
 ('3130','8000418004','982026111600000018','Q','1000','S',NOW(),'0'),
 ('3131','8000418004','982026111600000019','Q','1000','S',NOW(),'0'),
 ('3132','8000418004','982026111600000020','Q','1000','S',NOW(),'0'),
 ('3133','8000418004','982026111600000021','Q','1000','S',NOW(),'0'),
 ('3134','8000418004','982026111600000022','Q','1000','S',NOW(),'0'),
 ('3135','8000418004','982026111600000023','Q','1000','S',NOW(),'0'),
 ('3136','8000418004','982026111600000024','Q','1000','S',NOW(),'0'),
 ('3137','8000418004','982026111600000025','Q','1000','S',NOW(),'0');

-- ⑧ 菜单/权限：合同履约菜单 + 合同中心操作按钮权限 + 到期提醒任务模板
INSERT INTO `m_menu` (`m_id`,`name`,`g_id`,`url`,`seq`,`p_id`,`description`,`create_time`,`status_cd`,`is_show`)
SELECT '702026111600000001','合同履约','802020091723050020','/#/pages/admin/contractPerformance','9','502026111600000001','合同履约跟踪（付款节点/逾期）',NOW(),'0','Y'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `m_menu` WHERE `m_id` = '702026111600000001');

INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000001','合同履约','合同履约菜单查看',NOW(),'0','800900000003','/#/pages/admin/contractPerformance','702026111600000001'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000001');

INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000002','合同履约-查看','合同履约列表查看',NOW(),'0','800900000003','/callComponent/queryContract/listContractPerformances','702026111600000001'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000002');

INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000003','合同审核人设置','法务校对/领导审核审核人配置',NOW(),'0','800900000003','/callComponent/contract/saveContractAuditorConfig','702020091785290021'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000003');
INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000004','合同导出','台账/履约/提醒导出 Excel',NOW(),'0','800900000003','/callComponent/contract/exportContracts','702020091785290021'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000004');
INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000005','合同批量终止','批量终止合同',NOW(),'0','800900000003','/callComponent/contract/batchStopContract','702020091785290021'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000005');
INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000006','合同推送提醒','到期提醒批量推送',NOW(),'0','800900000003','/callComponent/contract/pushContractRemind','702021033149670076'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000006');
INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000007','合同登记付款','履约登记/撤销付款',NOW(),'0','800900000003','/callComponent/contract/saveContractPayment','702026111600000001'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000007');

INSERT INTO `task_template` (`template_id`, `template_name`, `template_desc`, `create_time`, `status_cd`, `class_bean`)
SELECT '1037', '合同到期提醒', '合同到期前30/15/7天推送提醒', NOW(), '0', 'expirationContractRemindTemplate'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `task_template` WHERE `template_id` = '1037');
```

## 2026-11-17

```sql
-- 合同类型下线（contract_type / contract_type_spec → 模板 category），执行顺序不可调换
-- 执行前备份：mysqldump -u<user> -p <db> contract contract_type contract_type_spec contract_type_template > contract_type_offline_bak.sql
ALTER TABLE `contract_type_template`
  ADD COLUMN `category` varchar(12) NOT NULL DEFAULT '2' COMMENT '1房屋出租 2采购 3维保' AFTER `contract_type_id`;

UPDATE `contract_type_template` t
  LEFT JOIN `contract_type` ct ON ct.`contract_type_id` = t.`contract_type_id`
SET t.`category` = IFNULL(ct.`category`, '2')
WHERE t.`status_cd` = '0';

ALTER TABLE `contract_type_template` DROP COLUMN `contract_type_id`;

ALTER TABLE `contract` DROP COLUMN `contract_type`;

ALTER TABLE `contract_change_plan_detail` DROP COLUMN `contract_type`;

DROP TABLE IF EXISTS `contract_type_spec`;
DROP TABLE IF EXISTS `contract_type`;

DELETE FROM `t_dict` WHERE `table_name` = 'contract_type';

DELETE FROM `c_route`
 WHERE `service_id` IN (
   SELECT `service_id` FROM `c_service`
    WHERE `service_code` IN (
      '/contract/saveContractType',
      '/contract/updateContractType',
      '/contract/deleteContractType',
      '/contract/queryContractType',
      '/contract/saveContractTypeSpec',
      '/contract/updateContractTypeSpec',
      '/contract/deleteContractTypeSpec',
      '/contract/queryContractTypeSpec'
    )
 );

DELETE FROM `c_service`
 WHERE `service_code` IN (
   '/contract/saveContractType',
   '/contract/updateContractType',
   '/contract/deleteContractType',
   '/contract/queryContractType',
   '/contract/saveContractTypeSpec',
   '/contract/updateContractTypeSpec',
   '/contract/deleteContractTypeSpec',
   '/contract/queryContractTypeSpec'
 );
```

## 2026-11-19 房屋费用处理

```sql
-- ① room.queryLeasableRooms（community-service，查询无业主的可出租房屋/商铺）
INSERT  INTO `c_service` (`id`,`service_id`,`service_code`,`business_type_cd`,`name`,`seq`,`messageQueueName`,`is_instance`,`url`,`method`,`timeout`,`retry_count`,`provide_app_id`,`create_time`,`status_cd`) VALUES
 ('2156','982026111600000026','room.queryLeasableRooms','API','查询可出租房源（无业主）','1',NULL,'CMD','http://community-service','GET','60','3','8000418004',NOW(),'0');

INSERT  INTO `c_route` (`id`,`app_id`,`service_id`,`order_type_cd`,`invoke_limit_times`,`invoke_model`,`create_time`,`status_cd`) VALUES
 ('3138','8000418004','982026111600000026','Q','1000','S',NOW(),'0');

-- ② payFeeRule.listRoomFeeRuleWithBill（房屋收费计费设置与账单合并视图）
DELETE FROM `c_route` WHERE `service_id` IN ('982026111914170001');
DELETE FROM `c_service` WHERE `service_code` IN ('payFeeRule.listRoomFeeRuleWithBill');

INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2157', '982026111914170001', 'payFeeRule.listRoomFeeRuleWithBill', 'API', '查询房屋计费设置与账单', '1', null, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-11-19 14:00:00', '0');

INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3139', '8000418004', '982026111914170001', 'Q', '1000', 'S', '2026-11-19 14:00:00', '0');

-- ③ payFeeRule.listRuleBills（按计费设置查询账单）
DELETE FROM `c_route` WHERE `service_id` IN ('982026111916300002');
DELETE FROM `c_service` WHERE `service_code` IN ('payFeeRule.listRuleBills');

INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2158', '982026111916300002', 'payFeeRule.listRuleBills', 'API', '查询计费设置下的账单', '1', null, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-11-19 16:30:00', '0');

INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3140', '8000418004', '982026111916300002', 'Q', '1000', 'S', '2026-11-19 16:30:00', '0');
```

## 2026-11-20 合同费用后端统一试算与合同标的支持商铺

```sql
-- ① contract_room 增加标的类型（3333 房屋 / 4444 商铺），存量数据默认按房屋处理
ALTER TABLE `contract_room`
  ADD COLUMN `payer_obj_type` varchar(4) NOT NULL DEFAULT '3333' COMMENT '标的对象类型 3333房屋 4444商铺' AFTER `room_name`;

-- ② fee.computeContractFeeAmount（service-fee，合同费用统一公式试算，GET 只读不落库）
DELETE FROM `c_route` WHERE `service_id` IN ('982026112010000001');
DELETE FROM `c_service` WHERE `service_code` IN ('fee.computeContractFeeAmount');

INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2159', '982026112010000001', 'fee.computeContractFeeAmount', 'API', '合同费用试算', '1', null, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-11-20 10:00:00', '0');

INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3141', '8000418004', '982026112010000001', 'Q', '1000', 'S', '2026-11-20 10:00:00', '0');

-- ③ 执行后刷新路由缓存：POST /app/cache.flushCache?cache=APP_ROUTE_SERVICE（未刷缓存会返回“服务未找到”），并重启 service-api 网关
-- ④ 回滚：DELETE FROM c_route WHERE service_id = '982026112010000001'; DELETE FROM c_service WHERE service_code = 'fee.computeContractFeeAmount';
--          ALTER TABLE contract_room DROP COLUMN payer_obj_type;
```

## 2026-11-20 — 合同增删改 CMD 化 + 正文变量渲染

```sql
-- 执行前备份：mysqldump -u<user> -p <db> contract_content c_service c_route > contract_cmd_bak.sql

-- ① contract_content 增加 template_content（含变量占位符的正文原文，编辑回显用；content 存渲染后正文）
ALTER TABLE `contract_content`
  ADD COLUMN `template_content` longtext COMMENT '含变量占位符的正文模板，编辑回显用' AFTER `content`;

-- ② 合同增删改迁移到 CMD（contract.saveContract / contract.updateContract / contract.deleteContract）
INSERT INTO `c_service` (`id`,`service_id`,`service_code`,`business_type_cd`,`name`,`seq`,`messageQueueName`,`is_instance`,`url`,`method`,`timeout`,`retry_count`,`provide_app_id`,`create_time`,`status_cd`) VALUES
 ('2160','982026112000000001','contract.saveContract','API','新增合同','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2161','982026112000000002','contract.updateContract','API','修改合同','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0'),
 ('2163','982026112000000003','contract.deleteContract','API','删除合同','1',NULL,'CMD','http://store-service','POST','60','3','8000418004',NOW(),'0');

INSERT INTO `c_route` (`id`,`app_id`,`service_id`,`order_type_cd`,`invoke_limit_times`,`invoke_model`,`create_time`,`status_cd`) VALUES
 ('3144','8000418004','982026112000000001','Q','1000','S',NOW(),'0'),
 ('3145','8000418004','982026112000000002','Q','1000','S',NOW(),'0'),
 ('3146','8000418004','982026112000000003','Q','1000','S',NOW(),'0');

-- ③ 旧 V0 接口下线（ContractApi 中三个方法已删除，避免两套入口并存）
UPDATE `c_service` SET `status_cd` = '1', `create_time` = NOW()
 WHERE `service_code` IN ('/contract/saveContract','/contract/updateContract','/contract/deleteContract');

-- ④ 执行后刷新缓存：POST /app/cache.flushCache?cache=APP_ROUTE_SERVICE（可再刷 MAPPING / SERVICE_SQL），或重启 service-api、service-store
-- ⑤ 回滚：DELETE FROM c_route WHERE service_id IN ('982026112000000001','982026112000000002','982026112000000003');
--          DELETE FROM c_service WHERE service_code IN ('contract.saveContract','contract.updateContract','contract.deleteContract');
--          UPDATE c_service SET status_cd = '0' WHERE service_code IN ('/contract/saveContract','/contract/updateContract','/contract/deleteContract');
--          ALTER TABLE contract_content DROP COLUMN template_content;
```

## 2026-11-20 — 合同审批重构：状态机 + 按钮权限 + 审批记录（剥离 Activiti）

> 执行前必须逻辑备份（第 ⑦ 步会 DROP TABLE，回滚只能靠备份）：
> `mysqldump -u<user> -p <db> contract contract_change_plan contract_audit contract_audit_record contract_audit_config p_privilege p_privilege_user p_privilege_rel m_menu c_service t_dict > contract_audit_refactor_bak_20261120.sql`

> 代码命名与表结构严格对齐：`contract_audit_log` ←→ `ContractAuditLogDto/Po`、`IContractAuditLogServiceDao`(+`ContractAuditLogServiceDaoImpl`)、`ContractAuditLogServiceDaoImplMapper.xml`、`IContractAuditInnerServiceSMO.saveContractAuditLog/queryContractAuditLogs`（原 `ContractAuditRecord*` 链路已全部删除，无 recordId↔log_id 别名）。
> 列表查询改 Cmd：`contract.listPendingAuditContracts`（states 11/56 + 最近一次审批记录一次带出，不再前端逐行查审批链）。

```sql
-- ① 审批记录表（一行 = 一次审核动作：谁、哪个环节、通过/驳回、意见、时间）
CREATE TABLE IF NOT EXISTS `contract_audit_log` (
  `log_id` varchar(30) NOT NULL COMMENT '主键，前缀94',
  `biz_type` char(2) NOT NULL DEFAULT '1' COMMENT '1合同 2变更',
  `biz_id` varchar(30) NOT NULL COMMENT '合同ID/变更单ID',
  `step` char(2) NOT NULL COMMENT '1法务校对 2领导审批',
  `result` char(2) NOT NULL COMMENT '1通过 2驳回',
  `opinion` varchar(500) DEFAULT NULL COMMENT '审批意见（驳回必填，前端限200字）',
  `auditor_id` varchar(30) DEFAULT NULL COMMENT '审核人ID',
  `auditor_name` varchar(100) DEFAULT NULL COMMENT '审核人姓名',
  `audit_time` datetime NOT NULL COMMENT '审批时间',
  `store_id` varchar(30) DEFAULT NULL COMMENT '商户ID',
  `status_cd` char(1) NOT NULL DEFAULT '0' COMMENT '数据状态 0有效 1删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_cal_biz` (`biz_id`,`biz_type`,`step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审批记录（法务校对/领导审批留痕）';

-- ② 状态字典：11 改「法务校对」，新增 56「领导审批」（合同与变更单同口径）
UPDATE `t_dict` SET `name` = '法务校对', `description` = '法务校对'
 WHERE `status_cd` = '11' AND `table_name` = 'contract' AND `table_columns` = 'state';

INSERT INTO `t_dict` (`status_cd`,`name`,`description`,`create_time`,`table_name`,`table_columns`)
SELECT '56','领导审批','领导审批',NOW(),'contract','state'
  FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `t_dict` WHERE `status_cd`='56' AND `table_name`='contract' AND `table_columns`='state');

UPDATE `t_dict` SET `name` = '法务校对', `description` = '法务校对'
 WHERE `status_cd` = '11' AND `table_name` = 'contract_change_plan' AND `table_columns` = 'state';

INSERT INTO `t_dict` (`status_cd`,`name`,`description`,`create_time`,`table_name`,`table_columns`)
SELECT '56','领导审批','领导审批',NOW(),'contract_change_plan','state'
  FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `t_dict` WHERE `status_cd`='56' AND `table_name`='contract_change_plan' AND `table_columns`='state');

-- ③ 菜单改名 + 两个审核按钮权限（挂在「合同待审核」菜单下）
UPDATE `m_menu` SET `name` = '合同待审核', `description` = '法务校对 / 领导审批 两级状态审核'
 WHERE `m_id` = '702026111600000002';

INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000009','合同-法务校对','合同待审核-法务校对（通过/驳回）',NOW(),'0','800900000003','/callComponent/contract/auditContractLegal','702026111600000002'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id`='502026111600000009');

INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000010','合同-领导审批','合同待审核-领导审批（通过/驳回）',NOW(),'0','800900000003','/callComponent/contract/auditContractLeader','702026111600000002'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id`='502026111600000010');

-- 停用审核人设置权限（审批人改为「有按钮权限的人」）
UPDATE `p_privilege` SET `status_cd`='1' WHERE `p_id`='502026111600000003';

-- ④ 停用审批实例/待办/审核人配置相关服务（保留 contract.auditContract、contract.queryContractAuditFlow）
UPDATE `c_service` SET `status_cd`='1'
 WHERE `service_code` IN ('contract.submitContractAudit','contract.cancelContractAudit',
   'contract.listContractAuditTasks','contract.listContractAuditHistory','contract.listContractAuditStatistics',
   'contract.saveContractAuditorConfig','contract.queryContractAuditorConfig');

-- ⑤ 存量迁移：旧已办记录 → contract_audit_log（执行前先 count 核对）
INSERT INTO `contract_audit_log`
  (`log_id`,`biz_type`,`biz_id`,`step`,`result`,`opinion`,`auditor_id`,`auditor_name`,`audit_time`,`store_id`,`status_cd`,`create_time`)
SELECT CONCAT('94', REPLACE(UUID(),'-','')), a.`audit_type`, a.`biz_id`, r.`step`, r.`result`, r.`opinion`,
       r.`auditor_id`, r.`auditor_name`, IFNULL(r.`audit_time`, NOW()), a.`store_id`, '0', NOW()
  FROM `contract_audit_record` r
  JOIN `contract_audit` a ON a.`audit_id` = r.`audit_id`
 WHERE r.`result` IN ('1','2') AND r.`status_cd` = '0'
   AND NOT EXISTS (SELECT 1 FROM `contract_audit_log` l WHERE l.`biz_id`=a.`biz_id` AND l.`audit_time`=r.`audit_time`);

-- ⑥ 存量在途实例回写合同/变更单状态（先回写再删表）
UPDATE `contract` c
  JOIN `contract_audit` a ON a.`biz_id`=c.`contract_id` AND a.`audit_type`='1' AND a.`status_cd`='0'
   SET c.`state` = CASE WHEN a.`state`='1' THEN '22' WHEN a.`state`='2' THEN '33'
                        WHEN a.`current_step`='2' THEN '56' ELSE '11' END
 WHERE c.`status_cd`='0' AND a.`state` IN ('0','1','2');

UPDATE `contract_change_plan` p
  JOIN `contract_audit` a ON a.`biz_id`=p.`plan_id` AND a.`audit_type`='2' AND a.`status_cd`='0'
   SET p.`state` = CASE WHEN a.`state`='1' THEN '22' WHEN a.`state`='2' THEN '33'
                        WHEN a.`current_step`='2' THEN '56' ELSE '11' END
 WHERE p.`status_cd`='0' AND a.`state` IN ('0','1','2');

-- ⑦ 旧表删除（⑤⑥ 迁移并核对无误后执行）
DROP TABLE IF EXISTS `contract_audit`;
DROP TABLE IF EXISTS `contract_audit_record`;
DROP TABLE IF EXISTS `contract_audit_config`;

-- ⑧ 合同待审核列表（查询走 Cmd，替代原 /contract/queryContract + 逐行查审批链）
INSERT INTO `c_service` (`id`,`service_id`,`service_code`,`business_type_cd`,`name`,`seq`,`messageQueueName`,`is_instance`,`url`,`method`,`timeout`,`retry_count`,`provide_app_id`,`create_time`,`status_cd`) VALUES
 ('2164','982026111900000026','contract.listPendingAuditContracts','API','合同待审核列表','1',NULL,'CMD','http://store-service','GET','60','3','8000418004',NOW(),'0');

INSERT INTO `c_route` (`id`,`app_id`,`service_id`,`order_type_cd`,`invoke_limit_times`,`invoke_model`,`create_time`,`status_cd`) VALUES
 ('3148','8000418004','982026111900000026','Q','1000','S',NOW(),'0');

-- ⑨ 授权上线清单：把旧「审核人配置」中的人员直接授权 502026111600000009（法务）/ 502026111600000010（领导），双人复核
-- ⑩ 执行后刷新缓存：POST /app/cache.flushCache?cache=APP_ROUTE_SERVICE（可再刷 MAPPING / SERVICE_SQL），或重启 service-api、store-service、community-service
-- ⑪ 回滚：mysql -u<user> -p <db> < contract_audit_refactor_bak_20261120.sql
--          + 代码回滚 + UPDATE c_service SET status_cd='0' WHERE service_code IN (...7个...)
--          + UPDATE p_privilege SET status_cd='0' WHERE p_id='502026111600000003'
--          + UPDATE m_menu SET name='合同审核' WHERE m_id='702026111600000002'
--          + DELETE FROM c_route WHERE id='3138'; DELETE FROM c_service WHERE id='2156'
```

## 2026-11-20 — 合同费用构成落库（contract_fee）+ 打印正文改读库中正文

背景：起草/编辑页选择的费用项（物业费、租金等）此前只在前端试算、保存时被后端丢弃，详情抽屉「费用构成与收款计划」无数据可展示；合同打印页另用模板 context 自行替换 `#中文名#`，导致 `{{partyB}}` 等标签原样打印且与详情抽屉正文不一致。

本次改动（**仅新增 1 张表，不新增服务码**）：

1. 新增表 `contract_fee`（一行 = 合同的一个费用项，含费用项快照 + 试算结果）。
2. `contract.saveContract` / `contract.updateContract` 的 `fees` 落库（更新时旧行 `status_cd='1'` 后重建；payload 不带 `fees` 时不动，兼容老客户端）；`contract.deleteContract` 时费用行同步失效。
3. `contract.listContractCollectionPlans`（`c_service` 已有，无变更）返回体新增 `fees` 与 `feeSummary{feeTotal, includeTotal}`，抽屉/编辑页一次请求拿全。
4. 打印接口 `/contract/printContractTemplate` 增加 `printContent` / `contentSource`（`content` 库中正文 / `rendered` 正文模板渲染 / `template` 合同模板兜底）/ `contentVersion`；正文主路径改为读 `contract_content.content`，不再打印模板。变更单审批通过回写主表后（`applyChangePlan`）自动重渲染并写回 `content`（`content_version` 自增）。
5. 前端 `i18n/menuLang.js` 补 `合同待审核` / `合同-法务校对` / `合同-领导审批` 三语，`layout.vue`、`PrivilegeTree.vue` 增加 `menuText` 兜底：**今后修改 `m_menu.name` / `p_privilege.name` 必须同步 `pc-web/src/i18n/menuLang.js` 三语，否则菜单会显示 `menu.xxx`**。

```sql
-- 执行前备份（示例）
-- mysqldump -u<user> -p <db> contract contract_content c_service > contract_fee_bak_20261120.sql

-- ① 合同费用构成表（费用项快照 + 试算结果，一行 = 合同的一个费用项）
CREATE TABLE IF NOT EXISTS `contract_fee` (
  `cf_id` varchar(30) NOT NULL COMMENT '主键，前缀94',
  `contract_id` varchar(30) NOT NULL COMMENT '合同ID',
  `fee_seq` int(6) NOT NULL DEFAULT '1' COMMENT '排序号（保存顺序）',
  `config_id` varchar(30) DEFAULT NULL COMMENT '费用项ID（pay_fee_config.config_id）',
  `fee_type_cd` varchar(12) DEFAULT NULL COMMENT '费用类型',
  `fee_type_name` varchar(50) DEFAULT NULL COMMENT '费用类型名称快照',
  `fee_name` varchar(100) DEFAULT NULL COMMENT '费用项名称快照',
  `computing_formula` char(4) DEFAULT NULL COMMENT '计算方式（含租金/面积/固定/手工等）',
  `computing_formula_name` varchar(50) DEFAULT NULL COMMENT '计算方式名称快照',
  `unit_price` decimal(10,2) DEFAULT '0.00' COMMENT '单价',
  `additional_amount` decimal(10,2) DEFAULT '0.00' COMMENT '附加费',
  `cycle` char(2) DEFAULT NULL COMMENT '付费周期（月）',
  `months` int(6) DEFAULT '0' COMMENT '期数（月）',
  `coefficient` decimal(10,4) DEFAULT '1.0000' COMMENT '系数',
  `manual_amount` decimal(10,2) DEFAULT NULL COMMENT '手工录入金额',
  `base_amount` decimal(12,2) DEFAULT '0.00' COMMENT '结算基数',
  `amount` decimal(12,2) DEFAULT '0.00' COMMENT '费用金额',
  `include_in_amount` char(1) NOT NULL DEFAULT '1' COMMENT '是否计入合同金额 1是 0否',
  `compute_remark` varchar(500) DEFAULT NULL COMMENT '计算说明',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `store_id` varchar(30) DEFAULT NULL COMMENT '商户ID',
  `status_cd` char(1) NOT NULL DEFAULT '0' COMMENT '数据状态 0有效 1失效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`cf_id`),
  KEY `idx_cf_contract` (`contract_id`,`status_cd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同费用构成（费用项快照 + 试算结果）';

-- ② 服务码：无新增。抽屉/编辑页复用 contract.listContractCollectionPlans（c_service 2148），返回体增加 fees / feeSummary

-- ③ 正文数据源体检（只查不改；打印已改为读 contract_content.content，需先摸清存量）
-- 3.1 有正文记录的合同：打印直接打 content
SELECT c.contract_id, c.contract_code, cc.content_id, cc.content_version,
       CHAR_LENGTH(IFNULL(cc.content,'')) AS content_len
FROM contract c
JOIN contract_content cc ON cc.contract_id = c.contract_id AND cc.status_cd = '0'
WHERE c.status_cd = '0' AND IFNULL(cc.content,'') <> ''
ORDER BY c.create_time DESC;

-- 3.2 正文为空 / 无正文记录的合同：打印走兜底渲染（render(template_content) → 模板 context），
--     建议业务逐条打开编辑页重存一次（重存即刷新 content，content_version 自增）
SELECT c.contract_id, c.contract_code, c.template_id, cc.content_id, cc.content_version
FROM contract c
LEFT JOIN contract_content cc ON cc.contract_id = c.contract_id AND cc.status_cd = '0'
WHERE c.status_cd = '0'
  AND (cc.content_id IS NULL OR IFNULL(cc.content,'') = '');

-- ④ 执行后：重启 store-service（未改 c_service/c_route，无需 flushCache）
-- ⑤ 回滚：DROP TABLE IF EXISTS `contract_fee`;  （代码回滚 + 重启 store-service；contract_content 未被改表结构，无需回滚）
```

## 2026-11-20 — 合同模块小区隔离（community_id 落地）


```sql
ALTER TABLE contract                ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_partya         ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_type_template  ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_change_plan    ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_collection_plan ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_audit_log      ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_remind_log     ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;

-- B. 子表加列
ALTER TABLE contract_room                    ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_fee                     ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_file                    ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER contract_id;
ALTER TABLE contract_attr                    ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_content                 ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER contract_id;
ALTER TABLE contract_change_plan_detail      ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_change_plan_room        ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;
ALTER TABLE contract_change_plan_detail_attr ADD COLUMN community_id varchar(30) NOT NULL DEFAULT '-1' COMMENT '小区ID' AFTER store_id;

-- C. 索引
CREATE INDEX idx_contract_community_id              ON contract(community_id);
CREATE INDEX idx_contract_partya_community_id       ON contract_partya(community_id);
CREATE INDEX idx_contract_template_community_id     ON contract_type_template(community_id);
CREATE INDEX idx_contract_change_community_id       ON contract_change_plan(community_id);
CREATE INDEX idx_contract_plan_community_id         ON contract_collection_plan(community_id);
CREATE INDEX idx_contract_audit_community_id        ON contract_audit_log(community_id, biz_id);
CREATE INDEX idx_contract_remind_community_id       ON contract_remind_log(community_id, contract_id);
CREATE INDEX idx_contract_room_community_id         ON contract_room(community_id, contract_id);
CREATE INDEX idx_contract_fee_community_id          ON contract_fee(community_id, contract_id);
CREATE INDEX idx_contract_file_community_id         ON contract_file(community_id, contract_id);
CREATE INDEX idx_contract_attr_community_id         ON contract_attr(community_id, contract_id);
CREATE INDEX idx_contract_content_community_id      ON contract_content(community_id, contract_id);
CREATE INDEX idx_contract_cpd_community_id          ON contract_change_plan_detail(community_id, plan_id);
CREATE INDEX idx_contract_cpr_community_id          ON contract_change_plan_room(community_id, plan_id);
CREATE INDEX idx_contract_cpda_community_id         ON contract_change_plan_detail_attr(community_id, detail_id);
```

## 2026-11-20 — 合同费用构成空串归一（修复 1366 Incorrect decimal value）+「关联费用」可选开关

背景：起草/编辑租赁合同保存时报 `Incorrect decimal value: '' for column 'manual_amount'`（严格模式下把空串当 decimal 绑定）。原因是费用行 `manualAmount` 非手工公式时恒为 `''`，后端 `BeanConvertUtil.covertBean` 原样转成 `Po.manualAmount=''`，Mapper 直绑 `#{manualAmount}`。

本次改动（**无 DDL、无 Mapper 变更、无新增服务码**）：

1. 新增 `service-store/.../store/util/ContractFeeNormalizeUtil.java`：费用行 / 收款计划行写库前的唯一归一收口。
   - `normalizeForSave`：空串、空白、null 一律补列默认值（`fee_seq=1`、金额 `0.00`、系数 `1.0000`、期数 `0`、`include_in_amount=1`）；`manual_amount` 空串转 `NULL`（保留“未手工录入”语义）；`cycle` 空串转 `NULL`。
   - `normalizeForUpdate`：空串补默认值，**null 保持 null**（软删/局部更新不被补值）；`includeInAmount` 为 null 时不参与更新。
   - `normalizePlanForSave` / `normalizePlanForUpdate`：`plan_amount` / `ratio` / `plan_seq` 同规则归一，`plan_time` / `pay_time` 空串转 `NULL`。
   - `normalizeIncludeInAmount`：布尔 / `'1'` / `'true'` → `'1'`，其余 → `'0'`（两个 BMO 的私有同名方法已删除，改调本工具）。
   - 非法数字（如 `'abc'`、`'--'`）抛 `IllegalArgumentException("费用项[xxx]的格式不正确…")`，由 CMD 层转成前端提示。
2. 归一调用点（写库唯一入口）：`ContractFeeInnerServiceSMOImpl.saveContractFee/updateContractFee`、`ContractCollectionPlanInnerServiceSMOImpl.saveContractCollectionPlan/updateContractCollectionPlan`。
3. 前端提交归一：`utils/contractFeeUtil.js` 新增 `normalizeFeeForSubmit` / `normalizePlanForSubmit`，`addContractList.vue` 的 `buildPayload()` 使用；`ContractFeeAddDialog.vue` 的手工金额提交前 `trim()`，非手工公式不提交 `manualAmount`（传 `null`）。
4. 前端交互（需求 2）：租赁合同 02 区块「关联出租房屋」工具栏右上角新增「关联费用 / 收起费用」按钮（`ContractTargetTable` 增加 `feeVisible` / `feeCount` / `feeDisabled` props、`toggle-fee` 事件、`fee-panel` 具名插槽），费用构成面板就地展开在房屋表格下方；**默认收起**，点击「收起费用」二次确认后清空已关联费用并联动合同金额；03 区块改为 `hasTemplate && !isRent`。
5. 费用构成改为**非必填**：`ContractSummaryPanel` 中租赁合同费用项标记 `optional`，`failedLabels()` 过滤可选项，不再拦截「提交审批」；未关联费用时 `fees=[]` 落库无费用行（编辑时旧行照旧 `status_cd='1'`）。
6. i18n：`contractWorkbenchLang.js` 三语补 `target.linkFee / collapseFee / feeCount / feeRoomFirst / feeOptionalTip / feeClearConfirm` 与 `integrity.feeOptional`。

```sql
-- 本次无 SQL / 无 DDL / 无 c_service、c_route 变更

-- 执行后：重启 store-service（Java 归一代码生效），前端重新发布即可
-- 回滚：代码回滚 + 重启 store-service，前端发布回滚；历史数据无需处理（本次未改表结构、未刷数据）
```

