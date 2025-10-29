package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.visit.VisitDto;
import com.java110.dto.visit.VisitSettingDto;
import com.java110.dto.oaWorkflow.WorkflowDto;
import com.java110.dto.audit.AuditUser;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.community.IVisitSettingV1InnerServiceSMO;
import com.java110.intf.community.IVisitV1InnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询待处理的访客申请命令类
 * 主要功能：查询当前用户需要处理的访客申请代办任务，包括工作流任务查询、访客数据刷新、业主信息补充等
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "visit.queryUndoVisit")
public class QueryUndoVisitCmd extends Cmd {

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowInnerServiceSMO oaWorkflowInnerServiceSMOImpl;

    @Autowired
    private IVisitV1InnerServiceSMO visitV1InnerServiceSMOImpl;

    @Autowired
    private IVisitSettingV1InnerServiceSMO visitSettingV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 主要验证分页参数是否合法
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException    命令异常
     * @throws ParseException  解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令主方法
     * 查询当前用户的访客申请代办任务，包括工作流任务查询、数据刷新和结果返回
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException    命令异常
     * @throws ParseException  解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID和店铺ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");

        // 查询访客类型的工作流配置
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setState(OaWorkflowDto.STATE_COMPLAINT); // 设置工作流状态
        oaWorkflowDto.setFlowType(OaWorkflowDto.FLOW_TYPE_VISIT); // 设置流程类型为访客
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);

        // 如果没有配置工作流，直接返回
        if (oaWorkflowDtos == null || oaWorkflowDtos.size() < 1) {
            return;
        }
        
        // 构建流程定义key列表
        List<String> flowIds = new ArrayList<>();
        for (OaWorkflowDto tmpOaWorkflowDto : oaWorkflowDtos) {
            flowIds.add(WorkflowDto.DEFAULT_PROCESS + tmpOaWorkflowDto.getFlowId());
        }

        // 设置审核用户查询参数
        AuditUser auditUser = new AuditUser();
        auditUser.setProcessDefinitionKeys(flowIds); // 设置流程定义key
        auditUser.setUserId(userId); // 设置用户ID
        auditUser.setStoreId(storeId); // 设置店铺ID
        auditUser.setPage(reqJson.getInteger("page")); // 设置页码
        auditUser.setRow(reqJson.getInteger("row")); // 设置每页行数

        // 查询用户待处理任务数量
        long count = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserTaskCount(auditUser);

        List<JSONObject> datas = null;

        if (count > 0) {
            // 查询用户待处理任务详情
            datas = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserTasks(auditUser);
            // 刷新表单数据
            refreshFormData(datas, reqJson);

            // 刷新流程设置信息
            refreshSetting(datas, reqJson);

            // 刷新业主信息
            refreshOwners(datas, reqJson);
        } else {
            datas = new ArrayList<>();
        }

        // 构建分页结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, datas);

        // 设置响应结果
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新业主信息
     * 根据访客数据中的业主ID，查询并补充业主姓名和电话信息
     *
     * @param datas   待处理的访客数据列表
     * @param reqJson 请求参数JSON对象
     */
    private void refreshOwners(List<JSONObject> datas, JSONObject reqJson) {
        // 数据为空检查
        if (datas == null || datas.size() < 1) {
            return;
        }

        // 收集所有业主ID
        List<String> ownerIds = new ArrayList<>();
        for (JSONObject apiVisitDataVo : datas) {
            ownerIds.add(apiVisitDataVo.getString("ownerId"));
        }

        // 查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setOwnerIds(ownerIds.toArray(new String[ownerIds.size()])); // 设置业主ID数组
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER); // 设置业主类型
        ownerDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 为每个访客记录补充业主信息
        for (JSONObject apiVisitDataVo : datas) {
            if (apiVisitDataVo.containsKey("ownerId") && StringUtil.isEmpty(apiVisitDataVo.getString("ownerId"))) {
                for (OwnerDto tmpOwnerDto : ownerDtos) {
                    // 匹配业主ID
                    if (!StringUtil.isEmpty(apiVisitDataVo.getString("ownerId")) &&
                            !StringUtil.isEmpty(tmpOwnerDto.getOwnerId()) && !apiVisitDataVo.getString("ownerId").equals(tmpOwnerDto.getOwnerId())) {
                        continue;
                    }
                    // 补充业主姓名和电话
                    apiVisitDataVo.put("ownerName", tmpOwnerDto.getName());
                    apiVisitDataVo.put("ownerTel", tmpOwnerDto.getLink());
                }
            }
        }
    }

    /**
     * 刷新表单数据
     * 根据任务ID查询访客详细信息并合并到结果数据中
     *
     * @param datas   待处理的访客数据列表
     * @param paramIn 请求参数JSON对象
     */
    private void refreshFormData(List<JSONObject> datas, JSONObject paramIn) {
        // 收集所有访客记录ID
        List<String> ids = new ArrayList<>();
        for (JSONObject data : datas) {
            ids.add(data.getString("id"));
        }
        if (ids.size() < 1) {
            return;
        }

        // 查询访客详细信息
        VisitDto visitDto = new VisitDto();
        visitDto.setvIds(ids.toArray(new String[ids.size()])); // 设置访客ID数组
        List<VisitDto> itemReleaseDtos = visitV1InnerServiceSMOImpl.queryVisits(visitDto);
        if (itemReleaseDtos == null || itemReleaseDtos.size() < 1) {
            return;
        }
        
        // 合并访客详细信息到结果数据中
        for (JSONObject data : datas) {
            for (VisitDto form : itemReleaseDtos) {
                if (data.getString("id").equals(form.getvId())) {
                    // 将访客详细信息转换为JSON并合并到当前数据
                    data.putAll(BeanConvertUtil.beanCovertJson(form));
                }
            }
        }
    }

    /**
     * 刷新流程设置信息
     * 查询小区访客设置信息，为每个任务添加流程ID
     *
     * @param datas   待处理的访客数据列表
     * @param reqJson 请求参数JSON对象
     */
    private void refreshSetting(List<JSONObject> datas, JSONObject reqJson) {
        // 查询小区访客设置
        VisitSettingDto visitSettingDto = new VisitSettingDto();
        visitSettingDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        List<VisitSettingDto> visitSettingDtos = visitSettingV1InnerServiceSMOImpl.queryVisitSettings(visitSettingDto);

        // 检查查询结果
        if (visitSettingDtos == null || visitSettingDtos.size() < 1) {
            return;
        }

        if (datas == null || datas.size() < 1) {
            return;
        }

        // 为每个任务添加流程ID
        for (JSONObject data : datas) {
            data.put("flowId", visitSettingDtos.get(0).getFlowId());
        }
    }
}