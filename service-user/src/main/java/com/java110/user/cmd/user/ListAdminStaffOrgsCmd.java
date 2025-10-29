/**
 * 用户管理命令类 - 查询管理员员工所属组织列表
 * 
 * 该命令类用于处理查询管理员员工所属组织列表的请求，主要功能包括：
 * 1. 验证请求参数和权限
 * 2. 查询员工与组织的关联关系
 * 3. 查询组织详细信息
 * 4. 构建完整的组织层级名称
 * 5. 返回查询结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
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
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "user.listAdminStaffOrgs")
public class ListAdminStaffOrgsCmd extends Cmd {

    /**
     * 组织员工关联关系服务接口
     */
    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    /**
     * 组织信息服务接口
     */
    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的参数，并进行管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含staffId参数
        Assert.hasKeyAndValue(reqJson, "staffId", "未包含 员工信息");
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令的主方法
     * 
     * 处理查询管理员员工所属组织列表的核心业务逻辑：
     * 1. 获取商户信息
     * 2. 查询员工组织关联关系
     * 3. 查询组织详细信息
     * 4. 构建组织层级名称
     * 5. 返回查询结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 从请求头中获取商户ID
        String storeId = context.getReqHeaders().get("store-id");
        // 验证商户ID不能为空
        Assert.hasLength(storeId, "未包含商户信息");

        // 创建组织员工关联关系查询对象
        OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
        // 设置要查询的员工ID
        orgStaffRelDto.setStaffId(reqJson.getString("staffId"));
        // 查询员工与组织的关联关系列表
        List<OrgStaffRelDto> orgStaffRels = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);

        // 如果查询结果为空，直接返回
        if (ListUtil.isNull(orgStaffRels)) {
            return;
        }

        // 创建组织查询对象
        OrgDto orgDto = new OrgDto();
        // 设置商户ID，使用第一个关联关系的商户ID
        orgDto.setStoreId(orgStaffRels.get(0).getStoreId());
        // 查询组织详细信息列表
        List<OrgDto> orgDtos = orgV1InnerServiceSMOImpl.queryOrgs(orgDto);
        // 如果组织信息为空，直接返回
        if (ListUtil.isNull(orgDtos)) {
            return;
        }

        // 刷新组织名称，构建完整的组织层级路径
        freshOrgName(orgDtos, orgStaffRels);
        // 创建结果对象，包含数据总数和数据列表
        ResultVo resultVo = new ResultVo(1, orgStaffRels.size(), orgStaffRels);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);

    }

    /**
     * 刷新组织名称方法
     * 
     * 为每个员工组织关联关系构建完整的组织层级名称路径
     * 
     * @param orgDtos 组织详细信息列表
     * @param orgStaffRels 员工组织关联关系列表
     */
    private void freshOrgName(List<OrgDto> orgDtos, List<OrgStaffRelDto> orgStaffRels) {

        // 遍历每个员工组织关联关系
        for (OrgStaffRelDto orgStaffRelDto : orgStaffRels) {
            // 设置父组织ID为当前组织ID，用于开始递归查找
            orgStaffRelDto.setParentOrgId(orgStaffRelDto.getOrgId());
            // 递归查找父级组织并构建组织名称路径
            findParents(orgStaffRelDto, orgDtos, null, 0);
        }
    }

    /**
     * 递归查找父级组织方法
     * 
     * 通过递归方式查找组织的所有父级组织，并构建完整的组织层级名称
     * 
     * @param orgStaffRelDto 员工组织关联关系对象
     * @param orgDtos 组织详细信息列表
     * @param curOrgDto 当前处理的组织对象
     * @param orgDeep 递归深度，用于防止无限递归
     */
    private void findParents(OrgStaffRelDto orgStaffRelDto, List<OrgDto> orgDtos, OrgDto curOrgDto, int orgDeep) {
        // 遍历所有组织信息
        for (OrgDto orgDto : orgDtos) {
            // 如果当前组织不是要查找的父组织，跳过
            if (!orgStaffRelDto.getParentOrgId().equals(orgDto.getOrgId())) {
                continue;
            }
            // 设置父组织ID为当前组织的父组织ID，用于继续向上查找
            orgStaffRelDto.setParentOrgId(orgDto.getParentOrgId());
            // 更新当前组织对象
            curOrgDto = orgDto;
            // 如果组织名称为空，直接设置当前组织名称
            if (StringUtil.isEmpty(orgStaffRelDto.getOrgName())) {
                orgStaffRelDto.setOrgName(orgDto.getOrgName());
                continue;
            }
            // 如果组织名称不为空，将当前组织名称添加到已有名称前面，形成层级路径
            orgStaffRelDto.setOrgName(orgDto.getOrgName() + " / " + orgStaffRelDto.getOrgName());
        }

        // 递归终止条件1：当前组织是商户级别的组织（最顶层）
        if (curOrgDto != null && OrgDto.ORG_LEVEL_STORE.equals(curOrgDto.getOrgLevel())) {
            return;
        }

        // 递归终止条件2：当前组织的父组织ID等于自身ID（防止循环引用）
        if (curOrgDto != null && curOrgDto.getParentOrgId().equals(curOrgDto.getOrgId())) {
            return;
        }

        // 递归终止条件3：当前组织的父组织ID为-1（表示没有父组织）
        if (curOrgDto != null && "-1".equals(curOrgDto.getParentOrgId())) {
            return;
        }
        
        // 增加递归深度计数
        orgDeep += 1;
        // 递归终止条件4：递归深度超过20层，防止无限递归
        if (orgDeep > 20) {
            return;
        }

        // 递归调用，继续查找上一级父组织
        findParents(orgStaffRelDto, orgDtos, curOrgDto, orgDeep);
    }
}