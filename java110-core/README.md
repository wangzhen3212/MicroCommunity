# Java110-Core 核心框架模块

## 项目概述

Java110-Core 是 MicroCommunity 微服务架构的核心框架模块，为整个系统提供基础架构支持和通用能力封装。该模块采用模块化设计，通过14个核心包实现了注解驱动、AOP切面、缓存管理、事件机制、代理模式等企业级功能。

## 模块说明

### 核心包结构

| 包名 | 功能描述 | 主要组件 |
|------|----------|----------|
| `annotation` | 自定义注解定义 | Java110Cmd、Java110Listener、Java110Transactional等 |
| `aop` | 切面编程实现 | 事务切面、同步切面、Feign拦截器等 |
| `base` | 基础类封装 | 控制器、服务、DAO、SMO基类 |
| `cache` | 缓存管理 | Redis配置、集群支持、缓存写入器 |
| `client` | 客户端封装 | 外部服务调用客户端 |
| `component` | 组件管理 | Spring组件扫描和注册 |
| `context` | 上下文管理 | 数据流上下文、事务日志、环境配置 |
| `event` | 事件机制 | 应用事件、命令事件、监听器 |
| `factory` | 工厂模式 | 对象创建和依赖管理 |
| `language` | 国际化 | 多语言支持 |
| `log` | 日志管理 | 日志记录和追踪 |
| `proxy` | 代理模式 | 服务代理、动态代理工厂 |
| `smo` | 服务管理对象 | 费用计算、数据校验、照片处理等 |
| `trace` | 链路追踪 | 分布式链路追踪支持 |

## 技术架构

### 核心依赖

- **持久层**: MyBatis Spring Boot Starter
- **缓存**: Spring Boot Starter Cache + Redis Session
- **微服务**: Spring Cloud (OpenFeign + Ribbon + Hystrix)
- **云服务**: 阿里云SDK、腾讯云SDK
- **工具**: SSH客户端 (JSch)、OkHttp、Okio

### 架构特点

1. **注解驱动开发**: 通过自定义注解简化业务开发
2. **AOP切面编程**: 统一处理事务、同步、拦截等横切关注点
3. **事件驱动架构**: 支持应用事件、命令事件等多种事件类型
4. **上下文管理**: 完整的数据流上下文管理机制
5. **代理模式**: 动态代理实现服务调用和扩展

## 核心功能

### 1. 注解系统 (`annotation`)

提供丰富的自定义注解支持：

- `@Java110Cmd`: 命令注解，用于定义业务命令
- `@Java110Listener`: 监听器注解，用于事件监听
- `@Java110Transactional`: 事务注解，支持分布式事务
- `@Java110Synchronized`: 同步注解，保证方法同步执行

### 2. AOP切面 (`aop`)

实现横切关注点的统一处理：

- **事务切面**: 统一事务管理和回滚
- **同步切面**: 方法级同步控制
- **Feign拦截器**: 微服务调用拦截和处理
- **事务拦截器**: 事务边界控制

### 3. 缓存管理 (`cache`)

提供多级缓存支持：

- **Redis配置**: 支持集群和单机模式
- **缓存写入器**: 自定义Redis缓存写入实现
- **缓存模板**: 统一的缓存操作接口

### 4. 上下文管理 (`context`)

完整的数据流上下文管理：

- **数据流上下文**: 抽象数据流处理上下文
- **命令上下文**: 命令执行上下文管理
- **事务日志**: 分布式事务日志记录
- **页面数据**: 统一页面数据封装

### 5. 事件机制 (`event`)

支持多种事件类型：

- **应用事件**: 应用级别事件处理
- **命令事件**: 命令执行事件
- **初始化事件**: 系统初始化事件
- **服务事件**: 服务调用事件

### 6. 服务管理对象 (`smo`)

核心业务服务管理：

- **费用计算**: 统一费用计算服务
- **数据校验**: 数据完整性校验
- **照片处理**: 图片上传和处理
- **事务日志**: 事务日志记录服务

### 7. 代理模式 (`proxy`)

动态代理实现：

- **服务代理**: 服务调用代理
- **代理工厂**: 动态代理对象创建
- **调用处理器**: 方法调用拦截处理

## 使用指南

### 1. 命令开发示例

```java
@Java110Cmd(serviceCode = "query.user.info")
public class QueryUserInfoCmd extends BaseCmd {
    
    @Override
    public void validate(CmdDataFlow context, ICmdDataFlowContext dataFlowContext) {
        // 参数校验逻辑
    }
    
    @Override
    public void doCmd(CmdDataFlow context, ICmdDataFlowContext dataFlowContext) {
        // 业务逻辑处理
    }
}
```

### 2. 事件监听器示例

```java
@Java110Listener
public class UserRegisterListener implements ApplicationListener<UserRegisterEvent> {
    
    @Override
    public void onApplicationEvent(UserRegisterEvent event) {
        // 处理用户注册事件
    }
}
```

### 3. 缓存使用示例

```java
@Service
public class UserService {
    
    @Cacheable(value = "userCache", key = "#userId")
    public User getUserById(String userId) {
        // 查询用户信息
        return userDao.getUserById(userId);
    }
}
```

### 4. 服务调用示例

```java
@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @GetMapping("/user/{userId}")
    User getUserById(@PathVariable("userId") String userId);
}
```

## 配置说明

### Redis配置

```properties
# Redis配置
spring.redis.host=127.0.0.1
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0
```

### Feign配置

```properties
# Feign配置
feign.okhttp.enabled=true
feign.hystrix.enabled=true
```

## 开发规范

1. **命令开发**: 所有业务命令必须继承BaseCmd并使用@Java110Cmd注解
2. **事件处理**: 事件监听器需实现ApplicationListener接口并使用@Java110Listener注解
3. **事务管理**: 需要事务支持的方法使用@Java110Transactional注解
4. **缓存使用**: 合理使用缓存注解，注意缓存一致性
5. **异常处理**: 统一异常处理机制，避免直接抛出运行时异常

## 注意事项

- 模块依赖java110-utils工具模块
- 支持Spring Boot和Spring Cloud两种部署模式
- 缓存配置支持Redis集群和单机模式
- 事件机制支持同步和异步处理
- 代理模式支持服务调用拦截和扩展