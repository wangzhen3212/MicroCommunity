package com.java110.community.cmd.inspectionTaskDetail;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.inspection.InspectionTaskDetailDto;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IInspectionTaskDetailInnerServiceSMO;
import com.java110.intf.community.IInspectionTaskDetailV1InnerServiceSMO;
import com.java110.intf.community.IInspectionTaskInnerServiceSMO;
import com.java110.intf.community.IInspectionTaskV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.inspection.InspectionTaskDetailPo;
import com.java110.po.inspection.InspectionTaskPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 巡检任务明细更新命令类
 * 负责处理巡检任务明细的更新操作，包括巡检点完成状态更新、照片处理、巡检任务状态更新等业务逻辑
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "inspectionTaskDetail.updateInspectionTaskDetail")
public class UpdateInspectionTaskDetailCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateInspectionTaskDetailCmd.class);

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IInspectionTaskInnerServiceSMO inspectionTaskInnerServiceSMOImpl;

    @Autowired
    private IInspectionTaskDetailInnerServiceSMO inspectionTaskDetailInnerServiceSMOImpl;

    @Autowired
    private IInspectionTaskDetailV1InnerServiceSMO inspectionTaskDetailV1InnerServiceSMOImpl;

    @Autowired
    private IInspectionTaskV1InnerServiceSMO inspectionTaskV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 验证请求参数中是否包含必要的字段
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 校验必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "taskDetailId", "请求报文中未包含taskDetailId");
        Assert.hasKeyAndValue(reqJson, "taskId", "请求报文中未包含taskId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "inspectionId", "请求报文中未包含inspectionId");
        Assert.hasKeyAndValue(reqJson, "photos", "请求报文中未包含照片");
    }

    /**
     * 执行巡检任务明细更新命令
     * 主要业务流程：处理照片、更新巡检明细、检查并更新巡检任务状态
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 处理上传的照片
        if (reqJson.containsKey("photos")) {
            dealPhotos(reqJson);
        }

        // 设置巡检点完成状态
        reqJson.put("state", "20200407"); // 巡检点完成状态码
        
        try {
            // 更新巡检任务明细
            updateInspectionTaskDetail(reqJson);
        } catch (ParseException e) {
            logger.error("更新明细失败", e);
            throw new CmdException(e.getMessage());
        }

        // 检查是否存在进行中的巡检任务
        InspectionTaskDto inspectionTaskDto = new InspectionTaskDto();
        inspectionTaskDto.setTaskId(reqJson.getString("taskId"));
        inspectionTaskDto.setCommunityId(reqJson.getString("communityId"));
        inspectionTaskDto.setState("20200405"); // 进行中状态码
        List<InspectionTaskDto> inspectionTaskDtos = inspectionTaskInnerServiceSMOImpl.queryInspectionTasks(inspectionTaskDto);

        // 如果存在进行中的巡检任务，更新为巡检中状态
        if (inspectionTaskDtos != null && inspectionTaskDtos.size() > 0) {
            reqJson.put("state", "20200406"); // 巡检中状态码
            updateInspectionTask(reqJson);
        }

        // 检查是否还有未完成的巡检点
        InspectionTaskDetailDto inspectionTaskDetailDto = new InspectionTaskDetailDto();
        inspectionTaskDetailDto.setCommunityId(reqJson.getString("communityId"));
        inspectionTaskDetailDto.setTaskId(reqJson.getString("taskId"));
        inspectionTaskDetailDto.setState("20200405"); // 未完成状态码
        int count = inspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetailsCount(inspectionTaskDetailDto);

        // 如果还有未完成的巡检点，直接返回
        if (count > 0) {
            return;
        }
        
        // 所有巡检点都已完成，更新巡检任务为完成状态
        reqJson.put("state", "20200407"); // 巡检完成状态码
        updateInspectionTask(reqJson);
    }

    /**
     * 处理照片信息
     * 遍历照片数组，为每张照片调用添加照片方法
     *
     * @param reqJson 请求参数JSON对象，包含照片数组
     */
    private void dealPhotos(JSONObject reqJson) {
        JSONArray photos = reqJson.getJSONArray("photos");
        // 遍历所有照片并逐一处理
        for (int photoIndex = 0; photoIndex < photos.size(); photoIndex++) {
            Object photo = photos.get(photoIndex);
            reqJson.put("fileName", photo.toString());
            addPhoto(reqJson);
        }
    }

    /**
     * 添加照片关联信息
     * 将照片信息保存到文件关联表中
     *
     * @param paramInJson 接口调用方传入的参数，包含文件名称等信息
     * @throws CmdException 当保存照片失败时抛出异常
     */
    public void addPhoto(JSONObject paramInJson) {
        // 构建文件关联业务对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("fileRelId", "-1");
        businessUnit.put("relTypeCd", "90000"); // 关联类型编码
        businessUnit.put("saveWay", "ftp"); // 保存方式
        businessUnit.put("objId", paramInJson.getString("taskDetailId")); // 关联对象ID
        businessUnit.put("fileRealName", paramInJson.getString("fileName")); // 文件真实名称
        businessUnit.put("fileSaveName", paramInJson.getString("fileName")); // 文件保存名称
        
        // 转换为PO对象并保存
        FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
        int flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
        
        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存图片失败");
        }
    }

    /**
     * 更新巡检任务明细
     * 更新巡检点的详细信息，包括巡检时间、状态等，并根据时间判断巡检状态
     *
     * @param paramInJson 接口调用方传入的参数
     * @throws ParseException 当时间解析失败时抛出异常
     * @throws CmdException 当更新失败时抛出异常
     */
    public void updateInspectionTaskDetail(JSONObject paramInJson) throws ParseException {
        // 查询现有的巡检任务明细
        InspectionTaskDetailDto inspectionTaskDetailDto = new InspectionTaskDetailDto();
        inspectionTaskDetailDto.setTaskDetailId(paramInJson.getString("taskDetailId"));
        inspectionTaskDetailDto.setCommunityId(paramInJson.getString("communityId"));
        List<InspectionTaskDetailDto> inspectionTaskDetailDtos = inspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetails(inspectionTaskDetailDto);
        
        // 确保只查询到一条记录
        Assert.listOnlyOne(inspectionTaskDetailDtos, "未找到需要修改的活动 或多条数据");
        
        // 合并原有数据和更新数据
        JSONObject businessInspectionTaskDetail = new JSONObject();
        businessInspectionTaskDetail.putAll(BeanConvertUtil.beanCovertMap(inspectionTaskDetailDtos.get(0)));
        businessInspectionTaskDetail.putAll(paramInJson);
        
        // 转换为PO对象
        InspectionTaskDetailPo inspectionTaskDetailPoPo = BeanConvertUtil.covertBean(businessInspectionTaskDetail, InspectionTaskDetailPo.class);
        inspectionTaskDetailPoPo.setInspectionTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置巡检时间
        inspectionTaskDetailPoPo.setActUserId(paramInJson.getString("userId")); // 设置操作用户ID
        inspectionTaskDetailPoPo.setActUserName(paramInJson.getString("userName")); // 设置操作用户名称
        
        // 查询关联的巡检任务信息
        String taskId = paramInJson.getString("taskId");
        InspectionTaskDto inspectionTaskDto = new InspectionTaskDto();
        inspectionTaskDto.setTaskId(taskId);
        List<InspectionTaskDto> inspectionTaskDtos = inspectionTaskInnerServiceSMOImpl.queryInspectionTasks(inspectionTaskDto);
        Assert.listOnlyOne(inspectionTaskDtos, "查询巡检任务数据错误！");
        
        // 时间格式化和当前日期获取
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = sdf1.format(date);

        // 获取巡检点的巡检时间限制
        String inspectionStartTime = "";
        String inspectionEndTime = "";
        long planStartTime;
        long planFinishTime;
        String pointStartTime = inspectionTaskDetailDtos.get(0).getPointStartTime();
        String pointEndTime = inspectionTaskDetailDtos.get(0).getPointEndTime();
        
        // 计算计划开始时间
        if (!StringUtil.isEmpty(pointStartTime)) {
            // 使用巡检点自定义的开始时间
            inspectionStartTime = currentDate + " " + pointStartTime + ":00";
            Date startTime = format.parse(inspectionStartTime);
            planStartTime = startTime.getTime();
        } else {
            // 使用巡检任务的计划开始时间
            String planInsTime = inspectionTaskDtos.get(0).getPlanInsTime();
            Date startTime = format.parse(planInsTime);
            planStartTime = startTime.getTime();
        }
        
        // 计算计划结束时间
        if (!StringUtil.isEmpty(pointEndTime)) {
            // 使用巡检点自定义的结束时间
            inspectionEndTime = currentDate + " " + pointEndTime + ":00";
            Date endTime = format.parse(inspectionEndTime);
            planFinishTime = endTime.getTime();
        } else {
            // 使用巡检任务的计划结束时间
            String planEndTime = inspectionTaskDtos.get(0).getPlanEndTime();
            Date endTime = format.parse(planEndTime);
            planFinishTime = endTime.getTime();
        }

        // 获取巡检点签到时间并判断巡检状态
        String inspectionTime = inspectionTaskDetailPoPo.getInspectionTime();
        Date inspectionDetailTime = format.parse(inspectionTime);
        long detailTime = inspectionDetailTime.getTime();
        
        // 根据签到时间判断巡检状态
        if (detailTime < planStartTime) {
            // 早到状态
            inspectionTaskDetailPoPo.setInspectionState("40000");
        } else if (detailTime > planFinishTime) {
            // 迟到状态
            inspectionTaskDetailPoPo.setInspectionState("50000");
        } else {
            // 正常状态
            inspectionTaskDetailPoPo.setInspectionState("60000");
        }

        // 执行更新操作
        int flag = inspectionTaskDetailV1InnerServiceSMOImpl.updateInspectionTaskDetail(inspectionTaskDetailPoPo);

        // 检查更新结果
        if (flag < 1) {
            throw new CmdException("更新任务明细失败");
        }
    }

    /**
     * 更新巡检任务信息
     * 更新巡检任务的状态、实际巡检时间等信息
     *
     * @param paramInJson 接口调用方传入的参数
     * @throws CmdException 当更新失败时抛出异常
     */
    public void updateInspectionTask(JSONObject paramInJson) {
        // 查询现有的巡检任务
        InspectionTaskDto inspectionTaskDto = new InspectionTaskDto();
        inspectionTaskDto.setTaskId(paramInJson.getString("taskId"));
        inspectionTaskDto.setCommunityId(paramInJson.getString("communityId"));
        List<InspectionTaskDto> inspectionTaskDtos = inspectionTaskInnerServiceSMOImpl.queryInspectionTasks(inspectionTaskDto);

        // 确保只查询到一条记录
        Assert.listOnlyOne(inspectionTaskDtos, "未找到需要修改的巡检任务 或多条数据");

        // 合并原有数据和更新数据
        JSONObject businessInspectionTask = new JSONObject();
        businessInspectionTask.putAll(BeanConvertUtil.beanCovertMap(inspectionTaskDtos.get(0)));

        // 转换为PO对象并设置更新字段
        InspectionTaskPo inspectionTaskPo = BeanConvertUtil.covertBean(businessInspectionTask, InspectionTaskPo.class);
        inspectionTaskPo.setActInsTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置实际巡检时间
        inspectionTaskPo.setActUserId(paramInJson.getString("userId")); // 设置操作用户ID
        inspectionTaskPo.setActUserName(paramInJson.getString("userName")); // 设置操作用户名称
        inspectionTaskPo.setState(paramInJson.getString("state")); // 设置任务状态
        
        // 处理任务转派情况
        if (!StringUtil.isEmpty(paramInJson.getString("taskType")) && paramInJson.getString("taskType").equals("2000")) {
            // 设置转派相关信息
            inspectionTaskPo.setPlanUserId(paramInJson.getString("staffId")); // 设置计划用户ID
            inspectionTaskPo.setPlanUserName(paramInJson.getString("staffName")); // 设置计划用户名称
            inspectionTaskPo.setTaskType(paramInJson.getString("taskType")); // 设置任务类型
            inspectionTaskPo.setTransferDesc(paramInJson.getString("transferDesc")); // 设置转派描述
        }
        
        // 执行更新操作
        int flag = inspectionTaskV1InnerServiceSMOImpl.updateInspectionTask(inspectionTaskPo);
        
        // 检查更新结果
        if (flag < 1) {
            throw new CmdException("修改任务失败");
        }
    }
}