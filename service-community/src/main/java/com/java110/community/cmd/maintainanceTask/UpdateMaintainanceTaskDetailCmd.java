/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.community.cmd.maintainanceTask;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.maintainance.MaintainanceTaskDto;
import com.java110.dto.maintainance.MaintainanceTaskDetailDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IMaintainanceTaskDetailV1InnerServiceSMO;
import com.java110.intf.community.IMaintainanceTaskV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.maintainance.MaintainanceTaskPo;
import com.java110.po.maintainance.MaintainanceTaskDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * 类表述：更新
 * 服务编码：maintainanceTaskDetail.updateMaintainanceTaskDetail
 * 请求路劲：/app/maintainanceTaskDetail.UpdateMaintainanceTaskDetail
 * add by 吴学文 at 2022-11-08 16:02:23 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 维护任务明细更新命令类
 * 负责处理维护任务明细的更新操作，包括巡检点完成状态更新、图片处理、任务状态同步等业务逻辑
 * 该类继承自Cmd基类，使用@Java110Cmd注解标识为命令处理器
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-08
 */
@Java110Cmd(serviceCode = "maintainanceTask.updateMaintainanceTaskDetail")
public class UpdateMaintainanceTaskDetailCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateMaintainanceTaskDetailCmd.class);

    /** 维护任务明细服务接口 */
    @Autowired
    private IMaintainanceTaskDetailV1InnerServiceSMO maintainanceTaskDetailV1InnerServiceSMOImpl;

    /** 维护任务服务接口 */
    @Autowired
    private IMaintainanceTaskV1InnerServiceSMO maintainanceTaskV1InnerServiceSMOImpl;

    /** 文件关联服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证任务明细ID不能为空
        Assert.hasKeyAndValue(reqJson, "taskDetailId", "taskDetailId不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 处理维护任务明细的更新逻辑，包括图片处理、明细更新、任务状态同步等
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 处理上传的图片
        if (reqJson.containsKey("photos")) {
            dealPhotos(reqJson);
        }

        // 设置巡检点完成状态
        reqJson.put("state", "20200407");//巡检点完成
        try {
            // 更新维护任务明细
            updateMaintainanceTaskDetail(reqJson);
        } catch (ParseException e) {
            logger.error("更新明细失败", e);
            throw new CmdException(e.getMessage());
        }

        // 检查是否还有未完成的巡检点
        MaintainanceTaskDto maintainanceTaskDto = new MaintainanceTaskDto();
        maintainanceTaskDto.setTaskId(reqJson.getString("taskId"));
        maintainanceTaskDto.setCommunityId(reqJson.getString("communityId"));
        maintainanceTaskDto.setState("20200405");// 巡检中状态
        List<MaintainanceTaskDto> maintainanceTaskDtos = maintainanceTaskV1InnerServiceSMOImpl.queryMaintainanceTasks(maintainanceTaskDto);

        // 如果存在巡检中的任务，更新任务状态为部分完成
        if (maintainanceTaskDtos != null && maintainanceTaskDtos.size() > 0) {
            reqJson.put("state", "20200406");// 部分完成状态
            updateMaintainanceTask(reqJson);
        }

        // 查询剩余未完成的巡检点数量
        MaintainanceTaskDetailDto maintainanceTaskDetailDto = new MaintainanceTaskDetailDto();
        maintainanceTaskDetailDto.setCommunityId(reqJson.getString("communityId"));
        maintainanceTaskDetailDto.setTaskId(reqJson.getString("taskId"));
        maintainanceTaskDetailDto.setState("20200405");// 未完成状态
        int count = maintainanceTaskDetailV1InnerServiceSMOImpl.queryMaintainanceTaskDetailsCount(maintainanceTaskDetailDto);

        // 如果还有未完成的巡检点，直接返回
        if (count > 0) {//说明还没有巡检完
            return;
        }
        
        // 所有巡检点都已完成，更新任务状态为已完成
        reqJson.put("state", "20200407");//巡检完成状态
        updateMaintainanceTask(reqJson);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 处理图片方法
     * 遍历图片数组，为每个图片创建文件关联记录
     *
     * @param reqJson 请求参数JSON对象，包含图片数组
     */
    private void dealPhotos(JSONObject reqJson) {
        // 获取图片数组
        JSONArray photos = reqJson.getJSONArray("photos");
        // 遍历所有图片
        for (int photoIndex = 0; photoIndex < photos.size(); photoIndex++) {
            Object photo = photos.get(photoIndex);
            // 设置文件名并添加图片
            reqJson.put("fileName", photo.toString());
            addPhoto(reqJson);
        }
    }

    /**
     * 添加图片关联记录
     * 创建文件关联记录，将图片与任务明细关联
     *
     * @param paramInJson 接口调用方传入的参数
     * @throws CmdException 当保存图片失败时抛出异常
     */
    public void addPhoto(JSONObject paramInJson) {
        // 构建文件关联业务对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("fileRelId", GenerateCodeFactory.getGeneratorId("10"));// 生成文件关联ID
        businessUnit.put("relTypeCd", "90000");// 关联类型编码
        businessUnit.put("saveWay", "ftp");// 保存方式
        businessUnit.put("objId", paramInJson.getString("taskDetailId"));// 关联对象ID
        businessUnit.put("fileRealName", paramInJson.getString("fileName"));// 文件真实名称
        businessUnit.put("fileSaveName", paramInJson.getString("fileName"));// 文件保存名称
        
        // 转换为PO对象并保存
        FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
        int flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
        if (flag < 1) {
            throw new CmdException("保存图片失败");
        }
    }

    /**
     * 更新维护任务明细
     * 更新巡检点的状态、巡检时间等信息，并根据时间判断巡检状态
     *
     * @param paramInJson 接口调用方传入的参数
     * @throws ParseException 当时间解析失败时抛出异常
     * @throws CmdException 当更新明细失败时抛出异常
     */
    public void updateMaintainanceTaskDetail(JSONObject paramInJson) throws ParseException {
        // 查询现有的任务明细信息
        MaintainanceTaskDetailDto maintainanceTaskDetailDto = new MaintainanceTaskDetailDto();
        maintainanceTaskDetailDto.setTaskDetailId(paramInJson.getString("taskDetailId"));
        maintainanceTaskDetailDto.setCommunityId(paramInJson.getString("communityId"));
        List<MaintainanceTaskDetailDto> maintainanceTaskDetailDtos = maintainanceTaskDetailV1InnerServiceSMOImpl.queryMaintainanceTaskDetails(maintainanceTaskDetailDto);
        
        // 验证只能找到一条记录
        Assert.listOnlyOne(maintainanceTaskDetailDtos, "未找到需要修改的活动 或多条数据");
        
        // 合并现有数据和更新数据
        JSONObject businessMaintainanceTaskDetail = new JSONObject();
        businessMaintainanceTaskDetail.putAll(BeanConvertUtil.beanCovertMap(maintainanceTaskDetailDtos.get(0)));
        businessMaintainanceTaskDetail.putAll(paramInJson);
        
        // 转换为PO对象
        MaintainanceTaskDetailPo maintainanceTaskDetailPoPo = BeanConvertUtil.covertBean(businessMaintainanceTaskDetail, MaintainanceTaskDetailPo.class);
        maintainanceTaskDetailPoPo.setInspectionTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));// 设置巡检时间
        maintainanceTaskDetailPoPo.setActUserId(paramInJson.getString("userId"));// 设置操作用户ID
        maintainanceTaskDetailPoPo.setActUserName(paramInJson.getString("userName"));// 设置操作用户名称
        
        // 查询关联的维护任务信息
        String taskId = paramInJson.getString("taskId");
        MaintainanceTaskDto maintainanceTaskDto = new MaintainanceTaskDto();
        maintainanceTaskDto.setTaskId(taskId);
        List<MaintainanceTaskDto> maintainanceTaskDtos = maintainanceTaskV1InnerServiceSMOImpl.queryMaintainanceTasks(maintainanceTaskDto);
        Assert.listOnlyOne(maintainanceTaskDtos, "查询巡检任务数据错误！");
        
        // 初始化时间格式
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = sdf1.format(date);

        // 获取巡检点的巡检时间限制
        String maintainanceStartTime = "";
        String maintainanceEndTime = "";
        long planStartTime;
        long planFinishTime;
        String pointStartTime = maintainanceTaskDetailDtos.get(0).getPointStartTime();
        String pointEndTime = maintainanceTaskDetailDtos.get(0).getPointEndTime();
        
        // 处理巡检开始时间
        if (!StringUtil.isEmpty(pointStartTime)) {
            maintainanceStartTime =   pointStartTime ;
            Date startTime = format.parse(maintainanceStartTime);
            planStartTime = startTime.getTime();
        } else {
            // 使用计划巡检开始时间作为默认值
            String planInsTime = maintainanceTaskDtos.get(0).getPlanInsTime();
            Date startTime = format.parse(planInsTime);
            planStartTime = startTime.getTime();
        }
        
        // 处理巡检结束时间
        if (!StringUtil.isEmpty(pointEndTime)) {
            maintainanceEndTime =  pointEndTime ;
            Date endTime = format.parse(maintainanceEndTime);
            planFinishTime = endTime.getTime();
        } else {
            // 使用计划巡检结束时间作为默认值
            String planEndTime = maintainanceTaskDtos.get(0).getPlanEndTime();
            Date endTime = format.parse(planEndTime);
            planFinishTime = endTime.getTime();
        }

        // 获取巡检点签到时间并进行时间比较
        String maintainanceTime = maintainanceTaskDetailPoPo.getInspectionTime();
        Date maintainanceDetailTime = format.parse(maintainanceTime);
        long detailTime = maintainanceDetailTime.getTime();
        
        // 设置巡检点状态为完成
        maintainanceTaskDetailPoPo.setState(MaintainanceTaskDetailDto.STATE_FINISH);

        // 执行更新操作
        int flag = maintainanceTaskDetailV1InnerServiceSMOImpl.updateMaintainanceTaskDetail(maintainanceTaskDetailPoPo);

        if (flag < 1) {
            throw new CmdException("更新任务明细失败");
        }
    }

    /**
     * 更新维护任务信息
     * 更新维护任务的状态、实际巡检时间等信息
     *
     * @param paramInJson 接口调用方传入的参数
     * @throws CmdException 当更新任务失败时抛出异常
     */
    public void updateMaintainanceTask(JSONObject paramInJson) {
        // 查询现有的任务信息
        MaintainanceTaskDto maintainanceTaskDto = new MaintainanceTaskDto();
        maintainanceTaskDto.setTaskId(paramInJson.getString("taskId"));
        maintainanceTaskDto.setCommunityId(paramInJson.getString("communityId"));
        List<MaintainanceTaskDto> maintainanceTaskDtos = maintainanceTaskV1InnerServiceSMOImpl.queryMaintainanceTasks(maintainanceTaskDto);

        // 验证只能找到一条记录
        Assert.listOnlyOne(maintainanceTaskDtos, "未找到需要修改的巡检任务 或多条数据");

        // 合并现有数据和更新数据
        JSONObject businessMaintainanceTask = new JSONObject();
        businessMaintainanceTask.putAll(BeanConvertUtil.beanCovertMap(maintainanceTaskDtos.get(0)));

        // 转换为PO对象
        MaintainanceTaskPo maintainanceTaskPo = BeanConvertUtil.covertBean(businessMaintainanceTask, MaintainanceTaskPo.class);
        maintainanceTaskPo.setActInsTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));// 设置实际巡检时间
        maintainanceTaskPo.setActUserId(paramInJson.getString("userId"));// 设置操作用户ID
        maintainanceTaskPo.setActUserName(paramInJson.getString("userName"));// 设置操作用户名称
        maintainanceTaskPo.setState(paramInJson.getString("state"));// 设置任务状态
        
        // 处理任务转移逻辑
        if (!StringUtil.isEmpty(paramInJson.getString("taskType")) && paramInJson.getString("taskType").equals("2000")) {
            // 设置转移后的计划用户信息
            maintainanceTaskPo.setPlanUserId(paramInJson.getString("staffId"));
            maintainanceTaskPo.setPlanUserName(paramInJson.getString("staffName"));
            maintainanceTaskPo.setTaskType(paramInJson.getString("taskType"));
            maintainanceTaskPo.setTransferDesc(paramInJson.getString("transferDesc"));
        }
        
        // 执行更新操作
        int flag = maintainanceTaskV1InnerServiceSMOImpl.updateMaintainanceTask(maintainanceTaskPo);
        if (flag < 1) {
            throw new CmdException("修改任务失败");
        }
    }
}