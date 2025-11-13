# Service-OA 工作流管理系统

## 模块概述

Service-OA 是基于 Spring Boot + Spring Cloud 构建的微服务模块，专注于企业办公自动化（OA）工作流管理。该模块提供完整的工作流生命周期管理，包括工作池管理、任务分配、工作类型定义、周期性任务调度等功能。

## 技术架构

### 核心框架
- **Spring Boot 2.x** - 微服务基础框架
- **Spring Cloud** - 微服务治理
- **Java 8** - 开发语言
- **Maven** - 项目构建工具

### 服务发现与通信
- **@EnableDiscoveryClient** - 服务注册与发现
- **@EnableFeignClients** - 声明式REST客户端
- **RestTemplate** - HTTP客户端（支持负载均衡）

### 核心注解
- **@Java110CmdDiscovery** - 命令接口自动发现（basePackages = "com.java110.oa.cmd"）
- **@Java110ListenerDiscovery** - 事件监听器自动发现

## 模块结构

```
service-oa/
├── src/main/java/com/java110/oa/
│   ├── cmd/                    # 命令接口核心包（主要提供接口）
│   │   ├── work/              # 工作池和工作任务管理
│   │   ├── workType/          # 工作类型管理
│   │   ├── workCycle/         # 工作周期管理
│   │   ├── workEvent/         # 工作事件管理
│   │   ├── oaWorkflow/        # OA工作流管理
│   │   └── workflowDataFile/  # 工作流数据文件管理
│   ├── api/                   # API接口定义
│   ├── bmo/                   # 业务模型对象
│   ├── dao/                   # 数据访问层
│   ├── smo/                   # 服务管理对象
│   └── OaServiceApplicationStart.java  # 应用启动类
├── pom.xml
├── pom-boot.xml
└── pom-cloud.xml
```

## 核心功能模块

### 1. 工作池管理 (work包)
负责工作池的创建、查询、更新和删除，以及工作任务的管理。

### 2. 工作类型管理 (workType包)
定义和管理不同类型的工作模板和分类。

### 3. 工作周期管理 (workCycle包)
处理周期性任务的调度和执行规则。

### 4. 工作事件管理 (workEvent包)
管理工作流中的事件触发和处理。

### 5. OA工作流管理 (oaWorkflow包)
提供工作流引擎的核心功能，包括流程定义和任务流转。

### 6. 工作流数据文件管理 (workflowDataFile包)
管理工作流相关的附件和数据文件。

## 核心命令接口清单

### work包 (21个接口)
**工作池管理：**
- `SaveWorkPoolCmd` - 创建工作池
- `UpdateWorkPoolCmd` - 更新工作池
- `DeleteWorkPoolCmd` - 删除工作池
- `ListWorkPoolCmd` - 查询工作池列表
- `ListAdminWorkPoolCmd` - 管理员查询工作池

**工作任务管理：**
- `FinishWorkTaskCmd` - 完成任务
- `ListWorkTaskCmd` - 查询任务列表
- `ListAdminWorkTaskCmd` - 管理员查询任务
- `ListWorkTaskItemCmd` - 查询任务项列表

**工作抄送管理：**
- `FinishWorkCopyCmd` - 完成抄送
- `ListWorkCopyCmd` - 查询抄送列表
- `ListAdminWorkCopyCmd` - 管理员查询抄送

**工作内容管理：**
- `ListWorkPoolContentCmd` - 查询工作内容
- `ListWorkPoolFileCmd` - 查询工作文件
- `ListAdminWorkPoolFileCmd` - 管理员查询工作文件

**工作查询：**
- `QueryStartWorkCmd` - 查询发起的工作
- `QueryTaskWorkCmd` - 查询任务工作
- `QueryCopyWorkCmd` - 查询抄送工作

**其他功能：**
- `StartOrStopWorkPoolCmd` - 启停工作池
- `ListWorkDeductionCmd` - 查询工作扣款

### workType包 (5个接口)
- `SaveWorkTypeCmd` - 创建工作类型
- `UpdateWorkTypeCmd` - 更新工作类型
- `DeleteWorkTypeCmd` - 删除工作类型
- `ListWorkTypeCmd` - 查询工作类型列表
- `ListAdminWorkTypeCmd` - 管理员查询工作类型

### workCycle包 (5个接口)
- `SaveWorkCycleCmd` - 创建工作周期
- `UpdateWorkCycleCmd` - 更新工作周期
- `DeleteWorkCycleCmd` - 删除工作周期
- `ListWorkCycleCmd` - 查询工作周期列表
- `ListAdminWorkCycleCmd` - 管理员查询工作周期

### workEvent包 (5个接口)
- `SaveWorkEventCmd` - 创建工作事件
- `UpdateWorkEventCmd` - 更新工作事件
- `DeleteWorkEventCmd` - 删除工作事件
- `ListWorkEventCmd` - 查询工作事件列表
- `ListAdminWorkEventCmd` - 管理员查询工作事件

### oaWorkflow包 (3个接口)
- `QueryFirstAuditStaffCmd` - 查询初审人员
- `QueryNextDealUserCmd` - 查询下一步处理人
- `UpdateOaWorkflowFormData` - 更新工作流表单数据

### workflowDataFile包 (4个接口)
- `SaveWorkflowDataFileCmd` - 保存工作流数据文件
- `UpdateWorkflowDataFileCmd` - 更新工作流数据文件
- `DeleteWorkflowDataFileCmd` - 删除工作流数据文件
- `ListWorkflowDataFileCmd` - 查询工作流数据文件列表

## 接口调用示例

### 创建工作池 (SaveWorkPoolCmd)
```json
{
  "communityId": "小区ID",
  "wtId": "工作类型ID",
  "workCycle": "工作周期类型",
  "startTime": "开始时间",
  "endTime": "结束时间",
  "staffs": [
    {
      "staffId": "处理人ID",
      "staffName": "处理人姓名"
    }
  ],
  "contents": [
    {
      "content": "工作内容"
    }
  ],
  "copyStaffs": [
    {
      "staffId": "抄送人ID",
      "staffName": "抄送人姓名"
    }
  ]
}
```

### 查询下一步处理人 (QueryNextDealUserCmd)
```json
{
  "startUserId": "创建人ID",
  "taskId": "任务ID"
}
```

## 部署配置

### 依赖关系
```xml
<dependency>
  <groupId>com.java110</groupId>
  <artifactId>java110-service</artifactId>
</dependency>
```

### 启动配置
- 主类：`OaServiceApplicationStart`
- 扫描包：`com.java110.oa`, `com.java110.service`, `com.java110.core`
- 服务发现：启用
- Feign客户端：启用

## 核心特性

1. **命令驱动架构** - 所有业务逻辑通过命令接口实现
2. **工作流引擎** - 支持复杂的工作流审批流程
3. **周期性任务** - 支持按日、周、月的周期性任务调度
4. **多租户支持** - 基于社区ID的多租户隔离
5. **文件管理** - 完整的工作流附件管理
6. **抄送机制** - 灵活的工作抄送和通知机制

## 开发说明

该模块采用标准的Java110微服务架构，所有业务功能通过cmd包中的命令接口对外提供服务。开发新功能时应在对应的cmd子包中创建新的命令类，并遵循现有的代码规范和事务管理机制。