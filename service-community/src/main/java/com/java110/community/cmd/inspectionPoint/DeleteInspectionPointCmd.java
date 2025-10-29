/**
 * 删除巡检点命令类
 * 
 * 该类负责处理删除巡检点的业务逻辑，包括验证删除条件和执行删除操作。
 * 在删除巡检点前会检查该巡检点是否被巡检路线关联，如果已被关联则不允许删除。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.inspectionPoint;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionRoutePointRelDto;
import com.java110.intf.community.IInspectionPointV1InnerServiceSMO;
import com.java110.intf.community.IInspectionRoutePointRelInnerServiceSMO;
import com.java110.po.inspection.InspectionPointPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "inspectionPoint.deleteInspectionPoint")
public class DeleteInspectionPointCmd extends Cmd {

    /**
     * 巡检路线点位关系服务接口
     * 用于查询巡检点与巡检路线的关联关系
     */
    @Autowired
    private IInspectionRoutePointRelInnerServiceSMO inspectionRoutePointRelInnerServiceSMOImpl;

    /**
     * 巡检点服务接口
     * 用于执行巡检点的删除操作
     */
    @Autowired
    private IInspectionPointV1InnerServiceSMO inspectionPointV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的巡检点ID信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含inspectionId字段
        Assert.hasKeyAndValue(reqJson, "inspectionId", "巡检点ID不能为空");
    }

    /**
     * 执行删除巡检点命令
     * 
     * 主要执行逻辑：
     * 1. 检查巡检点是否被巡检路线关联
     * 2. 如果未被关联，则执行删除操作
     * 3. 如果删除失败，抛出异常
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败或巡检点正在使用时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检点PO对象
        InspectionPointPo inspectionPointPo = BeanConvertUtil.covertBean(reqJson, InspectionPointPo.class);
        
        // 创建巡检路线点位关系查询对象
        InspectionRoutePointRelDto inspectionRoutePointRelDto = new InspectionRoutePointRelDto();
        // 设置要查询的巡检点ID
        inspectionRoutePointRelDto.setInspectionId(inspectionPointPo.getInspectionId());
        
        // 查询巡检点巡检路线关系表，检查该巡检点是否被巡检路线关联
        List<InspectionRoutePointRelDto> inspectionRoutePointRelDtos = inspectionRoutePointRelInnerServiceSMOImpl.queryInspectionRoutePointRels(inspectionRoutePointRelDto);
        
        // 如果查询结果不为空，说明巡检点正在被使用，不允许删除
        Assert.listIsNull(inspectionRoutePointRelDtos, "该巡检点正在使用，不能删除！");

        // 执行删除巡检点操作
        int flag = inspectionPointV1InnerServiceSMOImpl.deleteInspectionPoint(inspectionPointPo);
        
        // 检查删除操作是否成功，如果影响行数小于1，说明删除失败
        if (flag < 1) {
            throw new CmdException("删除巡检点失败");
        }
    }
}