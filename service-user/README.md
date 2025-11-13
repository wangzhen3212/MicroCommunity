# Service-User 模块技术文档

## 模块概述

Service-User 是一个基于 Spring Boot 的微服务模块，主要负责社区管理系统的用户信息维护和相关服务提供。该模块采用标准的微服务架构，集成了服务发现、Feign客户端、异步处理等关键技术组件，为社区管理系统提供完整的用户管理功能。

**核心设计理念：**
- **命令驱动架构**：所有业务功能通过命令处理层（cmd）提供标准化接口
- **分层设计**：清晰的API层、命令层、业务层、数据层分离
- **微服务集成**：通过Feign客户端实现服务间调用
- **事务管理**：支持分布式事务处理

## 技术架构

### 分层架构设计
```
src/main/java/com/java110/user/
├── api/          # API接口层 - 对外提供RESTful接口
├── bmo/          # 业务模型对象层 - 业务逻辑处理
├── cmd/          # 命令处理层 - 模块核心接口实现（26个功能模块）
├── dao/          # 数据访问层 - 数据库操作
├── smo/          # 服务管理对象层 - 服务调度和管理
└── UserServiceApplicationStart.java  # 主启动类
```

### 关键技术组件
- **Spring Boot 2.x**: 微服务框架
- **Spring Cloud**: 服务发现与注册 (`@EnableDiscoveryClient`)
- **OpenFeign**: 微服务间调用 (`@EnableFeignClients`)
- **异步处理**: `@EnableAsync` 支持异步任务处理
- **RESTful API**: 基于Spring MVC的REST接口
- **自定义注解**: 支持命令发现、监听器发现等扩展功能
- **事务管理**: `@Java110Transactional` 注解支持事务控制

## 命令处理层（CMD）核心接口

命令处理层是本模块的核心，包含26个功能模块，每个模块提供完整的业务操作接口：

### 1. 楼栋业主管理 (buildingOwner) - 4个命令
- **SaveBuildingOwnerCmd**: 保存业主信息
- **UpdateBuildingOwnerCmd**: 更新业主信息  
- **DeleteBuildingOwnerCmd**: 删除业主信息
- **ListBuildingOwnerCmd**: 查询业主列表

### 2. 车辆管理 (car) - 1个命令
- **QueryWaitPayFeeTempCarCmd**: 查询待缴费临时车辆

### 3. 车辆黑白名单管理 (carBlackWhite) - 4个命令
- **SaveCarBlackWhiteCmd**: 保存车辆黑白名单
- **UpdateCarBlackWhiteCmd**: 更新车辆黑白名单
- **DeleteCarBlackWhiteCmd**: 删除车辆黑白名单
- **ListCarBlackWhitesCmd**: 查询车辆黑白名单列表

### 4. 车辆进出管理 (carInout) - 3个命令
- **SaveCarInoutCmd**: 保存车辆进出记录
- **UpdateCarInoutCmd**: 更新车辆进出记录
- **ListCarInoutsCmd**: 查询车辆进出记录列表

### 5. 数据权限员工管理 (dataPrivilegeStaff) - 5个命令
- **SaveDataPrivilegeStaffCmd**: 保存数据权限员工
- **UpdateDataPrivilegeStaffCmd**: 更新数据权限员工
- **DeleteDataPrivilegeStaffCmd**: 删除数据权限员工
- **ListDataPrivilegeStaffCmd**: 查询数据权限员工列表
- **ListStaffNotInDataPrivilegeCmd**: 查询未分配数据权限的员工

### 6. 审核管理 (examine) - 15个命令
- **SaveExamineProjectCmd**: 保存审核项目
- **UpdateExamineProjectCmd**: 更新审核项目
- **DeleteExamineProjectCmd**: 删除审核项目
- **ListExamineProjectCmd**: 查询审核项目列表
- **SaveExamineStaffCmd**: 保存审核员工
- **UpdateExamineStaffCmd**: 更新审核员工
- **DeleteExamineStaffCmd**: 删除审核员工
- **ListExamineStaffCmd**: 查询审核员工列表
- **ListExamineStaffIntroductionCmd**: 查询审核员工介绍
- **SaveExamineStaffValueCmd**: 保存审核员工值
- **DeleteExamineStaffValueCmd**: 删除审核员工值
- **ListExamineStaffValueCmd**: 查询审核员工值列表
- **SaveExamineStaffProjectCmd**: 保存审核员工项目关联
- **DeleteExamineStaffProjectCmd**: 删除审核员工项目关联
- **ListExamineStaffProjectCmd**: 查询审核员工项目关联列表

### 7. 垃圾需求管理 (junkRequirement) - 4个命令
- **SaveJunkRequirementCmd**: 保存垃圾需求
- **UpdateJunkRequirementCmd**: 更新垃圾需求
- **DeleteJunkRequirementCmd**: 删除垃圾需求
- **ListJunkRequirementsCmd**: 查询垃圾需求列表

### 8. 登录管理 (login) - 8个命令（核心认证模块）
- **UserServiceLoginCmd**: 用户服务登录
- **PcUserLoginCmd**: PC端用户登录
- **PhoneWechatLoginCmd**: 手机微信登录
- **AccessTokenLoginCmd**: AccessToken登录
- **GetAccessTokenCmd**: 获取AccessToken
- **AdminLoginPropertyCmd**: 管理员物业登录
- **GeneratorHcCodeCmd**: 生成健康码
- **QueryTokenForTeldCmd**: 查询Teld令牌

### 9. 菜单管理 (menu) - 3个命令
- **ListCatalogCmd**: 查询目录列表
- **ListCatalogMenusCmd**: 查询目录菜单列表
- **QueryMenuInfoCmd**: 查询菜单信息

### 10. 菜单组管理 (menuGroup) - 3个命令
- **ListMenuGroupsCmd**: 查询菜单组列表
- **SaveMenuGroupCmd**: 保存菜单组
- **UpdateMenuGroupCmd**: 更新菜单组

### 11. 菜单组社区关联 (menuGroupCommunity) - 4个命令
- **DeleteMenuGroupCommunityCmd**: 删除菜单组社区关联
- **ListMenuGroupCommunitysCmd**: 查询菜单组社区关联列表
- **SaveMenuGroupCommunityCmd**: 保存菜单组社区关联
- **UpdateMenuGroupCommunityCmd**: 更新菜单组社区关联

### 12. 用户菜单关联 (menuUser) - 3个命令
- **DeleteMenuUserCmd**: 删除用户菜单关联
- **ListMenuUsersCmd**: 查询用户菜单关联列表
- **SaveMenuUserCmd**: 保存用户菜单关联

### 13. 记事本管理 (notepad) - 4个命令
- **DeleteNotepadCmd**: 删除记事本
- **ListNotepadsCmd**: 查询记事本列表
- **SaveNotepadCmd**: 保存记事本
- **UpdateNotepadCmd**: 更新记事本

### 14. 组织架构管理 (org) - 13个命令
- **SaveOrgCmd**: 保存组织
- **UpdateOrgCmd**: 更新组织
- **DeleteOrgCmd**: 删除组织
- **ListOrgsCmd**: 查询组织列表
- **ListOrgCmd**: 查询组织详情
- **ListOrgTreeCmd**: 查询组织树
- **ListParentOrgsCmd**: 查询父组织列表
- **QueryAdminOrgTreeCmd**: 查询管理员组织树
- **SaveOrgCommunityCmd**: 保存组织社区关联
- **DeleteOrgCommunityCmd**: 删除组织社区关联
- **ListOrgCommunitysCmd**: 查询组织社区关联列表
- **ListOrgNoCommunitysCmd**: 查询未关联社区的组织
- **ListOrgNoShopsCmd**: 查询未关联店铺的组织

### 15. 业主管理 (owner) - 4个命令
- **DeleteOwnerCmd**: 删除业主
- **ListOwnersCmd**: 查询业主列表
- **SaveOwnerCmd**: 保存业主
- **UpdateOwnerCmd**: 更新业主

### 16. 业主车辆属性 (ownerCarAttr) - 4个命令
- **DeleteOwnerCarAttrCmd**: 删除业主车辆属性
- **ListOwnerCarAttrsCmd**: 查询业主车辆属性列表
- **SaveOwnerCarAttrCmd**: 保存业主车辆属性
- **UpdateOwnerCarAttrCmd**: 更新业主车辆属性

### 17. 业主车辆开放用户 (ownerCarOpenUser) - 4个命令
- **DeleteOwnerCarOpenUserCmd**: 删除业主车辆开放用户
- **ListOwnerCarOpenUsersCmd**: 查询业主车辆开放用户列表
- **SaveOwnerCarOpenUserCmd**: 保存业主车辆开放用户
- **UpdateOwnerCarOpenUserCmd**: 更新业主车辆开放用户

### 18. 业主委员会 (ownerCommittee) - 4个命令
- **DeleteOwnerCommitteeCmd**: 删除业主委员会
- **ListOwnerCommitteesCmd**: 查询业主委员会列表
- **SaveOwnerCommitteeCmd**: 保存业主委员会
- **UpdateOwnerCommitteeCmd**: 更新业主委员会

### 19. 业主入驻 (ownerSettled) - 4个命令
- **DeleteOwnerSettledCmd**: 删除业主入驻信息
- **ListOwnerSettledsCmd**: 查询业主入驻信息列表
- **SaveOwnerSettledCmd**: 保存业主入驻信息
- **UpdateOwnerSettledCmd**: 更新业主入驻信息

### 20. 权限管理 (privilege) - 5个命令
- **SavePrivilegeGroupInfoCmd**: 保存权限组信息
- **EditPrivilegeGroupInfoCmd**: 编辑权限组信息
- **DeletePrivilegeGroupInfoCmd**: 删除权限组信息
- **AddPrivilegePrivilegeGroupCmd**: 添加权限到权限组
- **DeletePrivilegePrivilegeGroupCmd**: 从权限组删除权限

### 21. 问答管理 (question) - 4个命令
- **DeleteQuestionCmd**: 删除问题
- **ListQuestionsCmd**: 查询问题列表
- **SaveQuestionCmd**: 保存问题
- **UpdateQuestionCmd**: 更新问题

### 22. 注册管理 (register) - 4个命令
- **DeleteRegisterCmd**: 删除注册信息
- **ListRegistersCmd**: 查询注册信息列表
- **SaveRegisterCmd**: 保存注册信息
- **UpdateRegisterCmd**: 更新注册信息

### 23. 角色管理 (role) - 4个命令
- **DeleteRoleCmd**: 删除角色
- **ListRolesCmd**: 查询角色列表
- **SaveRoleCmd**: 保存角色
- **UpdateRoleCmd**: 更新角色

### 24. 角色社区关联 (roleCommunity) - 4个命令
- **DeleteRoleCommunityCmd**: 删除角色社区关联
- **ListRoleCommunitysCmd**: 查询角色社区关联列表
- **SaveRoleCommunityCmd**: 保存角色社区关联
- **UpdateRoleCommunityCmd**: 更新角色社区关联

### 25. 用户管理 (user) - 40个命令（核心用户模块）
- **SaveUserCmd**: 保存用户信息
- **UserStaffAddCmd**: 添加员工用户
- **UserStaffModifyCmd**: 修改员工用户
- **UserStaffDeleteCmd**: 删除员工用户
- **UserStaffEnableCmd**: 启用员工用户
- **UserStaffDisableCmd**: 禁用员工用户
- **UserLoginCmd**: 用户登录
- **OwnerUserLoginCmd**: 业主用户登录
- **OwnerUserLoginByOpenIdCmd**: 业主OpenID登录
- **GetUserInfoCmd**: 获取用户信息
- **ListUsersCmd**: 查询用户列表
- **ListSystemUsersCmd**: 查询系统用户列表
- **ChangeStaffPwdCmd**: 修改员工密码
- **ResetStaffPwdCmd**: 重置员工密码
- **ResetSystemUserPwdCmd**: 重置系统用户密码
- **ListStaffOrgsCmd**: 查询员工组织列表
- **ListStaffRolesCmd**: 查询员工角色列表
- **ListAdminStaffOrgsCmd**: 查询管理员员工组织列表
- **ListAdminStaffRolesCmd**: 查询管理员员工角色列表
- **ListStaffCommunitysCmd**: 查询员工社区列表
- **QueryStaffInfosCmd**: 查询员工信息
- **QueryStaffByNameCmd**: 按名称查询员工
- **QueryAdminPropertyStaffCmd**: 查询管理员物业员工
- **QueryAdminStaffPrivilegeCmd**: 查询管理员员工权限
- **QueryAdminStaffAppAuthCmd**: 查询管理员员工应用权限
- **QueryUserPrivilege**: 查询用户权限
- **QueryUserSecretCmd**: 查询用户密钥
- **QueryStorePrivilegeGroupCmd**: 查询店铺权限组
- **QueryPrivilegeGroupNoAddPrivilegeCmd**: 查询未添加权限的权限组
- **GeneratorUserQrCodeCmd**: 生成用户二维码
- **UserSendSmsCmd**: 用户发送短信
- **OwnerSendSmsCmd**: 业主发送短信
- **SaveStaffOrgRelCmd**: 保存员工组织关系
- **DeleteOrgRelStaffCmd**: 删除组织关系员工
- **DeleteSystemUserCmd**: 删除系统用户
- **SaveOrUpdateUserAttrCmd**: 保存或更新用户属性
- **ListSystemUserAttrCmd**: 查询系统用户属性列表
- **ListStaffsNoInOrgCmd**: 查询未分配组织的员工
- **CheckServiceLoginCmd**: 检查服务登录
- **UserLogoutServiceCmd**: 用户退出服务

### 26. 微信管理 (wechat) - 4个命令
- **DeleteWechatCmd**: 删除微信信息
- **ListWechatsCmd**: 查询微信信息列表
- **SaveWechatCmd**: 保存微信信息
- **UpdateWechatCmd**: 更新微信信息

## 命令接口规范

### 命令注解
```java
@Java110Cmd(serviceCode = "module.commandName")
```
- **serviceCode**: 服务编码，格式为"模块.命令名称"
- 用于标识和路由具体的业务命令

### 命令执行流程
1. **参数验证**: `validate()` 方法验证请求参数完整性
2. **业务执行**: `doCmd()` 方法执行业务逻辑
3. **事务管理**: `@Java110Transactional` 注解确保事务一致性
4. **结果返回**: 统一的结果格式返回

### 请求格式
```json
{
  "serviceCode": "user.saveUser",
  "reqData": {
    "userId": "123456",
    "name": "张三",
    // ... 其他参数
  }
}
```

## API接口映射

| API接口 | 对应命令模块 | 主要功能 |
|---------|-------------|----------|
| UserApi | user | 用户信息统一处理 |
| UserLoginApi | login | 用户登录管理 |
| OwnerApi | owner, buildingOwner | 业主信息管理 |
| StaffApi | user | 员工信息管理 |
| OrgApi | org | 组织架构管理 |
| MenuApi | menu, menuGroup | 菜单管理 |

## 部署配置

### 环境要求
- JDK 1.8+
- Maven 3.6+
- Spring Cloud 环境
- 服务注册中心（如Eureka、Nacos）

### 启动配置
```java
@SpringBootApplication(
    scanBasePackages = {
        "com.java110.service",
        "com.java110.core", 
        "com.java110.user",
        "com.java110.config.properties.code",
        "com.java110.db",
        "com.java110.doc"
    },
    excludeName = {"com.java110.intf.user"}
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
    "com.java110.intf.community",
    "com.java110.intf.common",
    "com.java110.intf.store",
    "com.java110.intf.oa",
    "com.java110.intf.fee",
    "com.java110.intf.order",
    "com.java110.intf.mall", 
    "com.java110.intf.report",
    "com.java110.intf.acct",
    "com.java110.intf.job"
})
@EnableAsync
public class UserServiceApplicationStart {
    // 主启动类
}
```

### 启动命令
```bash
mvn spring-boot:run
# 或打包后运行
java -jar service-user-1.0-SNAPSHOT.jar
```

## 依赖关系

### 主要依赖
- **java110-service**: 核心服务模块依赖
- **Spring Boot Starter Web**: Web服务支持
- **Spring Cloud Starter**: 微服务支持
- **OpenFeign**: 服务间调用
- **FastJSON**: JSON处理

### 服务间依赖
通过Feign客户端调用以下服务：
- 社区服务 (community)
- 通用服务 (common)
- 店铺服务 (store)
- OA服务 (oa)
- 费用服务 (fee)
- 订单服务 (order)
- 商城服务 (mall)
- 报表服务 (report)
- 账户服务 (acct)
- 任务服务 (job)

## 开发说明

### 命令开发规范
1. **继承Cmd基类**: 所有命令类必须继承 `Cmd` 基类
2. **注解声明**: 使用 `@Java110Cmd` 注解声明服务编码
3. **参数验证**: 在 `validate()` 方法中验证必需参数
4. **事务管理**: 在 `doCmd()` 方法上使用 `@Java110Transactional`
5. **统一响应**: 使用 `ResultVo.success()` 返回成功响应

### 扩展性设计
- **命令发现机制**: 支持动态命令注册和发现
- **监听器机制**: 支持事件监听和发布
- **异步处理**: 支持异步任务执行
- **文档生成**: 集成文档自动生成功能

## 核心优势

1. **标准化接口**: 所有功能通过统一的命令接口提供
2. **事务一致性**: 支持分布式事务管理
3. **微服务集成**: 完善的微服务间调用机制
4. **扩展性强**: 支持自定义命令和监听器
5. **文档完善**: 完整的接口文档和技术说明

---

*文档版本: 3.0*
*最后更新: 基于cmd目录深度分析生成*
*重点突出命令处理层作为模块核心提供接口*
*包含26个功能模块的完整命令接口清单*