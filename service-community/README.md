# Service-Community 社区服务模块

## 模块概述

Service-Community 是 MicroCommunity 微服务架构中的核心业务模块，专注于社区物业管理系统的业务逻辑处理。该模块包含651个Java文件，提供完整的社区管理、房屋管理、访客管理、报修管理、停车管理等核心功能。

## 技术架构

### 技术栈
- **框架**: Spring Boot 2.x + Spring Cloud
- **服务发现**: @EnableDiscoveryClient
- **负载均衡**: @LoadBalanced
- **远程调用**: @EnableFeignClients
- **数据库**: MySQL + MyBatis
- **构建工具**: Maven
- **Java版本**: 1.8

### 架构特点
- 基于注解驱动的命令模式（@Java110Cmd）
- 支持事务管理（@Java110Transactional）
- 完整的API文档生成（@Java110CmdDoc）
- 微服务架构，支持服务注册与发现

## 功能模块说明

### 1. 社区管理 (community)
- **SaveCommunityCmd**: 创建社区，包含基本信息、成员信息、费用配置和工作流配置
- **UpdateCommunityCmd**: 更新社区信息
- **DeleteCommunityCmd**: 删除社区
- **ListCommunitysCmd**: 查询社区列表
- **AuditCommunityCmd**: 审核社区
- **SendCommunityToIotCmd**: 同步社区信息到IoT设备

### 2. 房间管理 (room)
- **SaveRoomCmd**: 创建房间
- **UpdateRoomCmd**: 更新房间信息
- **DeleteRoomCmd**: 删除房间
- **QueryRoomsCmd**: 查询房间列表
- **SellRoomCmd**: 房间销售
- **ExitRoomCmd**: 房间退租
- **AddRoomBindingCmd**: 房间绑定

### 3. 单元管理 (unit)
- **SaveUnitCmd**: 创建单元
- **UpdateUnitCmd**: 更新单元信息
- **DeleteUnitCmd**: 删除单元
- **QueryUnitsCmd**: 查询单元列表

### 4. 访客管理 (visit)
- **SaveVisitCmd**: 创建访客记录
- **UpdateVisitCmd**: 更新访客信息
- **DeleteVisitCmd**: 删除访客记录
- **ListVisitsCmd**: 查询访客列表
- **AuditUndoVisitCmd**: 审核访客
- **QueryVisitQrCodeCmd**: 生成访客二维码
- **ExamineVisitCar**: 访客车辆审核

### 5. 报修管理 (repair)
- **ListRepairEventCmd**: 查询报修事件
- **QueryPhoneRepairsCmd**: 手机端报修查询
- **PhoneUrgeRepairCmd**: 手机端催单
- **SaveRepairSettingCmd**: 报修设置
- **ReplyRepairAppraiseCmd**: 报修评价回复

### 6. 停车管理 (parkingSpace)
- **SaveParkingSpaceCmd**: 创建停车位
- **BatchSaveParkingSpaceCmd**: 批量创建停车位
- **DeleteParkingSpaceCmd**: 删除停车位
- **EditParkingSpaceCmd**: 编辑停车位
- **SellParkingSpaceCmd**: 停车位销售
- **ExitParkingSpaceCmd**: 停车位退租
- **QueryParkingSpacesCmd**: 查询停车位列表

### 7. 巡检管理 (inspection)
- **QueryRouteInspectionPlanCmd**: 查询巡检路线计划
- **QueryRouteInspectionTaskCmd**: 查询巡检任务
- **QueryPointInspectionPlanCmd**: 查询巡检点计划
- **QueryInspectionTaskDetailCmd**: 查询巡检任务详情
- **QueryReportStaffInspectionCmd**: 查询员工巡检报告

### 8. 维护管理 (maintainance)
- **SaveMaintainanceItemCmd**: 创建维护项目
- **SaveMaintainanceStandardCmd**: 创建维护标准
- **ListMaintainanceItemCmd**: 查询维护项目
- **ListMaintainanceStandardCmd**: 查询维护标准
- **UpdateMaintainanceItemCmd**: 更新维护项目

### 9. 活动管理 (activities)
- **SaveActivitiesCmd**: 创建社区活动
- **UpdateActivitiesCmd**: 更新活动信息
- **DeleteActivitiesCmd**: 删除活动
- **ListActivitiessCmd**: 查询活动列表

### 10. 其他功能模块
- **社区位置管理** (communityLocation)
- **社区空间管理** (communitySpace)
- **社区场馆管理** (communityVenue)
- **权限管理** (basePrivilege, dataPrivilege)
- **字典管理** (dict)
- **楼层管理** (floor)
- **通知管理** (notice)
- **业主报修** (ownerRepair)
- **产权登记** (propertyRightRegistration)

## 核心接口列表

### 社区管理接口
```java
@Java110Cmd(serviceCode = "community.saveCommunity")
public class SaveCommunityCmd extends Cmd

@Java110Cmd(serviceCode = "community.updateCommunity")  
public class UpdateCommunityCmd extends Cmd

@Java110Cmd(serviceCode = "community.listCommunitys")
public class ListCommunitysCmd extends Cmd
```

### 房间管理接口
```java
@Java110Cmd(serviceCode = "room.saveRoom")
public class SaveRoomCmd extends Cmd

@Java110Cmd(serviceCode = "room.queryRooms") 
public class QueryRoomsCmd extends Cmd
```

### 访客管理接口
```java
@Java110Cmd(serviceCode = "visit.saveVisit")
public class SaveVisitCmd extends Cmd

@Java110Cmd(serviceCode = "visit.listVisits")
public class ListVisitsCmd extends Cmd
```

## 部署配置

### 依赖配置
```xml
<dependency>
    <groupId>com.java110</groupId>
    <artifactId>java110-service</artifactId>
</dependency>
```

### 启动配置
```java
@SpringBootApplication(scanBasePackages = {
    "com.java110.service", 
    "com.java110.community",
    "com.java110.core", 
    "com.java110.config.properties.code", 
    "com.java110.db",
    "com.java110.doc"
})
```

### 服务发现配置
```java
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
    "com.java110.intf.user", 
    "com.java110.intf.common",
    "com.java110.intf.fee",
    "com.java110.intf.order", 
    "com.java110.intf.store",
    "com.java110.intf.acct", 
    "com.java110.intf.oa", 
    "com.java110.intf.report",
    "com.java110.intf.job"
})
```

## 依赖关系

### 内部依赖
- **java110-service**: 核心服务框架
- **java110-core**: 核心工具类
- **java110-db**: 数据库操作
- **java110-doc**: API文档生成

### 外部服务接口
- 用户服务 (user)
- 通用服务 (common) 
- 费用服务 (fee)
- 订单服务 (order)
- 店铺服务 (store)
- 账户服务 (acct)
- OA服务 (oa)
- 报表服务 (report)
- 任务服务 (job)

## 开发规范

### 命令类命名规范
- 创建操作: Save[Xxx]Cmd
- 更新操作: Update[Xxx]Cmd  
- 删除操作: Delete[Xxx]Cmd
- 查询操作: Query[Xxx]Cmd / List[Xxx]Cmd

### 注解使用规范
```java
@Java110Cmd(serviceCode = "module.operation")
@Java110Transactional
@Java110CmdDoc(title = "接口标题", description = "接口描述")
```

### 包结构规范
- **cmd**: 命令处理类（核心业务逻辑入口）
- **bmo**: 业务操作类
- **smo**: 服务操作类  
- **dao**: 数据访问类
- **api**: API接口类

## 注意事项

1. **核心业务逻辑**均在 `cmd` 包下的命令类中实现
2. 所有命令类必须继承 `Cmd` 基类
3. 事务管理使用 `@Java110Transactional` 注解
4. API文档通过注解自动生成
5. 服务编码格式：`模块名.操作名`

## 版本信息

- **当前版本**: 1.0-SNAPSHOT
- **作者**: 吴学文
- **联系邮箱**: 928255095@qq.com
- **开源地址**: https://gitee.com/wuxw7/MicroCommunity
- **官网**: http://www.homecommunity.cn