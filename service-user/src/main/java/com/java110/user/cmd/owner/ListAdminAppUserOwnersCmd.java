/**
 * 业主管理命令类 - 管理员查询应用用户关联的业主列表
 * 
 * 该命令类用于处理管理员查询应用用户关联业主信息的请求，支持分页查询和数据权限控制。
 * 主要功能包括验证管理员权限、查询用户可访问的小区范围、查询业主信息并返回分页结果。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员查询应用用户关联业主列表命令类
 * 服务代码：owner.listAdminAppUserOwners
 */
@Java110Cmd(serviceCode = "owner.listAdminAppUserOwners")
public class ListAdminAppUserOwnersCmd extends Cmd {

    /**
     * 业主应用用户关系服务接口
     */
    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    /**
     * 菜单权限服务接口
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 员工小区关系服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数验证和管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 执行命令方法
     * 
     * 根据请求参数查询管理员权限下的业主应用用户关联信息，支持分页查询和数据权限过滤
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为OwnerAppUserDto对象
        OwnerAppUserDto ownerAppUserDto = BeanConvertUtil.covertBean(reqJson, OwnerAppUserDto.class);

        // 从上下文中获取当前用户ID
        String staffId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 查询当前用户有权限访问的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有权限访问的小区不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            ownerAppUserDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 设置要查询的系统用户ID
        ownerAppUserDto.setUserId(reqJson.getString("systemUserId"));
        
        // 查询符合条件的记录总数
        int count = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsersCount(ownerAppUserDto);
        
        // 获取每页显示行数
        int row = reqJson.getIntValue("row");

        List<OwnerAppUserDto> ownerAppUserDtos = null;
        
        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 查询业主应用用户关联列表
            ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        } else {
            // 如果没有数据，返回空列表
            ownerAppUserDtos = new ArrayList<>();
        }

        // 计算总页数并创建响应实体
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) count / (double) row), count, ownerAppUserDtos);
        
        // 设置响应结果到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 权限脱敏处理方法
     * 
     * 根据用户ID和资源标识检查用户权限，返回有权限的业主列表（用于数据脱敏）
     * 
     * @param resource 资源标识，用于权限验证
     * @param userId 用户ID，要检查权限的用户
     * @return List<Map> 返回用户有权限访问的业主权限列表
     */
    public List<Map> getPrivilegeOwnerList(String resource, String userId) {
        // 创建权限验证对象
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource(resource);
        basePrivilegeDto.setUserId(userId);
        
        // 检查用户是否有指定资源的访问权限
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        
        return privileges;
    }
}