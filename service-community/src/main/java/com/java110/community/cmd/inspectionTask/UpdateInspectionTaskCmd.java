/**
 * 巡检任务更新命令类
 * 
 * 该类负责处理巡检任务的更新操作，包括参数验证、数据查询和任务状态更新等功能。
 * 通过注解@Java110Cmd声明服务代码，用于接收前端更新巡检任务的请求。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.inspectionTask;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.community.IInspectionTaskInnerServiceSMO;
import com.java110.intf.community.IInspectionTaskV1InnerServiceSMO;
import com.java110.po.inspection.InspectionTaskPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "inspectionTask.updateInspectionTask")
public class UpdateInspectionTaskCmd extends Cmd {

    /**
     * 文件内部服务接口
     */
    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    /**
     * 巡检任务内部服务接口
     */
    @Autowired
    private IInspectionTaskInnerServiceSMO inspectionTaskInnerServiceSMOImpl;

    /**
     * 巡检任务V1版本内部服务接口
     */
    @Autowired
    private IInspectionTaskV1InnerServiceSMO inspectionTaskV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的参数，确保更新操作的数据完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含taskId参数
        Assert.hasKeyAndValue(reqJson, "taskId", "请求报文中未包含taskId");
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中必须包含inspectionPlanId参数
        Assert.hasKeyAndValue(reqJson, "inspectionPlanId", "请求报文中未包含inspectionPlanId");
    }

    /**
     * 执行巡检任务更新命令
     * 
     * 根据请求参数更新巡检任务信息，包括任务状态、执行人员等信息的修改
     * 支持普通更新和任务转派两种操作类型
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建查询条件对象，根据社区ID和任务ID查询巡检任务
        InspectionTaskDto inspectionTaskDto1 = new InspectionTaskDto();
        inspectionTaskDto1.setCommunityId(reqJson.getString("communityId"));
        inspectionTaskDto1.setTaskId(reqJson.getString("taskId"));
        List<InspectionTaskDto> inspectionTaskDtoList = inspectionTaskInnerServiceSMOImpl.queryInspectionTasks(inspectionTaskDto1);
        
        // 验证查询结果，确保只存在一条匹配的记录
        if (inspectionTaskDtoList.size() != 1) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "未找到巡检任务信息或找到多条！");
            context.setResponseEntity(responseEntity);
            return;
        }

        // 再次查询确认任务存在且唯一
        InspectionTaskDto inspectionTaskDto = new InspectionTaskDto();
        inspectionTaskDto.setTaskId(reqJson.getString("taskId"));
        inspectionTaskDto.setCommunityId(reqJson.getString("communityId"));
        List<InspectionTaskDto> inspectionTaskDtos = inspectionTaskInnerServiceSMOImpl.queryInspectionTasks(inspectionTaskDto);

        // 断言确保查询结果只有一条记录
        Assert.listOnlyOne(inspectionTaskDtos, "未找到需要修改的巡检任务 或多条数据");

        // 将查询到的巡检任务数据转换为JSON对象，便于后续处理
        JSONObject businessInspectionTask = new JSONObject();
        businessInspectionTask.putAll(BeanConvertUtil.beanCovertMap(inspectionTaskDtos.get(0)));

        // 将JSON对象转换为巡检任务PO对象
        InspectionTaskPo inspectionTaskPo = BeanConvertUtil.covertBean(businessInspectionTask, InspectionTaskPo.class);
        
        // 设置更新参数
        // 注意：巡检时间不在此处更新，因为可能还未开始巡检
        // inspectionTaskPo.setActInsTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        
        // 设置实际执行用户信息
        inspectionTaskPo.setActUserId(reqJson.getString("userId"));
        inspectionTaskPo.setActUserName(reqJson.getString("userName"));
        // 设置任务状态
        inspectionTaskPo.setState(reqJson.getString("state"));
        
        // 处理任务转派逻辑
        if(!StringUtil.isEmpty(reqJson.getString("taskType")) && reqJson.getString("taskType").equals("2000") ){
            // 如果是任务转派类型（2000），更新计划执行人员信息和转派描述
//            inspectionTaskPo.setOriginalPlanUserId(inspectionTaskPo.getPlanUserId());
//            inspectionTaskPo.setOriginalPlanUserName(inspectionTaskPo.getPlanUserName());
            inspectionTaskPo.setPlanUserId(reqJson.getString("staffId"));
            inspectionTaskPo.setPlanUserName(reqJson.getString("staffName"));
            inspectionTaskPo.setTaskType(reqJson.getString("taskType"));
            inspectionTaskPo.setTransferDesc(reqJson.getString("transferDesc"));
        }

        // 执行更新操作
        int flag = inspectionTaskV1InnerServiceSMOImpl.updateInspectionTask(inspectionTaskPo);
        
        // 检查更新结果，如果更新失败则抛出异常
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }
    }
}