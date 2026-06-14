# Service-API 网关模块

## 模块概述

`service-api` 是 HC 小区管理系统的**网关模块**，作为整个微服务架构的统一入口，承担着请求路由、认证授权、API文档管理、请求过滤等核心职责。该模块基于 Spring Cloud 技术栈构建，采用命令驱动架构设计。

## 技术架构

### 核心框架
- **Spring Boot 2.x** - 应用基础框架
- **Spring Cloud** - 微服务治理
- **Spring Cloud OpenFeign** - 服务间调用
- **Swagger 2** - API文档生成
- **WebSocket** - 实时通信支持

### 架构特点
- **统一入口**: 所有外部请求的统一接入点
- **服务发现**: 集成 Eureka 服务注册与发现
- **负载均衡**: 内置 Ribbon 负载均衡器
- **API文档**: 自动生成 Swagger API 文档
- **认证授权**: JWT Token 认证机制
- **请求过滤**: 多层过滤器链处理

## 模块结构

```
service-api/
├── src/main/java/com/java110/api/
│   ├── aop/                    # 切面编程
│   ├── components/             # 组件处理
│   ├── configuration/          # 配置类
│   │   ├── ServiceConfiguration.java    # 服务配置
│   │   └── WebSocketConfig.java         # WebSocket配置
│   ├── controller/             # 控制器层
│   │   ├── app/                # 移动端控制器
│   │   │   ├── file/           # 文件处理
│   │   │   ├── payment/        # 支付处理
│   │   │   ├── ActivitiController.java
│   │   │   ├── AppController.java
│   │   │   ├── AppTokenController.java
│   │   │   ├── EnvController.java
│   │   │   ├── OwnerController.java
│   │   │   ├── PaymentController.java
│   │   │   ├── PropertyController.java
│   │   │   └── WechatGatewayController.java
│   │   ├── component/          # 组件控制器
│   │   ├── goods/              # 商品控制器
│   │   ├── FlowController.java
│   │   ├── HomeController.java
│   │   └── SystemErrorController.java
│   ├── filter/                 # 过滤器
│   │   └── JwtFilter.java      # JWT认证过滤器
│   ├── importData/             # 数据导入
│   ├── listener/               # 事件监听器
│   ├── properties/             # 属性配置
│   ├── rest/                   # REST接口
│   │   ├── CreatePluginsConfig.java
│   │   └── RestApi.java
│   ├── smo/                    # 服务管理对象
│   │   ├── activiti/           # 工作流服务
│   │   ├── addOwner/           # 业主添加服务
│   │   ├── api/                # API服务
│   │   ├── assetExport/        # 资产导出
│   │   ├── assetImport/        # 资产导入
│   │   ├── basePrivilege/      # 基础权限
│   │   ├── cache/              # 缓存服务
│   │   ├── common/             # 通用服务
│   │   ├── configMenu/         # 菜单配置
│   │   ├── devServiceProvide/  # 开发服务提供
│   │   ├── dict/               # 字典服务
│   │   ├── fee/                # 费用服务
│   │   ├── file/               # 文件服务
│   │   ├── impl/               # 服务实现
│   │   ├── login/              # 登录服务
│   │   ├── mapping/            # 映射服务
│   │   ├── menu/               # 菜单服务
│   │   ├── meterWaterImport/   # 水电表导入
│   │   ├── msg/                # 消息服务
│   │   ├── org/                # 组织服务
│   │   ├── payment/            # 支付服务
│   │   ├── privilege/          # 权限服务
│   │   ├── staff/              # 员工服务
│   │   ├── sys/                # 系统服务
│   │   ├── undo/               # 撤销服务
│   │   ├── wechatGateway/      # 微信网关
│   │   └── 核心接口类文件
│   ├── websocket/              # WebSocket处理
│   ├── ApiApplicationStart.java # 应用启动类
│   └── package-info.java
├── src/main/resources/
│   ├── static/                 # 静态资源
│   ├── application-dev.yml     # 开发环境配置
│   ├── application-dynamic.yml # 动态配置
│   ├── application-zihao.yml   # 测试环境配置
│   ├── application.yml         # 主配置文件
│   ├── banner.txt             # 启动横幅
│   ├── java110.properties     # 系统属性
│   └── wechatAuth.properties  # 微信认证配置
├── pom.xml                    # Maven配置
├── pom-boot.xml              # Spring Boot配置
└── pom-cloud.xml             # Spring Cloud配置
```

## 核心功能

### 1. 网关路由
- 统一请求入口，支持 RESTful API
- 动态服务路由与负载均衡
- 请求转发与响应处理

### 2. 认证授权
- JWT Token 认证机制
- 权限验证与访问控制
- 白名单配置（登录、注册等接口免认证）

### 3. API文档
- 自动生成 Swagger API 文档
- 在线接口测试
- API版本管理

### 4. 请求过滤
- 请求参数验证
- 安全防护（XSS、SQL注入等）
- 请求日志记录

### 5. 文件处理
- 文件上传下载（支持50MB大文件）
- 图片、视频等多媒体文件处理
- 静态资源服务

### 6. 支付网关
- 微信支付集成
- 银联支付支持
- 支付回调处理

### 7. 实时通信
- WebSocket 实时消息推送
- 在线状态管理
- 即时通讯支持

## 启动配置

### 应用启动类
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableSwagger2
public class ApiApplicationStart {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplicationStart.class, args);
    }
}
```

### 主要配置项
- **服务发现**: 集成 Eureka 服务注册中心
- **Feign客户端**: 支持服务间调用
- **Swagger文档**: 自动生成API文档
- **文件上传**: 最大支持50MB文件
- **超时配置**: 连接超时10秒，读取超时10秒

## 依赖关系

### 核心依赖
```xml
<dependencies>
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-service</artifactId>
    </dependency>
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-interface</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
</dependencies>
```

### 服务接口依赖
模块通过 Feign 客户端依赖以下服务接口：
- `com.java110.intf.acct` - 账户服务
- `com.java110.intf.code` - 编码服务
- `com.java110.intf.common` - 通用服务
- `com.java110.intf.community` - 社区服务
- `com.java110.intf.demo` - 演示服务
- `com.java110.intf.dev` - 开发服务
- `com.java110.intf.fee` - 费用服务
- `com.java110.intf.goods` - 商品服务
- `com.java110.intf.job` - 任务服务
- `com.java110.intf.oa` - OA服务
- `com.java110.intf.order` - 订单服务
- `com.java110.intf.report` - 报表服务
- `com.java110.intf.store` - 店铺服务
- `com.java110.intf.user` - 用户服务

## 部署说明

### 环境要求
- JDK 1.8+
- Maven 3.6+
- Spring Boot 2.x
- Spring Cloud 相关组件

### 启动命令
```bash
# Spring Boot 模式
mvn spring-boot:run -f pom-boot.xml

# Spring Cloud 模式  
mvn spring-boot:run -f pom-cloud.xml
```

### 配置说明
- `application.yml` - 主配置文件
- `application-dev.yml` - 开发环境配置
- `java110.properties` - 系统属性配置
- `wechatAuth.properties` - 微信认证配置

## API文档访问

启动应用后，可通过以下地址访问API文档：
```
http://localhost:8008/swagger-ui.html
```

## 安全配置

### JWT过滤器配置
模块配置了JWT认证过滤器，支持以下免认证接口：
- 登录注册相关接口
- 支付回调接口
- 文件下载接口
- 微信相关接口
- 公共信息查询接口

### 文件上传限制
- 单文件最大: 50MB
- 请求最大: 50MB
- 支持格式: 图片、视频、文档等

## 监控与日志

- 集成 Spring Boot Actuator 监控端点
- SLF4J + Logback 日志框架
- 请求链路追踪
- 性能监控指标

## 网关调度机制详细说明

为了更好地理解网关的工作机制，我们提供了详细的阶段说明文档：

### 网关处理流程文档
- **[网关调度介绍.md](网关调度介绍.md)** - 完整的网关调度机制概述
- **[请求接收阶段说明.md](请求接收阶段说明.md)** - 请求接收阶段的详细说明
- **[权限验证阶段说明.md](权限验证阶段说明.md)** - 权限验证阶段的详细说明  
- **[服务分发阶段说明.md](服务分发阶段说明.md)** - 服务分发阶段的详细说明
- **[业务处理阶段说明.md](业务处理阶段说明.md)** - 业务处理阶段的详细说明
- **[响应返回阶段说明.md](响应返回阶段说明.md)** - 响应返回阶段的详细说明

### 处理流程概览
```
客户端请求
    ↓
请求接收阶段 (AppController)
    ↓
权限验证阶段 (IPrivilegeSMO)
    ↓
服务分发阶段 (IApiSMO → ApiSMOImpl)
    ↓
业务处理阶段 (IApiServiceSMO → ApiServiceSMOImpl)
    ↓
响应返回阶段 (统一响应封装)
```

## 注意事项

1. **服务依赖**: 启动前确保依赖的微服务已注册到服务发现中心
2. **配置管理**: 根据部署环境选择对应的配置文件
3. **安全配置**: 定期更新JWT密钥和微信配置信息
4. **性能优化**: 根据实际业务量调整连接池和超时配置
5. **文档参考**: 详细的技术实现请参考各阶段说明文档

## 版本信息

- **当前版本**: 1.0-SNAPSHOT
- **Spring Boot**: 2.x
- **Spring Cloud**: 对应版本
- **JDK**: 1.8+

---