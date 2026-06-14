# Service-Report 模块技术文档

## 模块概述

Service-Report 是微社区系统中的报表服务模块，主要负责各类业务数据的统计分析、报表生成和数据可视化展示。该模块采用命令驱动架构设计，通过统一的命令接口提供丰富的报表查询和分析功能。

**核心接口统计**：
- 命令接口总数：86个（分布在16个业务子目录中）
- API接口层：12个RESTful接口
- 数据访问层：29个DAO接口
- 统计服务层：7个统计服务接口

## 技术架构

### 核心目录结构
```
service-report/
├── src/main/java/com/java110/report/
│   ├── api/              # API接口层 - 提供RESTful接口
│   ├── bmo/              # 业务模型对象层 - 业务逻辑处理
│   ├── cmd/              # 命令处理层 - 核心业务接口（16个业务子目录）
│   ├── dao/              # 数据访问层 - 数据库操作接口
│   ├── smo/              # 服务管理对象层 - 服务实现
│   ├── statistics/       # 统计服务层 - 数据统计逻辑
│   └── ReportServiceApplicationStart.java  # 应用启动类
```

### 架构特点
- **命令驱动设计**：所有业务功能通过cmd命令接口提供
- **分层架构**：清晰的API-CMD-SMO-DAO分层设计
- **统计服务独立**：专门的statistics层处理复杂统计逻辑
- **模块化设计**：按业务领域划分16个独立的命令子目录

## 核心命令接口说明

### 1. 管理员报表接口 (admin/)
- **GetCommunityFeeSummaryCmd** (`admin.getCommunityFeeSummary`)
  - 功能：社区费用汇总查询
  - 参数：startDate, endDate, communityId等
  - 输出：历史欠费、当月欠费、应收费用、实收费用等9个统计指标

- **GetCommunityOperationalAnalysisCmd** (`admin.getCommunityOperationalAnalysis`)
  - 功能：社区运营分析报表

- **GetParkingFeeSummaryCmd** (`admin.getParkingFeeSummary`)
  - 功能：停车费用汇总查询

- **GetPropertyFeeSummaryCmd** (`admin.getPropertyFeeSummary`)
  - 功能：物业费用汇总查询

- **QueryAdminCountCmd** (`admin.queryAdminCount`)
  - 功能：管理员数据统计查询

- **QueryAppServiceDataCmd** (`admin.queryAppServiceData`)
  - 功能：应用服务数据查询

- **QueryCommunityFeeCmd** (`admin.queryCommunityFee`)
  - 功能：社区费用查询

- **QueryCommunityRepairCmd** (`admin.queryCommunityRepair`)
  - 功能：社区报修查询

- **QueryDevCountCmd** (`admin.queryDevCount`)
  - 功能：设备数量统计查询

### 2. 车辆管理报表 (car/)
- **ListCarStructureCmd** (`car.listCarStructure`)
  - 功能：车辆结构统计

- **QueryHisOwnerCarCmd** (`car.queryHisOwnerCar`)
  - 功能：历史业主车辆查询

### 3. 社区数据报表 (community/)
- **QueryCommunityFeeTypeTreeCmd** (`community.queryCommunityFeeTypeTree`)
  - 功能：费用类型树形结构查询

- **QueryCommunityFloorTreeCmd** (`community.queryCommunityFloorTree`)
  - 功能：楼层树形结构查询

- **QueryCommunityParkingTreeCmd** (`community.queryCommunityParkingTree`)
  - 功能：停车位树形结构查询

### 4. 数据报表中心 (dataReport/)
- **QueryDataReportFeeStatisticsCmd** (`dataReport.queryDataReportFeeStatistics`)
  - 功能：费用统计数据报表

- **QueryFeeDataReportCmd** (`dataReport.queryFeeDataReport`)
  - 功能：费用数据报表查询

- **QueryInoutDataReportCmd** (`dataReport.queryInoutDataReport`)
  - 功能：出入数据报表查询

- **QueryMonthOweDetailCmd** (`dataReport.queryMonthOweDetail`)
  - 功能：月度欠费明细查询

- **QueryMonthReceivedDetailCmd** (`dataReport.queryMonthReceivedDetail`)
  - 功能：月度实收明细查询

- **QueryOrderDataReportCmd** (`dataReport.queryOrderDataReport`)
  - 功能：订单数据报表查询

- **QueryOthersDataReportCmd** (`dataReport.queryOthersDataReport`)
  - 功能：其他数据报表查询

- **QueryOweDetailStatisticsCmd** (`dataReport.queryOweDetailStatistics`)
  - 功能：欠费明细统计查询

- **QueryOweStatisticsCmd** (`dataReport.queryOweStatistics`)
  - 功能：欠费统计查询

- **QueryReceivedDetailStatisticsCmd** (`dataReport.queryReceivedDetailStatistics`)
  - 功能：实收明细统计查询

- **QueryReceivedStatisticsCmd** (`dataReport.queryReceivedStatistics`)
  - 功能：实收统计查询

- **QueryReceivedWayStatisticsCmd** (`dataReport.queryReceivedWayStatistics`)
  - 功能：收款方式统计查询

### 5. 费用管理报表 (fee/)
- **QueryAdminHisFeeCmd** (`fee.queryAdminHisFee`)
  - 功能：管理员历史费用查询

- **QueryAdminOweFeeDetailCmd** (`fee.queryAdminOweFeeDetail`)
  - 功能：管理员欠费明细查询

- **QueryHisFeeConfigCmd** (`fee.queryHisFeeConfig`)
  - 功能：历史费用配置查询

### 6. 业主管理报表 (owner/)
- **QueryAdminHisOwnerCmd** (`owner.queryAdminHisOwner`)
  - 功能：管理员历史业主查询

- **QueryOwnerDiningCmd** (`owner.queryOwnerDining`)
  - 功能：业主餐饮数据查询

- **QueryOwnerReserveGoodsCmd** (`owner.queryOwnerReserveGoods`)
  - 功能：业主预约商品查询

### 7. 自定义报表系统 (reportCustom/)
- **SaveReportCustomCmd** (`reportCustom.saveReportCustom`)
  - 功能：保存自定义报表配置

- **ListReportCustomCmd** (`reportCustom.listReportCustom`)
  - 功能：查询自定义报表列表

- **UpdateReportCustomCmd** (`reportCustom.updateReportCustom`)
  - 功能：更新自定义报表配置

### 8. 自定义报表组件 (reportCustomComponent/)
- **ListReportCustomComponentDataCmd** (`reportCustomComponent.listReportCustomComponentData`)
  - 功能：查询自定义报表组件数据

- **SaveReportCustomComponentCmd** (`reportCustomComponent.saveReportCustomComponent`)
  - 功能：保存自定义报表组件

### 9. 报表组件条件 (reportCustomComponentCondition/)
- **SaveReportCustomComponentConditionCmd** (`reportCustomComponentCondition.saveReportCustomComponentCondition`)
  - 功能：保存报表组件查询条件

- **ListReportCustomComponentConditionCmd** (`reportCustomComponentCondition.listReportCustomComponentCondition`)
  - 功能：查询报表组件条件列表

### 10. 报表组件页脚 (reportCustomComponentFooter/)
- **SaveReportCustomComponentFooterCmd** (`reportCustomComponentFooter.saveReportCustomComponentFooter`)
  - 功能：保存报表组件页脚配置

### 11. 报表组件关联 (reportCustomComponentRel/)
- **SaveReportCustomComponentRelCmd** (`reportCustomComponentRel.saveReportCustomComponentRel`)
  - 功能：保存报表组件关联关系

### 12. 自定义报表分组 (reportCustomGroup/)
- **SaveReportCustomGroupCmd** (`reportCustomGroup.saveReportCustomGroup`)
  - 功能：保存自定义报表分组

### 13. 费用月度统计 (reportFeeMonthStatistics/)
- **QueryDeadlineFeeCmd** (`reportFeeMonthStatistics.queryDeadlineFee`)
  - 功能：到期费用查询

- **QueryPayFeeDetailCmd** (`reportFeeMonthStatistics.queryPayFeeDetail`)
  - 功能：缴费明细查询

- **QueryPrePaymentCmd** (`reportFeeMonthStatistics.queryPrePayment`)
  - 功能：预交费用查询

- **QueryReportConfigFeeSummaryCmd** (`reportFeeMonthStatistics.queryReportConfigFeeSummary`)
  - 功能：配置费用汇总查询

- **QueryReportFeeBreakdownDetailCmd** (`reportFeeMonthStatistics.queryReportFeeBreakdownDetail`)
  - 功能：费用明细分解查询

- **QueryReportFeeDetailCarCmd** (`reportFeeMonthStatistics.queryReportFeeDetailCar`)
  - 功能：车辆费用明细查询

- **QueryReportFeeDetailContractCmd** (`reportFeeMonthStatistics.queryReportFeeDetailContract`)
  - 功能：合同费用明细查询

- **QueryReportFeeDetailOwnerCmd** (`reportFeeMonthStatistics.queryReportFeeDetailOwner`)
  - 功能：业主费用明细查询

- **QueryReportFeeDetailRoomCmd** (`reportFeeMonthStatistics.queryReportFeeDetailRoom`)
  - 功能：房间费用明细查询

- **QueryReportFeeSummaryCmd** (`reportFeeMonthStatistics.queryReportFeeSummary`)
  - 功能：费用汇总统计查询

- **QueryReportFeeSummaryDetailCmd** (`reportFeeMonthStatistics.queryReportFeeSummaryDetail`)
  - 功能：费用汇总明细查询

- **QueryReportFloorFeeSummaryCmd** (`reportFeeMonthStatistics.queryReportFloorFeeSummary`)
  - 功能：楼层费用汇总查询

- **QueryReportFloorUnitFeeSummaryDetailCmd** (`reportFeeMonthStatistics.queryReportFloorUnitFeeSummaryDetail`)
  - 功能：楼层单元费用汇总明细查询

- **QueryReportProficientCmd** (`reportFeeMonthStatistics.queryReportProficient`)
  - 功能：专业报表查询

### 14. 信息反馈报表 (reportInfoAnswer/)
- **QueryReportInfoAnswerByOpenId** (`reportInfoAnswer.queryReportInfoAnswerByOpenId`)
  - 功能：根据OpenID查询信息反馈

### 15. 房间管理报表 (room/)
- **ListRoomStructureCmd** (`room.listRoomStructure`)
  - 功能：房间结构统计

- **QueryHisRoomCmd** (`room.queryHisRoom`)
  - 功能：历史房间数据查询

- **QueryRoomsTreeCmd** (`room.queryRoomsTree`)
  - 功能：房间树形结构查询

### 16. 数据搜索报表 (search/)
- **SearchCommunityDataCmd** (`search.searchCommunityData`)
  - 功能：社区数据搜索查询

## 数据访问层 (DAO)

### 核心数据访问接口
- **IFeeStatisticsServiceDao** - 费用统计数据访问
- **IBaseDataStatisticsServiceDao** - 基础数据统计访问
- **IReportFeeMonthStatisticsServiceDao** - 月度费用统计访问
- **IReportCustomV1ServiceDao** - 自定义报表数据访问

## 统计服务层 (Statistics)

### 核心统计服务
- **IFeeStatistics** - 费用统计服务
- **IBaseDataStatistics** - 基础数据统计服务
- **IFloorFeeStatistics** - 楼层费用统计服务
- **IInoutStatistics** - 出入统计服务

## API接口层

### 主要RESTful接口
- **ReportFeeMonthStatisticsApi** - 月度费用统计API
- **ReportOweFeeApi** - 欠费统计API
- **ReportOwnerPayFeeApi** - 业主缴费API
- **ReportInfoAnswerApi** - 信息反馈API

## 部署配置

### 依赖配置
```xml
<!-- 在pom.xml中配置相关依赖 -->
<dependency>
    <groupId>com.java110</groupId>
    <artifactId>service-report</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 启动配置
```java
// 应用启动类
@SpringBootApplication
public class ReportServiceApplicationStart {
    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplicationStart.class, args);
    }
}
```

## 使用说明

### 调用示例
```java
// 调用社区费用汇总查询
JSONObject reqJson = new JSONObject();
reqJson.put("serviceCode", "admin.getCommunityFeeSummary");
reqJson.put("startDate", "2024-01-01");
reqJson.put("endDate", "2024-01-31");
reqJson.put("communityId", "123456");

// 执行命令调用
CmdEvent event = new CmdEvent(reqJson);
cmdService.doService(event);
```

## 版本历史

- v1.0.0 (2024) - 初始版本，包含16个业务领域的报表功能
- 支持社区管理、费用统计、业主管理、自定义报表等核心功能

## 维护说明

该模块采用标准的Java110微服务架构，具有良好的扩展性和维护性。新增报表功能时，建议：
1. 在对应业务目录下添加新的Cmd类
2. 实现相应的统计服务接口
3. 配置API接口层调用
4. 更新数据访问层支持