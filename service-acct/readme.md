# Service-Acct 账户服务模块

## 模块概述

Service-Acct 是微社区系统中的核心账户服务模块，采用命令驱动架构设计，专注于账户管理、支付处理、优惠券管理、积分系统和发票管理等核心财务功能。该模块通过20个核心命令接口目录，为系统提供完整的账户和财务处理能力。

## 技术架构

### 命令驱动架构
- **架构模式**: 基于命令模式的设计，每个业务操作对应一个具体的命令类
- **服务标识**: 使用 `@Java110Cmd(serviceCode = "xxx")` 注解标识服务接口
- **统一处理**: 所有命令继承自 `Cmd` 基类，实现统一的参数验证和执行流程

### 核心组件
- **Cmd**: 命令基类，提供统一的验证和执行框架
- **BMO**: 业务模型对象，处理具体业务逻辑
- **SMO**: 服务模型对象，提供数据访问服务
- **DTO/PO**: 数据传输对象和持久化对象

## 核心命令接口说明

### 1. 账户管理模块 (account)
**功能**: 负责业主账户的创建、预存、查询和管理
**核心接口**:
- `OwnerPrestoreAccountCmd`: 业主账户预存，支持账户创建和金额预存
- `QueryOwnerAccountCmd`: 业主账户查询
- `QueryAdminOwnerAccountCmd`: 管理员账户查询
- `QueryAdminAccountDetailCmd`: 管理员账户明细查询
- `QueryCommunityOwnerAccountCmd`: 社区业主账户查询
- `CancelAccountDetailCmd`: 账户明细取消
- `CustomSendAcctCmd`: 自定义发送账户操作
- `DeleteAccountCmd`: 账户删除

### 2. 支付处理模块 (payment)
**功能**: 统一支付处理，支持多种支付方式和支付场景
**核心接口**:
- `UnifiedPaymentCmd`: 统一支付接口，支持微信支付等多种支付方式
- `CashierCmd`: 收银台支付
- `QrCodePaymentCmd`: 二维码支付
- `NativeQrcodePaymentCmd`: 原生二维码支付
- `GetNativeQrcodePaymentCmd`: 获取原生二维码支付
- `CheckPayFinishCmd`: 支付完成检查
- `ListPaymentPoolCmd`: 支付池列表查询
- `ListAdminPaymentCmd`: 管理员支付查询
- `ListPaymentAdaptCmd`: 支付适配器列表
- `ListPaymentKeyCmd`: 支付密钥列表
- `ListPaymentPoolConfigCmd`: 支付池配置列表
- `ListPaymentPoolValueCmd`: 支付池值列表
- `SavePaymentPoolCmd`: 保存支付池
- `SaveAdminPaymentPoolCmd`: 保存管理员支付池
- `UpdatePaymentPoolCmd`: 更新支付池
- `UpdateAdminPaymentPoolCmd`: 更新管理员支付池
- `DeletePaymentPoolCmd`: 删除支付池
- `DeleteAdminPaymentPoolCmd`: 删除管理员支付池

### 3. 积分管理模块 (integral)
**功能**: 积分规则配置、积分计算、积分使用和积分礼品管理
**核心接口**:
- `UseIntegralCmd`: 积分使用
- `ComputePayFeeIntegralCmd`: 费用支付积分计算
- `SaveIntegralRuleCmd`: 积分规则保存
- `SaveIntegralConfigCmd`: 积分配置保存
- `SaveIntegralRuleConfigCmd`: 积分规则配置保存
- `SaveIntegralRuleFeeCmd`: 积分规则费用保存
- `SaveIntegralSettingCmd`: 积分设置保存
- `UpdateIntegralRuleCmd`: 积分规则更新
- `UpdateIntegralConfigCmd`: 积分配置更新
- `UpdateIntegralRuleFeeCmd`: 积分规则费用更新
- `UpdateIntegralSettingCmd`: 积分设置更新
- `DeleteIntegralRuleCmd`: 积分规则删除
- `DeleteIntegralConfigCmd`: 积分配置删除
- `DeleteIntegralRuleConfigCmd`: 积分规则配置删除
- `DeleteIntegralRuleFeeCmd`: 积分规则费用删除
- `DeleteIntegralSettingCmd`: 积分设置删除
- `ListIntegralRuleCmd`: 积分规则列表查询
- `ListIntegralConfigCmd`: 积分配置列表查询
- `ListIntegralRuleConfigCmd`: 积分规则配置列表查询
- `ListIntegralRuleFeeCmd`: 积分规则费用列表查询
- `ListIntegralSettingCmd`: 积分设置列表查询
- `ListIntegralUserDetailCmd`: 用户积分明细查询
- `ListIntegralGiftDetailCmd`: 积分礼品明细查询
- `QueryAppUserIntegralCmd`: 应用用户积分查询

### 4. 优惠券管理模块 (coupon相关)
**功能**: 优惠券全生命周期管理，包括创建、发放、使用和统计

#### 4.1 优惠券池管理 (couponPool)
- `SaveCouponPoolCmd`: 创建优惠券池
- `ListCouponPoolCmd`: 优惠券池列表查询
- `UpdateCouponPoolCmd`: 优惠券池更新
- `DeleteCouponPoolCmd`: 优惠券池删除

#### 4.2 优惠券规则管理 (couponRule)
- 优惠券使用规则配置和管理

#### 4.3 用户优惠券管理 (couponUser)
- `SaveCouponUserCmd`: 用户优惠券发放
- `ListCouponUserCmd`: 用户优惠券列表查询
- `UpdateCouponUserCmd`: 用户优惠券更新
- `DeleteCouponUserCmd`: 用户优惠券删除

#### 4.4 优惠券计算 (coupon)
- `ComputePayFeeCouponCmd`: 费用支付优惠券计算

### 5. 发票管理模块 (invoice)
**功能**: 发票申请、审核、上传和管理
**核心接口**:
- `SaveInvoiceApplyCmd`: 发票申请保存
- `SaveInvoiceApplyItemCmd`: 发票申请项保存
- `SaveOwnerInvoiceCmd`: 业主发票保存
- `AuditInvoiceApplyCmd`: 发票申请审核
- `UploadInvoicePhotoCmd`: 发票照片上传
- `WriteInvoiceApplyCmd`: 发票申请写入
- `UpdateInvoiceApplyItemCmd`: 发票申请项更新
- `UpdateOwnerInvoiceCmd`: 业主发票更新
- `DeleteInvoiceApplyCmd`: 发票申请删除
- `DeleteInvoiceApplyItemCmd`: 发票申请项删除
- `DeleteOwnerInvoiceCmd`: 业主发票删除
- `ListInvoiceApplyCmd`: 发票申请列表查询
- `ListInvoiceApplyItemCmd`: 发票申请项列表查询
- `ListOwnerInvoiceCmd`: 业主发票列表查询
- `ListInvoiceEventCmd`: 发票事件列表查询

### 6. 支付宝支付模块 (alipay)
**功能**: 支付宝支付接口集成
**核心接口**: 支付宝支付相关命令接口

### 7. 在线支付模块 (onlinePay)
**功能**: 在线支付处理
**核心接口**: 在线支付相关命令接口

### 8. 停车优惠券模块 (parkingCoupon)
**功能**: 停车优惠券管理
**核心接口**: 停车优惠券的创建、发放和使用

### 9. 供应商优惠券模块 (supplierCoupon)
**功能**: 供应商优惠券管理
**核心接口**: 供应商优惠券的相关操作

## 业务领域分类

### 账户与支付领域
- **账户管理**: 业主账户创建、预存、查询
- **支付处理**: 统一支付、多种支付方式支持
- **支付池管理**: 支付配置和策略管理

### 营销与促销领域
- **积分系统**: 积分规则、积分使用、积分礼品
- **优惠券系统**: 优惠券池、优惠券规则、用户优惠券
- **停车优惠**: 停车场景优惠券管理

### 财务与票据领域
- **发票管理**: 发票申请、审核、上传
- **账户明细**: 交易记录和明细管理

## 使用示例

### 账户预存示例
```java
// 请求参数
{
  "communityId": "123456",
  "ownerId": "user001", 
  "amount": "100.00",
  "acctType": "property_fee",
  "primeRate": "wechat",
  "remark": "业主账户预存",
  "roomId": "room001"  // 当账户类型为property_fee或meter时需要房屋ID
}

// 调用服务
serviceCode: "account.ownerPrestoreAccount"
```

### 统一支付示例
```java
// 请求参数（以费用支付为例）
{
  "business": "payFee",           // 支付场景：费用支付
  "communityId": "123456",        // 小区ID
  "tradeType": "JSAPI",           // 支付类型：JSAPI（小程序支付）
  "feeId": "fee001",              // 费用ID
  "payAdapt": "wechatPaymentFactory"  // 支付适配器（可选，默认微信支付）
}

// 调用服务  
serviceCode: "payment.unifiedPayment"

// 注意：不同业务类型需要传递不同的参数，具体参考各业务接口文档
```

### 积分使用示例
```java
// 请求参数
{
  "communityId": "123456",
  "userId": "user001",
  "integralAmount": "100",
  "businessType": "shopping"
}

// 调用服务
serviceCode: "integral.useIntegral"
```

## 部署配置

### 依赖配置
```xml
<!-- 核心依赖 -->
<dependency>
  <groupId>com.java110</groupId>
  <artifactId>service-acct</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- 支付相关依赖 -->
<!-- 微信支付SDK -->
<!-- 支付宝支付SDK -->
```

### 环境配置
```properties
# 支付配置
payment.wechat.appid=your_appid
payment.wechat.mchid=your_mchid
payment.wechat.key=your_key

# 积分配置
integral.enabled=true
integral.ratio=1.0
```

## 核心特性

### 1. 命令驱动设计
- 每个业务操作对应一个独立的命令类
- 统一的参数验证和执行流程
- 易于扩展和维护

### 2. 多支付方式支持
- 微信支付、支付宝支付等主流支付方式
- 统一的支付接口，简化接入流程
- 支付池配置，支持不同业务场景

### 3. 完整的账户体系
- 业主账户管理
- 交易明细记录
- 账户余额管理

### 4. 灵活的营销工具
- 积分系统配置
- 优惠券管理
- 多种促销场景支持

## 开发指南

### 新增命令接口
1. 在对应业务目录下创建命令类
2. 继承 `Cmd` 基类并实现 `validate` 和 `doCmd` 方法
3. 使用 `@Java110Cmd` 注解注册服务
4. 在对应的 BMO 中实现业务逻辑

### 支付业务扩展
1. 实现 `IPaymentBusiness` 接口
2. 在 `payment` 目录下添加支付命令
3. 配置支付适配器工厂

### 积分规则配置
1. 使用 `SaveIntegralRuleCmd` 创建积分规则
2. 配置积分计算规则
3. 设置积分使用条件

## 注意事项

1. **支付安全**: 所有支付操作必须经过严格的参数验证
2. **事务管理**: 账户操作需要保证事务一致性
3. **性能优化**: 高频交易场景需要考虑性能优化
4. **日志记录**: 重要财务操作需要完整的日志记录

## 版本历史

- v1.0.0: 初始版本，包含基础账户和支付功能
- v1.1.0: 新增积分系统和优惠券管理
- v1.2.0: 完善发票管理和支付池配置
- v1.3.0: 文档审核通过，修正接口清单准确性，完善命令驱动架构说明

---

