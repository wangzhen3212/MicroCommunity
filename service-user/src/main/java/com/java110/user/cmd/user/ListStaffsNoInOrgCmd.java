package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.org.OrgDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询不在指定组织中的员工命令类
 * 该命令用于查询系统中不在特定组织架构下的员工信息，支持权限控制和分页查询
 * 主要功能包括：权限验证、组织架构过滤、员工信息查询等
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode user.listStaffsNoInOrg
 */
@Java110Cmd(serviceCode = "user.listStaffsNoInOrg")
public class ListStaffsNoInOrgCmd extends Cmd {

    /**
     * 用户信息服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 菜单权限服务接口
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 组织员工关系服务接口
     */
    @Autowired
    private IOrgStaffRelInnerServiceSMO iOrgStaffRelInnerServiceSMO;

    /**
     * 组织架构服务接口
     */
    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的组织信息和店铺信息
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文，包含请求和响应信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含组织ID
        Assert.hasKeyAndValue(reqJson, "orgId", "未包含组织信息");
        // 从请求头中获取店铺ID并验证
        String storeId = context.getReqHeaders().get("store-id");
        Assert.hasLength(storeId, "未找到账户信息");
    }

    /**
     * 命令执行方法
     * 根据用户权限和组织架构信息查询不在指定组织中的员工列表
     * 支持分页查询和权限过滤
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID和用户ID
        String storeId = context.getReqHeaders().get("store-id");
        String userId = context.getReqHeaders().get("user-id");
        
        // 构建查询条件对象
        UserDto userDto = new UserDto();
        userDto.setStoreId(storeId);
        userDto.setOrgId(reqJson.getString("orgId")); // 设置目标组织ID
        userDto.setStaffName(reqJson.getString("staffName")); // 设置员工姓名查询条件

        // 验证当前用户信息并检查权限
        UserDto userDto1 = new UserDto();
        userDto1.setUserId(userId);
        userDto1.setPage(1);
        userDto1.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto1);
        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 如果不是管理员用户，需要进行权限检查
        if (!UserDto.LEVEL_CD_ADMIN.equals(userDtos.get(0).getLevelCd())) {
            // 检查用户是否拥有查看所有组织架构的权限
            BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
            basePrivilegeDto.setResource("/viewAllOrganization");
            basePrivilegeDto.setUserId(userId);
            List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
            
            // 如果没有查看所有组织的权限，则只能查看当前用户所属的组织架构
            if (privileges.size() == 0) {
                // 查询员工所属的组织架构信息
                OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
                orgStaffRelDto.setStaffId(reqJson.getString("userId"));
                List<OrgStaffRelDto> orgStaffRelDtos = iOrgStaffRelInnerServiceSMO.queryOrgInfoByStaffIdsNew(orgStaffRelDto);
                
                // 如果员工有关联的组织架构，则构建可访问的组织ID列表
                if (orgStaffRelDtos.size() > 0) {
                    List<String> haveOrgList = new ArrayList<String>();
                    for (OrgStaffRelDto orgStaffRelDto1 : orgStaffRelDtos) {
                        OrgDto orgDto1 = new OrgDto();
                        orgDto1.setOrgId(orgStaffRelDto1.getOrgId());
                        List<OrgDto> orgDtoList = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                        // 递归查找相关的组织架构（上级、同级、下级）
                        findCompany(haveOrgList, orgDtoList);
                    }
                    // 设置用户可访问的组织ID列表
                    userDto.setOrgIds(haveOrgList.toArray(new String[haveOrgList.size()]));
                }
            }
        }

        // 查询不在指定组织中的员工总数
        int count = userV1InnerServiceSMOImpl.queryStaffsNoInOrgCount(userDto);
        List<UserDto> staffsDtos = null;
        
        // 如果有数据，则进行分页查询
        if (count > 0) {
            userDto.setPage(Integer.parseInt(reqJson.getString("page")));
            userDto.setRow(Integer.parseInt(reqJson.getString("row")));
            staffsDtos = userV1InnerServiceSMOImpl.queryStaffsNoInOrg(userDto);
        } else {
            staffsDtos = new ArrayList<>();
        }
        
        // 构建分页响应结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, staffsDtos);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 递归查找组织架构关系
     * 根据给定的组织列表，递归查找相关的上级、同级和下级组织
     * 主要用于构建用户可访问的组织ID列表
     *
     * @param haveOrgList 存储组织ID的列表，用于收集所有相关的组织ID
     * @param orgDtoList 需要处理的组织数据列表
     */
    private void findCompany(List<String> haveOrgList, List<OrgDto> orgDtoList) {
        // 遍历所有组织
        for (OrgDto orgDto : orgDtoList) {
            // 添加当前组织ID到列表中
            haveOrgList.add(orgDto.getOrgId());
            
            // 如果不是顶级组织（组织级别不为1）
            if (!"1".equals(orgDto.getOrgLevel())) {
                // 如果是二级组织
                if ("2".equals(orgDto.getOrgLevel())) {
                    // 查找上级组织（父组织）
                    OrgDto orgDto1 = new OrgDto();
                    orgDto1.setOrgId(orgDto.getParentOrgId());
                    List<OrgDto> orgDtoList1 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                    for (OrgDto orgDto2 : orgDtoList1) {
                        haveOrgList.add(orgDto2.getOrgId());
                    }
                    
                    // 查找同级组织（子组织）
                    OrgDto orgDto2 = new OrgDto();
                    orgDto2.setParentOrgId(orgDto.getOrgId());
                    List<OrgDto> orgDtoList2 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto2);
                    for (OrgDto orgDto3 : orgDtoList2) {
                        haveOrgList.add(orgDto3.getOrgId());
                    }
                } else {
                    // 对于非二级组织，递归查找上级组织
                    OrgDto orgDto1 = new OrgDto();
                    orgDto1.setOrgId(orgDto.getParentOrgId());
                    List<OrgDto> orgDtoList1 = orgV1InnerServiceSMOImpl.queryOrgs(orgDto1);
                    findCompany(haveOrgList, orgDtoList1);

                    // 如果不是二级组织，查找下级组织
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