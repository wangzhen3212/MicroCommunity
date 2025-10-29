package com.java110.community.cmd.inspectionPlan;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.intf.community.IInspectionPlanV1InnerServiceSMO;
import com.java110.intf.community.IInspectionTaskInnerServiceSMO;
import com.java110.po.inspection.InspectionPlanPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 删除巡检计划命令类
 * 
 * 该类负责处理删除巡检计划的业务逻辑，包括参数验证和执行删除操作。
 * 在删除巡检计划前会检查该计划是否正在被巡检任务使用，如果正在使用则不允许删除。
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode inspectionPlan.deleteInspectionPlan
 */
@Java110Cmd(serviceCode = "inspectionPlan.deleteInspectionPlan")
public class DeleteInspectionPlanCmd extends Cmd {

    /**
     * 巡检任务内部服务接口，用于查询巡检任务信息
     */
    @Autowired
    private IInspectionTaskInnerServiceSMO inspectionTaskInnerServiceSMOImpl;

    /**
     * 巡检计划内部服务接口，用于执行巡检计划的删除操作
     */
    @Autowired
    private IInspectionPlanV1InnerServiceSMO inspectionPlanV1InnerServiceSMOImpl;

    /**
     * 验证删除巡检计划的请求参数
     * 
     * 该方法主要验证：
     * 1. 请求参数中必须包含巡检计划ID
     * 2. 检查该巡检计划是否正在被巡检任务使用
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含巡检计划ID
        Assert.hasKeyAndValue(reqJson, "inspectionPlanId", "巡检计划名称不能为空");
        
        // 创建巡检任务查询条件对象
        InspectionTaskDto inspectionTaskDto = new InspectionTaskDto();
        // 设置查询条件：根据巡检计划ID查询
        inspectionTaskDto.setInspectionPlanId(reqJson.getString("inspectionPlanId"));
        
        // 根据巡检计划ID查询是否存在关联的巡检任务
        List<InspectionTaskDto> inspectionTaskDtos = inspectionTaskInnerServiceSMOImpl.queryInspectionTasks(inspectionTaskDto);
        
        // 如果存在关联的巡检任务，则不允许删除巡检计划
        Assert.listIsNull(inspectionTaskDtos, "该巡检计划正在使用，不能删除！");
    }

    /**
     * 执行删除巡检计划的操作
     * 
     * 该方法将请求参数转换为巡检计划对象，并调用服务层执行删除操作。
     * 如果删除失败（影响行数小于1），则抛出异常。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为巡检计划PO对象
        InspectionPlanPo inspectionPlanPo = BeanConvertUtil.covertBean(reqJson, InspectionPlanPo.class);
        
        // 调用服务层执行删除操作，返回影响的行数
        int flag = inspectionPlanV1InnerServiceSMOImpl.deleteInspectionPlan(inspectionPlanPo);

        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除巡检计划失败");
        }
    }
}