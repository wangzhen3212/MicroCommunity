package com.java110.user.cmd.ownerSettled;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerSettledApplyDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.user.IOwnerSettledApplyV1InnerServiceSMO;
import com.java110.intf.user.IOwnerSettledSettingV1InnerServiceSMO;
import com.java110.po.owner.OwnerSettledApplyPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 业主入驻申请审核撤销命令类
 * 负责处理业主入驻申请的审核撤销操作，包括办理、转单、退回等审核流程操作
 * 通过工作流引擎完成相应的审核任务处理，并更新申请状态
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "ownerSettled.auditUndoOwnerSettled")
public class AuditUndoOwnerSettledCmd extends Cmd {

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    @Autowired
    private IOwnerSettledApplyV1InnerServiceSMO ownerSettledApplyV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerSettledSettingV1InnerServiceSMO ownerSettledSettingV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查请求JSON中是否包含必要的参数，确保请求数据的完整性
     *
     * @param event   命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   当命令执行出现异常时抛出
     * @throws ParseException 当数据解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必须的参数是否存在
        Assert.hasKeyAndValue(reqJson, "taskId", "未包含任务");
        Assert.hasKeyAndValue(reqJson, "applyId", "未包含applyId");
        Assert.hasKeyAndValue(reqJson, "flowId", "未包含流程");
        Assert.hasKeyAndValue(reqJson, "auditCode", "未包含状态");
        Assert.hasKeyAndValue(reqJson, "auditMessage", "未包含状态说明");
    }

    /**
     * 执行审核撤销命令
     * 根据审核码执行不同的审核操作：办理、转单或退回
     * 更新业主入驻申请的状态并完成相应的工作流任务
     *
     * @param event   命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   当命令执行出现异常时抛出
     * @throws ParseException 当数据解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");

        // 根据申请ID查询业主入驻申请信息
        OwnerSettledApplyDto ownerSettledApplyDto = new OwnerSettledApplyDto();
        ownerSettledApplyDto.setApplyId(reqJson.getString("applyId"));
        List<OwnerSettledApplyDto> ownerSettledApplyDtos = ownerSettledApplyV1InnerServiceSMOImpl.queryOwnerSettledApplys(ownerSettledApplyDto);

        // 验证申请是否存在且唯一
        Assert.listOnlyOne(ownerSettledApplyDtos, "入驻申请不存在");

        // 准备更新申请状态的PO对象
        OwnerSettledApplyPo ownerSettledApplyPo = new OwnerSettledApplyPo();
        ownerSettledApplyPo.setApplyId(ownerSettledApplyDtos.get(0).getApplyId());
        
        // 在请求JSON中添加必要参数
        reqJson.put("id", reqJson.getString("applyId"));
        reqJson.put("storeId", storeId);

        // 根据审核码执行不同的审核操作
        String auditCode = reqJson.getString("auditCode");
        
        // 办理操作：1100-通过，1500-其他办理操作
        if ("1100".equals(auditCode) || "1500".equals(auditCode)) {
            // 设置下一处理人
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 完成当前任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            
            // 根据是否最后一个任务设置申请状态
            if (isLastTask) {
                ownerSettledApplyPo.setState(OwnerSettledApplyDto.STATE_COMPLETE); // 审核完成
            } else {
                ownerSettledApplyPo.setState(OwnerSettledApplyDto.STATE_DOING); // 审核中
            }
            // 更新申请状态
            ownerSettledApplyV1InnerServiceSMOImpl.updateOwnerSettledApply(ownerSettledApplyPo);
            
        } 
        // 转单操作：1300-转交给其他处理人
        else if ("1300".equals(auditCode)) {
            // 设置下一处理人
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 将任务转交给其他用户
            oaWorkflowUserInnerServiceSMOImpl.changeTaskToOtherUser(reqJson);
            // 设置申请状态为审核中
            ownerSettledApplyPo.setState(OwnerSettledApplyDto.STATE_DOING);
            // 更新申请状态
            ownerSettledApplyV1InnerServiceSMOImpl.updateOwnerSettledApply(ownerSettledApplyPo);
            
        } 
        // 退回操作：1200-退回，1400-其他退回操作
        else if ("1200".equals(auditCode) || "1400".equals(auditCode)) {
            // 退回任务
            oaWorkflowUserInnerServiceSMOImpl.goBackTask(reqJson);
            // 设置申请状态为审核失败
            ownerSettledApplyPo.setState(OwnerSettledApplyDto.STATE_FAIT);
            // 更新申请状态
            ownerSettledApplyV1InnerServiceSMOImpl.updateOwnerSettledApply(ownerSettledApplyPo);
            
        } 
        // 不支持的审核码类型
        else {
            throw new IllegalArgumentException("不支持的类型");
        }
    }
}