/**
 * 删除巡检路线命令类
 * 
 * 该命令类用于处理删除巡检路线的业务逻辑，包括参数验证和执行删除操作。
 * 在删除巡检路线前会检查该路线是否正在被巡检计划使用，确保数据完整性。
 * 
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionPlanDto;
import com.java110.intf.community.IInspectionPlanInnerServiceSMO;
import com.java110.intf.community.IInspectionRouteV1InnerServiceSMO;
import com.java110.po.inspection.InspectionRoutePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "inspectionRoute.deleteInspectionRoute")
public class DeleteInspectionRouteCmd extends Cmd {

    /**
     * 巡检计划内部服务接口
     */
    @Autowired
    private IInspectionPlanInnerServiceSMO inspectionPlanInnerServiceSMOImpl;

    /**
     * 巡检路线V1版本内部服务接口
     */
    @Autowired
    private IInspectionRouteV1InnerServiceSMO inspectionRouteV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证删除巡检路线所需的必要参数，并检查该巡检路线是否正在被使用。
     * 如果路线正在被巡检计划使用，则不允许删除。
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数JSON对象，包含前端传入的参数
     * @throws CmdException 当参数验证失败或路线正在使用时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含巡检路线ID和小区ID
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "路线ID不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");

        // 创建巡检计划查询对象
        InspectionPlanDto inspectionPlanDto = new InspectionPlanDto();
        // 设置查询条件：根据巡检路线ID查询
        inspectionPlanDto.setInspectionRouteId(reqJson.getString("inspectionRouteId"));
        
        // 根据巡检路线ID查询关联的巡检计划
        List<InspectionPlanDto> inspectionPlanDtos = inspectionPlanInnerServiceSMOImpl.queryInspectionPlans(inspectionPlanDto);
        
        // 如果存在关联的巡检计划，则不允许删除巡检路线
        Assert.listIsNull(inspectionPlanDtos, "该巡检路线正在使用，不能删除！");
    }

    /**
     * 执行删除巡检路线命令
     * 
     * 将请求参数转换为巡检路线PO对象，并调用服务层执行删除操作。
     * 如果删除失败，则抛出异常。
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数JSON对象，包含前端传入的参数
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为巡检路线PO对象
        InspectionRoutePo inspectionRoutePo = BeanConvertUtil.covertBean(reqJson, InspectionRoutePo.class);
        
        // 调用服务层删除巡检路线，返回影响的行数
        int flag = inspectionRouteV1InnerServiceSMOImpl.deleteInspectionRoute(inspectionRoutePo);

        // 检查删除操作是否成功，如果影响行数小于1，说明删除失败
        if (flag < 1) {
            throw new CmdException("删除巡检路线失败");
        }
    }
}