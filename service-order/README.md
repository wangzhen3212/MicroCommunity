# Service-Order 订单服务模块

## 模块概述

`service-order` 是微社区系统的核心订单处理模块，负责整个系统的订单受理、处理流程管理和事务协调。该模块采用Spring Boot + Spring Cloud架构，支持分布式部署和微服务治理。

## 技术架构

### 核心依赖
- **Spring Boot 2.x**: 应用框架
- **Spring Cloud**: 微服务治理
- **Zookeeper**: 分布式协调服务
- **java110-service**: 基础服务框架
- **java110-interface**: 接口定义

### 架构特点
- 基于事件驱动的订单处理机制
- 支持分布式事务协调
- 集成数据总线(DataBus)进行业务数据交换
- 支持异步处理和监听器模式

## 模块结构

```
service-order/
├── src/main/java/com/java110/order/
│   ├── api/                          # API接口层
│   │   ├── CenterApi.java           # 中心服务API（已废弃）
│   │   ├── HttpApi.java             # HTTP服务API
│   │   ├── OIdApi.java              # 订单ID服务API
│   │   ├── OrderApi.java            # 订单主服务API
│   │   └── PrivilegeApi.java        # 权限服务API
│   ├── dao/                         # 数据访问层
│   │   ├── ICenterServiceDAO.java   # 中心服务数据访问接口
│   │   ├── ICorderServiceDao.java   # 订单服务数据访问接口
│   │   ├── IPrivilegeDAO.java       # 权限数据访问接口
│   │   ├── ISnowflakeldWorker.java  # 雪花算法ID生成器
│   │   └── impl/                    # 数据访问实现
│   ├── listener/                    # 事件监听器
│   │   ├── ConfirmTransactionOrderInfoToDataBusListener.java  # 确认事务订单数据总线监听器
│   │   ├── DealUserIdListener.java                           # 用户ID处理监听器
│   │   ├── ReceiveRequestListener.java                       # 请求接收监听器
│   │   ├── RefreshStaffStoreIdFromSaveStoreInfoListener.java # 刷新员工店铺ID监听器
│   │   ├── ResponseStoreIdFromSaveStoreInfoListener.java     # 响应店铺ID监听器
│   │   └── TransactionOrderInfoToDataBusListener.java        # 事务订单数据总线监听器
│   ├── smo/                        # 服务管理对象层
│   │   ├── IAsynNotifySubService.java     # 异步通知订阅服务接口
│   │   ├── ICenterServiceSMO.java         # 中心服务SMO接口
│   │   ├── IOIdServiceSMO.java            # 订单ID服务SMO接口
│   │   ├── IOrderProcessServiceSMO.java   # 订单处理服务SMO接口
│   │   ├── IOrderServiceSMO.java          # 订单服务SMO接口
│   │   ├── IPrivilegeSMO.java             # 权限服务SMO接口
│   │   └── impl/                          # SMO实现类
│   ├── OrderServiceApplicationStart.java  # 应用启动类
│   └── package-info.java                  # 包说明
├── src/main/resources/             # 资源配置文件
├── doc/                            # 文档目录
├── target/                         # 编译输出目录
├── pom.xml                         # Maven主配置
├── pom-boot.xml                    # Spring Boot配置
└── pom-cloud.xml                   # Spring Cloud配置
```

## 核心功能

### 1. 订单受理服务
- **OrderApi**: 提供统一的订单受理接口，支持订单预处理、确认提交等流程
- 支持多种订单处理状态：预提交、确认提交、正常处理

### 2. 中心服务协调
- **CenterApi**: 中心服务统一入口（已废弃，推荐使用OrderApi）
- 提供业务系统间的服务协调和透传机制

### 3. 事务管理
- 支持分布式事务协调
- 集成数据总线进行业务数据交换
- 提供事务订单到数据总线的监听处理

### 4. 权限控制
- **PrivilegeApi**: 权限验证服务
- 支持服务级别和应用级别的权限控制

### 5. 异步处理
- 支持异步通知订阅
- 基于监听器模式的事件处理机制

## API接口说明

### 1. 订单主服务接口
**Endpoint**: `/orderApi/service`
- **方法**: POST
- **功能**: 订单统一受理服务
- **参数**: 
  - `orderInfo`: 订单信息JSON
  - `serviceCode`: 服务编码（请求头）
  - `appId`: 应用ID（请求头）

### 2. 订单处理流程
- **预提交处理**: `orderProcess = ORDER_PROCESS_ORDER_PRE_SUBMIT`
- **确认提交处理**: `orderProcess = ORDER_PROCESS_ORDER_CONFIRM_SUBMIT`
- **正常处理**: 默认处理流程

### 3. 权限验证接口
**Endpoint**: `/privilegeApi/**`
- 提供权限验证和访问控制功能

## 事件监听器

### 1. 事务订单数据总线监听器
- **TransactionOrderInfoToDataBusListener**: 将事务订单信息传输到数据总线
- **ConfirmTransactionOrderInfoToDataBusListener**: 确认事务订单信息传输

### 2. 用户ID处理监听器
- **DealUserIdListener**: 处理用户ID相关业务逻辑

### 3. 店铺信息监听器
- **RefreshStaffStoreIdFromSaveStoreInfoListener**: 刷新员工店铺ID
- **ResponseStoreIdFromSaveStoreInfoListener**: 响应店铺ID信息

## 部署配置

### 1. 环境要求
- JDK 1.8+
- Maven 3.6+
- Zookeeper 3.4+

### 2. 启动配置
```bash
# Spring Boot 方式启动
mvn spring-boot:run -f pom-boot.xml

# Spring Cloud 方式启动  
mvn spring-boot:run -f pom-cloud.xml
```

### 3. 关键配置项
- `zookeeper.connectString`: Zookeeper连接地址
- `databus.switch`: 数据总线开关（ON/OFF）
- `need.invoke.generate.id`: ID生成开关

## 依赖关系

### 内部依赖
- `java110-service`: 基础服务框架
- `java110-interface`: 接口定义
- `java110-core`: 核心组件

### 外部服务依赖
- `service-user`: 用户服务
- `service-community`: 社区服务  
- `service-fee`: 费用服务
- `service-common`: 公共服务
- `service-job`: 任务服务

## 开发规范

### 1. 代码结构
- API层负责请求接收和参数验证
- SMO层负责业务逻辑处理
- DAO层负责数据访问
- Listener层负责事件处理

### 2. 异常处理
- 使用统一的异常处理机制
- 业务异常使用`BusinessException`
- 系统异常使用`SMOException`

### 3. 日志规范
- 使用SLF4J日志框架
- 关键业务流程记录详细日志
- 异常情况记录错误堆栈

## 注意事项

1. **CenterApi已废弃**: 推荐使用OrderApi进行订单处理
2. **数据总线开关**: 需要配置`databus.switch`开启数据总线功能
3. **ID生成**: 需要配置Zookeeper支持分布式ID生成
4. **权限验证**: 所有请求必须包含有效的`serviceCode`和`appId`

## 版本历史

- **v1.0**: 初始版本，提供基础订单处理功能
- **当前版本**: 支持分布式事务和数据总线集成

---

*最后更新: 2024年*