/**
 * 领用审核订单查询命令类
 * 
 * 该命令类用于处理领用审核订单的查询请求，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 查询当前用户需要审核的领用订单
 * 3. 获取工作流相关信息
 * 4. 刷新和格式化表单数据
 * 5. 返回分页查询结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.collection;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.audit.AuditUser;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.oaWorkflow.WorkflowDto;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.common.IResourceEntryStoreInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "/collection/getCollectionAuditOrder")
public class GetCollectionAuditOrderCmd extends Cmd {

    /** 资源入库服务接口 */
    @Autowired
    private IResourceEntryStoreInnerServiceSMO resourceEntryStoreInnerServiceSMOImpl;

    /** OA工作流用户服务接口 */
    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    /** OA工作流服务接口 */
    @Autowired
    private IOaWorkflowInnerServiceSMO oaWorkflowInnerServiceSMOImpl;

    /** 采购申请服务接口 */
    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    /** OA工作流Activiti服务接口 */
    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowActivitiInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否包含必要的分页信息，并调用父类的分页信息验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中是否包含必要的分页字段
        Assert.hasKeyAndValue(reqJson, "row", "必填，请填写每页显示数");
        Assert.hasKeyAndValue(reqJson, "page", "必填，请填写页数");

        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 主要执行逻辑：
     * 1. 获取当前用户和店铺信息
     * 2. 查询相关的工作流定义
     * 3. 获取用户需要审核的任务列表
     * 4. 刷新表单数据并格式化返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID和店铺ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");

        // 构建工作流查询条件，查询资源出库类型的工作流
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setState(OaWorkflowDto.STATE_COMPLAINT); // 设置工作流状态
        oaWorkflowDto.setFlowType(OaWorkflowDto.FLOW_TYPE_RESOURCE_OUT); // 设置流程类型为资源出库
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);

        // 如果没有查询到相关的工作流定义，直接返回
        if (oaWorkflowDtos == null || oaWorkflowDtos.size() < 1) {
            return;
        }
        
        // 构建流程定义Key列表，用于后续查询用户任务
        List<String> flowIds = new ArrayList<>();
        for (OaWorkflowDto tmpOaWorkflowDto : oaWorkflowDtos) {
            flowIds.add(WorkflowDto.DEFAULT_PROCESS + tmpOaWorkflowDto.getFlowId());
        }

        // 构建审核用户查询条件
        AuditUser auditUser = new AuditUser();
        auditUser.setProcessDefinitionKeys(flowIds); // 设置流程定义Key列表
        auditUser.setUserId(userId); // 设置用户ID
        auditUser.setStoreId(storeId); // 设置店铺ID
        auditUser.setPage(reqJson.getInteger("page")); // 设置页码
        auditUser.setRow(reqJson.getInteger("row")); // 设置每页显示数量

        // 查询用户任务总数
        long count = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserTaskCount(auditUser);

        List<JSONObject> datas = null;

        // 如果有任务数据，则查询详细任务列表并刷新表单数据
        if (count > 0) {
            datas = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserTasks(auditUser);
            // 刷新表单数据，将工作流数据与业务数据关联
            refreshFormData(datas, reqJson, storeId, oaWorkflowDtos.get(0).getFlowId());
        } else {
            datas = new ArrayList<>();
        }

        // 构建分页返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, datas);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新表单数据方法
     * 
     * 将工作流任务数据与实际的采购申请数据进行关联和格式化：
     * 1. 提取任务ID列表
     * 2. 查询对应的采购申请数据
     * 3. 格式化状态和类型信息
     * 4. 获取当前任务节点信息
     * 5. 将业务数据合并到任务数据中
     * 
     * @param datas 工作流任务数据列表
     * @param paramIn 请求参数JSON对象
     * @param storeId 店铺ID
     * @param flowId 流程ID
     */
    private void refreshFormData(List<JSONObject> datas, JSONObject paramIn, String storeId, String flowId) {
        // 提取所有任务ID
        List<String> ids = new ArrayList<>();
        for (JSONObject data : datas) {
            ids.add(data.getString("id"));
            data.put("flowId", flowId); // 为每个任务数据添加流程ID
        }
        
        // 如果没有任务数据，直接返回
        if (ids.size() < 1) {
            return;
        }

        // 查询采购申请数据
        PurchaseApplyDto purchaseApplyDto = new PurchaseApplyDto();
        purchaseApplyDto.setStoreId(storeId); // 设置店铺ID
        purchaseApplyDto.setApplyOrderIds(ids.toArray(new String[ids.size()])); // 设置申请订单ID数组
        List<PurchaseApplyDto> tmpPurchaseApplyDtos = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplyAndDetails(purchaseApplyDto);

        // 如果没有查询到采购申请数据，直接返回
        if (tmpPurchaseApplyDtos == null || tmpPurchaseApplyDtos.size() < 1) {
            return;
        }

        // 格式化采购申请数据的状态和类型信息
        for (PurchaseApplyDto tmpPurchaseApplyDto : tmpPurchaseApplyDtos) {
            // 根据状态码设置状态名称
            switch (tmpPurchaseApplyDto.getState()) {
                case "1000":
                    tmpPurchaseApplyDto.setStateName("待审核");
                    break;
                case "1001":
                    tmpPurchaseApplyDto.setStateName("审核中");
                    break;
                case "1002":
                    tmpPurchaseApplyDto.setStateName("已审核");
                    break;
            }
            
            // 根据资源订单类型设置类型名称
            if (tmpPurchaseApplyDto.getResOrderType().equals("10000")) {
                tmpPurchaseApplyDto.setResOrderTypeName("采购申请");
            } else {
                tmpPurchaseApplyDto.setResOrderTypeName("出库申请");
            }
        }
        
        JSONObject curTaskNode = null;
        // 为每个任务数据添加当前节点信息和业务数据
        for (JSONObject data : datas) {
            // 查询当前任务节点信息
            curTaskNode = new JSONObject();
            curTaskNode.put("taskId", data.getString("taskId"));
            curTaskNode = oaWorkflowActivitiInnerServiceSMOImpl.getCurrentNodeTask(curTaskNode);
            data.put("curTaskName", curTaskNode.getString("curTaskName")); // 添加当前任务节点名称

            // 将采购申请数据合并到任务数据中
            for (PurchaseApplyDto form : tmpPurchaseApplyDtos) {
                if (data.getString("id").equals(form.getApplyOrderId())) {
                    data.putAll(BeanConvertUtil.beanCovertJson(form)); // 将Bean转换为JSON并合并
                }
            }
        }
    }
}