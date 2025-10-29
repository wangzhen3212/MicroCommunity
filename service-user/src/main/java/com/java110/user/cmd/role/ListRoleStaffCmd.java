/**
 * 角色员工列表查询命令类
 * 
 * 该类用于处理查询指定角色下员工列表的请求，包括参数验证和业务逻辑处理。
 * 通过权限用户服务查询角色关联的员工信息，并返回分页结果。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.role;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.PrivilegeUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IPrivilegeUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "role.listRoleStaff")
public class ListRoleStaffCmd extends Cmd {

    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的角色ID字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含roleId字段
        Assert.hasKeyAndValue(reqJson, "roleId", "未包含角色");
    }

    /**
     * 命令执行方法
     * 
     * 根据角色ID查询关联的员工列表，支持分页和按用户名搜索
     * 返回包含分页信息和员工列表的结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");

        // 构建权限用户查询条件
        PrivilegeUserDto privilegeUserDto = new PrivilegeUserDto();
        privilegeUserDto.setpId(reqJson.getString("roleId")); // 设置角色ID
        privilegeUserDto.setStoreId(storeId); // 设置店铺ID
        privilegeUserDto.setPrivilegeFlag(PrivilegeUserDto.PRIVILEGE_FLAG_GROUP); // 设置权限标志为组类型
        privilegeUserDto.setUserName(reqJson.getString("searchUserName")); // 设置搜索的用户名
        privilegeUserDto.setPage(Integer.parseInt(reqJson.getString("page"))); // 设置页码
        privilegeUserDto.setRow(Integer.parseInt(reqJson.getString("row"))); // 设置每页记录数

        // 查询符合条件的员工总数
        int count = privilegeUserV1InnerServiceSMOImpl.queryPrivilegeUserInfoCount(privilegeUserDto);

        List<UserDto> staffsDtos = null;

        // 根据查询结果决定是否查询详细数据
        if (count > 0) {
            // 查询员工详细信息列表
            staffsDtos = privilegeUserV1InnerServiceSMOImpl.queryPrivilegeUserInfos(privilegeUserDto);
        } else {
            // 如果没有数据，返回空列表
            staffsDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数/每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, staffsDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}