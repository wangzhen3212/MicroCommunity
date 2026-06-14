package com.java110.community.cmd.inspectionPlan;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IInspectionPlanV1InnerServiceSMO;
import com.java110.po.inspection.InspectionPlanPo;
import com.java110.po.inspection.InspectionPlanStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 更新巡检计划命令类
 * 
 * 该类负责处理巡检计划的更新操作，包括验证请求参数、更新巡检计划基本信息
 * 以及重新关联巡检人员信息等业务逻辑
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "inspectionPlan.updateInspectionPlan")
public class UpdateInspectionPlanCmd extends Cmd {

    /**
     * 巡检计划服务接口
     */
    @Autowired
    private IInspectionPlanV1InnerServiceSMO inspectionPlanV1InnerServiceSMOImpl;

    /**
     * 巡检计划人员服务接口
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证更新巡检计划所需的必填参数是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证巡检计划ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionPlanId", "inspectionPlanId不能为空");
        // 验证巡检计划名称不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionPlanName", "必填，请填写巡检计划名称");
        // 验证巡检路线ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "必填，请填写巡检路线");
        // 验证执行周期不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionPlanPeriod", "必填，请选择执行周期");
        // 验证开始时间不能为空
        Assert.hasKeyAndValue(reqJson, "startTime", "必填，请选择计划开始时间");
        // 验证结束时间不能为空
        Assert.hasKeyAndValue(reqJson, "endTime", "必填，请选择结束时间");
        // 验证签到方式不能为空
        Assert.hasKeyAndValue(reqJson, "signType", "必填，请填写签到方式");
        // 验证状态不能为空
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写签到方式");
    }

    /**
     * 执行巡检计划更新命令
     * 
     * 该方法负责执行巡检计划的更新操作，包括：
     * 1. 更新巡检计划基本信息
     * 2. 删除原有的巡检人员关联
     * 3. 重新添加新的巡检人员关联
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为巡检计划POJO对象
        InspectionPlanPo inspectionPlanPo = BeanConvertUtil.covertBean(reqJson, InspectionPlanPo.class);
        
        // 调用服务更新巡检计划基本信息
        int flag = inspectionPlanV1InnerServiceSMOImpl.updateInspectionPlan(inspectionPlanPo);
        
        // 检查更新操作是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("修改巡检计划失败");
        }

        // 创建巡检计划人员对象用于删除操作
        InspectionPlanStaffPo inspectionPlanStaffPo = null;
        inspectionPlanStaffPo = new InspectionPlanStaffPo();
        // 设置要删除的巡检计划ID
        inspectionPlanStaffPo.setInspectionPlanId(inspectionPlanPo.getInspectionPlanId());

        // 删除该巡检计划下原有的所有人员关联
        inspectionPlanStaffV1InnerServiceSMOImpl.deleteInspectionPlanStaff(inspectionPlanStaffPo);

        // 获取请求中的巡检人员列表
        JSONArray staffs = reqJson.getJSONArray("staffs");
        
        // 遍历巡检人员列表，重新建立人员关联
        for (int staffIndex = 0; staffIndex < staffs.size(); staffIndex++) {
            // 创建新的巡检计划人员对象
            inspectionPlanStaffPo = new InspectionPlanStaffPo();
            // 设置小区ID
            inspectionPlanStaffPo.setCommunityId(reqJson.getString("communityId"));
            // 设置结束时间
            inspectionPlanStaffPo.setEndTime(reqJson.getString("endTime"));
            // 设置巡检计划ID
            inspectionPlanStaffPo.setInspectionPlanId(inspectionPlanPo.getInspectionPlanId());
            // 生成巡检计划人员ID
            inspectionPlanStaffPo.setIpStaffId(GenerateCodeFactory.getGeneratorId("11"));
            // 设置人员ID
            inspectionPlanStaffPo.setStaffId(staffs.getJSONObject(staffIndex).getString("userId"));
            // 设置人员姓名
            inspectionPlanStaffPo.setStaffName(staffs.getJSONObject(staffIndex).getString("name"));
            // 设置开始时间
            inspectionPlanStaffPo.setStartTime(reqJson.getString("startTime"));
            
            // 保存巡检计划人员关联信息
            inspectionPlanStaffV1InnerServiceSMOImpl.saveInspectionPlanStaff(inspectionPlanStaffPo);
        }
    }
}