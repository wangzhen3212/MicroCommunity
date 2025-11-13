# java110-doc 模块技术文档

## 项目概述

`java110-doc` 是 HC 微服务社区项目的 API 文档生成模块，基于自定义注解实现智能化的 API 文档管理和展示功能。该模块为整个微服务架构提供统一的接口文档管理解决方案。

## 模块说明

- **模块类型**: API 文档生成与管理
- **技术栈**: Spring Boot + 自定义注解 + 前端静态页面
- **核心功能**: 通过注解自动生成 API 文档，提供可视化文档界面

## 技术架构

### 核心组件

1. **注解系统** (11个核心注解)
   - `@Java110ApiDoc`: 项目级文档配置
   - `@Java110CmdDoc`: 接口命令文档
   - `@Java110ParamsDoc`: 请求参数文档
   - `@Java110ResponseDoc`: 响应参数文档
   - `@Java110ExampleDoc`: 请求/响应示例

2. **控制器层**
   - `DocController`: 文档数据接口控制器

3. **数据实体层**
   - `ApiDocDto`: API 文档数据传输对象
   - `CmdDocDto`: 命令文档数据传输对象
   - `RequestMappingsDocDto`: 请求映射文档数据传输对象

4. **注册器层**
   - 注解发现与注册机制
   - 文档发布管理

5. **前端界面**
   - `doc-ui.html`: 文档展示主页面
   - 静态资源 (CSS/JS/Mock数据)

## 目录结构

```
java110-doc/
├── src/main/java/com/java110/doc/
│   ├── annotation/          # 注解定义 (11个核心注解)
│   ├── controller/          # 控制器层
│   ├── entity/             # 数据实体层
│   └── registrar/          # 注解注册器
├── src/main/resources/static/
│   ├── css/               # 样式文件
│   ├── js/                # JavaScript文件
│   ├── mock/              # 模拟数据
│   └── doc-ui.html        # 文档界面
└── pom.xml               # Maven配置
```

## 核心功能详解

### 1. 项目级文档配置

```java
@Java110ApiDoc(
    title = "HC小区管理系统api接口文档",
    description = "HC小区管理系统api接口文档",
    company="Java110工作室",
    version = "v1.4"
)
```

### 2. 微服务路由映射

```java
@Java110RequestMappingsDoc(
    mappingsDocs = {
        @Java110RequestMappingDoc(name="用户中心", resource="user", url="http://service-user/userDoc", seq=1),
        @Java110RequestMappingDoc(name="账户中心", resource="acct", url="http://service-acct/acctDoc", seq=2)
    }
)
```

### 3. 接口命令文档

```java
@Java110CmdDoc(
    title = "用户登录",
    description = "登录功能主要用于员工或管理员登录使用",
    httpMethod = "post",
    url = "/app/login.pcUserLogin",
    resource = "user",
    author = "吴学文"
)
```

### 4. 参数文档系统

- **请求参数**: `@Java110ParamsDoc` + `@Java110ParamDoc`
- **响应参数**: `@Java110ResponseDoc` + `@Java110ParamDoc`
- **示例数据**: `@Java110ExampleDoc`

## 依赖配置

```xml
<dependencies>
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
    </dependency>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 使用指南

### 1. 文档访问

访问地址: `http://ip:port/doc-ui.html`  
示例: `http://127.0.0.1:8008/doc-ui.html`

### 2. 注解使用流程

1. **项目配置**: 在入口类使用 `@Java110ApiDoc`
2. **服务映射**: 配置 `@Java110RequestMappingsDoc`
3. **接口定义**: 为每个接口添加 `@Java110CmdDoc`
4. **参数说明**: 添加请求和响应参数注解
5. **示例配置**: 提供请求响应示例

### 3. Nginx 配置示例

```nginx
server {
    listen 80;
    server_name apidoc.homecommunity.cn;
    
    location / {
        proxy_pass http://dev.api.java110.com:8008/doc-ui.html;
    }
    location /js {
        proxy_pass http://dev.api.java110.com:8008;
    }
    location /css {
        proxy_pass http://dev.api.java110.com:8008;
    }
    location /doc {
        add_header 'Access-Control-Allow-Origin' '*';
        proxy_pass http://dev.api.java110.com:8088;
    }
}
```

## 开发规范

1. **注解完整性**: 每个接口必须包含完整的注解配置
2. **参数说明**: 所有参数必须明确类型、长度、说明
3. **示例数据**: 提供真实的请求响应示例
4. **版本管理**: 及时更新文档版本信息

## 优势特点

- **自动化**: 基于注解自动生成文档，减少手动维护
- **标准化**: 统一的文档格式和规范
- **可视化**: 友好的Web界面展示
- **集成化**: 与微服务架构深度集成
- **可扩展**: 支持多服务、多版本的文档管理

## 注意事项

1. 确保所有微服务正确配置文档注解
2. 文档服务需要能够访问各微服务的文档接口
3. 生产环境建议通过Nginx进行反向代理
4. 定期检查文档与实际接口的一致性

---

