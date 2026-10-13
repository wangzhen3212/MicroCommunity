-- 场地预约模块优化（场地管理 / 场地预约 / 预约订单）
-- 说明：
-- 1. appointment_time 统一为日期语义 YYYY-MM-DD
-- 2. community_space_person_time 增加冗余日期列 appointment_date，便于占用判定与查询
-- 3. 补充收款/取消留痕字段
-- 4. 修正 state 字典并补齐时段状态、支付方式字典
-- 5. 注册预约订单汇总接口

-- 1. community_space_person：预约日期语义规范 + 索引 + 收款/取消留痕
ALTER TABLE `community_space_person`
    MODIFY COLUMN `appointment_time` varchar(10) NOT NULL COMMENT '预约日期 YYYY-MM-DD',
    ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '收款时间' AFTER `pay_way`,
    ADD COLUMN `cancel_time` datetime DEFAULT NULL COMMENT '取消时间' AFTER `pay_time`,
    ADD COLUMN `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因' AFTER `cancel_time`,
    ADD INDEX `idx_csp_space_date` (`space_id`, `appointment_time`),
    ADD INDEX `idx_csp_state` (`state`);

-- 2. community_space_person_time：新增冗余日期列 + 索引
ALTER TABLE `community_space_person_time`
    ADD COLUMN `appointment_date` varchar(10) NOT NULL DEFAULT '' COMMENT '预约日期 YYYY-MM-DD' AFTER `hours`,
    ADD INDEX `idx_cspt_space_date_hours` (`space_id`, `appointment_date`, `hours`);

-- 3. 存量数据清洗：appointment_time 截断为日期；回填 person_time.appointment_date
UPDATE `community_space_person` SET `appointment_time` = LEFT(`appointment_time`, 10)
 WHERE CHAR_LENGTH(`appointment_time`) > 10;

UPDATE `community_space_person_time` cspt
  JOIN `community_space_person` csp ON cspt.csp_id = csp.csp_id
   SET cspt.appointment_date = csp.appointment_time
 WHERE cspt.appointment_date = '' OR cspt.appointment_date IS NULL;

-- 4. 字典修正/补齐（与 TT.sql 保持一致）
-- 4.1 community_space_person.state 存量重复取值：F 同时表示"预约失败"和"待支付"，删除语义错误的"待支付"行
DELETE FROM `t_dict` WHERE `table_name` = 'community_space_person' AND `table_columns` = 'state'
   AND `status_cd` = 'F' AND `name` = '待支付';
-- 4.2 补齐取值（id 取当前最大字典 id 之后的空号，避免与既有字典冲突）
DELETE FROM `t_dict` WHERE `id` IN ('626353', '626354', '626355', '626356', '626357', '626358', '626359', '626360');
INSERT INTO `t_dict` (`id`,`status_cd`,`name`,`description`,`create_time`,`table_name`,`table_columns`) VALUES
  ('626353', 'WP', '待支付', '待支付', NOW(), 'community_space_person', 'state'),
  ('626354', 'C',  '已完成', '已完成', NOW(), 'community_space_person', 'state'),
  ('626355', 'W',  '待核销', '待核销', NOW(), 'community_space_person_time', 'state'),
  ('626356', 'C',  '已核销', '已核销', NOW(), 'community_space_person_time', 'state'),
  ('626357', 'CL', '已取消', '已取消', NOW(), 'community_space_person_time', 'state'),
  ('626358', '1',  '现金',   '现金',   NOW(), 'community_space_confirm_order', 'pay_way'),
  ('626359', '2',  '微信',   '微信',   NOW(), 'community_space_confirm_order', 'pay_way'),
  ('626360', '3',  '支付宝', '支付宝', NOW(), 'community_space_confirm_order', 'pay_way');

-- 5. 核销单收款留痕（用于二维码收款）
ALTER TABLE `community_space_confirm_order`
    ADD COLUMN `received_amount` decimal(10,2) DEFAULT '0.00' COMMENT '实收金额' AFTER `space_id`,
    ADD COLUMN `pay_way` varchar(12) DEFAULT NULL COMMENT '支付方式 1 现金 2 微信 3 支付宝' AFTER `received_amount`,
    ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '收款时间' AFTER `pay_way`;

-- 6. 新增汇总接口服务注册（c_service 2110、c_route 3096，接续基线最大编号）
DELETE FROM `c_service` WHERE `service_code` = 'communitySpace.listCommunitySpacePersonSummary';
DELETE FROM `c_route` WHERE `service_id` = '982026101315180001';

INSERT INTO `c_service` VALUES ('2110', '982026101315180001', 'communitySpace.listCommunitySpacePersonSummary', 'API', '场地预约订单汇总', '1', null, 'CMD', 'http://community-service', 'GET', '60', '3', '8000418002', NOW(), '0');

INSERT INTO `c_route` VALUES ('3096', '8000418004', '982026101315180001', 'Q', '1000', 'S', NOW(), '0');
