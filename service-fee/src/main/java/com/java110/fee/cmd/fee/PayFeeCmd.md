# PayFeeCmd 技术文档

## 1. 概述

PayFeeCmd 是费用支付系统的核心命令类，负责处理前台现金或转账收费的缴费业务。该类采用命令模式设计，通过 `@Java110Cmd(serviceCode = "fee.payFee")` 注解标识，支持多种支付场景和复杂的业务逻辑处理。

### 主要功能
- 参数验证和业务规则校验
- 多种支付场景处理（正常缴费、自定义金额、自定义时间段等）
- 折扣规则处理（赠送、打折、抵扣、滞纳金）
- 用户账户抵扣处理
- 分布式事务管理
- 并发控制
- 多业务系统通知

## 2. 整体架构设计

### 2.1 架构模式
- **命令模式**：继承 `Cmd` 基类，统一命令执行接口
- **依赖注入**：通过 `@Autowired` 注入所有依赖服务
- **AOP切面**：统一的事务管理和异常处理
- **微服务架构**：分布式服务间调用

### 2.2 核心依赖服务
```java
// 费用相关服务
@Autowired private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;
@Autowired private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

// 账户服务
@Autowired private IAccountInnerServiceSMO accountInnerServiceSMOImpl;
@Autowired private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

// 业务通知服务
@Autowired private IFinishFeeNotify finishFeeNotifyImpl;

// 计算服务
@Autowired private IComputeFeeSMO computeFeeSMOImpl;
```

## 3. 核心业务流程

### 3.1 参数验证流程 (validate方法)
1. **必填参数校验**：communityId、cycles、receivedAmount、feeId
2. **费用状态校验**：确保费用未结束
3. **周期合法性校验**：缴费周期不超过费用项结束时间
4. **折扣规则校验**：优惠ID和金额验证
5. **自定义时间段校验**：费用项配置验证

### 3.2 支付处理流程 (doCmd方法)
1. **分布式锁获取**：防止并发支付
2. **费用明细生成**：计算应收金额和结束时间
3. **折扣规则处理**：赠送月份、打折、抵扣等
4. **用户账户抵扣**：现金账户金额计算
5. **数据持久化**：保存费用明细和更新费用状态
6. **自定义时间段处理**：生成欠费记录
7. **账户扣款处理**：实际资金扣减
8. **业务系统通知**：停车、报修、房屋等系统状态更新

## 4. 关键技术实现

### 4.1 分布式锁机制
```java
// 锁键构造：类名 + 费用ID
String key = this.getClass().getSimpleName() + paramObj.get("feeId");
String requestId = DistributedLock.getLockUUID();

try {
    DistributedLock.waitGetDistributedLock(key, requestId);
    // 业务处理
} finally {
    DistributedLock.releaseDistributedLock(key, requestId);
}
```

**实现原理**：
- 基于Redis的SET NX PX命令
- 使用UUID作为请求标识，防止误删
- 自旋等待机制获取锁
- Lua脚本原子性释放锁

### 4.2 事务管理机制
```java
@Override
@Java110Transactional
public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject paramObj) 
```

**事务特点**：
- **分布式事务**：通过订单服务协调全局事务
- **事务传播**：支持创建者和观察者角色
- **异常回滚**：任何异常触发事务回滚
- **AOP管理**：统一的事务边界控制

### 4.3 多种支付场景

#### 4.3.1 正常周期缴费
- **识别**：cycles为数字字符串
- **计算**：应收金额 = 费用单价 × 周期数
- **结束时间**：根据周期数计算

#### 4.3.2 自定义金额缴费
- **识别**：cycles为"custom_amount"
- **计算**：周期数 = 实收金额 ÷ 费用单价
- **反算逻辑**：根据金额反算缴费周期

#### 4.3.3 自定义时间段缴费
- **识别**：cycles为"-105"
- **处理**：生成欠费记录和新的费用项
- **特殊逻辑**：费用项配置转换

### 4.4 折扣规则处理

#### 4.4.1 赠送月份
- **规则ID**：102020008
- **规格ID**：89002020980015
- **处理**：在结束时间基础上增加月份，计算赠送金额

#### 4.4.2 打折规则
- **类型**：discount/apply_discount
- **处理**：累加到discountAmount字段

#### 4.4.3 抵扣规则
- **类型**：deduction/apply_deduction
- **处理**：累加到deductionAmount字段

#### 4.4.4 滞纳金
- **类型**：late
- **处理**：累加到lateAmount字段

### 4.5 费用计算算法

#### 4.5.1 应收金额计算
```java
// 基础公式
BigDecimal feePrice = new BigDecimal(feePriceAll.get("feePrice").toString());
double tmpReceivableAmount = cycles.multiply(feePrice).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
```

#### 4.5.2 账户抵扣算法
```java
// 金额比较逻辑
int flag = amount.compareTo(receivedAmount);
if (flag == 1) { // 账户余额大于实收金额
    redepositAmount = receivedAmount;
} else if (flag == -1) { // 账户余额小于实收金额
    redepositAmount = amount;
}
```

#### 4.5.3 精度控制
- 使用BigDecimal进行精确计算
- setScale(2, BigDecimal.ROUND_HALF_EVEN) 四舍五入到2位小数
- 避免浮点数精度问题

## 5. 服务交互关系

### 5.1 费用服务集群
- **IPayFeeV1InnerServiceSMO**：费用主表操作
- **IFeeInnerServiceSMO**：费用信息查询
- **IPayFeeDetailV1InnerServiceSMO**：费用明细操作
- **IFeeConfigInnerServiceSMO**：费用配置查询

### 5.2 账户服务
- **IAccountInnerServiceSMO**：账户余额查询和扣款
- **IFeeAccountDetailServiceSMO**：费用账户抵扣明细

### 5.3 业务通知服务
- **IFinishFeeNotify**：统一的通知接口
  - `updateCarEndTime()`：停车结束时间更新
  - `updateRepair()`：报修单状态更新
  - `updateRoomEndTime()`：房屋结束时间更新

### 5.4 计算服务
- **IComputeFeeSMO**：核心计算逻辑
  - `getFeePrice()`：费用单价计算
  - `getFeeEndTimeByCycles()`：结束时间计算
  - `dealRentRateCycle()`：租金递增处理

## 6. 异常处理机制

### 6.1 异常分类
- **CmdException**：业务逻辑异常
- **IllegalArgumentException**：参数验证异常
- **UnsupportedOperationException**：不支持操作异常
- **Throwable**：系统级异常

### 6.2 处理策略
- **参数层**：前置条件验证，快速失败
- **业务层**：业务规则校验，明确错误提示
- **数据层**：操作结果验证，事务回滚
- **系统层**：基础设施异常，日志记录

### 6.3 事务回滚
```java
try {
    o = pjp.proceed();
    Java110TransactionalFactory.finishOId(); // 提交
} catch (Throwable e) {
    Java110TransactionalFactory.fallbackOId(); // 回滚
    throw e;
}
```

## 7. 设计模式应用

### 7.1 命令模式
- **抽象命令**：Cmd基类
- **具体命令**：PayFeeCmd实现
- **统一接口**：validate() + doCmd()

### 7.2 策略模式
- **支付策略**：不同支付场景的不同计算策略
- **折扣策略**：不同类型折扣的不同处理逻辑

### 7.3 观察者模式
- **主题**：费用支付完成事件
- **观察者**：停车、报修、房屋等业务系统
- **通知机制**：统一的FinishFeeNotify接口

### 7.4 工厂模式
- **ID生成**：GenerateCodeFactory
- **对象转换**：BeanConvertUtil

## 8. 代码结构说明

### 8.1 类结构
```java
public class PayFeeCmd extends Cmd {
    // 依赖注入服务（27个服务）
    // 核心方法
    public void validate(...)
    public void doCmd(...)
    // 辅助方法
    private void hasDiscount(...)
    private JSONObject addFeeDetail(...)
    private void dealUserAccount(...)
    // ... 其他私有方法
}
```

### 8.2 方法职责
- **validate()**：参数验证和业务规则校验
- **doCmd()**：核心业务逻辑处理
- **addFeeDetail()**：费用明细生成
- **modifyFee()**：费用信息更新
- **hasDiscount()**：折扣规则处理
- **dealUserAccount()**：用户账户抵扣
- **dealAccount()**：账户扣款处理

### 8.3 代码特点
- **单一职责**：每个方法功能明确
- **高内聚**：相关功能集中处理
- **松耦合**：通过接口依赖降低耦合
- **可测试**：依赖注入便于单元测试

## 9. 性能与可靠性

### 9.1 并发控制
- 分布式锁防止数据竞争
- 事务隔离保证数据一致性
- 乐观锁机制避免死锁

### 9.2 数据一致性
- 分布式事务保证多服务数据一致
- 数据库事务保证单服务数据原子性
- 最终一致性通过业务补偿机制保证

### 9.3 容错设计
- 完善的异常处理机制
- 资源释放保证（finally块）
- 服务降级和熔断机制

## 10. 扩展性设计

### 10.1 支付场景扩展
- 通过参数化设计支持新场景
- 策略模式便于添加新计算逻辑

### 10.2 折扣规则扩展
- 配置化的折扣规则管理
- 灵活的折扣类型支持

### 10.3 业务通知扩展
- 观察者模式支持新业务系统接入
- 统一的通知接口便于扩展

---

**文档版本**: 1.0  
**最后更新**: 2024年1月  
**维护人员**: 系统分析团队