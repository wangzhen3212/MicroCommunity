/**
 * 查询物业管理员员工信息命令类
 * 
 * 该命令类用于处理查询物业管理员员工信息的请求，包括员工基本信息、所属组织架构信息、
 * 员工头像图片等，并支持分页查询功能。
 * 
 * 主要功能：
 * 1. 查询员工基本信息
 * 2. 获取员工头像图片
 * 3. 构建员工所属组织架构树
 * 4. 生成员工姓名首字母
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.org.OrgDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.api.staff.ApiStaffDataVo;
import com.java110.vo.api.staff.ApiStaffVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "user.queryAdminPropertyStaff")
public class QueryAdminPropertyStaffCmd extends Cmd {

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelInnerServiceSMO iOrgStaffRelInnerServiceSMO;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括管理员权限验证和分页参数验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令的主方法
     * 
     * 处理查询物业管理员员工信息的核心业务逻辑，包括：
     * 1. 查询员工基本信息
     * 2. 获取员工头像图片
     * 3. 刷新员工姓名首字母
     * 4. 刷新员工所属组织信息
     * 5. 构建响应数据
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为UserDto对象
        UserDto userDto = BeanConvertUtil.covertBean(reqJson, UserDto.class);
        userDto.setStoreId(""); // 设置门店ID为空，查询所有门店的员工

        // 查询员工总数
        int count = userInnerServiceSMOImpl.getStaffCount(userDto);
        List<ApiStaffDataVo> staffs = new ArrayList<>();
        
        if (count > 0) {
            // 查询员工列表数据
            List<ApiStaffDataVo> staffList = BeanConvertUtil.covertBeanList(userInnerServiceSMOImpl.getStaffs(userDto), ApiStaffDataVo.class);
            
            // 遍历员工列表，获取每个员工的头像图片
            for (ApiStaffDataVo apiStaffDataVo : staffList) {
                FileRelDto fileRelDto = new FileRelDto();
                fileRelDto.setObjId(apiStaffDataVo.getUserId()); // 设置员工ID作为查询条件
                fileRelDto.setRelTypeCd("12000"); // 设置关系类型为员工图片
                List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
                
                // 如果存在图片记录，构建图片URL列表
                if (ListUtil.isNotNull(fileRelDtos)) {
                    List<String> urls = new ArrayList<>();
                    for (FileRelDto fileRel : fileRelDtos) {
                        urls.add(fileRel.getFileRealName()); // 添加图片真实名称作为URL
                    }
                    apiStaffDataVo.setUrls(urls); // 设置员工图片URL列表
                }
                staffs.add(apiStaffDataVo); // 将处理后的员工数据添加到结果列表
            }
            
            // 刷新员工姓名首字母
            refreshInitials(staffs);
            // 刷新员工所属组织信息
            refreshOrgs(staffs);
        } else {
            staffs = new ArrayList<>(); // 如果没有数据，返回空列表
        }
        
        // 构建响应VO对象
        ApiStaffVo apiStaffVo = new ApiStaffVo();
        apiStaffVo.setTotal(count); // 设置总记录数
        apiStaffVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiStaffVo.setStaffs(staffs); // 设置员工数据列表
        
        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiStaffVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity); // 设置响应实体
    }

    /**
     * 刷新员工所属组织信息
     * 
     * 为员工列表中的每个员工设置所属组织信息，包括组织ID、父组织ID和组织名称等
     * 
     * @param staffs 员工数据列表
     */
    private void refreshOrgs(List<ApiStaffDataVo> staffs) {
        // 如果员工列表为空，直接返回
        if (ListUtil.isNull(staffs)) {
            return;
        }
        
        // 收集所有员工ID
        List<String> staffIds = new ArrayList<>();
        for (ApiStaffDataVo apiStaffDataVo : staffs) {
            staffIds.add(apiStaffDataVo.getUserId());
        }
        
        // 查询门店下的所有组织
        OrgDto orgDto = new OrgDto();
        orgDto.setStoreId(staffs.get(0).getStoreId()); // 使用第一个员工的storeId
        List<OrgDto> orgDtos = orgV1InnerServiceSMOImpl.queryOrgs(orgDto);
        
        // 如果没有组织数据，直接返回
        if (ListUtil.isNull(orgDtos)) {
            return;
        }
        
        // 查询员工与组织的关联关系
        OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
        orgStaffRelDto.setStaffIds(staffIds.toArray(new String[staffIds.size()])); // 设置员工ID数组
        orgStaffRelDto.setStoreId(staffs.get(0).getStoreId()); // 设置门店ID
        List<OrgStaffRelDto> orgStaffRels = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);
        
        // 如果没有关联关系，直接返回
        if (ListUtil.isNull(orgStaffRels)) {
            return;
        }
        
        // 为每个员工设置直接所属组织信息
        for (ApiStaffDataVo apiStaffDataVo : staffs) {
            for (OrgStaffRelDto tmpOrgStaffRelDto : orgStaffRels) {
                // 匹配员工ID
                if (!apiStaffDataVo.getUserId().equals(tmpOrgStaffRelDto.getStaffId())) {
                    continue;
                }
                
                // 查询组织详细信息
                OrgDto org = new OrgDto();
                org.setOrgId(tmpOrgStaffRelDto.getOrgId());
                List<OrgDto> orgs = orgV1InnerServiceSMOImpl.queryOrgs(org);
                
                if (ListUtil.isNull(orgs)) {
                    continue;
                }
                
                // 设置员工的组织信息
                apiStaffDataVo.setOrgId(tmpOrgStaffRelDto.getOrgId()); // 设置组织ID
                apiStaffDataVo.setParentTwoOrgId(orgs.get(0).getParentOrgId()); // 设置二级父组织ID
            }
        }
        
        // 为每个员工构建完整的组织路径
        for (ApiStaffDataVo apiStaffDataVo : staffs) {
            if (StringUtil.isEmpty(apiStaffDataVo.getOrgId())) {
                continue; // 如果没有组织ID，跳过
            }
            
            apiStaffDataVo.setParentOrgId(apiStaffDataVo.getOrgId()); // 设置当前组织ID为父组织ID
            // 递归查找父级组织，构建组织路径
            findParents(apiStaffDataVo, orgDtos, null, 0);
        }
    }

    /**
     * 递归查找父级组织并构建组织路径
     * 
     * 通过递归方式查找员工的完整组织架构路径，构建组织名称的层级关系
     * 
     * @param apiStaffDataVo 员工数据对象
     * @param orgDtos 所有组织数据列表
     * @param curOrgDto 当前处理的组织对象
     * @param orgDeep 递归深度，用于防止无限递归
     */
    private void findParents(ApiStaffDataVo apiStaffDataVo, List<OrgDto> orgDtos, OrgDto curOrgDto, int orgDeep) {
        // 遍历所有组织，查找匹配的父组织
        for (OrgDto orgDto : orgDtos) {
            curOrgDto = orgDto;
            // 如果当前组织不是要查找的父组织，跳过
            if (!apiStaffDataVo.getParentOrgId().equals(orgDto.getOrgId())) {
                continue;
            }
            
            // 如果已经到达门店级别组织，停止递归
            if (OrgDto.ORG_LEVEL_STORE.equals(apiStaffDataVo.getOrgLevel())) {
                continue;
            }
            
            // 设置下一级父组织ID
            apiStaffDataVo.setParentOrgId(orgDto.getParentOrgId());
            
            // 构建组织名称路径
            if (StringUtil.isEmpty(apiStaffDataVo.getOrgName())) {
                apiStaffDataVo.setOrgName(orgDto.getOrgName()); // 第一次设置组织名称
                continue;
            }
            
            // 拼接组织名称，形成层级路径
            apiStaffDataVo.setOrgName(orgDto.getOrgName() + " / " + apiStaffDataVo.getOrgName());
            apiStaffDataVo.setOrgLevel(orgDto.getOrgLevel()); // 设置组织级别
        }
        
        // 递归终止条件检查
        if (curOrgDto != null && OrgDto.ORG_LEVEL_STORE.equals(curOrgDto.getOrgLevel())) {
            return; // 到达门店级别，停止递归
        }
        if (curOrgDto != null && curOrgDto.getParentOrgId().equals(curOrgDto.getOrgId())) {
            return; // 父组织ID等于自身ID，停止递归（防止循环）
        }
        if (curOrgDto != null && "-1".equals(curOrgDto.getParentOrgId())) {
            return; // 父组织ID为-1，表示根组织，停止递归
        }
        
        // 递归深度控制，防止无限递归
        orgDeep += 1;
        if (orgDeep > 20) {
            return; // 超过最大递归深度，停止递归
        }
        
        // 递归调用，继续查找上一级父组织
        findParents(apiStaffDataVo, orgDtos, curOrgDto, orgDeep);
    }

    /**
     * 刷新员工姓名首字母
     * 
     * 为每个员工设置其姓名的拼音首字母，用于排序或搜索功能
     * 
     * @param staffs 员工数据列表
     */
    private void refreshInitials(List<ApiStaffDataVo> staffs) {
        // 遍历员工列表，为每个有姓名的员工设置首字母
        for (ApiStaffDataVo staffDataVo : staffs) {
            if (StringUtil.isEmpty(staffDataVo.getName())) {
                continue; // 如果姓名为空，跳过
            }
            
            // 获取姓名拼音首字母并转换为大写
            staffDataVo.setInitials(PinYinUtil.getFirstSpell(staffDataVo.getName()).toUpperCase().charAt(0) + "");
        }
    }
}