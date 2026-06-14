/**
 * 巡检计划状态更新命令类
 * 
 * 该类用于处理巡检计划状态更新的业务逻辑，通过接收前端请求参数，
 * 验证参数合法性后更新指定巡检计划的状态信息。
 * 主要功能包括参数验证、数据查询和状态更新操作。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.inspectionPlan;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionPlanDto;
import com.java110.intf.community.IInspectionPlanInnerServiceSMO;
import com.java110.intf.community.IInspectionPlanV1InnerServiceSMO;
import com.java110.po.inspection.InspectionPlanPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "inspectionPlan.updateInspectionPlanState")
public class UpdateInspectionPlanStateCmd extends Cmd {

    /**
     * 巡检计划内部服务接口
     * 用于查询巡检计划相关信息
     */
    @Autowired
    private IInspectionPlanInnerServiceSMO inspectionPlanInnerServiceSMOImpl;

    /**
     * 巡检计划V1版本内部服务接口
     * 用于更新巡检计划状态信息
     */
    @Autowired
    private IInspectionPlanV1InnerServiceSMO inspectionPlanV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证巡检计划ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionPlanId", "inspectionPlanId不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        // 验证状态字段不能为空
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写状态");
    }

    /**
     * 执行命令方法
     * 
     * 根据请求参数更新巡检计划状态，包括查询现有计划信息、
     * 构建更新对象和执行更新操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败或数据异常时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 创建巡检计划数据传输对象
        InspectionPlanDto inspectionPlanDto = new InspectionPlanDto();
        // 设置小区ID用于查询
        inspectionPlanDto.setCommunityId(reqJson.getString("communityId"));
        // 设置巡检计划ID用于查询
        inspectionPlanDto.setInspectionPlanId(reqJson.getString("inspectionPlanId"));
        
        // 根据条件查询巡检计划列表
        List<InspectionPlanDto> inspectionPlanDtos = inspectionPlanInnerServiceSMOImpl.queryInspectionPlans(inspectionPlanDto);

        // 验证查询结果只能有一条记录，确保数据准确性
        Assert.listOnlyOne(inspectionPlanDtos, "根据计划ID查询到多条记录，请检查数据");
        
        // 创建业务巡检计划JSON对象
        JSONObject businessInspectionPlan = new JSONObject();
        // 将查询到的巡检计划对象转换为Map并放入JSON对象
        businessInspectionPlan.putAll(BeanConvertUtil.beanCovertMap(inspectionPlanDtos.get(0)));
        // 设置新的状态值
        businessInspectionPlan.put("state", reqJson.getString("state"));
        
        // 将JSON对象转换为巡检计划持久化对象
        InspectionPlanPo inspectionPlanPo = BeanConvertUtil.covertBean(businessInspectionPlan, InspectionPlanPo.class);
        
        // 执行更新操作并获取影响行数
        int flag = inspectionPlanV1InnerServiceSMOImpl.updateInspectionPlan(inspectionPlanPo);
        
        // 验证更新操作是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("修改巡检计划失败");
        }
    }
}