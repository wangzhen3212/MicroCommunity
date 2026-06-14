# java110-service 模块技术文档

## 模块概述

`java110-service` 是 MicroCommunity 微服务架构中的核心服务模块，提供统一的服务调度和数据查询功能。该模块基于 Spring Boot 框架构建，集成了服务发现、消息队列、数据库访问等核心组件，为整个系统提供标准化的服务调用和数据操作接口。

## 技术架构

### 架构设计
- **框架基础**: Spring Boot 2.x
- **服务注册**: Eureka Client
- **数据访问**: MyBatis + MySQL
- **消息中间件**: ActiveMQ
- **日志系统**: Log4j2
- **API文档**: Swagger2
- **配置管理**: Spring Configuration

### 核心特性
1. **统一服务调度**: 通过标准化的API接口处理各类服务请求
2. **动态SQL执行**: 支持模板化SQL查询和动态参数绑定
3. **脚本执行引擎**: 集成BeanShell支持Java脚本动态执行
4. **服务熔断机制**: 提供fallback机制确保服务可用性
5. **链路追踪**: 集成分布式链路追踪功能
6. **统一异常处理**: 标准化的异常处理和错误日志记录

## 包结构说明

### 1. api 包 - 接口层
提供RESTful API接口，统一处理外部服务请求。

#### 核心类：
- **BusinessApi**: 业务查询API，支持GET/POST方式的统一查询服务
  - `/businessApi/query`: 通用查询接口
  - `/businessApi/do`: 数据操作接口
  - `/businessApi/fallBack`: 服务降级接口
- **CmdApi**: 命令执行API，支持动态服务调用
  - `/cmd/{service}`: 微服务命令接口
  - `/cmd/{resource}/{action}`: 资源操作接口

### 2. configuration 包 - 配置层
Spring MVC相关配置和字符编码处理。

#### 核心类：
- **CustomMVCConfiguration**: 
  - 配置UTF-8字符编码
  - 禁用路径扩展名匹配
  - 注册自定义消息转换器
- **StringDecoderForHeaderConverter**: 
  - HTTP Header中文字符解码
  - 支持URL编码自动解码

### 3. context 包 - 上下文层
数据查询上下文管理和工厂类。

#### 核心类：
- **DataQuery**: 数据查询上下文对象
  - 封装查询参数、SQL模板、响应信息
  - 提供SQL执行和服务调用方法
  - 支持事务管理和连接池
- **DataQueryFactory**: 数据查询工厂类
  - 创建DataQuery实例
  - 获取服务SQL配置缓存

### 4. develop 包 - 开发层
动态脚本执行功能，支持业务逻辑的灵活扩展。

#### 核心接口：
- **IDevelop**: 开发脚本执行接口
  - `execute(DataQuery)`: 执行开发脚本

#### 实现类：
- **GetNoticesByJava**: 公告查询Java实现
- **GetUserByJava**: 用户查询Java实现

### 5. init 包 - 初始化层
服务启动初始化和配置管理。

#### 核心类：
- **ServiceStartInit**: 服务启动初始化
  - 系统配置初始化
  - 日志开关配置
  - 启动成功标识输出
- **ServiceInfoListener**: 服务信息监听器
  - 监听WebServer初始化事件
  - 获取服务端口和主机信息

### 6. smo 包 - 服务管理层
核心业务逻辑实现，提供统一的服务管理。

#### 核心接口：
- **IQueryServiceSMO**: 查询服务管理接口
  - `commonQueryService()`: 通用查询服务
  - `commonDoService()`: 通用操作服务
  - `fallBack()`: 服务降级处理
  - `execQuerySql()`: SQL查询执行
  - `execJava()`: Java脚本执行

- **ICmdServiceSMO**: 命令服务管理接口
  - `cmd()`: 命令服务调度

- **ISaveSystemErrorSMO**: 系统错误日志保存接口

#### 实现类：
- **QueryServiceSMOImpl**: 查询服务实现
  - 支持SQL、Java、存储过程三种执行模式
  - 动态SQL模板解析和参数绑定
  - 条件语句动态处理（<if>标签）
  - 结果集模板化输出

- **CmdServiceSMOImpl**: 命令服务实现
  - 服务命令调度
  - 事件发布机制
  - 链路追踪和性能监控

- **SaveSystemErrorSMOImpl**: 系统错误日志保存实现

## 依赖关系

### 核心依赖
```xml
<dependencies>
    <!-- 基础工具 -->
    <dependency>java110-utils</dependency>
    <dependency>java110-core</dependency>
    
    <!-- Spring Boot -->
    <dependency>spring-boot-starter-aop</dependency>
    <dependency>spring-cloud-starter-netflix-eureka-client</dependency>
    
    <!-- 数据访问 -->
    <dependency>java110-db</dependency>
    <dependency>mysql-connector-java</dependency>
    
    <!-- 消息队列 -->
    <dependency>activemq-pool</dependency>
    <dependency>activemq-core</dependency>
    <dependency>spring-jms</dependency>
    
    <!-- 日志系统 -->
    <dependency>log4j-core</dependency>
    <dependency>log4j-api</dependency>
    <dependency>log4j-slf4j-impl</dependency>
    
    <!-- API文档 -->
    <dependency>springfox-swagger2</dependency>
    <dependency>springfox-swagger-ui</dependency>
    
    <!-- 脚本引擎 -->
    <dependency>bsh-core</dependency>
    <dependency>javassist</dependency>
</dependencies>
```

## 配置说明

### 服务配置
- **服务端口**: 通过Spring Boot配置
- **数据库连接**: 集成java110-db模块配置
- **消息队列**: ActiveMQ连接池配置
- **服务注册**: Eureka客户端配置

### 日志配置
- **日志级别**: 通过环境变量`LOG_SWITCH`控制
- **日志输出**: Log4j2配置文件
- **错误日志**: 系统错误自动记录到数据库

## 功能特性详解

### 1. 统一查询服务
支持多种查询模式：
- **SQL模式**: 基于配置的SQL模板查询
- **Java模式**: 动态Java脚本执行
- **存储过程**: 数据库存储过程调用

### 2. 动态SQL处理
```java
// 支持条件语句动态处理
String sql = "SELECT * FROM table WHERE 1=1 " +
             "<if test=\"name != null && name != ''\">" +
             "AND name = #name#" +
             "</if>";
```

### 3. 脚本执行引擎
集成BeanShell脚本引擎，支持动态业务逻辑：
```java
// 动态执行Java代码
Interpreter interpreter = new Interpreter();
interpreter.eval(javaCode);
interpreter.set("dataQuery", dataQuery);
JSONObject result = interpreter.eval("execute(dataQuery)");
```

### 4. 服务熔断机制
提供完整的服务降级方案：
- 异常捕获和错误日志记录
- Fallback SQL执行回退
- 统一的异常响应格式

### 5. 性能监控
- 链路追踪集成
- 服务调用耗时统计
- 性能数据Kafka上报

## 使用指南

### 1. 服务启动
```java
// 系统初始化
ServiceStartInit.initSystemConfig(applicationContext);
ServiceStartInit.preInitSystemConfig();
```

### 2. 查询服务调用
```java
// 创建查询上下文
DataQuery dataQuery = DataQueryFactory.newInstance()
    .builder(businessInfo);

// 执行查询服务
queryServiceSMOImpl.commonQueryService(dataQuery);

// 获取响应
ResponseEntity<String> response = dataQuery.getResponseEntity();
```

### 3. 命令服务调用
```java
// 命令服务调度
ResponseEntity<String> response = cmdServiceSMOImpl.cmd(reqJson, headers);
```

### 4. 开发脚本使用
```java
// 实现IDevelop接口
public class CustomDevelop implements IDevelop {
    @Override
    public JSONObject execute(DataQuery dataQuery) {
        // 自定义业务逻辑
        return result;
    }
}
```

## 注意事项

1. **服务配置**: 确保数据库连接、消息队列等中间件配置正确
2. **SQL安全**: 动态SQL需注意SQL注入防护
3. **脚本安全**: Java脚本执行需进行权限控制
4. **性能监控**: 合理配置日志级别避免性能影响
5. **服务发现**: 确保Eureka服务注册正常

## 版本信息

- **当前版本**: 1.0-SNAPSHOT
- **Spring Boot**: 2.x
- **Java版本**: 1.8+
- **数据库**: MySQL 5.7+

## 维护说明

该模块作为微服务架构的核心组件，需要确保：
- 服务调度的稳定性和性能
- 数据查询的准确性和安全性
- 异常处理的完整性和可追溯性
- 与其他模块的兼容性和版本一致性