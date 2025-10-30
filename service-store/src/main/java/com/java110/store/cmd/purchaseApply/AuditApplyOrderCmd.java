package com.java110.store.cmd.purchaseApply;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 采购申请审核命令类
 * 
 * 该类负责处理采购申请单的审核流程，包括审核通过、退回、转单等操作，
 * 并与工作流引擎交互完成相应的业务流程处理
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode purchaseApply.auditApplyOrder
 */
@Java110Cmd(serviceCode = "purchaseApply.auditApplyOrder")
public class AuditApplyOrderCmd extends Cmd {

    /**
     * 采购申请服务接口
     */
    @Autowired
    private IPurchaseApplyV1InnerServiceSMO purchaseApplyV1InnerServiceSMOImpl;

    /**
     * 工作流服务接口
     */
    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证任务ID是否存在
        Assert.hasKeyAndValue(reqJson, "taskId", "未包含任务");
        // 验证采购申请单ID是否存在
        Assert.hasKeyAndValue(reqJson, "id", "订单号不能为空");
        // 验证审核状态码是否存在
        Assert.hasKeyAndValue(reqJson, "auditCode", "未包含状态");
        // 验证审核说明是否存在
        Assert.hasKeyAndValue(reqJson, "auditMessage", "未包含状态说明");
    }

    /**
     * 执行审核命令
     * 
     * 根据不同的审核状态码执行相应的业务逻辑，包括：
     * - 审核通过操作
     * - 重新提交操作
     * - 转单操作
     * - 退回操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");
        
        // 根据申请单ID查询采购申请信息
        PurchaseApplyDto purchaseApplyDto = new PurchaseApplyDto();
        purchaseApplyDto.setApplyOrderId(reqJson.getString("id"));
        List<PurchaseApplyDto> purchaseApplyDtos = purchaseApplyV1InnerServiceSMOImpl.queryPurchaseApplys(purchaseApplyDto);
        
        // 验证查询结果是否唯一
        Assert.listOnlyOne(purchaseApplyDtos, "采购申请不存在");
        
        // 准备更新采购申请单的对象
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(purchaseApplyDtos.get(0).getApplyOrderId());
        
        // 设置请求参数
        reqJson.put("id", reqJson.getString("id"));
        reqJson.put("storeId", storeId);
        
        // 根据审核状态码执行不同的业务逻辑
        // 状态码说明：
        // 1100: 审核通过
        // 1500: 审核通过（另一种情况）
        if ("1100".equals(reqJson.getString("auditCode"))
                || "1500".equals(reqJson.getString("auditCode"))) { 
            // 办理操作 - 审核通过
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 完成当前任务并判断是否为最后一个任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            if (isLastTask) {
                // 如果是最后一个任务，设置状态为已完成
                purchaseApplyPo.setState(PurchaseApplyDto.STATE_END);
            } else {
                // 如果不是最后一个任务，设置状态为处理中
                purchaseApplyPo.setState(PurchaseApplyDto.STATE_DEALING);
            }
            // 更新采购申请单状态
            purchaseApplyV1InnerServiceSMOImpl.updatePurchaseApply(purchaseApplyPo);
            
        } else if ("1600".equals(reqJson.getString("auditCode"))) { 
            // 重新提交操作
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 完成当前任务并判断是否为最后一个任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            if (isLastTask) {
                // 如果是最后一个任务，设置状态为已完成
                purchaseApplyPo.setState(PurchaseApplyDto.STATE_END);
            } else {
                // 如果不是最后一个任务，设置状态为待处理
                purchaseApplyPo.setState(PurchaseApplyDto.STATE_WAIT_DEAL);
            }
            // 更新采购申请单状态
            purchaseApplyV1InnerServiceSMOImpl.updatePurchaseApply(purchaseApplyPo);
            
        } else if ("1300".equals(reqJson.getString("auditCode"))) { 
            // 转单操作 - 将任务转给其他用户处理
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 将任务转给其他用户
            oaWorkflowUserInnerServiceSMOImpl.changeTaskToOtherUser(reqJson);
            // 设置状态为处理中
            purchaseApplyPo.setState(PurchaseApplyDto.STATE_DEALING);
            // 更新采购申请单状态
            purchaseApplyV1InnerServiceSMOImpl.updatePurchaseApply(purchaseApplyPo);
            
        } else if ("1200".equals(reqJson.getString("auditCode"))
                || "1400".equals(reqJson.getString("auditCode"))
        ) { 
            // 退回操作 - 将任务退回给发起人
            reqJson.put("startUserId", purchaseApplyDtos.get(0).getCreateUserId());
            // 退回任务
            oaWorkflowUserInnerServiceSMOImpl.goBackTask(reqJson);
            // 设置状态为审核不通过
            purchaseApplyPo.setState(PurchaseApplyDto.STATE_NOT_PASS);
            // 更新采购申请单状态
            purchaseApplyV1InnerServiceSMOImpl.updatePurchaseApply(purchaseApplyPo);
        } else {
            // 不支持的审核状态码
            throw new IllegalArgumentException("不支持的类型");
        }
    }
}