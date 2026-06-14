# Service-Job 模块技术文档

## 模块概述
Service-Job 是基于 Spring Boot 的微服务模块，主要负责任务调度、数据导出、物联网设备管理、商城接口、打印服务等核心业务功能。该模块采用命令驱动架构设计，通过 cmd 目录下的 **34个核心命令接口** 提供统一的服务接口。

## 技术架构

### 核心依赖
- **Spring Boot**: 微服务框架基础
- **Quartz**: 分布式任务调度引擎
- **Thymeleaf**: 模板引擎，用于报表生成和页面渲染
- **Maven**: 项目构建工具

### 架构特点
- 命令驱动架构：所有业务逻辑通过 cmd 命令类实现
- 异步处理：数据导出采用队列机制异步处理
- 任务调度：基于 Quartz 实现定时任务管理
- 模块化设计：按业务领域划分命令类目录

## 核心命令接口

Service-Job 模块共包含 **34个核心命令接口**，按业务领域分布在9个子目录中：

### 1. 数据导出模块 (export) - 1个接口
**ExportDataCmd** - 数据导出处理命令
- 服务编码: `export.exportData`
- 功能: 异步数据导出，支持 Excel 格式
- 核心流程:
  - 验证小区ID和页面路径参数
  - 创建用户下载文件记录
  - 将导出任务加入队列异步处理
  - 返回文件生成状态

### 2. 物联网设备管理 (iot)
包含 13 个命令类，主要功能：

**设备接入控制**
- `GetOpenApiCmd` - 开放API获取
- `GetAdminOpenApiCmd` - 管理员开放API获取
- `PostOpenApiCmd` - 开放API提交
- `GetIotTokenCmd` - 物联网令牌获取
- `OpenAccessControlDoorCmd` - 门禁开门控制

**设备状态查询**
- `ListAdminAccessControlCmd` - 门禁设备列表
- `ListAdminBarrierCmd` - 道闸设备列表
- `ListAdminChargeMachineCmd` - 充电桩设备列表
- `ListAdminMeterMachineCmd` - 计量表设备列表

**数据记录查询**
- `ListAdminAccessControlInoutCmd` - 门禁进出记录
- `ListAdminCarInoutDetailCmd` - 车辆进出详情
- `ListAdminChargeMachineOrderCmd` - 充电订单记录
- `ListAdminMeterChargeCmd` - 计量收费记录

### 3. 任务调度 (job)
**RunJobCmd** - 任务运行命令
- 功能: 基于 Quartz 的任务调度执行
- 集成: 继承 TaskSystemQuartz 抽象类实现具体业务逻辑

### 4. 商城服务 (mall)
包含 11 个命令类，主要功能：

**商城基础服务**
- `GetAdminMallOpenApiCmd` - 管理员商城API
- `GetMallOpenApiCmd` - 商城开放API
- `GetMallTokenCmd` - 商城令牌获取

**商品管理**
- `ListProductGroupCmd` - 商品分组列表
- `ListProductSeckillCmd` - 秒杀商品列表
- `QueryMainCategoryCmd` - 主分类查询
- `QueryPhoneMainCategoryProductCmd` - 手机端商品查询

**业务查询**
- `QueryCommunityRecommendCmd` - 小区推荐查询
- `QueryHousekeepingTypeCmd` - 家政类型查询
- `QueryShopTypeCmd` - 店铺类型查询

### 5. 计量设备 (meterMachine)
**ListMeterMachineCmd** - 计量设备列表
- 功能: 查询计量表设备信息
- 应用: 水电燃气等计量设备管理

### 6. 业主服务 (owner)
**业主设备管理**
- `ListOwnerMachinesCmd` - 业主设备列表查询
- `QueryOwnerAcVideoUrlCmd` - 业主视频门禁URL查询

### 7. 插件管理 (plugin)
**GetPluginTokenCmd** - 插件令牌获取
- 功能: 为第三方插件提供认证令牌
- 应用: 插件系统集成

### 8. 打印服务 (print)
**票据打印**
- `PrintAccountReceiptCmd` - 账户收据打印
- `PrintPayFeeDetailCmd` - 缴费明细打印

### 9. 用户文件下载 (userDownloadFile)
**下载文件管理**
- `DeleteUserDownloadFileCmd` - 删除用户下载文件
- `ListUserDownloadFileCmd` - 用户下载文件列表查询

## 任务调度实现

### Quartz 调度框架
**TaskSystemQuartz** - 抽象任务调度基类
- 提供任务执行模板方法
- 支持任务状态管理和错误处理
- 集成小区和商户信息查询

**核心方法**:
- `startTask(TaskDto taskDto)`: 任务启动入口，接收任务参数
- `process(TaskDto taskDto)`: 抽象业务处理方法
- `prepare(TaskDto taskDto)`: 前置处理钩子
- `after(TaskDto taskDto)`: 后置处理钩子

### 数据导出队列机制
**ExportDataQueue** - 数据导出队列
- 基于 LinkedBlockingQueue 实现
- 支持异步处理导出任务
- 队列容量: 100个任务
- 超时时间: 3秒（添加任务时的等待超时）

## 部署配置

### Maven 配置
```xml
<dependencies>
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-service</artifactId>
    </dependency>
    <dependency>
        <groupId>org.quartz-scheduler</groupId>
        <artifactId>quartz</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

### 环境要求
- Java 1.8+
- Spring Boot 2.x
- Maven 3.6+

## 使用说明

### 数据导出示例
```java
// 调用导出命令
JSONObject reqJson = new JSONObject();
reqJson.put("communityId", "123456");
reqJson.put("pagePath", "userList");
// 执行导出，返回文件生成状态
```

### 任务调度配置
继承 TaskSystemQuartz 实现具体业务逻辑：
```java
public class CustomTask extends TaskSystemQuartz {
    @Override
    protected void process(TaskDto taskDto) throws Exception {
        // 实现具体业务逻辑
    }
}
```

## 模块特点

1. **命令驱动**: 所有业务通过命令类实现，架构清晰
2. **异步处理**: 耗时操作采用队列异步处理
3. **调度灵活**: 基于 Quartz 的分布式任务调度
4. **扩展性强**: 模块化设计，易于扩展新功能
5. **错误处理**: 完善的异常处理和日志记录

## 维护说明

- 新增命令类需在对应业务目录下创建
- 任务调度类需继承 TaskSystemQuartz
- 数据导出需使用 ExportDataQueue 队列机制
- 所有命令类需使用 @Java110Cmd 注解注册服务编码