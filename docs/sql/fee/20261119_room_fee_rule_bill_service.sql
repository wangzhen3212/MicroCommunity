-- ============================================================
-- 房屋收费「计费设置与账单」合并视图服务路由注册(2026-11-19)
-- 执行环境: 物业库(TT)
-- 说明: 新增 serviceCode
--       payFeeRule.listRoomFeeRuleWithBill (service-fee / ListRoomFeeRuleWithBillCmd, GET)
-- 执行后需刷新服务路由缓存(AppRouteCache)或重启 service-api
-- id 取值说明: c_service 2156 / c_route 3138 已被 contract room.queryLeasableRooms 占用,故本次取 2157 / 3139
-- 脚本可重复执行
-- ============================================================

DELETE FROM `c_route` WHERE `service_id` IN ('982026111914170001');
DELETE FROM `c_service` WHERE `service_code` IN ('payFeeRule.listRoomFeeRuleWithBill');

-- ① 服务定义 c_service
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2157', '982026111914170001', 'payFeeRule.listRoomFeeRuleWithBill', 'API', '查询房屋计费设置与账单', '1', null, 'CMD', 'http://fee-service', 'GET', '60', '3', '8000418002', '2026-11-19 14:00:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004, GET 用 Q)
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3139', '8000418004', '982026111914170001', 'Q', '1000', 'S', '2026-11-19 14:00:00', '0');
