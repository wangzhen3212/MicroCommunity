package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.org.OrgDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.po.org.OrgStaffRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 保存员工组织关系命令类
 * 
 * 该类负责处理员工与组织关系的保存操作，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 批量保存员工与组织的关联关系
 * 3. 处理重复关联的跳过逻辑
 * 4. 确保事务性操作
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode user.saveStaffOrgRel
 */
@Java110Cmd(serviceCode = "user.saveStaffOrgRel")
public class SaveStaffOrgRelCmd extends Cmd {

    /**
     * 组织员工关系服务接口
     */
    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的组织ID和员工ID列表
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中是否包含组织ID
        Assert.hasKeyAndValue(reqJson, "orgId", "未包含组织");
        // 验证请求中是否包含员工ID列表
        Assert.hasKeyAndValue(reqJson, "staffIds", "未包含员工");
    }

    /**
     * 执行保存员工组织关系命令
     * 
     * 该方法会批量处理员工与组织的关联关系，主要逻辑包括：
     * 1. 解析员工ID列表
     * 2. 检查已存在的关联关系
     * 3. 为不存在的关联关系创建新记录
     * 4. 确保操作的原子性（事务管理）
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存操作失败或参数无效时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 从请求参数中获取员工ID列表，并按逗号分割成数组
        String[] staffIds = reqJson.getString("staffIds").split(",");

        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");

        // 验证员工ID列表是否有效
        if (staffIds == null || staffIds.length < 1) {
            throw new CmdException("未包含员工信息");
        }
        
        // 定义临时变量
        OrgStaffRelDto orgRelDto = null;  // 用于查询的DTO对象
        int count = 0;                    // 记录查询结果数量
        OrgStaffRelPo orgStaffRelPo = null; // 用于保存的PO对象
        int flag = 0;                     // 保存操作结果标志

        // 遍历所有员工ID，逐个处理关联关系
        for(String staffId : staffIds){
            // 创建查询DTO，检查是否已存在关联关系
            orgRelDto = new OrgStaffRelDto();
            orgRelDto.setOrgId(reqJson.getString("orgId"));
            orgRelDto.setStaffId(staffId);
            
            // 查询该员工与组织是否已存在关联关系
            count = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRelsCount(orgRelDto);
            
            // 如果已存在关联关系，则跳过当前员工
            if(count > 0){
                continue;
            }
            
            // 创建新的组织员工关联关系对象
            orgStaffRelPo = new OrgStaffRelPo();
            orgStaffRelPo.setStoreId(storeId);                           // 设置店铺ID
            orgStaffRelPo.setRelCd(OrgStaffRelDto.REL_CD_PUBLIC);        // 设置关系编码为公共类型
            orgStaffRelPo.setRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId)); // 生成唯一关系ID
            orgStaffRelPo.setStaffId(staffId);                           // 设置员工ID
            orgStaffRelPo.setOrgId(reqJson.getString("orgId"));          // 设置组织ID
            orgStaffRelPo.setbId("-1");                                  // 设置业务ID为默认值
            
            // 执行保存操作
            flag =  orgStaffRelV1InnerServiceSMOImpl.saveOrgStaffRel(orgStaffRelPo);
            
            // 检查保存操作是否成功
            if(flag < 1){
                throw new CmdException("关联员工失败");
            }
        }

    }
}