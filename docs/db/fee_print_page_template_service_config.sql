-- =====================================================
-- 自定义收据模板 - 后端服务配置SQL脚本
-- =====================================================
-- 功能：为 feePrintPageTemplate 模块添加保存、更新、删除接口
-- 适用项目：MicroCommunity_Copy
-- 创建时间：2026-06-14
-- 说明：执行此脚本后需要重启后端服务或刷新Redis缓存
-- =====================================================

-- =====================================================
-- 第一步：在 c_service 表中添加3个新服务
-- =====================================================

-- 1. 保存收据模板服务
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
    2045,  -- id（需要唯一，请根据实际情况调整）
    '982021091677750028',  -- service_id（保持与现有格式一致）
    'feePrintPageTemplate.saveFeePrintPageTemplate',  -- service_code（与前端调用一致）
    'API',  -- business_type_cd
    '保存收据模板',  -- name
    1,  -- seq（顺序）
    '',  -- messageQueueName
    'CMD',  -- is_instance
    'http://fee-service',  -- url（指向fee-service微服务）
    'POST',  -- method（保存用POST）
    60,  -- timeout（超时时间60秒）
    3,  -- retry_count（重试次数3次）
    '8000418002',  -- provide_app_id（提供服务的应用ID）
    NOW(),  -- create_time（当前时间）
    '0'  -- status_cd（0表示在用）
);

-- 2. 更新收据模板服务
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
    2046,
    '982021091677750029',
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

-- 3. 删除收据模板服务
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
    2047,
    '982021091677750030',
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

-- =====================================================
-- 第二步：在 c_route 表中为 appId 8000418004 添加路由权限
-- =====================================================
-- 说明：order_type_cd: D=DELETE(增删改), Q=QUERY(查询)
--       invoke_model: S=同步, A=异步

-- 1. 保存接口的路由配置
INSERT INTO `c_route` (
    `app_id`, 
    `service_id`, 
    `order_type_cd`, 
    `invoke_limit_times`, 
    `invoke_model`, 
    `status_cd`
) VALUES (
    '8000418004',  -- app_id（小区管理系统web端）
    '982021091677750028',  -- service_id（对应上面的保存服务）
    'D',  -- order_type_cd（D表示数据操作：增删改）
    1000,  -- invoke_limit_times（每分钟最多调用1000次）
    'S',  -- invoke_model（S表示同步调用）
    '0'  -- status_cd（0表示在用）
);

-- 2. 更新接口的路由配置
INSERT INTO `c_route` (
    `app_id`, 
    `service_id`, 
    `order_type_cd`, 
    `invoke_limit_times`, 
    `invoke_model`, 
    `status_cd`
) VALUES (
    '8000418004',
    '982021091677750029',
    'D',
    1000,
    'S',
    '0'
);

-- 3. 删除接口的路由配置
INSERT INTO `c_route` (
    `app_id`, 
    `service_id`, 
    `order_type_cd`, 
    `invoke_limit_times`, 
    `invoke_model`, 
    `status_cd`
) VALUES (
    '8000418004',
    '982021091677750030',
    'D',
    1000,
    'S',
    '0'
);

-- =====================================================
-- 第三步：验证配置是否成功
-- =====================================================

-- 1. 验证服务是否添加成功
SELECT 
    id,
    service_id,
    service_code,
    name,
    method,
    status_cd
FROM c_service
WHERE service_code LIKE 'feePrintPageTemplate.%'
ORDER BY id;

-- 预期结果：应该看到4条记录
-- - feePrintPageTemplate.listFeePrintPageTemplate (查询)
-- - feePrintPageTemplate.saveFeePrintPageTemplate (保存)
-- - feePrintPageTemplate.updateFeePrintPageTemplate (更新)
-- - feePrintPageTemplate.deleteFeePrintPageTemplate (删除)

-- 2. 验证路由是否添加成功
SELECT 
    r.app_id,
    r.service_id,
    s.service_code,
    s.name,
    r.order_type_cd,
    r.invoke_model,
    r.status_cd
FROM c_route r
LEFT JOIN c_service s ON r.service_id = s.service_id
WHERE r.app_id = '8000418004' 
  AND s.service_code LIKE 'feePrintPageTemplate.%'
ORDER BY r.id;

-- 预期结果：应该看到4条路由记录，对应上面的4个服务

-- =====================================================
-- 第四步：后续操作
-- =====================================================

/*
重要提示：
1. 执行完上述SQL后，必须重启后端服务，或者调用刷新缓存接口
2. 后端启动时会自动从数据库加载所有app的路由配置到Redis
3. 如果不想重启服务，可以手动调用刷新缓存接口（如果有的话）

验证步骤：
1. 重启后端服务
2. 在前端设计器中点击"保存模板"按钮
3. 检查浏览器控制台是否有错误
4. 如果成功，应该返回 {"code": 0, "msg": "success"}

常见问题：
Q1: 如果仍然报"鉴权失败"怎么办？
A1: 检查Redis中是否有对应的路由缓存
    命令：redis-cli
    执行：GET "8000418004_SUFFIX_APP_ROUTE"
    如果返回nil，说明缓存未加载，需要重启服务

Q2: 如果报"服务不存在"怎么办？
A2: 检查c_service表中的service_code是否与前端调用完全一致
    注意大小写和拼写

Q3: 如何确认appId是否正确？
A3: 前端配置文件 src/conf/config.js 中的 appId 应该是 '8000418004'
*/

-- =====================================================
-- 可选：如果需要为其他appId也添加权限，取消下面的注释并修改appId
-- =====================================================

/*
-- 例如：为业主小程序 (992019111758490006) 也添加权限
INSERT INTO `c_route` (`app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `status_cd`) VALUES
('992019111758490006', '982021091677750028', 'D', 1000, 'S', '0'),
('992019111758490006', '982021091677750029', 'D', 1000, 'S', '0'),
('992019111758490006', '982021091677750030', 'D', 1000, 'S', '0');
*/

-- =====================================================
-- 脚本结束
-- =====================================================
