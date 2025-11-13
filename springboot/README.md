# SpringBoot 主启动模块

## 模块概述

SpringBoot模块是MicroCommunity社区管理系统的Spring Boot版本主启动模块，负责整合所有业务服务模块并提供统一的启动入口。该模块采用Spring Boot框架，集成了社区管理、用户管理、财务管理、订单管理、报表统计等12个核心业务模块，提供完整的社区管理解决方案。

## 技术架构

### 核心框架
- **Spring Boot 2.x**: 主框架，提供快速应用开发能力
- **Spring MVC**: Web MVC框架
- **Spring Cloud**: 微服务支持（可选）
- **MyBatis**: 数据持久层框架
- **Druid**: 数据库连接池

### 数据库支持
- **MySQL**: 主数据库
- **Redis**: 缓存数据库

### 其他技术组件
- **WebSocket**: 实时通信支持
- **Thymeleaf**: 模板引擎
- **Quartz**: 定时任务调度（已完全注释，需手动启用）
- **Activiti**: 工作流引擎（已排除自动配置）

## 功能说明

### 集成业务模块
本模块集成了以下12个核心业务模块：
- **service-acct**: 账户管理模块
- **service-common**: 通用服务模块
- **service-community**: 社区管理模块
- **service-dev**: 开发工具模块
- **service-fee**: 费用管理模块
- **service-job**: 任务调度模块
- **service-oa**: 办公自动化模块
- **service-order**: 订单管理模块
- **service-report**: 报表统计模块
- **service-store**: 商户管理模块
- **service-user**: 用户管理模块
- **service-scm**: 供应链管理模块

### 核心功能特性
1. **统一启动入口**: 提供BootApplicationStart主启动类
2. **命令驱动架构**: 支持命令发现和事件发布机制
3. **API文档自动生成**: 集成API文档自动发现功能
4. **多环境配置**: 支持debug、dev等多环境配置
5. **缓存管理**: 集成Redis缓存和缓存刷新机制

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.31.250:3306/TT?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8&useSSL=false
    username: TT
    password: hc12345678
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Redis配置
```yaml
spring:
  redis:
    database: 0
    host: 127.0.0.1
    port: 6379
    password: hc
```

### 服务器配置
```yaml
server:
  port: 8008
  tomcat:
    uri-encoding: UTF-8
```

### FTP文件服务配置
```properties
java110.ftpServer = 121.4.123.73
java110.ftpPort = 6069
java110.ftpUserName = hcftp
java110.ftpUserPassword = 123456
```

## 部署指南

### 环境要求
- **JDK**: 1.8+
- **Maven**: 3.5+
- **MySQL**: 5.7+
- **Redis**: 3.2+

### 构建步骤
1. 克隆项目到本地
2. 配置数据库连接信息
3. 执行Maven构建命令：
   ```bash
   mvn clean package spring-boot:repackage -Dmaven.test.skip=true
   ```
4. 运行生成的JAR文件：
   ```bash
   java -jar target/MicroCommunityBoot.jar
   ```

### 启动参数
- `-Dcache`: 启动时强制刷新缓存
- **自动缓存检测**: 系统启动时会自动检测Redis中是否存在`java110_hc_version`缓存键，如果不存在会自动触发缓存刷新
- 默认端口: 8008

### 多环境部署
- **开发环境**: 使用application-dev.yml配置
- **调试环境**: 使用application-debug.yml配置（默认）
- **生产环境**: 可创建application-prod.yml配置

## API接口

### 命令接口
模块采用命令驱动架构，支持以下业务域的API接口：

#### 账户管理接口 (com.java110.acct.cmd)
- 账户查询、资金管理、交易记录等命令接口

#### 通用服务接口 (com.java110.common.cmd)
- 系统配置、字典管理、文件上传等通用接口

#### 社区管理接口 (com.java110.community.cmd)
- 社区信息、楼栋管理、房屋管理等接口

#### 开发工具接口 (com.java110.dev.cmd)
- 缓存管理、系统监控等开发工具接口

#### 费用管理接口 (com.java110.fee.cmd)
- 费用设置、账单管理、缴费记录等接口

#### 任务调度接口 (com.java110.job.cmd)
- 定时任务管理、任务执行监控等接口

#### 办公自动化接口 (com.java110.oa.cmd)
- 流程审批、公文管理等OA接口

#### 订单管理接口 (com.java110.order.cmd)
- 订单创建、状态管理、支付处理等接口

#### 报表统计接口 (com.java110.report.cmd)
- 数据统计、报表生成、分析图表等接口

#### 商户管理接口 (com.java110.store.cmd)
- 商户入驻、商品管理、库存管理等接口

#### 用户管理接口 (com.java110.user.cmd)
- 用户注册、登录认证、权限管理等接口

#### 供应链管理接口 (com.java110.scm.cmd)
- 供应商管理、采购订单、库存管理等接口

### REST接口
模块还提供REST风格的API接口，位于`com.java110.boot.rest`包中，支持：
- HTTP/HTTPS协议
- JSON数据格式
- 统一的错误处理机制
- API文档自动生成

## 依赖关系

### 核心依赖
- `java110-boot`: 核心启动框架
- `java110-service`: 基础服务框架
- `spring-boot-starter-websocket`: WebSocket支持
- `spring-boot-starter-thymeleaf`: 模板引擎

### 业务模块依赖
集成了所有MicroCommunity业务模块，确保功能完整性

## 注意事项

1. **缓存刷新**: 系统支持两种缓存刷新方式：
   - **自动检测**: 启动时自动检测Redis中`java110_hc_version`缓存键，不存在时自动刷新
   - **强制刷新**: 使用`-Dcache`参数强制刷新缓存，适用于首次启动或配置变更
2. **数据库初始化**: 确保数据库已正确初始化并包含必要的表结构
3. **Redis连接**: 确保Redis服务正常运行且配置正确
4. **文件上传**: 文件上传大小限制为100MB，可在配置中调整
5. **多环境切换**: 通过修改`spring.profiles.active`切换运行环境

## 技术支持

如有技术问题，请联系开发团队或查阅相关技术文档。