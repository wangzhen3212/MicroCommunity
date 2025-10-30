package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.oaWorkflow.WorkflowDto;
import com.java110.dto.oaWorkflow.WorkflowStepStaffDto;
import com.java110.dto.purchase.AllocationStorehouseApplyDto;
import com.java110.dto.audit.AuditUser;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.intf.common.IAllocationStorehouseUserInnerServiceSMO;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.common.IWorkflowStepStaffInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.store.IAllocationStorehouseApplyInnerServiceSMO;
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

/**
 * 调拨审核订单查询命令类
 * 功能：查询当前用户需要审核的调拨订单列表，支持分页查询
 * 用途：用于仓库管理系统中的调拨审核流程，展示待审核的调拨申请信息
 */
@Java110Cmd(serviceCode = "resourceStore.listAllocationStoreAuditOrders")
public class ListAllocationStoreAuditOrdersCmd extends Cmd {

    @Autowired
    private IAllocationStorehouseUserInnerServiceSMO allocationStorehouseUserInnerServiceSMOImpl;

    @Autowired
    private IWorkflowStepStaffInnerServiceSMO workflowStepStaffInnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowInnerServiceSMO oaWorkflowInnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowActivitiInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的分页信息
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数是否存在
        Assert.hasKeyAndValue(reqJson, "row", "必填，请填写每页显示数");
        Assert.hasKeyAndValue(reqJson, "page", "必填，请填写页数");

        // 调用父类分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 查询当前用户的调拨审核订单列表
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException    命令执行异常
     * @throws ParseException  数据解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID和店铺ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");

        // 查询调拨类型的有效工作流
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setState(OaWorkflowDto.STATE_COMPLAINT); // 设置工作流状态为有效
        oaWorkflowDto.setFlowType(OaWorkflowDto.FLOW_TYPE_ALLOCATION); // 设置流程类型为调拨
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);

        // 如果没有找到对应的工作流，直接返回空结果
        if (oaWorkflowDtos == null || oaWorkflowDtos.size() < 1) {
            return;
        }

        // 构建流程定义key列表，用于查询用户任务
        List<String> flowIds = new ArrayList<>();
        for (OaWorkflowDto tmpOaWorkflowDto : oaWorkflowDtos) {
            // 拼接流程定义key，格式为：DEFAULT_PROCESS + flowId
            flowIds.add(WorkflowDto.DEFAULT_PROCESS + tmpOaWorkflowDto.getFlowId());
        }

        // 构建审核用户查询条件
        AuditUser auditUser = new AuditUser();
        auditUser.setProcessDefinitionKeys(flowIds); // 设置流程定义key列表
        auditUser.setUserId(userId); // 设置用户ID
        auditUser.setStoreId(storeId); // 设置店铺ID
        auditUser.setPage(reqJson.getInteger("page")); // 设置页码
        auditUser.setRow(reqJson.getInteger("row")); // 设置每页显示数量

        // 查询用户任务总数
        long count = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserTaskCount(auditUser);

        List<JSONObject> datas = null;

        if (count > 0) {
            // 如果有任务数据，查询具体的任务列表
            datas = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserTasks(auditUser);
            // 刷新表单数据，补充调拨申请的具体信息
            refreshFormData(datas, reqJson, storeId, oaWorkflowDtos.get(0).getFlowId());
        } else {
            // 如果没有任务数据，返回空列表
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
     * 为工作流任务数据补充调拨申请的具体信息和当前节点信息
     *
     * @param datas    工作流任务数据列表
     * @param paramIn  请求参数
     * @param storeId  店铺ID
     * @param flowId   流程ID
     */
    private void refreshFormData(List<JSONObject> datas, JSONObject paramIn, String storeId, String flowId) {
        // 构建调拨申请ID列表
        List<String> ids = new ArrayList<>();
        for (JSONObject data : datas) {
            data.put("flowId", flowId); // 为每个任务数据添加流程ID
            ids.add(data.getString("id")); // 收集调拨申请ID
        }

        // 如果没有申请ID，直接返回
        if (ids.size() < 1) {
            return;
        }

        // 查询调拨申请详细信息
        AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        allocationStorehouseApplyDto.setStoreId(storeId); // 设置店铺ID
        allocationStorehouseApplyDto.setApplyIds(ids.toArray(new String[ids.size()])); // 设置申请ID数组
        List<AllocationStorehouseApplyDto> tmpAllocationStorehouseApplyDtos
                = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(allocationStorehouseApplyDto);

        // 如果没有查询到调拨申请数据，直接返回
        if (tmpAllocationStorehouseApplyDtos == null || tmpAllocationStorehouseApplyDtos.size() < 1) {
            return;
        }

        // 为调拨申请状态设置中文名称
        for (AllocationStorehouseApplyDto tmpAllocationStorehouseApplyDto : tmpAllocationStorehouseApplyDtos) {
            switch (tmpAllocationStorehouseApplyDto.getState()) {
                case "1000":
                    tmpAllocationStorehouseApplyDto.setStateName("待审核");
                    break;
                case "1001":
                    tmpAllocationStorehouseApplyDto.setStateName("审核中");
                    break;
                case "1002":
                    tmpAllocationStorehouseApplyDto.setStateName("已审核");
                    break;
            }
        }

        // 为每个任务数据补充当前节点名称和表单数据
        JSONObject curTaskNode = null;
        for (JSONObject data : datas) {
            // 查询当前任务节点信息
            curTaskNode = new JSONObject();
            curTaskNode.put("taskId", data.getString("taskId")); // 设置任务ID
            curTaskNode = oaWorkflowActivitiInnerServiceSMOImpl.getCurrentNodeTask(curTaskNode); // 查询当前节点
            data.put("curTaskName", curTaskNode.getString("curTaskName")); // 添加当前节点名称

            // 将调拨申请表单数据合并到任务数据中
            for (AllocationStorehouseApplyDto form : tmpAllocationStorehouseApplyDtos) {
                if (data.getString("id").equals(form.getApplyId())) {
                    // 使用Bean转换工具将表单对象转换为JSON并合并
                    data.putAll(BeanConvertUtil.beanCovertJson(form));
                }
            }
        }
    }
}