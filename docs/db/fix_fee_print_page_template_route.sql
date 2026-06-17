-- =====================================================
-- 修复 feePrintPageTemplate 路由权限问题
-- =====================================================
-- 问题：AppId 没有权限访问 serviceCode = feePrintPageTemplate.saveFeePrintPageTemplate
-- 原因：Redis缓存未更新或路由配置缺失
-- 解决：重新添加路由配置并刷新缓存
-- =====================================================

-- 第一步：检查现有路由配置
SELECT '=== 检查现有路由配置 ===' as step;

SELECT r.id, r.app_id, r.service_id, s.service_code, s.name, r.order_type_cd, r.status_cd
FROM c_route r
LEFT JOIN c_service s ON r.service_id = s.service_id
WHERE r.app_id = '8000418004' 
  AND s.service_code LIKE 'feePrintPageTemplate.%'
ORDER BY r.id;

-- 预期结果：应该看到4条记录
-- 1. listFeePrintPageTemplate (Q) - ID: 1589
-- 2. saveFeePrintPageTemplate (D) - ID: 3032
-- 3. updateFeePrintPageTemplate (D) - ID: 3033
-- 4. deleteFeePrintPageTemplate (D) - ID: 3034

-- 第二步：如果缺少路由，执行以下INSERT语句
-- （如果上面查询已有4条记录，跳过此步骤）

-- 保存接口路由
INSERT INTO `c_route` (`app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `status_cd`) 
VALUES ('8000418004', '982026061566580007', 'D', '1000', 'S', '0')
ON DUPLICATE KEY UPDATE status_cd = '0';

-- 更新接口路由
INSERT INTO `c_route` (`app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `status_cd`) 
VALUES ('8000418004', '982026061579900008', 'D', '1000', 'S', '0')
ON DUPLICATE KEY UPDATE status_cd = '0';

-- 删除接口路由
INSERT INTO `c_route` (`app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `status_cd`) 
VALUES ('8000418004', '982026061542850009', 'D', '1000', 'S', '0')
ON DUPLICATE KEY UPDATE status_cd = '0';

-- 第三步：验证路由配置
SELECT '=== 验证路由配置 ===' as step;

SELECT r.id, r.app_id, r.service_id, s.service_code, s.name, r.order_type_cd, r.status_cd
FROM c_route r
LEFT JOIN c_service s ON r.service_id = s.service_id
WHERE r.app_id = '8000418004' 
  AND s.service_code LIKE 'feePrintPageTemplate.%'
ORDER BY r.id;

-- 预期结果：必须看到4条记录，且status_cd都为'0'

-- 第四步：检查服务配置
SELECT '=== 检查服务配置 ===' as step;

SELECT id, service_id, service_code, name, method, status_cd
FROM c_service
WHERE service_code LIKE 'feePrintPageTemplate.%'
ORDER BY id;

-- 预期结果：4条记录，status_cd都为'0'

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
