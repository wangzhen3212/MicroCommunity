-- ============================================================
-- 房屋收费「按计费设置查询账单」服务路由注册(2026-11-19)
-- 执行环境: 物业库(TT)
-- 说明: 新增 serviceCode
--       payFeeRule.listRuleBills (service-fee / ListRuleBillsCmd, GET)
-- 执行后需刷新服务路由缓存(AppRouteCache)或重启 service-api
-- id 取值说明: c_service 2156 / c_route 3138 已被 contract 占用,
--              2157 / 3139 已被 payFeeRule.listRoomFeeRuleWithBill 占用,故本次取 2158 / 3140
-- 脚本可重复执行
-- ============================================================

DELETE FROM `c_route` WHERE `service_id` IN ('982026111916300002');
DELETE FROM `c_service` WHERE `service_code` IN ('payFeeRule.listRuleBills');

-- ① 服务定义 c_service
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2158', '982026111916300002', 'payFeeRule.listRuleBills', 'API', '查询计费设置下的账单', '1', null, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-11-19 16:30:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004, GET 用 Q)
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3140', '8000418004', '982026111916300002', 'Q', '1000', 'S', '2026-11-19 16:30:00', '0');
