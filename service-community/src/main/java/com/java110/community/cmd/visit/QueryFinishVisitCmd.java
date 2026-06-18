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
 * 查询访客已办单命令类
 * 负责查询当前用户已处理的访客申请记录，包括访客信息、业主信息和流程设置信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "visit.queryFinishVisit")
public class QueryFinishVisitCmd extends Cmd {

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
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 查询当前用户已处理的访客申请记录，并组装返回数据
     *
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID和店铺ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");

        // 查询访客流程配置信息
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setState(OaWorkflowDto.STATE_COMPLAINT);
        oaWorkflowDto.setFlowType(OaWorkflowDto.FLOW_TYPE_VISIT);
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);

        // 如果没有配置访客流程，直接返回
        if (oaWorkflowDtos == null || oaWorkflowDtos.size() < 1) {
            return;
        }
        
        // 构建流程定义key列表
        List<String> flowIds = new ArrayList<>();
        for (OaWorkflowDto tmpOaWorkflowDto : oaWorkflowDtos) {
            flowIds.add(WorkflowDto.DEFAULT_PROCESS + tmpOaWorkflowDto.getFlowId());
        }

        // 设置审计用户查询条件
        AuditUser auditUser = new AuditUser();
        auditUser.setProcessDefinitionKeys(flowIds);
        auditUser.setUserId(userId);
        auditUser.setStoreId(storeId);
        auditUser.setPage(reqJson.getInteger("page"));
        auditUser.setRow(reqJson.getInteger("row"));

        // 查询用户历史任务数量
        long count = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserHistoryTaskCount(auditUser);

        List<JSONObject> datas = null;

        if (count > 0) {
            // 查询用户历史任务详情
            datas = oaWorkflowUserInnerServiceSMOImpl.getDefinitionKeysUserHistoryTasks(auditUser);
            // 刷新表单数据
            refreshFormData(datas, reqJson);

            // 刷新流程设置信息
            refreshSetting(datas, reqJson);

            // 刷新业主信息
            refreshOwners(datas, reqJson);

        } else {
            datas = new ArrayList<>();
        }

        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, datas);

        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);

    }

    /**
     * 刷新业主信息
     * 根据访客记录中的业主ID查询业主详细信息并填充到结果中
     *
     * @param datas 访客数据列表
     * @param reqJson 请求参数JSON对象
     */
    private void refreshOwners(List<JSONObject> datas, JSONObject reqJson) {

        if(datas == null || datas.size() < 1){
            return ;
        }

        // 收集所有业主ID
        List<String> ownerIds = new ArrayList<>();
        for(JSONObject apiVisitDataVo: datas){
            ownerIds.add(apiVisitDataVo.getString("ownerId"));
        }

        // 查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setOwnerIds(ownerIds.toArray(new String[ownerIds.size()]));
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos =  ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 将业主信息填充到访客数据中
        for(JSONObject apiVisitDataVo: datas){
            for(OwnerDto tmpOwnerDto : ownerDtos){
                // 跳过空值检查
                if(StringUtil.isEmpty(apiVisitDataVo.getString("ownerId"))){
                    continue;
                }
                if(StringUtil.isEmpty(tmpOwnerDto.getOwnerId())){
                    continue;
                }
                // 匹配业主ID
                if(!apiVisitDataVo.getString("ownerId").equals(tmpOwnerDto.getOwnerId())){
                    continue;
                }
                // 填充业主姓名和电话
                apiVisitDataVo.put("ownerName",tmpOwnerDto.getName());
                apiVisitDataVo.put("ownerTel",tmpOwnerDto.getLink());
            }
        }

    }

    /**
     * 刷新表单数据
     * 根据流程实例ID查询访客详细信息并合并到结果中
     *
     * @param datas 访客数据列表
     * @param paramIn 请求参数JSON对象
     */
    private void refreshFormData(List<JSONObject> datas, JSONObject paramIn) {

        // 收集所有访客ID
        List<String> ids = new ArrayList<>();
        for (JSONObject data : datas) {
            ids.add(data.getString("id"));
        }
        if (ids.size() < 1) {
            return;
        }

        // 查询访客详细信息
        VisitDto visitDto = new VisitDto();
        visitDto.setvIds(ids.toArray(new String[ids.size()]));
        List<VisitDto> itemReleaseDtos = visitV1InnerServiceSMOImpl.queryVisits(visitDto);
        if (itemReleaseDtos == null || itemReleaseDtos.size() < 1) {
            return;
        }
        
        // 将访客详细信息合并到结果数据中
        for (JSONObject data : datas) {
            for (VisitDto form : itemReleaseDtos) {
                if (data.getString("id").equals(form.getvId())) {
                    data.putAll(BeanConvertUtil.beanCovertJson(form));
                }
            }
        }
    }

    /**
     * 刷新流程设置信息
     * 查询社区访客流程设置并将流程ID填充到结果中
     *
     * @param datas 访客数据列表
     * @param reqJson 请求参数JSON对象
     */
    private void refreshSetting(List<JSONObject> datas, JSONObject reqJson) {
        // 查询访客流程设置
        VisitSettingDto visitSettingDto = new VisitSettingDto();
        visitSettingDto.setCommunityId(reqJson.getString("communityId"));
        List<VisitSettingDto> visitSettingDtos = visitSettingV1InnerServiceSMOImpl.queryVisitSettings(visitSettingDto);

        if (visitSettingDtos == null || visitSettingDtos.size() < 1) {
            return;
        }

        if(datas == null || datas.size() < 1){
            return;
        }

        // 将流程ID填充到所有数据中
        for(JSONObject data : datas){
            data.put("flowId",visitSettingDtos.get(0).getFlowId());
        }

    }

}