/**
 * 巡检计划保存命令类
 * 
 * 该类用于处理巡检计划的新增保存操作，包括巡检计划基本信息的保存和关联巡检人员的保存
 * 实现了巡检计划数据的完整保存流程
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.inspectionPlan;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IInspectionPlanV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.inspection.InspectionPlanPo;
import com.java110.po.inspection.InspectionPlanStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "inspectionPlan.saveInspectionPlan")
public class SaveInspectionPlanCmd extends Cmd {

    /**
     * 巡检计划服务接口
     */
    @Autowired
    private IInspectionPlanV1InnerServiceSMO inspectionPlanV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 巡检计划人员服务接口
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * 对传入的请求参数进行校验，确保必填字段不为空
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 校验巡检计划名称不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionPlanName", "必填，请填写巡检计划名称");
        // 校验巡检路线ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "必填，请填写巡检路线");
        // 校验执行周期不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionPlanPeriod", "必填，请选择执行周期");
        // 校验开始时间不能为空
        Assert.hasKeyAndValue(reqJson, "startTime", "必填，请选择计划开始时间");
        // 校验结束时间不能为空
        Assert.hasKeyAndValue(reqJson, "endTime", "必填，请选择结束时间");
        // 校验签到方式不能为空
        Assert.hasKeyAndValue(reqJson, "signType", "必填，请填写签到方式");
        // 校验状态不能为空
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写签到方式");
    }

    /**
     * 执行命令方法
     * 
     * 处理巡检计划的保存逻辑，包括：
     * 1. 获取当前操作用户信息
     * 2. 保存巡检计划基本信息
     * 3. 保存巡检计划关联的巡检人员信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取当前用户ID
        String userId = context.getReqHeaders().get("user-id");
        
        // 构建用户查询条件
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setRow(1);
        userDto.setPage(1);
        
        // 查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 校验用户存在且唯一
        Assert.listOnlyOne(userDtos, "员工不存在");

        // 创建巡检计划业务对象
        JSONObject businessInspectionPlan = new JSONObject();
        // 复制请求参数到业务对象
        businessInspectionPlan.putAll(reqJson);
        // 生成巡检计划ID
        businessInspectionPlan.put("inspectionPlanId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_inspectionPlanId));
        
        // 将JSON对象转换为巡检计划PO对象
        InspectionPlanPo inspectionPlanPo = BeanConvertUtil.covertBean(businessInspectionPlan, InspectionPlanPo.class);
        // 设置创建用户ID
        inspectionPlanPo.setCreateUserId(userId);
        // 设置创建用户姓名
        inspectionPlanPo.setCreateUserName(userDtos.get(0).getName());
        
        // 保存巡检计划信息
        int flag = inspectionPlanV1InnerServiceSMOImpl.saveInspectionPlan(inspectionPlanPo);
        if (flag < 1) {
            // 保存失败时抛出异常
            throw new CmdException("保存巡检计划失败");
        }

        // 获取巡检人员列表
        JSONArray staffs = reqJson.getJSONArray("staffs");
        InspectionPlanStaffPo inspectionPlanStaffPo = null;
        
        // 遍历巡检人员列表，保存每个巡检人员信息
        for (int staffIndex = 0; staffIndex < staffs.size(); staffIndex++) {
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
            
            // 保存巡检计划人员信息
            inspectionPlanStaffV1InnerServiceSMOImpl.saveInspectionPlanStaff(inspectionPlanStaffPo);
        }
    }
}