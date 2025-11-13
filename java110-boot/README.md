# java110-boot 模块说明文档

## 项目概述

java110-boot 是 MicroCommunity 项目的核心启动模块，采用 Maven 构建，基于 Java 8 开发。本模块作为微服务架构中的启动入口，负责管理和协调各个业务模块的运行。

## 项目结构

```
java110-boot/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── java110/
│                   ├── acct/                    # 账户管理模块
│                   │   └── AcctServiceApplicationStart.java
│                   ├── api/                     # API接口模块
│                   │   └── ApiApplicationStart.java
│                   ├── common/                  # 通用工具模块
│                   │   └── CommonServiceApplicationStart.java
│                   ├── community/               # 社区管理模块
│                   │   └── CommunityServiceApplicationStart.java
│                   ├── dev/                     # 开发工具模块
│                   │   └── DevServiceApplicationStart.java
│                   ├── fee/                     # 费用管理模块
│                   │   └── FeeServiceApplicationStart.java
│                   ├── job/                     # 任务调度模块
│                   │   └── JobServiceApplication.java
│                   ├── oa/                      # 办公自动化模块
│                   │   └── OaServiceApplicationStart.java
│                   ├── order/                   # 订单管理模块
│                   │   └── OrderServiceApplicationStart.java
│                   ├── report/                  # 报表统计模块
│                   │   └── ReportServiceApplicationStart.java
│                   ├── scm/                     # 供应链管理模块
│                   │   └── ScmServiceApplicationStart.java
│                   ├── store/                   # 商户管理模块
│                   │   └── StoreServiceApplicationStart.java
│                   └── user/                    # 用户管理模块
│                       ├── UserServiceApplicationStart.java
│                       └── test.java
├── target/
└── pom.xml
```

## 模块功能说明

### 1. acct (账户管理模块)
- **功能职责**: 负责账户相关的业务逻辑处理
- **核心类**: `AcctServiceApplicationStart.java`

### 2. api (API接口模块)
- **功能职责**: 提供对外API接口服务
- **核心类**: `ApiApplicationStart.java`

### 3. common (通用工具模块)
- **功能职责**: 提供通用工具类和基础服务
- **核心类**: `CommonServiceApplicationStart.java`

### 4. community (社区管理模块)
- **功能职责**: 管理社区信息和相关业务
- **核心类**: `CommunityServiceApplicationStart.java`

### 5. dev (开发工具模块)
- **功能职责**: 提供开发相关的工具和功能
- **核心类**: `DevServiceApplicationStart.java`

### 6. fee (费用管理模块)
- **功能职责**: 处理费用计算、收费等业务
- **核心类**: `FeeServiceApplicationStart.java`

### 7. job (任务调度模块)
- **功能职责**: 负责定时任务和作业调度
- **核心类**: `JobServiceApplication.java`

### 8. oa (办公自动化模块)
- **功能职责**: 办公自动化相关功能
- **核心类**: `OaServiceApplicationStart.java`

### 9. order (订单管理模块)
- **功能职责**: 订单创建、处理和管理
- **核心类**: `OrderServiceApplicationStart.java`

### 10. report (报表统计模块)
- **功能职责**: 数据统计和报表生成
- **核心类**: `ReportServiceApplicationStart.java`

### 11. scm (供应链管理模块)
- **功能职责**: 供应链相关业务处理
- **核心类**: `ScmServiceApplicationStart.java`

### 12. store (商户管理模块)
- **功能职责**: 商户信息管理和业务处理
- **核心类**: `StoreServiceApplicationStart.java`

### 13. user (用户管理模块)
- **功能职责**: 用户注册、登录、权限管理等
- **核心类**: `UserServiceApplicationStart.java`

## 技术架构

### 开发环境
- **JDK版本**: 1.8
- **构建工具**: Maven
- **编码**: UTF-8

### 项目配置
- **父项目**: MicroCommunity
- **GroupId**: com.java110
- **ArtifactId**: java110-boot
- **版本**: 1.0-SNAPSHOT

### 依赖管理
项目依赖管理统一在父项目 MicroCommunity 中配置，当前模块主要依赖：
- JUnit 4.11 (测试范围)

## 使用指南

### 环境要求
- JDK 1.8 或更高版本
- Maven 3.0 或更高版本

### 构建项目
```bash
mvn clean compile
```

### 打包项目
```bash
mvn clean package
```

### 运行项目
由于当前各模块启动类为空，需要根据具体业务需求完善启动逻辑。

## 开发说明

### 模块扩展
1. 在对应模块包下添加业务逻辑代码
2. 完善启动类，添加Spring Boot启动注解
3. 配置相应的application.properties文件

### 注意事项
- 各模块启动类当前为空，需要根据实际需求实现
- 建议为每个模块创建独立的配置文件
- 模块间调用需要明确依赖关系

## 维护信息

- **版权**: Copyright 2017-2020 吴学文 and java110 team
- **许可证**: Apache License 2.0

---

*本文档基于项目结构分析生成，具体实现细节请参考各模块源代码。*