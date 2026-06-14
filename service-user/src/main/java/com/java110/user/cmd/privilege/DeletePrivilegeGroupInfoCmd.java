package com.java110.user.cmd.privilege;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.PrivilegeRelDto;
import com.java110.dto.privilege.PrivilegeUserDto;
import com.java110.intf.order.IPrivilegeInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IPrivilegeGroupV1InnerServiceSMO;
import com.java110.intf.user.IPrivilegeRelV1InnerServiceSMO;
import com.java110.intf.user.IPrivilegeUserV1InnerServiceSMO;
import com.java110.po.privilege.PrivilegeGroupPo;
import com.java110.po.privilege.PrivilegeRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 删除权限组信息命令类
 * 该命令用于删除权限组信息，包括权限组本身及其关联的权限关系
 * 在删除前会检查权限组下是否有关联的员工，如果存在关联员工则不允许删除
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode delete.privilegeGroup.info
 */
@Java110Cmd(serviceCode = "delete.privilegeGroup.info")
public class DeletePrivilegeGroupInfoCmd extends Cmd {

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IPrivilegeInnerServiceSMO privilegeInnerServiceSMOImpl;

    @Autowired
    private IPrivilegeGroupV1InnerServiceSMO privilegeGroupV1InnerServiceSMOImpl;

    @Autowired
    private IPrivilegeRelV1InnerServiceSMO privilegeRelV1InnerServiceSMOImpl;

    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的pgId字段
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含pgId字段
        Assert.hasKeyAndValue(reqJson, "pgId", "角色不存在");
    }

    /**
     * 执行删除权限组信息的核心命令方法
     * 该方法会先检查权限组下是否有关联员工，如果没有则删除权限组及其所有关联的权限关系
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象，包含要删除的权限组信息
     * @throws CmdException    当删除操作失败时抛出异常
     * @throws ParseException  当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建权限用户查询条件对象
        PrivilegeUserDto privilegeUserDto = new PrivilegeUserDto();
        privilegeUserDto.setpId(reqJson.getString("pgId"));
        
        // 查询该权限组下是否有关联的员工
        List<PrivilegeUserDto> privilegeUserDtos = privilegeUserV1InnerServiceSMOImpl.queryPrivilegeUsers(privilegeUserDto);
        
        // 如果存在关联员工，则不允许删除，抛出异常
        if (!ListUtil.isNull(privilegeUserDtos)) {
            throw new IllegalArgumentException("该角色下有关联员工，请先删除关联员工！");
        }
        
        // 将请求JSON数据转换为权限组PO对象
        PrivilegeGroupPo privilegeGroupPo = BeanConvertUtil.covertBean(reqJson, PrivilegeGroupPo.class);
        
        // 执行权限组删除操作
        int flag = privilegeGroupV1InnerServiceSMOImpl.deletePrivilegeGroup(privilegeGroupPo);
        
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除失败");
        }
        
        // 创建权限关系查询条件对象
        PrivilegeRelDto privilegeRelDto = new PrivilegeRelDto();
        privilegeRelDto.setPgId(reqJson.getString("pgId"));
        
        // 查询该权限组下的所有权限关系
        List<PrivilegeRelDto> privilegeRelDtos = privilegeRelV1InnerServiceSMOImpl.queryPrivilegeRels(privilegeRelDto);
        
        // 如果没有权限关系，直接返回
        if (privilegeRelDtos == null || privilegeRelDtos.size() < 1) {
            return;
        }
        
        // 遍历所有权限关系，逐个删除
        PrivilegeRelPo privilegeRelPo = null;
        for (PrivilegeRelDto tmpPrivilegeDto : privilegeRelDtos) {
            privilegeRelPo = new PrivilegeRelPo();
            privilegeRelPo.setRelId(tmpPrivilegeDto.getRelId());
            
            // 删除单个权限关系
            flag = privilegeRelV1InnerServiceSMOImpl.deletePrivilegeRel(privilegeRelPo);
            
            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("删除失败");
            }
        }
    }
}