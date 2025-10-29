package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionRoutePointRelV1InnerServiceSMO;
import com.java110.po.inspection.InspectionRoutePointRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 巡检路线点位关联保存命令类
 * 
 * 该类用于处理巡检路线与巡检点位的关联关系保存操作，支持单个点位和批量点位的关联保存
 * 通过注解@Java110Cmd标识服务代码为inspectionRoute.saveInspectionRoutePoint
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspectionRoute.saveInspectionRoutePoint")
public class SaveInspectionRoutePointCmd extends Cmd {
    
    /**
     * 巡检路线点位关联服务接口
     */
    @Autowired
    private IInspectionRoutePointRelV1InnerServiceSMO inspectionRoutePointRelV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，包括巡检点、巡检路线和小区ID等必要参数
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查请求参数中是否包含单个巡检点ID
        if (reqJson.containsKey("inspectionId")) {
            // 如果包含单个巡检点ID，则验证该字段不能为空
            Assert.hasKeyAndValue(reqJson, "inspectionId", "必填，请填写巡检点");
        } else {
            // 如果不包含单个巡检点ID，则验证批量巡检点数组不能为空
            Assert.hasKeyAndValue(reqJson, "points", "必填，请填写多个巡检点");
        }
        // 验证巡检路线ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "必填，请填写巡检路线");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 根据请求参数执行巡检路线点位的保存操作，支持单个点位和批量点位两种模式
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 判断是否为单个巡检点保存模式
        if (reqJson.containsKey("inspectionId")) {
            // 单个巡检点模式：直接添加巡检路线关联
            addInspectionRoute(reqJson);
        } else { 
            // 批量巡检点模式：遍历巡检点数组，逐个添加关联关系
            JSONArray points = reqJson.getJSONArray("points");
            for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {
                // 从巡检点数组中获取当前巡检点的ID和名称
                reqJson.put("inspectionId", points.getJSONObject(pointIndex).getString("inspectionId"));
                reqJson.put("inspectionName", points.getJSONObject(pointIndex).getString("inspectionName"));
                // 为每个巡检点添加路线关联
                addInspectionRoute(reqJson);
            }
        }
    }

    /**
     * 添加巡检路线关联关系
     * 
     * 生成关联关系ID，将参数转换为PO对象并调用服务层保存关联关系
     * 
     * @param paramInJson 包含巡检路线和点位信息的参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    public void addInspectionRoute(JSONObject paramInJson) {
        // 生成巡检路线点位关联关系ID
        paramInJson.put("irpRelId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_irpRelId));
        
        // 将JSON参数转换为巡检路线点位关联PO对象
        InspectionRoutePointRelPo inspectionRoutePointRelPo = BeanConvertUtil.covertBean(paramInJson, InspectionRoutePointRelPo.class);
        
        // 调用服务层保存关联关系，返回影响的行数
        int flag = inspectionRoutePointRelV1InnerServiceSMOImpl.saveInspectionRoutePointRel(inspectionRoutePointRelPo);
        
        // 检查保存操作是否成功，如果影响行数小于1则抛出异常
        if (flag < 1) {
            throw new CmdException("删除巡检路线失败");
        }
    }
}