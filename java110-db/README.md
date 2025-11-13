# java110-db 模块技术文档

## 项目概述

`java110-db` 是 MicroCommunity 项目的数据库基础模块，负责整个系统的数据访问层配置和管理。该模块集成了 MyBatis、Druid 连接池等主流数据库技术，提供了统一的数据源配置、SQL监控、事务管理等功能。

## 模块说明

### 核心功能
- **数据源配置管理**：基于 Druid 连接池的数据源配置
- **MyBatis 集成**：完整的 MyBatis 配置和 Mapper 扫描
- **SQL 监控**：集成 Druid 监控和 SQL 执行日志
- **事务管理**：基于 Spring 的事务管理配置
- **SQL 拦截器**：自定义 MyBatis 拦截器实现事务日志记录

### 技术架构
- **Java 版本**：JDK 1.8
- **构建工具**：Maven
- **数据库框架**：MyBatis + MyBatis Spring Boot Starter
- **连接池**：Alibaba Druid
- **事务管理**：Spring Transaction Management

## 目录结构

```
java110-db/
├── src/main/java/com/java110/db/
│   ├── dao/                          # 数据访问层
│   │   ├── impl/
│   │   │   └── QueryServiceDAOImpl.java    # 查询服务实现类
│   │   └── IQueryServiceDAO.java           # 查询服务接口
│   ├── druid/                        # Druid 配置
│   │   ├── DruidConfiguration.java         # Druid Servlet 配置
│   │   └── DuridConfig.java                # Druid 过滤器配置
│   ├── BeanInitCostTimeBeanPostProcessor.java.txt
│   ├── DruidDataSourceConfig.java          # 数据源配置类
│   ├── Java110MybatisInterceptor.java      # MyBatis 拦截器
│   ├── MyBatisConfig.java                  # MyBatis 主配置
│   ├── MyBatisMapperScannerConfig.java     # Mapper 扫描配置
│   └── SharejdbcDataSourceConfig.java.txt  # 分库分表配置（暂未启用）
├── pom.xml                           # Maven 依赖配置
└── README.md                         # 模块说明文档
```

## 核心组件说明

### 1. 数据源配置 (DruidDataSourceConfig)
- 基于 Alibaba Druid 连接池的数据源配置
- 支持连接池参数配置：初始大小、最小空闲连接、最大活跃连接等
- 集成 SQL 监控和日志打印功能
- 配置 Log4j2 作为日志实现

### 2. MyBatis 配置 (MyBatisConfig)
- 配置 SqlSessionFactory 和 SqlSessionTemplate
- 集成自定义拦截器：`Java110MybatisInterceptor` 和 `Java110TraceSqlInterceptor`
- 支持动态 Mapper 文件路径配置
- 基于 DataSourceTransactionManager 的事务管理

### 3. Mapper 扫描配置 (MyBatisMapperScannerConfig)
- 自动扫描 `mapper` 包下的接口
- 延迟加载确保在 MyBatis 配置完成后执行

### 4. SQL 拦截器 (Java110MybatisInterceptor)
- 拦截 INSERT、UPDATE、DELETE 操作
- 记录事务日志到订单服务
- 支持事务回滚日志记录
- 自动解析 SQL 参数并生成前后数据快照

### 5. 查询服务 (IQueryServiceDAO)
- 提供原生 SQL 执行能力
- 支持存储过程调用
- 防止 SQL 注入攻击
- 返回结果集映射为 Map 列表

## 依赖配置

### 主要依赖
```xml
<dependencies>
    <!-- MyBatis 集成 -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Druid 连接池 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid</artifactId>
    </dependency>
    
    <!-- 通用 Mapper -->
    <dependency>
        <groupId>tk.mybatis</groupId>
        <artifactId>mapper-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- 项目内部模块 -->
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-interface</artifactId>
    </dependency>
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-bean</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-core</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 配置说明

### 数据源配置示例
```properties
# 数据库连接
spring.datasource.url=jdbc:mysql://localhost:3306/micro_community
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Druid 连接池配置
spring.datasource.druid.initial-size=5
spring.datasource.druid.min-idle=5
spring.datasource.druid.max-active=20
spring.datasource.druid.max-wait=60000
```

### Mapper 文件路径配置
```properties
# MyBatis Mapper 文件路径
java110.mapping.path=classpath:mapper/*.xml,classpath:mapper/*/*.xml
```

## 使用指南

### 1. 数据源使用
模块自动配置 Druid 数据源，应用只需在配置文件中设置数据库连接参数即可。

### 2. MyBatis 使用
- 在 `mapper` 包下创建接口
- 在配置的 Mapper 路径下创建对应的 XML 文件
- 使用 `@Mapper` 注解标记接口

### 3. 事务管理
- 使用 `@Transactional` 注解管理事务
- 支持声明式事务管理
- 自动记录事务操作日志

### 4. SQL 监控
- 访问 `/druid` 路径查看 SQL 监控
- 支持 SQL 执行统计、慢SQL检测等功能

## 注意事项

1. **分库分表功能**：当前分库分表配置处于注释状态，如需使用需取消注释相关依赖
2. **事务日志**：SQL 拦截器会自动记录所有数据变更操作，确保事务可追溯
3. **性能监控**：Druid 提供了完整的连接池监控，建议在生产环境开启
4. **SQL 注入防护**：查询服务使用 PreparedStatement 防止 SQL 注入攻击

## 扩展功能

### 自定义拦截器
可以通过实现 `Interceptor` 接口创建自定义 MyBatis 拦截器，在 `MyBatisConfig` 中注册即可。

### 多数据源支持
当前模块支持单数据源配置，如需多数据源需扩展数据源配置类。

---

**文档版本**: 1.0  
**最后更新**: 2024年  
**维护团队**: MicroCommunity 开发团队