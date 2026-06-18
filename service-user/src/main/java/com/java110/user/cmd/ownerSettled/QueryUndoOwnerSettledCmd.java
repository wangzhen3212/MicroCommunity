package com.java110.user.cmd.ownerSettled;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.owner.OwnerSettledApplyDto;
import com.java110.dto.oaWorkflow.WorkflowDto;
import com.java110.dto.audit.AuditUser;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.user.IOwnerSettledApplyV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询待审核业主入驻申请命令类
 * 功能：查询当前用户需要处理的待审核业主入驻申请列表
 * 用途：在业主入驻审批流程中，获取待当前用户审批的申请记录
 */
@Java110Cmd(serviceCode = "ownerSettled.queryUndoOwnerSettled")
public class QueryUndoOwnerSettledCmd extends Cmd {

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowInnerServiceSMO oaWorkflowInnerServiceSMOImpl;

    @Autowired
    private IOwnerSettledApplyV1InnerServiceSMO ownerSettledApplyV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法 - 查询待审核业主入驻申请
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID和商铺ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");

        // 查询业主入驻类型的工作流定义
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setState(OaWorkflowDto.STATE_COMPLAINT); // 设置工作流状态为投诉状态
        oaWorkflowDto.setFlowType(OaWorkflowDto.FLOW_TYPE_OWNER_SETTLED); // 设置流程类型为业主入驻
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);

        // 如果没有找到对应的工作流定义，直接返回
        if (oaWorkflowDtos == null || oaWorkflowDtos.size() < 1) {
            return;
        }
        
        // 构建流程定义key列表
        List<String> flowIds = new ArrayList<>();
        for (OaWorkflowDto tmpOaWorkflowDto : oaWorkflowDtos) {
            // 拼接流程定义key：默认前缀 + 流程ID
            flowIds.add(WorkflowDto.DEFAULT_PROCESS + tmpOaWorkflowDto.getFlowId());
        }

        // 构建审核用户查询条件
        AuditUser auditUser = new AuditUser();
        auditUser.setProcessDefinitionKeys(flowIds); // 设置流程定义key列表
        auditUser.setUserId(userId); // 设置用户ID
        auditUser.setStoreId(storeId); // 设置商铺ID
        auditUser.setPage(reqJson.getInteger("page")); // 设置页码
        auditUser.setRow(reqJson.getInteger("row")); // 设置每页行数

        // 查询待处理任务数量
        long count = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserTaskCount(auditUser);

        List<JSONObject> datas = null;

        if (count > 0) {
            // 查询待处理任务详情
            datas = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserTasks(auditUser);
            // 刷新表单数据，补充业主入驻申请的具体信息
            refreshFormData(datas, reqJson);
        } else {
            datas = new ArrayList<>();
        }

        // 构建分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, datas);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新表单数据方法
     * 功能：根据任务ID列表查询对应的业主入驻申请详情，并补充到任务数据中
     * @param datas 任务数据列表
     * @param paramIn 请求参数
     */
    private void refreshFormData(List<JSONObject> datas, JSONObject paramIn) {
        // 从任务数据中提取申请ID列表
        List<String> ids = new ArrayList<>();
        for (JSONObject data : datas) {
            ids.add(data.getString("id"));
        }
        
        // 如果没有申请ID，直接返回
        if (ids.size() < 1) {
            return;
        }

        // 构建业主入驻申请查询条件
        OwnerSettledApplyDto ownerSettledApplyDto = new OwnerSettledApplyDto();
        ownerSettledApplyDto.setApplyIds(ids.toArray(new String[ids.size()])); // 设置申请ID数组
        // 查询业主入驻申请详情
        List<OwnerSettledApplyDto> ownerSettledApplyDtos = ownerSettledApplyV1InnerServiceSMOImpl.queryOwnerSettledApplys(ownerSettledApplyDto);
        
        // 如果没有查询到申请详情，直接返回
        if (ownerSettledApplyDtos == null || ownerSettledApplyDtos.size() < 1) {
            return;
        }
        
        // 将业主入驻申请详情合并到任务数据中
        for (JSONObject data : datas) {
            for (OwnerSettledApplyDto form : ownerSettledApplyDtos) {
                // 根据申请ID匹配对应的申请详情
                if (data.getString("id").equals(form.getApplyId())) {
                    // 将申请详情转换为JSON并合并到任务数据中
                    data.putAll(BeanConvertUtil.beanCovertJson(form));
                }
            }
        }
    }
}