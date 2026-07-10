-- =====================================================
-- 自定义收据模板 - 添加保存、更新、删除服务及路由
-- =====================================================
-- 说明：此脚本在已有查询接口的基础上，添加增删改功能
-- 执行前请确认：feePrintPageTemplate.listFeePrintPageTemplate 已存在
-- =====================================================

-- 第一步：检查现有配置
SELECT '=== 检查现有配置 ===' as step;

-- 查询已有的服务配置
SELECT id, service_id, service_code, name, method, status_cd
FROM c_service
WHERE service_code LIKE 'feePrintPageTemplate.%'
ORDER BY id;

-- 查询已有的路由配置
SELECT r.id, r.app_id, r.service_id, s.service_code, s.name, r.order_type_cd, r.status_cd
FROM c_route r
LEFT JOIN c_service s ON r.service_id = s.service_id
WHERE r.app_id = '8000418004' 
  AND s.service_code LIKE 'feePrintPageTemplate.%'
ORDER BY r.id;

-- 第二步：获取当前最大ID，避免冲突
SELECT '=== 获取当前最大ID ===' as step;

SELECT MAX(id) as max_service_id FROM c_service;
SELECT MAX(id) as max_route_id FROM c_route;

-- 第三步：添加服务配置（使用大于max_service_id的ID）
-- 注意：如果下面的ID与您的数据库冲突，请手动调整

SELECT '=== 添加服务配置 ===' as step;

-- 保存收据模板服务
INSERT INTO `c_service` (
    `id`,
    `service_id`, 
    `service_code`, 
    `business_type_cd`, 
    `name`, 
    `seq`, 
    `messageQueueName`, 
    `is_instance`, 
    `url`, 
    `method`, 
    `timeout`, 
    `retry_count`, 
    `provide_app_id`, 
    `create_time`,
    `status_cd`
) VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM c_service),  -- 自动获取下一个ID
    '982026061566580007',  -- service_id（保持唯一）
    'feePrintPageTemplate.saveFeePrintPageTemplate',  -- service_code
    'API',  -- business_type_cd
    '保存收据模板',  -- name
    1,  -- seq
    '',  -- messageQueueName
    'CMD',  -- is_instance
    'http://fee-service',  -- url
    'POST',  -- method
    60,  -- timeout
    3,  -- retry_count
    '8000418002',  -- provide_app_id
    NOW(),  -- create_time
    '0'  -- status_cd
);

-- 更新收据模板服务
INSERT INTO `c_service` (
    `id`,
    `service_id`, 
    `service_code`, 
    `business_type_cd`, 
    `name`, 
    `seq`, 
    `messageQueueName`, 
    `is_instance`, 
    `url`, 
    `method`, 
    `timeout`, 
    `retry_count`, 
    `provide_app_id`, 
    `create_time`,
    `status_cd`
) VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM c_service),
    '982026061579900008',
    'feePrintPageTemplate.updateFeePrintPageTemplate',
    'API',
    '更新收据模板',
    1,
    '',
    'CMD',
    'http://fee-service',
    'POST',
    60,
    3,
    '8000418002',
    NOW(),
    '0'
);

-- 删除收据模板服务
INSERT INTO `c_service` (
    `id`,
    `service_id`, 
    `service_code`, 
    `business_type_cd`, 
    `name`, 
    `seq`, 
    `messageQueueName`, 
    `is_instance`, 
    `url`, 
    `method`, 
    `timeout`, 
    `retry_count`, 
    `provide_app_id`, 
    `create_time`,
    `status_cd`
) VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM c_service),
    '982026061542850009',
    'feePrintPageTemplate.deleteFeePrintPageTemplate',
    'API',
    '删除收据模板',
    1,
    '',
    'CMD',
    'http://fee-service',
    'POST',
    60,
    3,
    '8000418002',
    NOW(),
    '0'
);

-- 第四步：添加路由配置（使用大于max_route_id的ID）
SELECT '=== 添加路由配置 ===' as step;

-- 保存接口路由
INSERT INTO `c_route` (
    `id`,
    `app_id`, 
    `service_id`, 
    `order_type_cd`, 
    `invoke_limit_times`, 
    `invoke_model`, 
    `create_time`,
    `status_cd`
) VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM c_route),  -- 自动获取下一个ID
    '8000418004',  -- app_id（小区管理系统web端）
    '982026061566580007',  -- service_id（对应上面的保存服务）
    'D',  -- order_type_cd（D=增删改操作）
    1000,  -- invoke_limit_times
    'S',  -- invoke_model（S=同步）
    NOW(),  -- create_time
    '0'  -- status_cd
);

-- 更新接口路由
INSERT INTO `c_route` (
    `id`,
    `app_id`, 
    `service_id`, 
    `order_type_cd`, 
    `invoke_limit_times`, 
    `invoke_model`, 
    `create_time`,
    `status_cd`
) VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM c_route),
    '8000418004',
    '982026061579900008',
    'D',
    1000,
    'S',
    NOW(),
    '0'
);

-- 删除接口路由
INSERT INTO `c_route` (
    `id`,
    `app_id`, 
    `service_id`, 
    `order_type_cd`, 
    `invoke_limit_times`, 
    `invoke_model`, 
    `create_time`,
    `status_cd`
) VALUES (
    (SELECT IFNULL(MAX(id), 0) + 1 FROM c_route),
    '8000418004',
    '982026061542850009',
    'D',
    1000,
    'S',
    NOW(),
    '0'
);

-- 第五步：验证配置
SELECT '=== 验证配置 ===' as step;

-- 验证服务配置
SELECT id, service_id, service_code, name, method, status_cd
FROM c_service
WHERE service_code LIKE 'feePrintPageTemplate.%'
ORDER BY id;

-- 预期结果：应该看到4条记录（list + save + update + delete）

-- 验证路由配置
SELECT r.id, r.app_id, r.service_id, s.service_code, s.name, r.order_type_cd, r.status_cd
FROM c_route r
LEFT JOIN c_service s ON r.service_id = s.service_id
WHERE r.app_id = '8000418004' 
  AND s.service_code LIKE 'feePrintPageTemplate.%'
ORDER BY r.id;

-- 预期结果：应该看到4条路由记录

-- =====================================================
-- 重要提示：执行完SQL后，必须重启后端服务！
-- =====================================================

/*
重启命令：
1. 停止服务
   kill -9 $(ps aux | grep service-api | grep -v grep | awk '{print $2}')

2. 启动服务
   cd /path/to/springboot/target
   nohup java -jar service-api.jar > logs/service-api.log 2>&1 &

3. 查看日志
   tail -f logs/service-api.log

4. 等待30秒后测试前端功能
*/
