# 合同中心 SQL 变更记录

> 本次合同模块原型优化（方案 `.txcode/plan-code/20261116111521/20261116111521_方案.md`）全部数据库变更统一记录在本文件，
> 不再新增独立 `.sql` 文件。按小节顺序执行，每小节均可重复执行（`IF NOT EXISTS` / `INSERT IGNORE` / 先判空），
> 执行完成后在本文件对应小节追加「执行人 / 执行时间」。

## 一、DDL：合同主表扩展

```sql
-- 合同主表：甲方主体、模板、付款方式（正文不落主表，见 contract_content）
ALTER TABLE `contract`
  ADD COLUMN `partya_id` varchar(30) DEFAULT NULL COMMENT '甲方主体ID' AFTER `party_a`,
  ADD COLUMN `template_id` varchar(30) DEFAULT NULL COMMENT '合同模板ID' AFTER `contract_type`,
  ADD COLUMN `pay_mode` varchar(100) DEFAULT NULL COMMENT '付款方式' AFTER `amount`;
```

> 若字段已存在（重复执行报 1060），忽略继续。

## 二、DDL：新增表

```sql
-- 合同正文（独立表，列表/统计不 join）
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

-- 两级审批实例（替代 Activiti 流程实例）
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

-- 审批环节记录
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

-- 审核人配置
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

-- 到期提醒推送留痕
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
```

## 三、DDL：既有业务表字段扩展

```sql
-- 合同类型分类：1 房屋出租 / 2 采购 / 3 维保
ALTER TABLE `contract_type`
  ADD COLUMN `category` varchar(4) DEFAULT '2' COMMENT '1房屋出租 2采购 3维保' AFTER `type_name`;

-- 合同模板卡片信息
ALTER TABLE `contract_type_template`
  ADD COLUMN `template_name` varchar(100) DEFAULT NULL COMMENT '模板名称' AFTER `template_id`,
  ADD COLUMN `template_version` varchar(20) DEFAULT 'V1.0' COMMENT '模板版本' AFTER `template_name`,
  ADD COLUMN `template_desc` varchar(500) DEFAULT NULL COMMENT '模板描述' AFTER `template_version`,
  ADD COLUMN `use_count` int DEFAULT '0' COMMENT '使用次数' AFTER `template_desc`,
  ADD COLUMN `source_type` varchar(20) DEFAULT 'online' COMMENT '来源 online/word/paste' AFTER `use_count`;

-- 甲方主体资料
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

-- 收款计划 -> 履约节点
ALTER TABLE `contract_collection_plan`
  ADD COLUMN `plan_seq` int DEFAULT NULL COMMENT '期次' AFTER `plan_name`,
  ADD COLUMN `plan_time` datetime DEFAULT NULL COMMENT '计划收款日期' AFTER `plan_seq`,
  ADD COLUMN `plan_amount` decimal(12,2) DEFAULT '0.00' COMMENT '计划金额' AFTER `plan_time`,
  ADD COLUMN `ratio` decimal(6,4) DEFAULT '0.0000' COMMENT '比例' AFTER `plan_amount`,
  ADD COLUMN `pay_state` char(1) DEFAULT '0' COMMENT '0未到期 1已付 2逾期' AFTER `ratio`,
  ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '实付时间' AFTER `pay_state`,
  ADD COLUMN `trigger_desc` varchar(200) DEFAULT NULL COMMENT '履约节点描述' AFTER `pay_time`,
  ADD COLUMN `fee_type` varchar(20) DEFAULT NULL COMMENT '费用类型 rent/property/other/water' AFTER `trigger_desc`;
```

## 四、字典：合同状态新增「草稿」

```sql
INSERT INTO `t_dict` (`status_cd`, `name`, `description`, `create_time`, `table_name`, `table_columns`)
SELECT '10', '草稿', '草稿', NOW(), 'contract', 'state'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_dict` WHERE `status_cd` = '10' AND `table_name` = 'contract' AND `table_columns` = 'state');
```

## 五、数据回填：存量合同补齐甲方主体

```sql
-- 按 party_a 名称匹配 contract_partya，回填 partya_id（仅在为空时更新）
UPDATE `contract` t
JOIN `contract_partya` p
  ON p.`party_a` = t.`party_a` AND p.`status_cd` = '0' AND p.`store_id` = t.`store_id`
SET t.`partya_id` = p.`partya_id`
WHERE (t.`partya_id` IS NULL OR t.`partya_id` = '');

-- 存量合同类型按 typeName 关键字兜底归类
UPDATE `contract_type` SET `category` = '1' WHERE `category` IS NULL AND (`type_name` LIKE '%租%');
UPDATE `contract_type` SET `category` = '3' WHERE `category` IS NULL AND (`type_name` LIKE '%维保%' OR `type_name` LIKE '%维修%');
UPDATE `contract_type` SET `category` = '2' WHERE `category` IS NULL;
```

## 六、存量在途审批迁移（Activiti -> 两级审批）

> 执行前先 dry-run 出清单，执行后核对：所有 `contract.state = '11'` 的单据都存在 `contract_audit.state = '0'` 的实例。
> Activiti 流程实例只读保留，不再推进，避免双写。

```sql
-- 6.1 在途合同清单（dry-run）
SELECT c.`contract_id`, c.`contract_code`, c.`contract_name`, c.`state`
FROM `contract` c
WHERE c.`state` = '11' AND c.`status_cd` = '0';

-- 6.2 为每个在途合同创建审批实例（已存在审批中的实例则跳过）
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

-- 6.3 为迁移实例生成法务校对/领导审核两条环节记录（审核人取 contract_audit_config）
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

-- 6.4 已办历史任务（Activiti ACT_HI_TASKINST）按 business_key 映射写入通过记录，供历史意见查询
-- 说明：节点名含「法务」映射 step=1，含「领导/总经理/经理」映射 step=2，其余忽略。
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

-- 6.5 迁移结果自检
SELECT a.`audit_id`, a.`biz_id`, a.`current_step`, a.`state`, COUNT(r.`record_id`) recordCount
FROM `contract_audit` a
LEFT JOIN `contract_audit_record` r ON r.`audit_id` = a.`audit_id`
WHERE a.`audit_type` = '1' AND a.`state` = '0'
GROUP BY a.`audit_id`, a.`biz_id`, a.`current_step`, a.`state`;
```

> 变更单（`contract_change_plan`）的在途审批迁移方式相同，把 `audit_type` 置 `2`、`biz_id` 取 `plan_id` 即可。

## 七、服务注册（c_service + c_route）

> `appId = 8000418004`（物业端），`is_instance = 'CMD'`，`url = http://store-service`。
> 执行后需刷新 `AppRouteCache` 或重启 `service-api`。

```sql
-- 合同中心新增服务（service_id 统一 9820261116000000xx）
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
```

## 八、菜单与权限

```sql
-- 8.1 菜单项：合同履约（挂在合同管理菜单组 802020091723050020 下）
-- 注意：m_menu 实际字段为 (m_id,name,g_id,url,seq,p_id,description,create_time,status_cd,is_show)，无 icon / pg_id 字段
INSERT INTO `m_menu` (`m_id`,`name`,`g_id`,`url`,`seq`,`p_id`,`description`,`create_time`,`status_cd`,`is_show`)
SELECT '702026111600000001','合同履约','802020091723050020','/#/pages/admin/contractPerformance','9','502026111600000001','合同履约跟踪（付款节点/逾期）',NOW(),'0','Y'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `m_menu` WHERE `m_id` = '702026111600000001');

-- 8.2 菜单权限（p_privilege.m_id 必填，指向所属菜单；domain 取物业 800900000003）
INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000001','合同履约','合同履约菜单查看',NOW(),'0','800900000003','/#/pages/admin/contractPerformance','702026111600000001'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000001');

INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000002','合同履约-查看','合同履约列表查看',NOW(),'0','800900000003','/callComponent/queryContract/listContractPerformances','702026111600000001'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id` = '502026111600000002');

-- 8.3 合同中心操作按钮权限码（审核人设置 / 导出 / 批量终止 / 推送提醒 / 登记付款）
-- 按钮权限挂到合同查询菜单 702020091785290021（物业端合同中心入口）
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
```

> 说明：`m_menu` 无 `icon`/`pg_id` 字段，菜单组用 `g_id`（合同管理组 `802020091723050020`）；`p_privilege.m_id` 为 NOT NULL，按钮权限须挂到所属菜单；
> `m_menu` / `p_privilege` / `p_privilege_rel` 的具体菜单组、`domain` 需按实际租户数据核对后再执行；
> `p_privilege_rel` 实际字段为 `(rel_id 自增, p_id, pg_id, create_time, status_cd)`（`pg_id` 是权限组，不是角色），
> 授权行按权限组插入或在后台「权限分配」页面勾选：
> `INSERT INTO p_privilege_rel (p_id, pg_id, create_time, status_cd) SELECT '502026111600000001', '权限组ID', NOW(), '0' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM p_privilege_rel WHERE p_id='502026111600000001' AND pg_id='权限组ID');`。

## 九、定时任务（合同到期提醒）

> 每日扫描生效中合同，到期前 30 / 15 / 7 天写入 `contract_remind_log`（`channel=inner`）；当天同类型提醒不重复推送。
> 定时表达式按每日 08:00 在「定时任务」页面配置（`task.taskTemplate` 引用 `class_bean=expirationContractRemindTemplate`）。

```sql
INSERT INTO `task_template` (`template_id`, `template_name`, `template_desc`, `create_time`, `status_cd`, `class_bean`)
SELECT '1037', '合同到期提醒', '合同到期前30/15/7天推送提醒', NOW(), '0', 'expirationContractRemindTemplate'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `task_template` WHERE `template_id` = '1037');
```

## 十、执行记录

| 小节 | 执行人 | 执行时间 | 备注 |
|---|---|---|---|
| 一、二、三、四 |  |  |  |
| 五、六 |  |  | 存量回填与审批迁移，需先 dry-run |
| 七 |  |  | 执行后刷新 AppRouteCache |
| 八 |  |  | 需核对 pg_id / domain |
| 九 |  |  | 需在定时任务页面新增每日任务 |
| 十一 |  |  | 合同菜单对齐原型：改名/排序/下线起草合同/新增合同审核菜单与权限，执行后重启 service-user |

## 十一、菜单与原型对齐

> 目标：合同菜单组 `802020091723050020` 下仅 7 项，顺序为 合同甲方 → 合同模板 → 合同台账 → 合同履约 → 合同变更 → 到期提醒 → 合同审核。
> 只改数据（`m_menu` / `p_privilege` / `p_privilege_rel` / `m_menu_user`），不删物理行，回滚把 `status_cd` 改回 `'0'`、`is_show` 改回 `'Y'` 即可。
> 全部语句幂等，可重复执行。

```sql
-- 11.1 菜单重命名与排序（合同组 802020091723050020）
UPDATE `m_menu` SET seq='1' WHERE m_id='702021042214040302'; -- 合同甲方（名称不变，仅排序）
UPDATE `m_menu` SET name='合同模板', seq='2' WHERE m_id='702020091843810076';
UPDATE `m_menu` SET name='合同台账', seq='3' WHERE m_id='702020091785290021';
UPDATE `m_menu` SET name='合同履约', seq='4' WHERE m_id='702026111600000001';
UPDATE `m_menu` SET name='合同变更', seq='5' WHERE m_id='702021033017840295';
UPDATE `m_menu` SET name='到期提醒', seq='6' WHERE m_id='702021033149670076';

-- 11.2 下线「起草合同」菜单（原型无该入口；起草走合同台账「新建合同」，路由 /#/pages/admin/newContractManage 保留）
UPDATE `m_menu` SET status_cd='1', is_show='N' WHERE m_id='702021033187310391';
UPDATE `p_privilege` SET status_cd='1' WHERE m_id='702021033187310391';

-- 11.3 新增「合同审核」菜单
INSERT INTO `m_menu` (`m_id`,`name`,`g_id`,`url`,`seq`,`p_id`,`description`,`create_time`,`status_cd`,`is_show`)
SELECT '702026111600000002','合同审核','802020091723050020','/#/pages/admin/contractApplyAuditOrders',
       '7','-1','待我审批/已通过/已驳回（合同与变更统一审核）',NOW(),'0','Y'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `m_menu` WHERE `m_id`='702026111600000002');

-- 11.4 菜单权限 + 按钮权限归位
INSERT INTO `p_privilege` (`p_id`,`name`,`description`,`create_time`,`status_cd`,`domain`,`resource`,`m_id`)
SELECT '502026111600000008','合同审核','合同审核菜单查看',NOW(),'0','800900000003',
       '/#/pages/admin/contractApplyAuditOrders','702026111600000002'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `p_privilege` WHERE `p_id`='502026111600000008');

-- 「合同审核人设置」按钮权限改挂到「合同审核」菜单
UPDATE `p_privilege` SET m_id='702026111600000002' WHERE p_id='502026111600000003';

-- 11.5 权限名称同步（合同甲方权限名保持原样）+ 修正历史 resource 指向
UPDATE `p_privilege` SET name='合同模板' WHERE p_id='502020091858500077';
UPDATE `p_privilege` SET name='合同台账' WHERE p_id='502020091779900022';
UPDATE `p_privilege` SET name='到期提醒', resource='/#/pages/admin/expirationContractManage'
 WHERE p_id='502021033136470077';

-- 11.6 权限组授权（pg_id 按实际合同管理员权限组替换；或到「权限分配」页面勾选）
-- 需授权的菜单/按钮权限：合同履约 502026111600000001、合同履约-查看 502026111600000002、
-- 合同审核 502026111600000008、合同审核人设置 502026111600000003、合同导出 502026111600000004、
-- 合同批量终止 502026111600000005、合同推送提醒 502026111600000006、合同登记付款 502026111600000007
INSERT INTO `p_privilege_rel` (`p_id`,`pg_id`,`create_time`,`status_cd`)
SELECT '502026111600000008','权限组ID',NOW(),'0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `p_privilege_rel` WHERE p_id='502026111600000008' AND pg_id='权限组ID');


-- 11.7 收藏项名称同步（前端菜单收藏显示新名称）
UPDATE `m_menu_user` SET name='合同台账' WHERE m_id='702020091785290021';
UPDATE `m_menu_user` SET name='合同模板' WHERE m_id='702020091843810076';
UPDATE `m_menu_user` SET name='到期提醒' WHERE m_id='702021033149670076';
```

> 执行顺序建议：先跑 11.1~11.5（菜单改名/排序/新增审核菜单），观察菜单结构后，再按权限组执行 11.6 授权。
> 执行完成后必须刷新菜单缓存：重启 `service-user`（或清 `AppRouteCache`），前端重新登录以刷新菜单树。
> 「合同审核」未授权时仅超级管理员可见（`p_privilege_rel` 未写入即不展示）。

## 十二、合同类型下线（contract_type / contract_type_spec → 模板 category）

> 迁移脚本：`docs/sql/contract/20261117_contract_type_offline.sql`
> 执行顺序不可调换：① 模板新增 `category` → ② 按原类型 `category` 回填模板 → ③ 删 `contract_type_template.contract_type_id` → ④ 删 `contract.contract_type` → ⑤ 删 `contract_change_plan_detail.contract_type` → ⑥ 删两表 → ⑦ 清字典与接口注册。
> 执行前备份四表：`mysqldump -u<user> -p <db> contract contract_type contract_type_spec contract_type_template > contract_type_offline_bak.sql`。

```sql
-- 1. 模板新增类型（1 房屋出租 / 2 采购 / 3 维保），位置在 contract_type_id 之后
ALTER TABLE `contract_type_template`
  ADD COLUMN `category` varchar(12) NOT NULL DEFAULT '2' COMMENT '1房屋出租 2采购 3维保' AFTER `contract_type_id`;

-- 2. 回填（必须在删表之前；contract_type.category 由 2026-11-13 增量脚本提供，缺列先补）
UPDATE `contract_type_template` t
LEFT JOIN `contract_type` ct ON ct.`contract_type_id` = t.`contract_type_id`
SET t.`category` = IFNULL(ct.`category`, '2')
WHERE t.`status_cd` = '0';

-- 3. 删模板与类型的关联字段
ALTER TABLE `contract_type_template` DROP COLUMN `contract_type_id`;

-- 4. 删合同上的类型字段（历史合同凭 template_id 追溯形态）
ALTER TABLE `contract` DROP COLUMN `contract_type`;

-- 5. 删变更明细的类型快照（原 NOT NULL，类型表删除后无法回填）
ALTER TABLE `contract_change_plan_detail` DROP COLUMN `contract_type`;

-- 6. 删废弃表
DROP TABLE IF EXISTS `contract_type_spec`;
DROP TABLE IF EXISTS `contract_type`;

-- 7. 字典与接口注册（模板接口名同样含 ContractType，故用精确清单）
DELETE FROM `t_dict` WHERE `table_name` = 'contract_type';           -- id 373 / 374
DELETE FROM `c_route` WHERE `id` IN ('734','735','736','737','738','739','740','741');
DELETE FROM `c_service` WHERE `id` IN ('499','500','501','502','503','504','505','506');
```

> 菜单：`m_menu` 中无 `contractTypeSpecManage` 记录（该项无需停用）；`合同类型` 菜单（`702020091843810076` → `/#/pages/admin/contractTypeManage`）实为**合同模板库**入口，保留不动。
> `docs/db/TT.sql` 基线本次未合并（该文件为历史快照且体量过大），新建库请在全量导入后补执行本小节脚本。

### 代码侧变更（无 DDL）

- 后端：删除 `ContractTypePo/Dto`、`ContractTypeSpecPo/Dto`、`IContractTypeInnerServiceSMO`、`IContractTypeSpecInnerServiceSMO`、两套 DAO/Impl、两套 InnerServiceSMOImpl、`bmo/contractType/`、`bmo/contractTypeSpec/` 与两个 Mapper XML；`ContractTypeTemplatePo/Dto` 去掉 `contractTypeId`、增加 `category`；`ContractPo` 去掉 `contractType`，`ContractDto` 去掉 `contractType/contractTypeName` 改出 `templateName`；`contract_change_plan_detail` 相关 Dto/Po 去掉 `contractType`。
- Mapper：`ContractServiceDaoImplMapper.xml` 全部合同查询改 `left join contract_type_template ctt on ctt.template_id = t.template_id`，输出 `ctt.template_name templateName` 与 `ifnull(ctt.category,'2') category`，新增 `templateId` 过滤；`ContractTypeTemplateServiceDaoImplMapper.xml` 重写（`category` 读写、去掉类型关联）；审核/变更相关 Mapper 输出 `templateName`；变更明细 Mapper 通过 `contract` 关联模板取 `templateName`。
- 审核口径：`SaveContractBMOImpl`、`BatchSubmitContractCmd` 不再读 `contract_type.audit`，统一走合同审核流程。
- 前端：模板库删除「类型维护」抽屉与类型/类型规格组件（`addContractType`、`editContractType`、`deleteContractType`、`contractTemplateEditor`、`addTemplateView`、`*ContractTypeSpec*`、两个 api 封装与两个页面）；模板编辑页新增「类型」必选下拉（提交 `category`）；合同工作台去掉合同类型下拉、模板改必选；台账/起草/履约/详情/审核/选择合同等处「合同类型」列统一改「合同模板」并取模板名；三语 i18n 同步。



