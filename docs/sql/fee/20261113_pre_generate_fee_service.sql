-- ============================================================
-- 房屋设置费用页「预生成账单」服务路由注册(2026-11-13)
-- 执行环境: 物业库(TT)
-- 说明: 新增 serviceCode
--       fee.queryPreGenerateFeePeriods (service-fee / QueryPreGenerateFeePeriodsCmd, GET)
--       fee.preGenerateFee             (service-fee / PreGenerateFeeCmd, POST)
-- 执行后需刷新服务路由缓存(AppRouteCache)或重启 service-api
-- ============================================================

DELETE FROM `c_service` WHERE `service_code` IN (
  'fee.queryPreGenerateFeePeriods',
  'fee.preGenerateFee');
DELETE FROM `c_route` WHERE `service_id` IN ('982026111309000001', '982026111309000002');

-- ① 服务定义 c_service (id 取当前最大 2126 + 1，如冲突请改为自增)
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2127', '982026111309000001', 'fee.queryPreGenerateFeePeriods', 'API', '查询预生成账单期次', '1', null, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-11-13 09:00:00', '0');

INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2128', '982026111309000002', 'fee.preGenerateFee', 'API', '预生成费用账单', '1', null, 'CMD', 'http://fee-service', 'POST', '60', '3', '8000418002', '2026-11-13 09:00:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004, GET 用 Q、POST 用 D)
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3109', '8000418004', '982026111309000001', 'Q', '1000', 'S', '2026-11-13 09:00:00', '0');

INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3110', '8000418004', '982026111309000002', 'D', '1000', 'S', '2026-11-13 09:00:00', '0');
