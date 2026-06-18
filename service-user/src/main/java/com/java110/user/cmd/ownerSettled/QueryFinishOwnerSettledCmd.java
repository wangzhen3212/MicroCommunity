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
 * 查询已办业主入驻申请命令类
 * 负责查询当前用户已处理的业主入驻申请流程记录
 * 通过工作流引擎获取历史任务记录，并关联查询对应的业主入驻申请表单数据
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode ownerSettled.queryFinishOwnerSettled
 */
@Java110Cmd(serviceCode = "ownerSettled.queryFinishOwnerSettled")
public class QueryFinishOwnerSettledCmd extends Cmd {

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
     * 主要验证分页参数是否合法
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 查询当前用户已处理的业主入驻申请流程记录
     * 包括工作流历史任务记录和对应的表单数据
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID和店铺ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");

        // 查询业主入驻类型的工作流定义
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setState(OaWorkflowDto.STATE_COMPLAINT); // 设置工作流状态为已投诉
        oaWorkflowDto.setFlowType(OaWorkflowDto.FLOW_TYPE_OWNER_SETTLED); // 设置流程类型为业主入驻
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);

        // 如果没有找到对应的工作流定义，直接返回
        if (oaWorkflowDtos == null || oaWorkflowDtos.size() < 1) {
            return;
        }
        
        // 构建流程定义Key列表，添加默认前缀
        List<String> flowIds = new ArrayList<>();
        for (OaWorkflowDto tmpOaWorkflowDto : oaWorkflowDtos) {
            flowIds.add(WorkflowDto.DEFAULT_PROCESS + tmpOaWorkflowDto.getFlowId());
        }

        // 设置审计用户查询参数
        AuditUser auditUser = new AuditUser();
        auditUser.setProcessDefinitionKeys(flowIds); // 设置流程定义Key列表
        auditUser.setUserId(userId); // 设置用户ID
        auditUser.setStoreId(storeId); // 设置店铺ID
        auditUser.setPage(reqJson.getInteger("page")); // 设置页码
        auditUser.setRow(reqJson.getInteger("row")); // 设置每页记录数

        // 查询用户历史任务数量
        long count = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserHistoryTaskCount(auditUser);

        List<JSONObject> datas = null;

        if (count > 0) {
            // 查询用户历史任务详情
            datas = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserHistoryTasks(auditUser);
            // 刷新表单数据，关联查询业主入驻申请信息
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
     * 根据工作流任务ID关联查询业主入驻申请表单数据
     * 并将表单数据合并到任务数据中
     *
     * @param datas 工作流任务数据列表
     * @param paramIn 请求参数JSON对象
     */
    private void refreshFormData(List<JSONObject> datas, JSONObject paramIn) {
        // 提取所有任务ID
        List<String> ids = new ArrayList<>();
        for (JSONObject data : datas) {
            ids.add(data.getString("id"));
        }
        
        // 如果没有任务ID，直接返回
        if (ids.size() < 1) {
            return;
        }

        // 根据任务ID查询对应的业主入驻申请记录
        OwnerSettledApplyDto ownerSettledApplyDto = new OwnerSettledApplyDto();
        ownerSettledApplyDto.setApplyIds(ids.toArray(new String[ids.size()]));
        List<OwnerSettledApplyDto> ownerSettledApplyDtos = ownerSettledApplyV1InnerServiceSMOImpl.queryOwnerSettledApplys(ownerSettledApplyDto);
        
        // 如果没有查询到申请记录，直接返回
        if (ownerSettledApplyDtos == null || ownerSettledApplyDtos.size() < 1) {
            return;
        }
        
        // 将业主入驻申请表单数据合并到任务数据中
        for (JSONObject data : datas) {
            for (OwnerSettledApplyDto form : ownerSettledApplyDtos) {
                // 根据申请ID匹配对应的表单数据
                if (data.getString("id").equals(form.getApplyId())) {
                    // 将表单对象转换为JSON并合并到任务数据中
                    data.putAll(BeanConvertUtil.beanCovertJson(form));
                }
            }
        }
    }
}