# Service-Store 模块技术文档

## 模块概述
Service-Store 是微社区系统中的店铺管理微服务模块，采用命令驱动架构设计，提供完整的店铺运营管理功能。本模块包含186个命令接口，覆盖29个业务领域，包括店铺管理、资源调度、投诉处理、预约服务、采购管理、排班管理等核心业务功能。

## 技术架构

### 命令驱动架构
本模块采用命令驱动架构设计，所有业务操作通过cmd目录下的命令接口实现：
- **命令模式**：每个业务操作对应一个具体的命令类，实现高内聚低耦合
- **统一接口**：所有命令类继承自Cmd基类，实现validate()和execute()方法
- **服务标识**：通过@Java110Cmd注解标识服务代码，实现统一路由
- **职责分离**：命令类专注于业务逻辑处理，实现清晰的业务边界

### 核心包结构
```
src/main/java/com/java110/store/
├── cmd/          # 命令接口层（186个核心业务接口）
├── api/          # API接口层
├── bmo/          # 业务模型对象
├── dao/          # 数据访问层
├── rest/         # REST接口
└── smo/          # 服务模型对象
```

## 核心命令接口清单

### 1. 店铺管理 (shop) - 3个接口
- `ApplyShopCodeCmd` - 申请店铺临时票据
- `ListMyEnteredShopsCmd` - 查询用户入驻店铺
- `QueryShopCmd` - 通用店铺查询

### 2. 店铺基础管理 (store) - 8个接口
- `ListStoresCmd` - 查询店铺列表
- `SaveStoreInfoCmd` - 保存店铺信息
- `UpdateStoreInfoCmd` - 更新店铺信息
- `UpdateStoreAttrCmd` - 更新店铺属性
- `QueryStoreByUserCmd` - 根据用户查询店铺
- `ListStoresByCommunityCmd` - 按社区查询店铺
- `ListCommunityStoreShopCmd` - 查询社区店铺
- `PropertySaveStoreAndShopCmd` - 物业保存店铺和商店
- `PropertyDeleteStoreAndShopCmd` - 物业删除店铺和商店

### 3. 投诉管理 (complaint) - 11个接口
- `SaveComplaintCmd` - 保存投诉
- `UpdateComplaintCmd` - 更新投诉
- `DeleteComplaintCmd` - 删除投诉
- `ListComplaintsCmd` - 查询投诉列表
- `ListAdminComplaintsCmd` - 管理员查询投诉列表
- `AuditComplaintCmd` - 审核投诉
- `ListComplaintEventCmd` - 查询投诉事件
- `ListAdminComplaintEventCmd` - 管理员查询投诉事件
- `ListPhoneComplaintEventCmd` - 电话投诉事件查询
- `QueryPhoneComplaintTypeCmd` - 查询电话投诉类型
- `QueryUserComplaintsCmd` - 查询用户投诉

### 4. 投诉类型管理 (complaintType) - 6个接口
- `ListComplaintTypeCmd` - 查询投诉类型
- `SaveComplaintTypeCmd` - 保存投诉类型
- `DeleteComplaintTypeCmd` - 删除投诉类型
- `UpdateComplaintTypeCmd` - 更新投诉类型
- `ListComplaintTypeUserCmd` - 查询用户投诉类型
- `ListAdminComplaintTypeCmd` - 管理员查询投诉类型

### 5. 投诉评价管理 (complaintAppraise) - 5个接口
- `ListComplaintAppraiseCmd` - 查询投诉评价
- `SaveComplaintAppraiseCmd` - 保存投诉评价
- `ReplyComplaintAppraiseCmd` - 回复投诉评价
- `DeleteComplaintAppraiseCmd` - 删除投诉评价
- `ListAdminComplaintAppraiseCmd` - 管理员查询投诉评价

### 6. 资源管理 (resourceStore) - 37个接口
**资源库存管理**
- `SaveResourceStoreCmd` - 保存资源库存
- `UpdateResourceStoreCmd` - 更新资源库存
- `DeleteResourceStoreCmd` - 删除资源库存
- `ListResourceStoresCmd` - 查询资源库存列表
- `ImportResourceStoreCmd` - 导入资源库存
- `ListResourceStoresAndTimesCmd` - 查询资源和时间

**资源规格管理**
- `SaveResourceStoreSpecificationCmd` - 保存资源规格
- `UpdateResourceStoreSpecificationCmd` - 更新资源规格
- `DeleteResourceStoreSpecificationCmd` - 删除资源规格
- `ListResourceStoreSpecificationsCmd` - 查询资源规格列表

**仓库管理**
- `SaveStorehouseCmd` - 保存仓库
- `UpdateStorehouseCmd` - 更新仓库
- `DeleteStorehouseCmd` - 删除仓库
- `ListStorehousesCmd` - 查询仓库列表
- `ListUserStorehousesCmd` - 查询用户仓库

**资源调拨管理**
- `SaveAllocationStorehouseCmd` - 保存调拨申请
- `UpdateAllocationStorehouseCmd` - 更新调拨申请
- `DeleteAllocationStorehouseCmd` - 删除调拨申请
- `ListAllocationStorehousesCmd` - 查询调拨申请列表
- `AuditAllocationStoreOrderCmd` - 审核调拨订单
- `ListAllocationStoreAuditOrdersCmd` - 查询调拨审核订单
- `ListAllocationStoreHisAuditOrdersCmd` - 查询调拨历史审核订单
- `ListAllocationStorehouseApplysCmd` - 查询调拨申请
- `SaveAllocationUserStorehouseCmd` - 保存用户调拨仓库
- `ListAllocationUserStorehousesCmd` - 查询用户调拨仓库
- `AllocationStoreEnterCmd` - 调拨入库

**资源使用记录**
- `SaveResourceStoreUseRecordCmd` - 保存资源使用记录
- `UpdateResourceStoreUseRecordCmd` - 更新资源使用记录
- `DeleteResourceStoreUseRecordCmd` - 删除资源使用记录
- `ListResourceStoreUseRecordsCmd` - 查询资源使用记录
- `ListAdminStoreUseRecordsCmd` - 管理员查询使用记录

**资源审核流程**
- `SaveResourceAuditFlowCmd` - 保存资源审核流程
- `UpdateResourceAuditFlowCmd` - 更新资源审核流程
- `DeleteResourceAuditFlowCmd` - 删除资源审核流程
- `ListResourceAuditFlowCmd` - 查询资源审核流程

**资源类型管理**
- `ListResourceStoreTypesCmd` - 查询资源类型
- `ListResourceStoreTypeTreeCmd` - 查询资源类型树

### 7. 资源类型管理 (resourceStoreType) - 3个接口
- `SaveResourceStoreTypeCmd` - 保存资源类型
- `UpdateResourceStoreTypeCmd` - 更新资源类型
- `DeleteResourceStoreTypeCmd` - 删除资源类型

### 8. 资源时间管理 (resourceStoreTimes) - 4个接口
- `SaveResourceStoreTimesCmd` - 保存资源时间
- `UpdateResourceStoreTimesCmd` - 更新资源时间
- `DeleteResourceStoreTimesCmd` - 删除资源时间
- `ListResourceStoreTimesCmd` - 查询资源时间

### 9. 资源供应商管理 (resourceSupplier) - 4个接口
- `SaveResourceSupplierCmd` - 保存资源供应商
- `UpdateResourceSupplierCmd` - 更新资源供应商
- `DeleteResourceSupplierCmd` - 删除资源供应商
- `ListResourceSuppliersCmd` - 查询资源供应商

### 10. 预约管理 (reserve) - 13个接口
**预约目录管理**
- `SaveReserveCatalogCmd` - 保存预约目录
- `UpdateReserveCatalogCmd` - 更新预约目录
- `DeleteReserveCatalogCmd` - 删除预约目录
- `ListReserveCatalogCmd` - 查询预约目录

**预约商品管理**
- `SaveReserveGoodsCmd` - 保存预约商品
- `UpdateReserveGoodsCmd` - 更新预约商品
- `DeleteReserveGoodsCmd` - 删除预约商品
- `ListReserveGoodsCmd` - 查询预约商品

**预约参数管理**
- `SaveReserveParamsCmd` - 保存预约参数
- `UpdateReserveParamsCmd` - 更新预约参数
- `DeleteReserveParamsCmd` - 删除预约参数
- `ListReserveParamsCmd` - 查询预约参数
- `UpdateReserveParamsOpenTimeCmd` - 更新预约开放时间

### 11. 预约订单管理 (reserveOrder) - 7个接口
- `DoDiningCmd` - 用餐操作
- `ListReserveGoodsOrderCmd` - 查询预约商品订单
- `SaveReserveGoodsOrderCmd` - 保存预约商品订单
- `DeleteReserveGoodsPersonCmd` - 删除预约人员
- `ListMyReserveGoodsOrderTimeCmd` - 查询我的预约时间
- `ListReserveGoodsConfirmOrderCmd` - 查询预约确认订单
- `SaveReserveGoodsConfirmOrderCmd` - 保存预约确认订单

### 12. 采购管理 (purchase) - 4个接口
- `PurchaseApplyCmd` - 采购申请
- `PurchaseStorageCmd` - 采购入库
- `ResourceEnterCmd` - 资源入库
- `UrgentPurchaseApplyCmd` - 紧急采购申请

### 13. 采购申请管理 (purchaseApply) - 6个接口
- `AuditApplyOrderCmd` - 审核申请订单
- `SavePurchaseApplyCmd` - 保存采购申请
- `ListPurchaseApplysCmd` - 查询采购申请
- `DeletePurchaseApplyCmd` - 删除采购申请
- `UpdatePurchaseApplyCmd` - 更新采购申请
- `ListAdminPurchaseApplysCmd` - 管理员查询采购申请

### 14. 采购申请详情管理 (purchaseApplyDetail) - 1个接口
- `ListPurchaseApplyDetailsCmd` - 查询采购申请详情

### 15. 排班管理 (scheduleClasses) - 11个接口
**班次管理**
- `SaveScheduleClassesCmd` - 保存班次
- `UpdateScheduleClassesCmd` - 更新班次
- `DeleteScheduleClassesCmd` - 删除班次
- `ListScheduleClassesCmd` - 查询班次列表
- `UpdateScheduleClassesStateCmd` - 更新班次状态

**班次人员管理**
- `SaveScheduleClassesStaffCmd` - 保存班次人员
- `UpdateScheduleClassesStaffCmd` - 更新班次人员
- `DeleteScheduleClassesStaffCmd` - 删除班次人员
- `ListScheduleClassesStaffCmd` - 查询班次人员

**排班查询**
- `ListScheduleClassesDayCmd` - 查询每日排班
- `StaffMonthScheduleClassesCmd` - 员工月度排班
- `StaffAdminMonthScheduleClassesCmd` - 管理员月度排班

### 16. 班次管理 (classes) - 5个接口
- `ListClassesCmd` - 查询班次
- `SaveClassesCmd` - 保存班次
- `DeleteClassesCmd` - 删除班次
- `UpdateClassesCmd` - 更新班次
- `UpdateClassesStateCmd` - 更新班次状态

### 17. 合同管理 (contract) - 4个接口
- `QueryContractCmd` - 查询合同
- `QueryAdminContractCmd` - 管理员查询合同
- `QueryAdminOwnerContractCmd` - 管理员查询业主合同
- `QueryContractByRoomIdCmd` - 根据房间ID查询合同

### 18. 资产盘点管理 (assetInventory) - 4个接口
- `SaveAssetInventoryCmd` - 保存资产盘点
- `UpdateAssetInventoryCmd` - 更新资产盘点
- `DeleteAssetInventoryCmd` - 删除资产盘点
- `ListAssetInventoryCmd` - 查询资产盘点列表

### 19. 资产盘点详情管理 (assetInventoryDetail) - 5个接口
- `ListAssetInventoryDetailCmd` - 查询资产盘点详情
- `SaveAssetInventoryDetailCmd` - 保存资产盘点详情
- `ListAssetInventoryDetailsCmd` - 查询资产盘点详情列表
- `DeleteAssetInventoryDetailCmd` - 删除资产盘点详情
- `UpdateAssetInventoryDetailCmd` - 更新资产盘点详情

### 20. 收藏管理 (collection) - 3个接口
- `GoodsCollectionCmd` - 商品收藏
- `ResourceOutCmd` - 资源出库
- `GetCollectionAuditOrderCmd` - 获取收藏审核订单

### 21. 微信管理 (smallWechat) - 10个接口
- `SaveSmallWechatCmd` - 保存微信配置
- `UpdateSmallWechatCmd` - 更新微信配置
- `DeleteSmallWeChatCmd` - 删除微信配置
- `ListSmallWeChatsCmd` - 查询微信配置列表
- `ListAdminSmallWeChatsCmd` - 管理员查询微信配置
- `SaveAdminSmallWechatCmd` - 管理员保存微信配置
- `UpdateAdminSmallWechatCmd` - 管理员更新微信配置
- `ListSmallWechatAttrsCmd` - 查询微信属性
- `UpdateSmallWechatAttrCmd` - 更新微信属性
- `ListSmallWeChatOnlyAppIdCmd` - 仅查询AppId

### 22. 微信属性管理 (smallWechatAttr) - 3个接口
- `ListSmallWechatAttrCmd` - 查询微信属性
- `SaveSmallWechatAttrCmd` - 保存微信属性
- `DeleteSmallWechatAttrCmd` - 删除微信属性

### 23. 微信菜单管理 (wechatMenu) - 5个接口
- `SaveWechatMenuCmd` - 保存微信菜单
- `ListWechatMenusCmd` - 查询微信菜单
- `DeleteWechatMenuCmd` - 删除微信菜单
- `UpdateWechatMenuCmd` - 更新微信菜单
- `PublishWechatMenuCmd` - 发布微信菜单

### 24. 店铺用户管理 (storeUser) - 7个接口
- `ListStoreUserCmd` - 查询店铺用户
- `SaveStoreUserCmd` - 保存店铺用户
- `DeleteStoreUserCmd` - 删除店铺用户
- `UpdateStoreUserCmd` - 更新店铺用户
- `ListAdminStoreUserCmd` - 管理员查询店铺用户
- `ListStoreStaffLeaderCmd` - 查询店铺员工领导
- `SaveStoreStaffLeaderCmd` - 保存店铺员工领导
- `DeleteStoreStaffLeaderCmd` - 删除店铺员工领导

### 25. 店铺员工领导管理 (storeStaffLeader) - 已包含在storeUser中

### 26. 组织员工关系管理 (orgStaffRel) - 4个接口
- `ListOrgStaffRelCmd` - 查询组织员工关系
- `SaveOrgStaffRelCmd` - 保存组织员工关系
- `DeleteOrgStaffRelCmd` - 删除组织员工关系
- `UpdateOrgStaffRelCmd` - 更新组织员工关系

### 27. 物业属性管理 (property) - 7个接口
- `ListPropertyCmd` - 查询物业
- `SavePropertyCmd` - 保存物业
- `DeletePropertyCmd` - 删除物业
- `UpdatePropertyCmd` - 更新物业
- `MemberJoinCommunityCmd` - 成员加入社区
- `MemberQuitCommunityCmd` - 成员退出社区
- `CheckPropertyStaffHasPropertyCmd` - 检查物业员工是否有物业

### 28. 物业指标管理 (propertyIndex) - 4个接口
- `QueryRepairIndexCmd` - 查询维修指标
- `QueryComplaintIndexCmd` - 查询投诉指标
- `QueryOwnerRegisterIndexCmd` - 查询业主注册指标
- `QueryPropertyAssetsIndexCmd` - 查询物业资产指标

## 部署配置

### 依赖关系
```xml
<!-- 查看 pom.xml 获取详细依赖配置 -->
```

### 启动配置
```bash
# 启动服务
java -jar service-store.jar
```

## 开发指南

### 命令接口开发规范
1. **命名规范**：命令类命名采用"动词+名词+Cmd"格式
2. **接口实现**：每个命令类必须实现validate()和execute()方法
3. **单一职责**：每个命令类专注于单一业务职责
4. **异常处理**：统一使用CmdException进行异常处理
5. **服务标识**：使用@Java110Cmd注解标识服务代码

### 接口调用方式
通过统一的命令分发器调用各命令接口，确保接口调用的统一性和可维护性。

## 版本历史
- v1.0.0 - 初始版本，包含186个核心命令接口
- 采用命令驱动架构设计，提升系统可扩展性

---
*最后更新: 2024年*
*总接口数: 186个*
*业务领域: 29个*