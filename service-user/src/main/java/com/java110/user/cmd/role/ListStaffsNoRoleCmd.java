package com.java110.user.cmd.role;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.org.OrgDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.dto.privilege.PrivilegeUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.intf.user.IPrivilegeUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.listener.ListenerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询不在指定角色内的员工命令类
 * 
 * 该命令用于查询系统中不属于指定角色的员工列表，支持分页查询和按组织架构权限过滤
 * 主要功能包括：
 * 1. 验证请求参数
 * 2. 根据用户权限确定可查询的组织范围
 * 3. 查询不在指定角色中的员工信息
 * 4. 返回分页结果
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode role.listStaffsNoRole
 */
@Java110Cmd(serviceCode = "role.listStaffsNoRole")
public class ListStaffsNoRoleCmd extends Cmd {

    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelInnerServiceSMO iOrgStaffRelInnerServiceSMO;

    /**
     * 验证请求参数
     * 
     * 检查请求JSON中是否包含必需的roleId参数
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "roleId", "未包含角色");
    }

    /**
     * 执行查询不在角色内的员工命令
     * 
     * 根据用户权限和组织架构关系，查询不属于指定角色的员工列表
     * 支持分页查询和按用户名搜索
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID和用户ID
        String storeId = context.getReqHeaders().get("store-id");
        String userId = context.getReqHeaders().get("user-id");

        // 构建权限用户查询条件
        PrivilegeUserDto privilegeUserDto = new PrivilegeUserDto();
        privilegeUserDto.setpId(reqJson.getString("roleId")); // 设置角色ID
        privilegeUserDto.setStoreId(storeId); // 设置店铺ID
        privilegeUserDto.setPrivilegeFlag(PrivilegeUserDto.PRIVILEGE_FLAG_GROUP); // 设置权限标志为组
        privilegeUserDto.setUserName(reqJson.getString("searchUserName")); // 设置搜索用户名
        privilegeUserDto.setPage(Integer.parseInt(reqJson.getString("page"))); // 设置页码
        privilegeUserDto.setRow(Integer.parseInt(reqJson.getString("row"))); // 设置每页行数
        privilegeUserDto.setLevelCd(UserDto.LEVEL_CD_STAFF); // 设置用户级别为员工

        // 查询当前用户信息，用于权限判断
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 如果不是管理员用户，需要根据权限限制查询范围
        if (!UserDto.LEVEL_CD_ADMIN.equals(userDtos.get(0).getLevelCd())) {
            // 检查用户是否拥有查看所有组织架构的权限
            BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
            basePrivilegeDto.setResource("/viewAllOrganization");
            basePrivilegeDto.setUserId(userId);
            List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
            
            // 如果没有查看所有组织的权限，则限制只能查看当前用户所属的组织架构
            if (ListUtil.isNull(privileges)) {
                // 查询员工所属的组织架构信息
                OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
                orgStaffRelDto.setStaffId(reqJson.getString("userId"));
                List<OrgStaffRelDto> orgStaffRelDtos = iOrgStaffRelInnerServiceSMO.queryOrgInfoByStaffIdsNew(orgStaffRelDto);
                
                if (!ListUtil.isNull(orgStaffRelDtos)) {
                    List<String> haveOrgList = new ArrayList<String>();
                    // 遍历员工所属的组织关系，构建可查询的组织ID列表
                    for (OrgStaffRelDto orgStaffRelDto1 : orgStaffRelDtos) {
                        OrgDto orgDto1 = new OrgDto();
                        orgDto1.setOrgId(orgStaffRelDto1.getOrgId());
                        List<OrgDto> orgDtoList = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                        // 递归查找相关的公司组织架构
                        findCompany(haveOrgList, orgDtoList);
                    }
                    // 设置可查询的组织ID范围
                    privilegeUserDto.setOrgIds(haveOrgList.toArray(new String[haveOrgList.size()]));
                }
            }
        }

        // 查询不在角色内的员工总数
        int count = privilegeUserV1InnerServiceSMOImpl.queryStaffsNoRoleCount(privilegeUserDto);

        List<UserDto> staffsDtos = null;

        // 如果有数据，则查询员工详细信息
        if (count > 0) {
            staffsDtos = privilegeUserV1InnerServiceSMOImpl.queryStaffsNoRoleInfos(privilegeUserDto);
        } else {
            staffsDtos = new ArrayList<>();
        }

        // 构建分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, staffsDtos);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        context.setResponseEntity(responseEntity);
    }

    /**
     * 递归查找相关的公司组织架构
     * 
     * 根据给定的组织列表，递归查找其上级、同级和下级组织，并将组织ID添加到结果列表中
     * 主要用于构建用户有权限访问的组织范围
     * 
     * @param haveOrgList 存储组织ID的结果列表
     * @param orgDtoList 需要处理的原始组织列表
     */
    private void findCompany(List<String> haveOrgList, List<OrgDto> orgDtoList) {
        // 遍历所有组织
        for (OrgDto orgDto : orgDtoList) {
            // 添加当前组织ID到结果列表
            haveOrgList.add(orgDto.getOrgId());
            
            // 如果不是顶级组织（组织级别不为1）
            if (!"1".equals(orgDto.getOrgLevel())) {
                // 如果是二级组织
                if ("2".equals(orgDto.getOrgLevel())) {
                    // 查询上级组织（一级组织）
                    OrgDto orgDto1 = new OrgDto();
                    orgDto1.setOrgId(orgDto.getParentOrgId());
                    List<OrgDto> orgDtoList1 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                    for (OrgDto orgDto2 : orgDtoList1) {
                        haveOrgList.add(orgDto2.getOrgId());
                    }
                    
                    // 查询下级组织（三级组织）
                    OrgDto orgDto2 = new OrgDto();
                    orgDto2.setParentOrgId(orgDto.getOrgId());
                    List<OrgDto> orgDtoList2 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto2);
                    for (OrgDto orgDto3 : orgDtoList2) {
                        haveOrgList.add(orgDto3.getOrgId());
                    }
                } else {
                    // 对于三级及以下组织，递归查询上级组织
                    OrgDto orgDto1 = new OrgDto();
                    orgDto1.setOrgId(orgDto.getParentOrgId());
                    List<OrgDto> orgDtoList1 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                    findCompany(haveOrgList, orgDtoList1);

                    // 如果不是二级组织，查询下级组织
                    if (!"2".equals(orgDto.getOrgLevel())) {
                        OrgDto orgDto3 = new OrgDto();
                        orgDto3.setParentOrgId(orgDto.getOrgId());
                        List<OrgDto> orgDtoList2 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto3);
                        for (OrgDto orgDto4 : orgDtoList2) {
                            haveOrgList.add(orgDto4.getOrgId());
                        }
                    }
                }
            }
        }
    }
}