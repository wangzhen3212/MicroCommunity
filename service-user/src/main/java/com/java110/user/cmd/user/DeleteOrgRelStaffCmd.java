/**
 * 删除组织员工关系命令类
 * 
 * 该命令类用于处理删除组织与员工关联关系的业务逻辑，主要功能包括：
 * 1. 验证删除操作的必要参数
 * 2. 检查组织关系数量限制（至少保留一个组织关系）
 * 3. 执行组织员工关系的删除操作
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
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.po.org.OrgStaffRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "org.deleteOrgRelStaff")
public class DeleteOrgRelStaffCmd extends Cmd {
    
    /**
     * 组织员工关系服务接口
     */
    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段信息，确保删除操作的基本数据完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含用户ID
        Assert.hasKeyAndValue(reqJson, "userId", "未包含用户信息");
        // 验证请求中必须包含商户ID
        Assert.hasKeyAndValue(reqJson, "storeId", "未包含商户信息");
        // 验证请求中必须包含组织ID
        Assert.hasKeyAndValue(reqJson, "orgId", "未包含组织信息");
    }

    /**
     * 执行删除组织员工关系命令
     * 
     * 主要执行逻辑：
     * 1. 检查用户当前的组织关系数量，确保至少保留一个组织关系
     * 2. 验证要删除的关系是否存在
     * 3. 执行删除操作并处理结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败或业务规则不满足时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建组织员工关系数据传输对象
        OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
        // 设置员工ID
        orgStaffRelDto.setStaffId(reqJson.getString("userId"));
        
        // 查询该员工当前所有的组织关系
        List<OrgStaffRelDto> orgStaffRelDtos1 = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);
        
        // 业务规则：员工必须至少保留一个组织关系
        if (orgStaffRelDtos1.size() < 2) {
            throw new CmdException("至少保留一个组织关系，暂时无法删除！");
        }
        
        // 设置要删除的关系ID
        orgStaffRelDto.setRelId(reqJson.getString("relId"));
        // 查询指定的组织员工关系是否存在
        List<OrgStaffRelDto> orgStaffRelDtos = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);
        
        // 验证关系是否存在
        if (orgStaffRelDtos == null || orgStaffRelDtos.size() < 1) {
            throw new CmdException("关系不存在");
        }
        
        // 遍历查询到的组织关系列表，逐个删除
        for (OrgStaffRelDto tmpOrgStaffRelDto : orgStaffRelDtos) {
            // 创建组织员工关系持久化对象
            OrgStaffRelPo orgStaffRelPo = new OrgStaffRelPo();
            // 设置要删除的关系ID
            orgStaffRelPo.setRelId(tmpOrgStaffRelDto.getRelId());
            
            // 执行删除操作
            int flag = orgStaffRelV1InnerServiceSMOImpl.deleteOrgStaffRel(orgStaffRelPo);
            
            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("关联员工失败");
            }
        }
    }
}