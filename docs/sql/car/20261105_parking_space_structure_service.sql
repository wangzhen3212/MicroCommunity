-- ============================================================
-- 车位结构图(左树右卡片) 新增服务路由注册(2026-11-05)
-- 执行环境: 物业库(TT)
-- 说明: 新增 serviceCode car.listParkingSpaceStructures
--       (service-report / ListParkingSpaceStructuresCmd)
-- 执行后需刷新服务路由缓存(AppRouteCache)或重启 service-api
-- ============================================================

-- ① 服务定义 c_service (id 取当前最大 2125 + 1, 如冲突请改为自增)
INSERT INTO `c_service` (`id`, `service_id`, `service_code`, `business_type_cd`, `name`, `seq`, `messageQueueName`, `is_instance`, `url`, `method`, `timeout`, `retry_count`, `provide_app_id`, `create_time`, `status_cd`)
VALUES ('2126', '982026110515300001', 'car.listParkingSpaceStructures', 'API', '查询车位结构图数据', '1', null, 'CMD', 'http://report-service', 'GET', '60', '3', '8000418002', '2026-11-05 15:30:00', '0');

-- ② 路由关系 c_route(物业端 app 8000418004, GET 用 Q)
INSERT INTO `c_route` (`id`, `app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `create_time`, `status_cd`)
VALUES ('3108', '8000418004', '982026110515300001', 'Q', '1000', 'S', '2026-11-05 15:30:00', '0');
