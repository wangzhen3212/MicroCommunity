/**
 * 员工组织查询命令类
 * 
 * 该命令类用于查询指定员工所属的组织信息，包括组织层级关系和组织名称的完整路径
 * 通过递归方式构建组织层级关系，返回员工所在组织的完整路径信息
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
import com.java110.dto.org.OrgDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "user.listStaffOrgs")
public class ListStaffOrgsCmd extends Cmd {

    /**
     * 组织员工关系服务接口
     */
    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    /**
     * 组织信息服务接口
     */
    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的员工ID信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含staffId字段
        Assert.hasKeyAndValue(reqJson, "staffId", "未包含 员工信息");
    }

    /**
     * 命令执行方法
     * 
     * 根据员工ID查询其所属的组织信息，构建组织层级关系并返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 从请求头中获取商户ID
        String storeId = context.getReqHeaders().get("store-id");
        Assert.hasLength(storeId, "未包含商户信息");

        // 查询指定商户下的所有组织信息
        OrgDto orgDto = new OrgDto();
        orgDto.setStoreId(storeId);
        List<OrgDto> orgDtos = orgV1InnerServiceSMOImpl.queryOrgs(orgDto);
        
        // 如果商户下没有组织信息，直接返回
        if (orgDtos == null || orgDtos.size() < 1) {
            return;
        }
        
        // 查询员工与组织的关系信息
        OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
        orgStaffRelDto.setStaffId(reqJson.getString("staffId")); // 设置员工ID
        orgStaffRelDto.setStoreId(storeId); // 设置商户ID
        List<OrgStaffRelDto> orgStaffRels = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);

        // 如果员工没有关联任何组织，直接返回
        if (orgStaffRels == null || orgStaffRels.size() < 1) {
            return;
        }

        // 刷新组织名称，构建完整的组织路径
        freshOrgName(orgDtos, orgStaffRels);
        
        // 构建返回结果
        ResultVo resultVo = new ResultVo(1, orgStaffRels.size(), orgStaffRels);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新组织名称方法
     * 
     * 为每个员工组织关系设置父组织ID，并递归查找组织层级关系
     * 
     * @param orgDtos 组织数据列表
     * @param orgStaffRels 员工组织关系列表
     */
    private void freshOrgName(List<OrgDto> orgDtos, List<OrgStaffRelDto> orgStaffRels) {
        // 遍历每个员工组织关系，构建组织层级路径
        for (OrgStaffRelDto orgStaffRelDto : orgStaffRels) {
            // 设置当前组织ID为父组织ID，开始递归查找
            orgStaffRelDto.setParentOrgId(orgStaffRelDto.getOrgId());
            findParents(orgStaffRelDto, orgDtos, null, 0);
        }
    }

    /**
     * 递归查找父组织方法
     * 
     * 通过递归方式查找组织的所有父级组织，构建完整的组织路径名称
     * 
     * @param orgStaffRelDto 员工组织关系对象
     * @param orgDtos 组织数据列表
     * @param curOrgDto 当前组织对象
     * @param orgDeep 当前递归深度，用于防止无限递归
     */
    private void findParents(OrgStaffRelDto orgStaffRelDto, List<OrgDto> orgDtos, OrgDto curOrgDto, int orgDeep) {
        // 遍历所有组织，查找匹配的父组织
        for (OrgDto orgDto : orgDtos) {
            // 跳过非父组织的记录
            if (!orgStaffRelDto.getParentOrgId().equals(orgDto.getOrgId())) {
                continue;
            }
            
            // 设置下一级父组织ID
            orgStaffRelDto.setParentOrgId(orgDto.getParentOrgId());
            curOrgDto = orgDto;
            
            // 构建组织名称路径
            if (StringUtil.isEmpty(orgStaffRelDto.getOrgName())) {
                // 如果是第一个组织，直接设置组织名称
                orgStaffRelDto.setOrgName(orgDto.getOrgName());
                continue;
            }
            // 如果已有组织名称，使用斜杠分隔符拼接组织路径
            orgStaffRelDto.setOrgName(orgDto.getOrgName() + " / " + orgStaffRelDto.getOrgName());
        }

        // 递归终止条件1：当前组织是商户级别的组织
        if (curOrgDto != null && OrgDto.ORG_LEVEL_STORE.equals(curOrgDto.getOrgLevel())) {
            return;
        }

        // 递归终止条件2：当前组织的父组织ID等于自身ID（根组织）
        if (curOrgDto != null && curOrgDto.getParentOrgId().equals(curOrgDto.getOrgId())) {
            return;
        }

        // 递归终止条件3：当前组织的父组织ID为-1（特殊标识的根组织）
        if (curOrgDto != null && "-1".equals(curOrgDto.getParentOrgId())) {
            return;
        }
        
        // 递归深度控制，防止无限递归
        orgDeep += 1;
        if (orgDeep > 20) {
            return;
        }

        // 递归调用，继续查找上一级父组织
        findParents(orgStaffRelDto, orgDtos, curOrgDto, orgDeep);
    }
}