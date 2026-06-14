/**
 * 资源调拨历史审核订单查询命令类
 * 
 * 该命令类用于处理资源调拨历史审核订单的查询请求，主要功能包括：
 * 1. 验证请求参数的完整性
 * 2. 查询用户参与过的调拨流程历史任务
 * 3. 刷新和补充表单数据
 * 4. 返回分页查询结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.oaWorkflow.WorkflowDto;
import com.java110.dto.purchase.AllocationStorehouseApplyDto;
import com.java110.dto.audit.AuditUser;
import com.java110.intf.common.IAllocationStorehouseUserInnerServiceSMO;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.common.IWorkflowStepStaffInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.store.IAllocationStorehouseApplyInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "resourceStore.listAllocationStoreHisAuditOrders")
public class ListAllocationStoreHisAuditOrdersCmd extends Cmd {

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
     * 验证请求参数
     * 
     * 该方法用于验证请求参数的完整性和有效性，确保必要的参数都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户ID");
        Assert.hasKeyAndValue(reqJson, "userId", "必填，请填写用户ID");
        Assert.hasKeyAndValue(reqJson, "row", "必填，请填写每页显示数");
        Assert.hasKeyAndValue(reqJson, "page", "必填，请填写页数");
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令主逻辑
     * 
     * 该方法处理资源调拨历史审核订单的查询业务逻辑，包括：
     * 1. 查询调拨流程定义
     * 2. 获取用户历史任务
     * 3. 刷新表单数据
     * 4. 构建响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 数据解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID和商户ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");
        
        // 构建OA工作流查询条件，查询调拨类型的已完成流程
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setState(OaWorkflowDto.STATE_COMPLAINT); // 设置状态为已完成
        oaWorkflowDto.setFlowType(OaWorkflowDto.FLOW_TYPE_ALLOCATION); // 设置流程类型为调拨
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);
        
        // 如果没有查询到相关流程定义，直接返回
        if (oaWorkflowDtos == null || oaWorkflowDtos.size() < 1) {
            return;
        }
        
        // 构建流程ID列表，添加默认前缀
        List<String> flowIds = new ArrayList<>();
        for (OaWorkflowDto tmpOaWorkflowDto : oaWorkflowDtos) {
            flowIds.add(WorkflowDto.DEFAULT_PROCESS + tmpOaWorkflowDto.getFlowId());
        }
        
        // 构建审核用户查询条件
        AuditUser auditUser = new AuditUser();
        auditUser.setProcessDefinitionKeys(flowIds); // 设置流程定义键
        auditUser.setUserId(userId); // 设置用户ID
        auditUser.setStoreId(storeId); // 设置商户ID
        auditUser.setPage(reqJson.getInteger("page")); // 设置页码
        auditUser.setRow(reqJson.getInteger("row")); // 设置每页显示数量
        
        // 查询用户历史任务数量
        long count = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserHistoryTaskCount(auditUser);
        List<JSONObject> datas = null;
        
        if (count > 0) {
            // 如果有历史任务，获取任务详情并刷新表单数据
            datas = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserHistoryTasks(auditUser);
            datas = refreshFormData(datas, reqJson, storeId);
        } else {
            // 如果没有历史任务，返回空列表
            datas = new ArrayList<>();
        }
        
        // 构建分页响应结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) datas.size() / (double) reqJson.getInteger("row")), datas.size(), datas);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新表单数据
     * 
     * 该方法用于刷新和补充历史任务的表单数据，包括：
     * 1. 查询调拨申请详细信息
     * 2. 转换状态码为状态名称
     * 3. 合并任务数据和表单数据
     * 
     * @param datas 原始任务数据列表
     * @param paramIn 请求参数
     * @param storeId 商户ID
     * @return 刷新后的数据列表
     */
    private List<JSONObject> refreshFormData(List<JSONObject> datas, JSONObject paramIn, String storeId) {
        List<JSONObject> params = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        
        // 提取所有任务的ID
        for (JSONObject data : datas) {
            ids.add(data.getString("id"));
        }
        
        // 如果没有任务ID，直接返回空列表
        if (ids.size() < 1) {
            return params;
        }
        
        // 查询调拨申请信息
        AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        allocationStorehouseApplyDto.setStoreId(storeId); // 设置商户ID
        allocationStorehouseApplyDto.setApplyIds(ids.toArray(new String[ids.size()])); // 设置申请ID数组
        List<AllocationStorehouseApplyDto> tmpAllocationStorehouseApplyDtos
                = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(allocationStorehouseApplyDto);
        
        // 如果没有查询到调拨申请信息，返回空列表
        if (tmpAllocationStorehouseApplyDtos == null || tmpAllocationStorehouseApplyDtos.size() < 1) {
            return params;
        }
        
        // 转换状态码为状态名称
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
        
        // 合并任务数据和表单数据
        JSONObject curTaskNode = null;
        for (JSONObject data : datas) {
            outerLoop:
            for (AllocationStorehouseApplyDto form : tmpAllocationStorehouseApplyDtos) {
                // 跳过状态为1200的表单数据
                if (!StringUtil.isEmpty(form.getState()) && form.getState().equals("1200")) {
                    continue;
                }
                
                // 如果任务ID匹配申请ID，合并数据
                if (data.getString("id").equals(form.getApplyId())) {
                    // 将表单对象转换为JSON并合并到任务数据中
                    data.putAll(BeanConvertUtil.beanCovertJson(form));
                    
                    // 检查是否已存在相同ID的数据，避免重复添加
                    if (params.size() > 0) {
                        for (JSONObject param : params) {
                            if (data.getString("id").equals(param.getString("id"))) {
                                break outerLoop;
                            }
                        }
                    }
                    
                    // 将合并后的数据添加到结果列表
                    params.add(data);
                }
            }
        }
        
        return params;
    }
}