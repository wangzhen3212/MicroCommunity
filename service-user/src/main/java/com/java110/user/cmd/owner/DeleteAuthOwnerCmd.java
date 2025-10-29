/**
 * 删除业主认证命令类
 * 
 * 该命令用于处理删除业主认证记录的请求，主要功能包括：
 * 1. 验证认证记录的存在性和删除权限
 * 2. 删除符合条件的业主认证记录
 * 3. 防止误删已完成的认证记录
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
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "owner.deleteAuthOwner")
public class DeleteAuthOwnerCmd extends Cmd {

    /**
     * 业主认证用户服务接口
     * 用于操作业主认证相关的数据
     */
    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证删除业主认证请求的合法性，包括：
     * 1. 检查必要参数是否存在
     * 2. 验证认证记录是否存在
     * 3. 检查认证状态是否允许删除
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取用户信息等
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中是否包含appUserId字段
        Assert.hasKeyAndValue(reqJson, "appUserId", "未包含审核记录");

        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);

        // 构建查询条件对象
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId); // 设置用户ID
        ownerAppUserDto.setAppUserId(reqJson.getString("appUserId")); // 设置认证记录ID
        
        // 查询符合条件的业主认证记录
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        // 检查查询结果是否为空
        if (ListUtil.isNull(ownerAppUserDtos)) {
            throw new CmdException("认证记录不存在");
        }

        // 检查认证记录状态，已完成的认证记录不允许删除
        if (OwnerAppUserDto.STATE_AUDIT_SUCCESS.equals(ownerAppUserDtos.get(0).getState())) {
            throw new CmdException("认证完成记录不能删除，请联系物业删除");
        }
    }

    /**
     * 执行删除业主认证命令
     * 
     * 该方法负责执行实际的删除操作，将符合条件的认证记录从系统中删除
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建删除对象
        OwnerAppUserPo ownerAppUserPo = new OwnerAppUserPo();
        ownerAppUserPo.setAppUserId(reqJson.getString("appUserId")); // 设置要删除的认证记录ID
        
        // 调用服务层执行删除操作
        ownerAppUserV1InnerServiceSMOImpl.deleteOwnerAppUser(ownerAppUserPo);
    }
}