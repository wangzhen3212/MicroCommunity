# java110-bean 模块说明文档

## 模块概述

`java110-bean` 是 MicroCommunity 项目的数据实体模块，负责管理系统中所有的数据实体对象。该模块采用分层架构设计，将数据对象按照不同的使用场景和职责进行划分，确保数据在不同层次间的安全性和一致性。

## 项目信息

- **项目类型**: Maven 项目
- **ArtifactId**: `java110-bean`
- **父项目**: `MicroCommunity` (com.java110:1.0-SNAPSHOT)
- **打包方式**: jar
- **主要依赖**: 
  - `fastjson` - 阿里巴巴的JSON处理库

## 包结构说明

### 1. DTO (Data Transfer Object) 包
- **位置**: `com.java110.dto`
- **子包数量**: 100个
- **主要用途**: 数据传输对象，主要用于查询操作
- **特点**:
  - 字段不需要与数据库字段一一对应
  - 支持复杂查询结果的封装
  - 继承自基础类 `Dto`（实现 `Serializable` 接口）

**主要子包示例**:
- `accessControl` - 门禁控制相关DTO
- `account` - 账户管理相关DTO  
- `activities` - 活动管理相关DTO
- `advert` - 广告管理相关DTO
- `fee` - 费用管理相关DTO
- `user` - 用户管理相关DTO
- ...等100个业务领域子包

### 2. PO (Persistent Object) 包
- **位置**: `com.java110.po`
- **子包数量**: 86个
- **主要用途**: 持久化对象，主要用于数据库增删改操作
- **特点**:
  - 字段与数据库表字段一一对应
  - 直接映射数据库表结构
  - 用于ORM框架操作

**主要子包示例**:
- `accessControl` - 门禁控制相关PO
- `account` - 账户管理相关PO
- `activities` - 活动管理相关PO
- `fee` - 费用管理相关PO
- `user` - 用户管理相关PO
- ...等86个业务领域子包

### 3. VO (View Object) 包
- **位置**: `com.java110.vo`
- **子包数量**: 1个（api子包） + 7个核心类
- **主要用途**: 视图对象，主要用于前端数据返回
- **特点**:
  - 控制数据安全性，选择性返回前端所需字段
  - 防止敏感信息泄露（如用户密码）
  - 统一的接口输出协议

**核心类**:
- `ResultVo` - 统一的接口返回结果封装类
- `Vo` - VO基础类
- `FeeDetailResultVo` - 费用明细返回VO
- `FloorVo` - 楼层信息VO
- `MachineTaskVo` - 设备任务VO
- `MorePageVo` - 分页VO
- `api`子包 - API接口专用VO

## 包关系与数据流转

### 数据流转流程
1. **数据库操作层** ↔ **PO对象**：直接与数据库交互
2. **业务逻辑层** ↔ **DTO对象**：处理复杂查询和业务逻辑
3. **接口层** ↔ **VO对象**：返回给前端的安全数据

### 职责分离
- **PO**: 专注于数据持久化，与数据库表结构强关联
- **DTO**: 专注于业务数据传输，支持复杂查询结果封装
- **VO**: 专注于前端展示，确保数据安全性

## 核心类说明

### ResultVo 类
作为统一的接口返回封装类，提供：
- 标准化的响应码和消息定义
- 分页数据支持
- 数据对象封装
- 便捷的静态方法创建响应实体

**常用静态方法**:
```java
ResultVo.success(); // 成功响应
ResultVo.error("错误信息"); // 错误响应
ResultVo.createResponseEntity(data); // 创建响应实体
```

### Dto 基础类
所有DTO对象的基类，实现 `Serializable` 接口，支持序列化。

## 使用示例

### 查询场景
```java
// 使用DTO进行复杂查询
UserDto userDto = userService.queryUser(userQueryDto);

// 转换为VO返回前端
UserVo userVo = convertToVo(userDto);
return ResultVo.createResponseEntity(userVo);
```

### 数据操作场景
```java
// 使用PO进行数据库操作
UserPo userPo = new UserPo();
userPo.setUserName("test");
userMapper.insert(userPo);
```

## 注意事项

1. **数据安全**: VO对象必须过滤敏感信息，确保不返回密码等敏感字段
2. **职责明确**: 各包对象应严格遵循其设计用途，避免混用
3. **包依赖**: 建议遵循 PO → DTO → VO 的单向依赖关系
4. **序列化**: 所有DTO对象都支持序列化，便于网络传输

## 版本信息

- **创建时间**: 2020-12-13
- **维护者**: java110 team
- **许可证**: Apache License 2.0

---

*本文档基于模块实际代码结构分析生成，反映了当前模块的设计理念和实现方式。*