# java110-interface 模块技术文档

## 模块概述

`java110-interface` 是 MicroCommunity 微服务架构中的接口定义模块，基于 Spring Cloud OpenFeign 技术栈，为系统内各微服务提供统一的 Feign 客户端接口定义。本模块包含 584 个业务接口，覆盖社区管理系统的所有核心业务领域。

## 技术架构

### 核心组件

1. **Feign 客户端配置**
   - `FeignConfiguration`: Feign 客户端全局配置类
   - `UserErrorDecoder`: 自定义错误解码器，处理服务调用异常

2. **接口定义规范**
   - 所有接口均使用 `@FeignClient` 注解声明
   - 统一的服务间调用异常处理机制
   - 支持日志级别动态配置

### 技术栈

- **Spring Cloud OpenFeign**: 微服务接口调用框架
- **Spring Boot**: 应用基础框架
- **Maven**: 项目构建工具
- **JUnit**: 单元测试框架

## 依赖关系

```xml
<dependencies>
    <dependency>
        <groupId>com.java110</groupId>
        <artifactId>java110-bean</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 核心功能模块

### 1. 用户管理模块 (73个接口)
- **业主管理**: `IOwnerInnerServiceSMO`, `IOwnerV1InnerServiceSMO`
- **员工管理**: `IStaffAppAuthInnerServiceSMO`, `IStaffCommunityV1InnerServiceSMO`
- **权限管理**: `IPrivilegeGroupV1InnerServiceSMO`, `IPrivilegeUserV1InnerServiceSMO`
- **组织架构**: `IOrgInnerServiceSMO`, `IOrgV1InnerServiceSMO`
- **车辆管理**: `IOwnerCarInnerServiceSMO`, `IOwnerCarV1InnerServiceSMO`

### 2. 费用管理模块 (60个接口)
- **费用配置**: `IFeeConfigInnerServiceSMO`, `IPayFeeConfigV1InnerServiceSMO`
- **收费管理**: `IFeeInnerServiceSMO`, `IPayFeeV1InnerServiceSMO`
- **费用折扣**: `IFeeDiscountInnerServiceSMO`, `IFeeDiscountRuleInnerServiceSMO`
- **打印管理**: `IFeePrintPageV1InnerServiceSMO`, `IFeePrintSpecInnerServiceSMO`
- **临时车费**: `ITempCarFeeConfigInnerServiceSMO`, `IComputeTempCarFee`

### 3. 社区管理模块 (99个接口)
- **社区基础**: `ICommunityInnerServiceSMO`, `ICommunityV1InnerServiceSMO`
- **楼栋单元**: `IFloorInnerServiceSMO`, `IUnitInnerServiceSMO`
- **房间管理**: `IRoomInnerServiceSMO`, `IRoomV1InnerServiceSMO`
- **巡检管理**: `IInspectionInnerServiceSMO`, `IInspectionPlanInnerServiceSMO`
- **维修管理**: `IRepairInnerServiceSMO`, `IRepairUserInnerServiceSMO`
- **停车场管理**: `IParkingAreaInnerServiceSMO`, `IParkingSpaceInnerServiceSMO`

### 4. 其他业务模块
- **订单管理**: `IOrderInnerServiceSMO`, `ICordersInnerServiceSMO`
- **商品管理**: `goods` 目录下的接口
- **OA系统**: `oa` 目录下的接口
- **报表管理**: `report` 目录下的接口
- **供应链管理**: `scm` 目录下的接口

## 配置说明

### Feign 客户端配置

```java
@Configuration
public class FeignConfiguration {
    @Bean
    public ErrorDecoder errorDecoder() {
        return new UserErrorDecoder();
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        if (SystemLogDto.getLogSwatch()) {
            return Logger.Level.FULL;
        } else {
            return Logger.Level.NONE;
        }
    }
}
```

### 自定义错误解码器

```java
public class UserErrorDecoder implements ErrorDecoder {
    public Exception decode(String methodKey, Response response) {
        // 处理服务调用异常，400+状态码转换为 HystrixBadRequestException
        // 其他异常记录日志并返回 RuntimeException
    }
}
```

## 使用示例

### 1. 服务调用示例

```java
@Autowired
private IUserInnerServiceSMO userInnerService;

// 查询用户信息
List<UserDto> users = userInnerService.getUsers(userDto);

// 查询员工数量
int staffCount = userInnerService.getStaffCount(userDto);
```

### 2. 接口定义规范

```java
@FeignClient(name = "user-service", configuration = {FeignConfiguration.class})
@RequestMapping("/userApi")
public interface IUserInnerServiceSMO {
    
    @RequestMapping(value = "/getUserInfo", method = RequestMethod.GET)
    List<UserDto> getUserInfo(@RequestParam("userIds") String[] userIds);
    
    @RequestMapping(value = "/getStaffs", method = RequestMethod.POST)
    List<UserDto> getStaffs(@RequestBody UserDto userDto);
}
```

## 开发规范

### 1. 接口命名规范
- 接口类名以 `I` 开头，以 `InnerServiceSMO` 结尾
- 方法名使用驼峰命名法，清晰表达业务功能
- 版本控制使用 `V1` 后缀标识

### 2. 参数传递规范
- 简单参数使用 `@RequestParam` 注解
- 复杂对象使用 `@RequestBody` 注解
- 批量查询支持数组参数

### 3. 异常处理规范
- 服务调用异常由 `UserErrorDecoder` 统一处理
- 400+ 状态码转换为 `HystrixBadRequestException`
- 其他异常记录详细日志信息

## 目录结构

```
java110-interface/
├── src/main/java/com/java110/
│   ├── config/                    # 配置类
│   │   ├── feign/                # Feign 配置
│   │   │   ├── FeignConfiguration.java
│   │   │   └── UserErrorDecoder.java
│   │   └── App.java              # 启动类
│   └── intf/                     # 业务接口定义
│       ├── user/                 # 用户管理接口 (73个)
│       ├── fee/                  # 费用管理接口 (60个)
│       ├── community/            # 社区管理接口 (99个)
│       ├── order/                # 订单管理接口 (4个)
│       └── ...                   # 其他业务接口
├── pom.xml                       # Maven 配置
└── README.md                     # 本文档
```

## 注意事项

1. **服务发现**: 确保目标服务在服务注册中心可用
2. **超时配置**: 根据业务需求合理设置调用超时时间
3. **熔断机制**: 结合 Hystrix 实现服务熔断和降级
4. **日志管理**: 通过系统日志开关控制 Feign 日志级别

## 版本信息

- **当前版本**: 1.0-SNAPSHOT
- **父项目**: MicroCommunity
- **打包方式**: jar

---

