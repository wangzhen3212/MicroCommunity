package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.PrivilegeUserDto;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.intf.user.IPrivilegeUserV1InnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员员工角色列表查询命令类
 * 
 * 该类用于查询管理员员工的角色信息，包括角色关联的小区信息
 * 通过服务码"user.listAdminStaffRoles"标识，处理管理员员工角色列表查询请求
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "user.listAdminStaffRoles")
public class ListAdminStaffRolesCmd extends Cmd {

    /**
     * 权限用户服务接口，用于查询用户权限信息
     */
    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    /**
     * 组织架构服务接口
     */
    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 角色小区关联服务接口，用于查询角色关联的小区信息
     */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的员工ID信息，并进行管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败或权限验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含staffId字段
        Assert.hasKeyAndValue(reqJson, "staffId", "未包含 员工信息");
        // 调用父类的管理员权限验证方法
        super.validateAdmin(context);
    }

    /**
     * 执行命令的主方法
     * 
     * 查询指定员工的管理员角色信息，并关联查询角色对应的小区信息
     * 返回包含角色列表和小区信息的响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为权限用户DTO对象
        PrivilegeUserDto privilegeUserDto = BeanConvertUtil.covertBean(reqJson, PrivilegeUserDto.class);
        // 设置门店ID为空，查询所有门店的权限
        privilegeUserDto.setStoreId("");
        // 设置用户ID为请求参数中的员工ID
        privilegeUserDto.setUserId(reqJson.getString("staffId"));
        // 设置权限标志为集团权限
        privilegeUserDto.setPrivilegeFlag(PrivilegeUserDto.PRIVILEGE_FLAG_GROUP);

        // 查询符合条件的权限用户数量
        int count = privilegeUserV1InnerServiceSMOImpl.queryPrivilegeUsersCount(privilegeUserDto);
        List<PrivilegeUserDto> roles = null;

        // 如果存在权限记录，则查询详细信息
        if (count > 0) {
            // 查询权限用户列表
            roles = privilegeUserV1InnerServiceSMOImpl.queryPrivilegeUsers(privilegeUserDto);
            
            // 遍历每个权限用户，查询其关联的小区信息
            for (PrivilegeUserDto privilegeUserDto1 : roles) {
                // 创建角色小区查询条件
                RoleCommunityDto roleCommunityDto = new RoleCommunityDto();
                // 设置角色ID为权限ID
                roleCommunityDto.setRoleId(privilegeUserDto1.getpId());
                // 设置查询行数
                roleCommunityDto.setRow(Integer.valueOf("999"));
                // 查询角色关联的小区信息
                List<RoleCommunityDto> roleCommunityDtos = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitys(roleCommunityDto);
                // 将小区信息设置到权限用户对象中
                privilegeUserDto1.setRoleCommunityDtoList(roleCommunityDtos);
            }
        } else {
            // 如果没有权限记录，返回空列表
            roles = new ArrayList<>();
        }

        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(1, roles.size(), roles);
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}