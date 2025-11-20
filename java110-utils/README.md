# java110-utils 模块技术文档

## 模块概述

java110-utils 是 MicroCommunity 微服务社区平台的核心工具模块，为整个系统提供统一的工具类支持。该模块封装了系统开发中常用的工具方法、缓存管理、消息队列、异常处理等基础功能，是平台架构的技术支撑层。

## 技术架构

### 架构设计理念
- **统一工具层**：提供标准化的工具方法，避免代码重复
- **功能模块化**：按功能领域划分工具包，职责清晰
- **依赖解耦**：通过工具类封装第三方依赖，降低业务代码耦合度
- **异常统一**：提供完整的异常处理体系，规范错误处理

### 核心功能模块
1. **基础工具类** (util包)：字符串处理、日期转换、数据验证等
2. **缓存管理** (cache包)：多级缓存支持，包括Redis、Ehcache等
3. **消息队列** (kafka包)：异步消息处理支持
4. **异常处理** (exception包)：统一的异常分类和处理机制
5. **工厂模式** (factory包)：对象创建和管理
6. **并发控制** (lock包)：分布式锁支持
7. **日志管理** (log包)：统一的日志处理
8. **命名空间** (namespace包)：资源命名管理
9. **常量定义** (constant包)：系统常量统一管理

## 依赖说明

### 核心依赖
- **java110-bean**：实体对象支持
- **java110-interface**：接口定义支持

### 第三方工具依赖
- **文件存储**：
  - `aliyun-sdk-oss`：阿里云对象存储
  - `cos_api`：腾讯云对象存储
- **数据处理**：
  - `commons-codec`：编码解码工具
  - `commons-lang3`：Apache通用工具库
  - `commons-validator`：数据验证工具
  - `commons-beanutils`：Bean操作工具
- **缓存支持**：
  - `jedis`：Redis客户端
  - `ehcache`：本地缓存
- **消息队列**：
  - `spring-kafka`：Kafka消息队列
- **安全认证**：
  - `java-jwt`：JWT令牌处理
- **文档处理**：
  - `poi`, `poi-ooxml`：Excel文件处理
- **工具扩展**：
  - `pinyin4j`：拼音转换
  - `dom4j`：XML处理
  - `commons-net`：网络工具

### 日志框架
- `slf4j-api`：日志门面
- `logback-core`, `logback-classic`：日志实现

## 包结构说明

### 1. util包 (39个工具类)
核心工具类集合，涵盖系统开发各个方面的工具方法：
- **数据转换**：`BeanConvertUtil`, `ApacheBeanConvertUtil`, `Java110Converter`
- **字符串处理**：`StringUtil`, `Java110StringConvert`, `PinYinUtil`
- **加密解密**：`Base64Convert`, `TokenUtil`
- **文件处理**：`OSSUtil`, `COSUtil`, `ImportExcelUtils`
- **日期处理**：`DateUtil`
- **数值处理**：`MoneyUtil`, `NumberUtil`, `Money2ChineseUtil`
- **验证工具**：`Assert`, `ValidatorUtil`
- **缓存工具**：`CacheUtil`
- **支付工具**：`PayUtil`
- **协议处理**：`ProtocolUtil`, `WebServiceAxisClient`

### 2. cache包 (12个缓存类)
多级缓存管理，支持本地缓存和分布式缓存：
- `BaseCache`：缓存基类
- `Jedis`：Redis缓存操作
- `CommonCache`：通用缓存
- `JWTCache`：JWT令牌缓存
- `MappingCache`：映射关系缓存
- `PrivilegeCache`：权限缓存
- `ServiceBusinessCache`：服务业务缓存
- `UrlCache`：URL路径缓存

### 3. exception包 (23个异常类)
完整的异常处理体系：
- `BusinessException`：业务异常
- `DAOException`：数据访问异常
- `RuleException`：规则异常
- `SMOException`：服务管理异常
- `NoAuthorityException`：无权限异常
- `Result`：统一返回结果封装

### 4. 其他功能包
- **kafka包**：`KafkaFactory` - 消息队列工厂
- **factory包**：对象工厂模式实现
- **lock包**：分布式锁支持
- **log包**：日志处理工具
- **namespace包**：命名空间管理
- **constant包**：系统常量定义

## 功能分类

### 1. 数据处理工具
- 对象转换：Bean转换、JSON序列化
- 字符串处理：校验、加密、拼音转换
- 数值计算：金额处理、数字转换

### 2. 文件存储工具
- 云存储：阿里云OSS、腾讯云COS
- 文档处理：Excel导入导出
- 图片处理：图片工具类

### 3. 缓存管理工具
- 本地缓存：Ehcache集成
- 分布式缓存：Redis客户端
- 缓存策略：多级缓存支持

### 4. 消息队列工具
- Kafka集成：消息生产消费
- 异步处理：事件驱动支持

### 5. 安全认证工具
- JWT令牌：生成验证
- 权限控制：访问权限管理
- 数据加密：Base64编码解码

### 6. 系统工具
- 日期时间：日期格式化
- 网络通信：HTTP客户端、WebService
- 配置管理：系统配置工具

## 使用指南

### 1. 基础工具使用
```java
// 字符串处理
StringUtil.isEmpty(str);
StringUtil.isNotEmpty(str);

// 对象转换
BeanConvertUtil.covertBean(source, targetClass);

// 日期处理
DateUtil.getCurrentDate();
DateUtil.formatDate(date, pattern);
```

### 2. 缓存使用
```java
// 获取缓存值
CommonCache.getValue(cacheKey);

// 设置缓存
CommonCache.setValue(cacheKey, value, expireTime);
```

### 3. 异常处理
```java
try {
    // 业务逻辑
} catch (BusinessException e) {
    // 业务异常处理
} catch (DAOException e) {
    // 数据访问异常处理
}
```

### 4. 消息队列
```java
// 发送消息
KafkaFactory.sendKafkaMessage(topic, message);
```

## 最佳实践

1. **工具类选择**：根据具体场景选择合适的工具类
2. **异常处理**：使用统一的异常处理机制
3. **缓存策略**：合理设置缓存过期时间
4. **资源释放**：及时释放文件、网络等资源
5. **线程安全**：注意工具类的线程安全性

## 版本信息

- **当前版本**：1.0-SNAPSHOT
- **依赖版本**：与父项目 MicroCommunity 保持一致
- **更新日期**：文档生成日期

---
