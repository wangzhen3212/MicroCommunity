# Service-Common 模块技术文档

## 模块概述
Service-Common 是 MicroCommunity 微服务架构中的通用服务模块，提供社区管理系统中的通用功能服务。该模块采用命令驱动架构设计，通过35个核心命令接口为系统提供广告管理、区域管理、文件管理、工作流引擎、充电设备管理、门禁密钥管理等通用业务能力。

## 技术架构

### 核心架构
- **框架**: Spring Boot + Spring Cloud
- **架构模式**: 命令驱动架构 (Command-Driven Architecture)
- **数据库**: MySQL + MyBatis
- **工作流引擎**: Activiti 6.0.0
- **文件存储**: 支持本地存储和OSS云存储

### 项目结构
```
service-common/
├── src/main/java/com/java110/common/
│   ├── cmd/                    # 命令接口层 (35个核心接口)
│   ├── activity/               # 活动管理
│   ├── api/                    # API接口
│   ├── bmo/                    # 业务模型对象
│   ├── dao/                    # 数据访问层
│   ├── smo/                    # 服务管理对象
│   ├── thread/                 # 线程管理
│   └── CommonServiceApplicationStart.java
├── .bpmn/                     # 工作流定义文件
└── pom.xml                    # Maven配置
```

### 依赖关系
```xml
<dependencies>
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-service</artifactId>
    </dependency>
    <dependency>
        <groupId>org.activiti</groupId>
        <artifactId>activiti-bpmn-layout</artifactId>
    </dependency>
    <dependency>
        <groupId>org.activiti</groupId>
        <artifactId>activiti-spring-boot-starter-basic</artifactId>
    </dependency>
    <!-- 其他依赖... -->
</dependencies>
```

## 核心命令接口清单

### 1. 广告管理 (advert)
- **SaveAdvertCmd**: 创建广告
  - 功能: 创建新的广告信息，支持图片和视频广告
  - 调用方式: POST /app/advert.saveAdvert
  - 业务场景: 物业管理人员创建社区广告

- **ListAdvertsCmd**: 查询广告列表
  - 功能: 分页查询广告信息，支持按类型筛选
  - 调用方式: POST /app/advert.listAdverts
  - 业务场景: 前端展示广告列表

- **UpdateAdvertCmd**: 更新广告
- **DeleteAdvertCmd**: 删除广告
- **ListAdvertItemsCmd**: 查询广告项
- **ListAdvertPhotoCmd**: 查询广告图片
- **ListAdvertPhotoAndVediosCmd**: 查询广告图片和视频

### 2. 应用密钥管理 (applicationKey)
- **SaveApplicationKeyCmd**: 创建应用密钥
  - 功能: 生成门禁访问密钥，支持临时密码和长期密钥
  - 调用方式: POST /app/applicationKey.saveApplicationKey
  - 业务场景: 业主申请门禁访问权限

- **ApplyApplicationKeyCmd**: 申请应用密钥
- **ApplyVisitorApplicationKeyCmd**: 访客申请密钥
- **AuditApplicationKeyCmd**: 审核应用密钥
- **AuthApplicationKeyCmd**: 认证应用密钥
- **ListApplicationKeysCmd**: 查询密钥列表
- **UpdateApplicationKeyCmd**: 更新密钥信息
- **DeleteApplicationKeyCmd**: 删除密钥

### 3. 区域管理 (area)
- **ListAreas**: 查询区域列表
  - 功能: 获取行政区划信息
  - 调用方式: POST /app/area.listAreas
  - 业务场景: 地址选择、区域统计

- **QueryAreaTreeCmd**: 查询区域树
- **SaveCityAreaCmd**: 保存城市区域
- **UpdateCityAreaCmd**: 更新城市区域
- **DeleteCityAreaCmd**: 删除城市区域

### 4. 考勤管理 (attendanceClasses)
- 提供考勤班次管理功能

### 5. 用户审核 (auditUser)
- 提供用户信息审核功能

### 6. 车辆进出管理 (carInout)
- 提供车辆进出记录管理

### 7. 充电设备管理 (chargeMachine)
- **SaveChargeMachineCmd**: 创建充电设备
- **ListChargeMachinePortCmd**: 查询充电端口
- **ListChargeMachineOrderCmd**: 查询充电订单
- **ListPhoneChargeMachineCmd**: 手机端查询充电设备
- **SaveChargeMachineFactoryCmd**: 创建设备厂商
- **SaveChargeMachinePortCmd**: 创建充电端口
- **SaveChargeMachineSpecCmd**: 创建设备规格
- 其他相关管理接口...

### 8. 充电卡管理 (chargeCard)
- 提供充电卡管理功能

### 9. 充电规则管理 (chargeRule)
- 提供充电计费规则管理

### 10. 订单管理 (corders)
- 提供通用订单管理功能

### 11. 设备台账管理 (equipmentAccount)
- 提供设备资产台账管理

### 12. 设备操作日志 (equipmentOperatingLog)
- 记录设备操作日志

### 13. 文件管理 (file)
- **SaveFileCmd**: 保存文件
  - 功能: 上传并保存文件，支持图片、文档等
  - 调用方式: POST /app/file.saveFile
  - 业务场景: 用户上传头像、证件照等

- **GetFileCmd**: 获取文件
- **GetFileByObjIdCmd**: 按对象ID获取文件

### 14. 物品发布管理 (itemRelease)
- 提供物品发布管理功能

### 15. 日志管理 (log)
- 提供系统日志管理

### 16. 设备管理 (machine)
- 提供通用设备管理功能

### 17. 设备认证 (machineAuth)
- 提供设备认证管理

### 18. 设备记录 (machineRecord)
- 记录设备操作记录

### 19. 设备翻译 (machineTranslate)
- 提供设备数据翻译功能

### 20. 设备类型 (machineType)
- 管理设备类型信息

### 21. 商城管理 (mall)
- 提供商城相关功能

### 22. 营销黑名单 (marketBlacklist)
- 管理营销黑名单

### 23. 营销商品 (marketGoods)
- 管理营销商品信息

### 24. 营销图片 (marketPic)
- 管理营销相关图片

### 25. 营销规则 (marketRule)
- 配置营销活动规则

### 26. 营销短信 (marketSms)
- 管理营销短信发送

### 27. 营销文本 (marketText)
- 管理营销文本内容

### 28. 计量设备 (meterMachine)
- 管理计量设备信息

### 29. 消息管理 (msg)
- 提供消息推送管理

### 30. 打印机管理 (printer)
- 提供打印设备管理

### 31. 宣传管理 (publicity)
- 提供宣传内容管理

### 32. 系统管理 (system)
- 提供系统配置管理

### 33. 工作流管理 (workflow)
- **SaveWorkflowCmd**: 创建工作流
  - 功能: 创建新的工作流定义
  - 调用方式: POST /app/workflow.saveWorkflow
  - 业务场景: 管理员配置业务流程

- **ListWorkflowCmd**: 查询工作流
- **ListWorkflowsCmd**: 查询工作流列表
- **ListWorkflowStepsCmd**: 查询工作流步骤
- **ListWorkflowStepStaffsCmd**: 查询步骤处理人员
- **ListWorkflowAuditInfoCmd**: 查询审核信息
- **ListWorkflowImageCmd**: 查询工作流图片
- **ListRunWorkflowImageCmd**: 查询运行中工作流图片
- **GetFirstStaffCmd**: 获取首节点处理人员
- **ListWorkflowNextNodeCmd**: 查询下一节点
- **UpdateWorkflowCmd**: 更新工作流
- **DeleteWorkflowCmd**: 删除工作流

## 核心业务功能

### 1. 广告投放系统
- 支持图片、视频广告
- 多终端投放（业主端、员工端）
- 精准投放（按小区、楼栋、单元、房间）
- 时间控制（开始时间、结束时间）

### 2. 门禁密钥管理
- 长期密钥和临时密码
- 密钥审核机制
- 设备绑定管理
- 访客权限管理

### 3. 工作流引擎
- 基于Activiti的工作流引擎
- 可视化流程设计
- 多级审核流程
- 动态节点配置

### 4. 充电设备管理
- 充电设备注册管理
- 充电端口配置
- 充电订单处理
- 设备厂商管理

### 5. 文件管理系统
- 多格式文件支持
- 文件关联管理
- 存储方式灵活（本地/云存储）
- 文件权限控制

## 部署配置

### 启动配置
```bash
# Spring Boot 方式启动
mvn spring-boot:run -f pom-boot.xml

# Spring Cloud 方式启动  
mvn spring-boot:run -f pom-cloud.xml
```

### 环境配置
- 开发环境: application-dev.properties
- 测试环境: application-test.properties  
- 生产环境: application-prod.properties

## 接口调用示例

### 创建广告接口调用
```json
POST /app/advert.saveAdvert
{
  "adName": "社区活动通知",
  "adTypeCd": "2000",
  "classify": "9001",
  "locationTypeCd": "1000",
  "locationObjId": "-1",
  "seq": "1",
  "startTime": "2023-09-01 00:00:00",
  "endTime": "2023-12-31 23:59:59",
  "photos": [
    {
      "url": "https://example.com/image.jpg",
      "fileId": "img/20230901/abc123.jpg"
    }
  ]
}
```

### 创建工作流接口调用
```json
POST /app/workflow.saveWorkflow
{
  "flowId": "WF001",
  "flowName": "费用报销流程",
  "skipLevel": "1",
  "communityId": "9999",
  "storeId": "1001",
  "statusCd": "0",
  "flowType": "EXPENSE"
}
```

## 注意事项

1. **事务管理**: 关键业务操作使用@Java110Transactional注解确保数据一致性
2. **参数校验**: 所有接口都包含完整的参数校验逻辑
3. **异常处理**: 统一的异常处理机制，返回标准化的错误信息
4. **权限控制**: 接口调用需要相应的权限认证
5. **日志记录**: 关键操作都有详细的日志记录

## 版本信息
- 当前版本: 1.0-SNAPSHOT
- 最后更新: 2024年1月
- 维护团队: Java110 Team

---
