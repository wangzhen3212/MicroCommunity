# 自定义收据模板 - 后端服务配置执行说明

## 📋 概述

本文档说明如何为自定义收据模板功能添加后端服务配置，解决"鉴权失败：AppId 没有权限访问"的问题。

---

## ⚠️ 问题描述

**错误信息**：
```json
{
  "code": 404,
  "msg": "鉴权失败：AppId 没有权限访问 serviceCode = feePrintPageTemplate.saveFeePrintPageTemplate"
}
```

**问题原因**：
- 数据库中只配置了查询接口 `feePrintPageTemplate.listFeePrintPageTemplate`
- 缺少保存、更新、删除接口的服务配置和路由配置
- 前端调用保存接口时，后端鉴权失败

---

## 🎯 解决方案

### 方案概览

1. ✅ 在 `c_service` 表中添加3个新服务（保存、更新、删除）
2. ✅ 在 `c_route` 表中为 appId `8000418004` 添加路由权限
3. ✅ 重启后端服务加载新配置到Redis缓存

---

## 📝 执行步骤

### 步骤1：备份数据库（重要！）

```bash
# 备份数据库
mysqldump -u root -p your_database_name > backup_$(date +%Y%m%d_%H%M%S).sql

# 或者只备份相关表
mysqldump -u root -p your_database_name c_service c_route > backup_service_route.sql
```

### 步骤2：执行SQL脚本

**方法一：使用MySQL命令行**

```bash
# 进入MySQL
mysql -u root -p

# 选择数据库
use your_database_name;

# 执行SQL脚本
source /data/ftp_data/编程源码/MicroCommunity_Copy/docs/db/fee_print_page_template_service_config.sql;
```

**方法二：使用图形化工具**

1. 打开 Navicat / DBeaver / phpMyAdmin 等工具
2. 连接到数据库
3. 打开 SQL 文件：`docs/db/fee_print_page_template_service_config.sql`
4. 执行全部SQL语句

**方法三：直接复制粘贴**

1. 打开 SQL 文件
2. 复制所有 INSERT 语句
3. 在数据库管理工具中执行

### 步骤3：验证配置

执行SQL脚本中的验证查询：

```sql
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
```

```sql
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

-- 预期结果：应该看到4条路由记录
```

### 步骤4：重启后端服务

**重要**：执行完SQL后必须重启后端服务，让系统从数据库加载新的路由配置到Redis缓存。

```bash
# 方法一：如果是jar包启动
cd /path/to/service-api
kill -9 $(ps aux | grep service-api | grep -v grep | awk '{print $2}')
nohup java -jar service-api.jar &

# 方法二：如果是IDE启动
# 在IDE中停止服务，然后重新启动

# 方法三：如果是Docker启动
docker restart service-api-container
```

### 步骤5：测试功能

1. **打开前端设计器**
   - 访问：`http://your-domain/#/views/system/templateColumnDesigner`

2. **配置模板**
   - 设置纸张尺寸
   - 添加表格列
   - 调整印章位置
   - 等等...

3. **点击"保存模板"按钮**

4. **检查浏览器控制台**
   - 按 F12 打开开发者工具
   - 查看 Network 标签
   - 找到 `feePrintPageTemplate.saveFeePrintPageTemplate` 请求
   - 检查响应状态码应该是 200
   - 响应内容应该是：`{"code": 0, "msg": "success"}`

5. **如果成功**
   - 会显示"保存成功"提示
   - 可以在模板列表中看到新保存的模板

6. **如果失败**
   - 查看错误信息
   - 参考下面的"常见问题"部分

---

## 🔍 常见问题

### Q1: 仍然报"鉴权失败"怎么办？

**可能原因**：Redis缓存未更新

**解决方法**：

```bash
# 1. 连接Redis
redis-cli

# 2. 检查是否有路由缓存
GET "8000418004_SUFFIX_APP_ROUTE"

# 3. 如果返回nil，说明缓存未加载
# 需要重启后端服务

# 4. 如果返回数据，检查是否包含新的service_id
# 可以使用以下命令查看所有key
KEYS "*8000418004*"
```

### Q2: 报"服务不存在"怎么办？

**可能原因**：service_code拼写错误

**检查方法**：

```sql
-- 检查service_code是否正确
SELECT service_code FROM c_service WHERE service_code LIKE '%feePrintPageTemplate%';

-- 应该看到：
-- feePrintPageTemplate.listFeePrintPageTemplate
-- feePrintPageTemplate.saveFeePrintPageTemplate
-- feePrintPageTemplate.updateFeePrintPageTemplate
-- feePrintPageTemplate.deleteFeePrintPageTemplate
```

**注意**：service_code必须与前端调用的完全一致（包括大小写）

### Q3: 如何确认appId是否正确？

**检查前端配置**：

```javascript
// 文件：src/conf/config.js
export default {
    appId: '8000418004',  // ← 这个值
}
```

**检查数据库**：

```sql
SELECT app_id, name FROM c_app WHERE app_id = '8000418004';

-- 应该返回：
-- app_id: 8000418004
-- name: 小区管理系统web端
```

### Q4: ID冲突怎么办？

如果执行SQL时报错"Duplicate entry for key 'PRIMARY'"，说明id或service_id冲突。

**解决方法**：

```sql
-- 1. 查询当前最大的id
SELECT MAX(id) FROM c_service;

-- 2. 修改SQL脚本中的id值，确保唯一
-- 例如：将 1041, 1042, 1043 改为更大的值

-- 3. 查询当前最大的service_id
SELECT MAX(service_id) FROM c_service WHERE service_id LIKE '98202109167775%';

-- 4. 修改service_id，保持格式一致但数值递增
```

### Q5: 如何为其他appId也添加权限？

如果需要让其他应用（如业主小程序）也能调用这些接口：

```sql
-- 取消SQL脚本末尾的注释，并修改appId
INSERT INTO `c_route` (`app_id`, `service_id`, `order_type_cd`, `invoke_limit_times`, `invoke_model`, `status_cd`) VALUES
('992019111758490006', '982021091677750028', 'D', 1000, 'S', '0'),  -- 业主小程序
('992019111758490006', '982021091677750029', 'D', 1000, 'S', '0'),
('992019111758490006', '982021091677750030', 'D', 1000, 'S', '0');
```

常见的appId列表：
- `8000418004` - 小区管理系统web端（前端设计器使用）
- `992019111758490006` - 业主小程序
- `992020022270580001` - HC掌上物业（物业APP）
- `992020061452450002` - 业主公众号

---

## 📊 配置说明

### c_service 表字段说明

| 字段 | 说明 | 示例值 |
|------|------|--------|
| id | 主键ID（唯一） | 1041 |
| service_id | 服务ID（唯一） | 982021091677750028 |
| service_code | 服务编码（前端调用时使用） | feePrintPageTemplate.saveFeePrintPageTemplate |
| business_type_cd | 业务类型 | API |
| name | 服务名称 | 保存收据模板 |
| seq | 调用顺序 | 1 |
| is_instance | 是否需要实例化 | CMD |
| url | 目标服务地址 | http://fee-service |
| method | HTTP方法 | POST |
| timeout | 超时时间（秒） | 60 |
| retry_count | 重试次数 | 3 |
| provide_app_id | 提供服务的应用ID | 8000418002 |
| status_cd | 状态（0在用，1失效） | 0 |

### c_route 表字段说明

| 字段 | 说明 | 示例值 |
|------|------|--------|
| app_id | 应用ID | 8000418004 |
| service_id | 服务ID（关联c_service表） | 982021091677750028 |
| order_type_cd | 订单类型（Q=查询，D=增删改） | D |
| invoke_limit_times | 每分钟调用次数限制 | 1000 |
| invoke_model | 调用模式（S=同步，A=异步） | S |
| status_cd | 状态（0在用，1失效） | 0 |

---

## 🎉 完成标志

当您在设计器中成功保存模板后，会看到：

1. ✅ 浏览器控制台显示：`{"code": 0, "msg": "success"}`
2. ✅ 页面弹出"保存成功"提示
3. ✅ 可以在模板列表中看到新保存的模板
4. ✅ 点击模板可以预览生成的HTML

---

## 📞 技术支持

如果遇到问题，请检查：

1. **数据库日志**：查看是否有SQL执行错误
2. **后端日志**：查看service-api服务的日志文件
3. **Redis日志**：查看缓存是否正常加载
4. **浏览器控制台**：查看前端请求和响应

---

**文档版本**: v1.0  
**创建时间**: 2026-06-14  
**适用项目**: MicroCommunity_Copy
