# Service-Fee 费用管理微服务模块

## 模块概述

`service-fee` 是社区管理系统中的核心费用管理微服务模块，采用**命令驱动架构**设计理念，负责处理社区内所有费用相关的业务逻辑。该模块包含23个命令子模块，覆盖费用管理、停车计费、抄表管理、收据管理等核心功能。

## 技术架构

### 命令驱动架构设计

本模块采用命令驱动架构，所有业务操作都通过具体的命令类实现，每个命令类对应一个特定的业务场景，通过 `@Java110Cmd` 注解声明服务代码，实现业务逻辑的解耦和复用。

### 核心架构特点

1. **命令模式实现**：每个业务操作封装为独立的命令类
2. **事务管理**：使用 `@Java110Transactional` 注解确保数据一致性
3. **参数验证**：统一的参数验证机制，确保输入数据的有效性
4. **服务编排**：通过服务接口调用实现业务逻辑的组合

## 模块结构

```
service-fee/
├── src/main/java/com/java110/fee/
│   ├── cmd/                    # 命令处理层（核心）
│   │   ├── carMonth/          # 月租车月卡管理
│   │   ├── fee/               # 费用核心管理
│   │   ├── feeCombo/          # 费用套餐管理
│   │   ├── feeComboMember/    # 费用套餐成员管理
│   │   ├── feeConfig/         # 费用配置管理
│   │   ├── feePrintPage/      # 费用打印页面管理
│   │   ├── feePrintPageTemplate/ # 费用打印模板管理
│   │   ├── feeSub/            # 费用子项管理
│   │   ├── meter/             # 抄表管理
│   │   ├── meterType/         # 表类型管理
│   │   ├── meterWater/        # 水表抄表管理
│   │   ├── oweFeeCallable/    # 欠费管理
│   │   ├── ownerCarNew/       # 业主车辆管理
│   │   ├── payFeeBatch/       # 批量缴费管理
│   │   ├── payFeeDetailDiscountNew/ # 缴费明细折扣管理
│   │   ├── payFeeDetailNew/   # 缴费明细管理
│   │   ├── payFeeQrcode/      # 缴费二维码管理
│   │   ├── payFeeRule/        # 缴费规则管理
│   │   ├── receipt/           # 收据管理
│   │   ├── returnPayFee/      # 退费管理
│   │   ├── tempCarFee/        # 临时停车费管理
│   │   └── tempCarFeeConfigAttr/ # 临时停车费配置属性管理
│   ├── api/                   # API接口层
│   ├── bmo/                   # 业务模型对象
│   ├── dao/                   # 数据访问层
│   └── smo/                   # 服务模型对象
```

## 核心命令接口清单

### 1. 月租车月卡管理 (carMonth)

| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `BuyCarMonthOrderCmd` | `carMonth.buyCarMonthOrder` | 购买月租车月卡订单处理 |
| `QueryAppCarMonthCardCmd` | `carMonth.queryAppCarMonthCard` | 查询停车月卡信息 |

**核心功能**：
- 月卡购买订单处理
- 月卡信息查询
- 车辆信息验证
- 物联网服务集成

### 2. 费用核心管理 (fee) - 44个命令接口

**费用操作类**：
| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `PayFeeCmd` | `fee.payFee` | 前台现金或转账收费处理 |
| `ListFeeCmd` | `fee.listFee` | 费用列表查询 |
| `UpdateFeeCmd` | `fee.updateFee` | 费用信息更新 |
| `DeleteFeeCmd` | `fee.deleteFee` | 费用删除 |
| `FinishFeeCmd` | `fee.finishFee` | 费用完成处理 |

**费用查询类**：
| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `ListAdminFeeCmd` | `fee.listAdminFee` | 管理员费用查询 |
| `ListOwnerOweFeeCmd` | `fee.listOwnerOweFee` | 业主欠费查询 |
| `QueryFeeDetailCmd` | `fee.queryFeeDetail` | 费用明细查询 |
| `ListBillCmd` | `fee.listBill` | 账单列表查询 |

**批量操作类**：
| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `PayBatchFeeCmd` | `fee.payBatchFee` | 批量缴费处理 |
| `PayBatchFeeACmd` | `fee.payBatchFeeA` | 批量缴费处理A |

**费用创建类**：
| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `SaveRoomCreateFeeCmd` | `fee.saveRoomCreateFee` | 保存房屋创建费用 |
| `SaveParkingSpaceCreateFeeCmd` | `fee.saveParkingSpaceCreateFee` | 保存车位创建费用 |

### 3. 费用配置管理 (feeConfig) - 7个命令接口

| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `SaveFeeConfigCmd` | `feeConfig.saveFeeConfig` | 保存费用配置 |
| `UpdateFeeConfigCmd` | `feeConfig.updateFeeConfig` | 更新费用配置 |
| `DeleteFeeConfigCmd` | `feeConfig.deleteFeeConfig` | 删除费用配置 |
| `ListFeeConfigsCmd` | `feeConfig.listFeeConfigs` | 费用配置列表查询 |
| `ListConfigFeeObjsCmd` | `feeConfig.listConfigFeeObjs` | 配置费用对象列表查询 |
| `ListAdminConfigFeeObjsCmd` | `feeConfig.listAdminConfigFeeObjs` | 管理员配置费用对象查询 |
| `QueryAdminFeeConfigsCmd` | `feeConfig.queryAdminFeeConfigs` | 管理员费用配置查询 |

**核心功能**：
- 费用项配置管理
- 计费规则设置
- 费用类型定义
- 支付方式配置

### 4. 费用套餐管理 (feeCombo) - 4个命令接口

| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `SaveFeeComboCmd` | `feeCombo.saveFeeCombo` | 保存费用套餐 |
| `UpdateFeeComboCmd` | `feeCombo.updateFeeCombo` | 更新费用套餐 |
| `DeleteFeeComboCmd` | `feeCombo.deleteFeeCombo` | 删除费用套餐 |
| `ListFeeComboCmd` | `feeCombo.listFeeCombo` | 费用套餐列表查询 |

### 5. 抄表管理 (meter) - 9个命令接口

**楼层分摊抄表**：
| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `SaveFloorShareReadingCmd` | `meter.saveFloorShareReading` | 保存楼层分摊抄表 |
| `AuditFloorShareReadingCmd` | `meter.auditFloorShareReading` | 审核楼层分摊抄表 |
| `ListFloorShareReadingCmd` | `meter.listFloorShareReading` | 楼层分摊抄表列表查询 |
| `DeleteFloorShareReadingCmd` | `meter.deleteFloorShareReading` | 删除楼层分摊抄表 |

**楼层分摊表管理**：
| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `SaveFloorShareMeterCmd` | `meter.saveFloorShareMeter` | 保存楼层分摊表 |
| `UpdateFloorShareMeterCmd` | `meter.updateFloorShareMeter` | 更新楼层分摊表 |
| `DeleteFloorShareMeterCmd` | `meter.deleteFloorShareMeter` | 删除楼层分摊表 |
| `ListFloorShareMeterCmd` | `meter.listFloorShareMeter` | 楼层分摊表列表查询 |

**费用查询**：
| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `ListFloorShareFeeCmd` | `meter.listFloorShareFee` | 楼层分摊费用列表查询 |

### 6. 水表抄表管理 (meterWater) - 6个命令接口

| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `SaveMeterWaterCmd` | `meterWater.saveMeterWater` | 保存水表抄表记录 |
| `SaveProxyFeeCmd` | `meterWater.saveProxyFee` | 保存代收费记录 |
| `UpdateMeterWaterCmd` | `meterWater.updateMeterWater` | 更新水表抄表记录 |
| `DeleteMeterWaterCmd` | `meterWater.deleteMeterWater` | 删除水表抄表记录 |
| `ListMeterWatersCmd` | `meterWater.listMeterWaters` | 水表抄表列表查询 |
| `ListAdminMeterWatersCmd` | `meterWater.listAdminMeterWaters` | 管理员水表抄表查询 |

### 7. 欠费管理 (oweFeeCallable) - 5个命令接口

| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `SaveOweFeeCallableCmd` | `oweFeeCallable.saveOweFeeCallable` | 保存欠费记录 |
| `WriteOweFeeCallableCmd` | `oweFeeCallable.writeOweFeeCallable` | 写入欠费记录 |
| `DeleteOweFeeCallableCmd` | `oweFeeCallable.deleteOweFeeCallable` | 删除欠费记录 |
| `ListOweFeeCallableCmd` | `oweFeeCallable.listOweFeeCallable` | 欠费记录列表查询 |
| `ListAdminOweFeeCallableCmd` | `oweFeeCallable.listAdminOweFeeCallable` | 管理员欠费记录查询 |

### 8. 临时停车费管理 (tempCarFee) - 3个命令接口

| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `GetTempCarFeeOrderCmd` | `tempCarFee.getTempCarFeeOrder` | 获取临时停车费订单 |
| `QueryTempCarFeeOrderCmd` | `tempCarFee.queryTempCarFeeOrder` | 查询临时停车费订单 |
| `NotifyTempCarFeeOrderCmd` | `tempCarFee.notifyTempCarFeeOrder` | 临时停车费订单通知 |

### 9. 收据管理 (receipt) - 4个命令接口

| 命令接口 | 服务代码 | 功能描述 |
|---------|---------|---------|
| `GeneratorReceiptCmd` | `receipt.generatorReceipt` | 生成收据 |
| `ListAccountReceiptCmd` | `receipt.listAccountReceipt` | 账户收据列表查询 |
| `ListAdminAccountReceiptCmd` | `receipt.listAdminAccountReceipt` | 管理员账户收据查询 |
| `QueryAdminFeeReceiptCmd` | `receipt.queryAdminFeeReceipt` | 管理员费用收据查询 |

### 10. 其他重要模块

**费用套餐成员管理 (feeComboMember)**：
- 费用套餐成员CRUD操作
- 套餐成员关系管理

**缴费明细管理 (payFeeDetailNew)**：
- 缴费明细CRUD操作
- 明细状态管理

**退费管理 (returnPayFee)**：
- 退费申请处理
- 退费审核流程

**缴费二维码管理 (payFeeQrcode)**：
- 缴费二维码生成
- 扫码支付处理

## 业务功能分类

### 1. 费用生命周期管理
- **费用创建**：`SaveRoomCreateFeeCmd`, `SaveParkingSpaceCreateFeeCmd`
- **费用查询**：`ListFeeCmd`, `QueryFeeDetailCmd`
- **费用支付**：`PayFeeCmd`, `PayMonthFeeCmd`
- **费用更新**：`UpdateFeeCmd`, `FinishFeeCmd`
- **费用删除**：`DeleteFeeCmd`

### 2. 停车费用管理
- **月租车月卡**：购买、查询、续费
- **临时停车费**：订单生成、查询、通知
- **停车位费用**：车位费用设置、查询

### 3. 抄表费用管理
- **水表抄表**：抄表记录、费用计算
- **楼层分摊**：公共区域费用分摊
- **代收费管理**：代收费用处理

### 4. 收据和账单管理
- **收据生成**：缴费收据生成和管理
- **账单查询**：费用账单列表查询
- **欠费管理**：欠费查询和催缴

## 技术实现特点

### 1. 命令模式实现
```java
@Java110Cmd(serviceCode = "fee.payFee")
public class PayFeeCmd extends Cmd {
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) {
        // 参数验证逻辑
    }
    
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) {
        // 业务逻辑处理
    }
}
```

### 2. 事务管理
- 使用 `@Java110Transactional` 注解确保业务操作的原子性
- 分布式锁机制防止并发问题
- 事务ID统一管理

### 3. 参数验证机制
- 统一的参数验证框架
- 业务规则校验
- 数据完整性检查

### 4. 服务编排
- 通过服务接口调用组合业务逻辑
- 支持服务降级和容错处理
- 异步处理支持

## 部署和配置

### 依赖服务
- `service-user`：用户信息服务
- `service-community`：小区信息服务
- `service-store`：商户信息服务
- 物联网服务：停车系统集成

### 配置说明
- 数据库配置：费用相关表结构
- 缓存配置：费用计算缓存
- 事务配置：分布式事务管理

## 开发规范

### 命令类命名规范
- 操作类型 + 业务对象 + Cmd
- 例如：`SaveFeeConfigCmd`, `ListFeeCmd`

### 服务代码规范
- 模块名.操作名
- 例如：`fee.payFee`, `feeConfig.saveFeeConfig`

### 事务使用规范
- 写操作必须使用 `@Java110Transactional`
- 读操作不使用事务注解
- 复杂业务操作需要分布式锁保护

## 扩展和定制

### 新增费用类型
1. 在 `feeConfig` 模块添加费用配置
2. 在 `fee` 模块添加对应的命令接口
3. 实现费用计算逻辑

### 集成第三方支付
1. 实现支付接口适配器
2. 在支付命令中集成支付逻辑
3. 处理支付回调通知

## 总结

`service-fee` 模块通过命令驱动架构实现了完整的费用管理体系，具备高度的可扩展性和维护性。23个命令子模块覆盖了社区费用管理的各个方面，为社区运营提供了稳定可靠的费用处理能力。