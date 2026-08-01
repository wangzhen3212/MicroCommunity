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
import com.java110.core.log.LoggerFactory;
import com.java110.dto.inspection.InspectionPlanStaffDto;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.dto.notice.NoticeStaffDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IInspectionTaskInnerServiceSMO;
import com.java110.intf.community.IInspectionTaskV1InnerServiceSMO;
import com.java110.intf.job.IMsgNotifyInnerServiceSMO;
import com.java110.po.inspection.InspectionPlanStaffPo;
import com.java110.po.inspection.InspectionTaskPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "inspectionTask.updateInspectionTask")
public class UpdateInspectionTaskCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateInspectionTaskCmd.class);

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
     * 巡检计划员工V1版本内部服务接口
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 消息通知接口（通过 Feign 调用 job-service 推送微信消息）
     */
    @Autowired
    private IMsgNotifyInnerServiceSMO msgNotifyInnerServiceSMOImpl;

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
        String originalPlanUserId = null;
        String inspectionPlanId = null;
        if(!StringUtil.isEmpty(reqJson.getString("taskType")) && reqJson.getString("taskType").equals("2000") ){
            // 从任务数据中获取巡检计划ID（不依赖前端传入）
            inspectionPlanId = inspectionTaskDtos.get(0).getInspectionPlanId();

            // 1. 判断流转员工是否已在同一巡检计划内（只查有效记录 statusCd=0）
            InspectionPlanStaffDto checkPlanStaffDto = new InspectionPlanStaffDto();
            checkPlanStaffDto.setInspectionPlanId(inspectionPlanId);
            checkPlanStaffDto.setStaffId(reqJson.getString("staffId"));
            checkPlanStaffDto.setCommunityId(reqJson.getString("communityId"));
            checkPlanStaffDto.setStatusCd("0");
            List<InspectionPlanStaffDto> existPlanStaffs = inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(checkPlanStaffDto);
            if (!ListUtil.isNull(existPlanStaffs)) {
                throw new CmdException("流转员工已在相同巡检计划内，不能流转");
            }

            // 保存原员工ID（在更新前）
            originalPlanUserId = inspectionTaskPo.getPlanUserId();

            // 2. 设置当前任务的流转信息
            inspectionTaskPo.setPlanUserId(reqJson.getString("staffId"));
            inspectionTaskPo.setPlanUserName(reqJson.getString("staffName"));
            inspectionTaskPo.setTaskType(reqJson.getString("taskType"));
            inspectionTaskPo.setTransferDesc(reqJson.getString("transferDesc"));
        }

        // 执行更新操作（流转当前任务）
        int flag = inspectionTaskV1InnerServiceSMOImpl.updateInspectionTask(inspectionTaskPo);

        // 检查更新结果，如果更新失败则抛出异常
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 流转成功后：判断是否修改计划员工关系表，再推送
        if (!StringUtil.isEmpty(reqJson.getString("taskType")) && "2000".equals(reqJson.getString("taskType"))) {
            // allTransfer=Y 时修改计划员工关系表，后续生成的任务也分配给新员工
            if ("Y".equals(reqJson.getString("allTransfer"))) {
                InspectionPlanStaffDto originalStaffDto = new InspectionPlanStaffDto();
                originalStaffDto.setInspectionPlanId(inspectionPlanId);
                originalStaffDto.setStaffId(originalPlanUserId);
                originalStaffDto.setCommunityId(reqJson.getString("communityId"));
                originalStaffDto.setStatusCd("0");
                List<InspectionPlanStaffDto> originalStaffDtos = inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(originalStaffDto);
                if (!ListUtil.isNull(originalStaffDtos)) {
                    InspectionPlanStaffDto originalStaff = originalStaffDtos.get(0);
                    InspectionPlanStaffPo planStaffPo = new InspectionPlanStaffPo();
                    planStaffPo.setIpStaffId(originalStaff.getIpStaffId());
                    planStaffPo.setInspectionPlanId(originalStaff.getInspectionPlanId());
                    planStaffPo.setCommunityId(originalStaff.getCommunityId());
                    planStaffPo.setStaffId(reqJson.getString("staffId"));
                    planStaffPo.setStaffName(reqJson.getString("staffName"));
                    planStaffPo.setStartTime(originalStaff.getStartTime());
                    planStaffPo.setEndTime(originalStaff.getEndTime());
                    int updateFlag = inspectionPlanStaffV1InnerServiceSMOImpl.updateInspectionPlanStaff(planStaffPo);
                    if (updateFlag < 1) {
                        throw new CmdException("修改计划员工关系失败");
                    }
                }
            }

            // 推送微信通知给新员工
            try {
                NoticeStaffDto noticeStaffDto = new NoticeStaffDto(
                        reqJson.getString("communityId"),
                        reqJson.getString("staffId"),
                        "您有一个巡检任务已流转给您，请及时处理",
                        "系统管理员"
                );
                msgNotifyInnerServiceSMOImpl.sendStaffMsg(noticeStaffDto);
            } catch (Exception e) {
                // 推送失败不阻断流转业务，仅记录日志
                logger.error("流转后推送微信通知失败，staffId=" + reqJson.getString("staffId"), e);
            }
        }
    }
}