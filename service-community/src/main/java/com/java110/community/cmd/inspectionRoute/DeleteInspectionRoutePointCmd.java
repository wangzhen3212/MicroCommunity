/**
 * 删除巡检路线点命令类
 * 
 * 该命令类负责处理删除巡检路线与巡检点关联关系的业务逻辑。
 * 通过验证请求参数的有效性，查询关联关系并执行删除操作。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionRoutePointRelDto;
import com.java110.intf.community.IInspectionRoutePointRelInnerServiceSMO;
import com.java110.intf.community.IInspectionRoutePointRelV1InnerServiceSMO;
import com.java110.po.inspection.InspectionRoutePointRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "inspectionRoute.deleteInspectionRoutePoint")
public class DeleteInspectionRoutePointCmd extends Cmd {

    /**
     * 巡检路线点关联关系内部服务接口
     */
    @Autowired
    private IInspectionRoutePointRelInnerServiceSMO inspectionRoutePointRelInnerServiceSMOImpl;

    /**
     * 巡检路线点关联关系V1版本内部服务接口
     */
    @Autowired
    private IInspectionRoutePointRelV1InnerServiceSMO inspectionRoutePointRelV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作的完整性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证巡检路线ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "路线巡检路线不能为空");
        // 验证巡检点ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionId", "路线巡检点不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
    }

    /**
     * 执行删除巡检路线点命令
     * 
     * 根据请求参数查询巡检路线点关联关系，验证唯一性后执行删除操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败或查询结果不符合预期时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建查询条件DTO对象
        InspectionRoutePointRelDto inspectionRoutePointRelDto = new InspectionRoutePointRelDto();
        // 设置小区ID查询条件
        inspectionRoutePointRelDto.setCommunityId(reqJson.getString("communityId"));
        // 设置巡检点ID查询条件
        inspectionRoutePointRelDto.setInspectionId(reqJson.getString("inspectionId"));
        // 设置巡检路线ID查询条件
        inspectionRoutePointRelDto.setInspectionRouteId(reqJson.getString("inspectionRouteId"));
        
        // 根据查询条件获取巡检路线点关联关系列表
        List<InspectionRoutePointRelDto> inspectionRoutePointRelDtos = inspectionRoutePointRelInnerServiceSMOImpl.queryInspectionRoutePointRels(inspectionRoutePointRelDto);

        // 验证查询结果必须且只能有一条记录
        Assert.listOnlyOne(inspectionRoutePointRelDtos, "未查询到（或多条）要删除的 巡检路线下的巡检点");
        
        // 创建业务JSON对象用于数据转换
        JSONObject businessInspectionRoute = new JSONObject();
        // 将查询到的第一条记录转换为Map并放入JSON对象
        businessInspectionRoute.putAll(BeanConvertUtil.beanCovertMap(inspectionRoutePointRelDtos.get(0)));

        // 将JSON对象转换为PO（持久化对象）
        InspectionRoutePointRelPo inspectionRoutePointRelPo = BeanConvertUtil.covertBean(businessInspectionRoute, InspectionRoutePointRelPo.class);

        // 执行删除操作并获取影响行数
        int flag = inspectionRoutePointRelV1InnerServiceSMOImpl.deleteInspectionRoutePointRel(inspectionRoutePointRelPo);
        
        // 验证删除操作是否成功（影响行数应大于等于1）
        if (flag < 1) {
            throw new CmdException("删除巡检路线失败");
        }
    }
}