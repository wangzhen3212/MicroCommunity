/**
 * 巡检路线保存命令类
 * 
 * 该类用于处理巡检路线的新增保存操作，包含参数验证和业务处理逻辑
 * 通过注解@Java110Cmd声明服务代码为"inspectionRoute.saveInspectionRoute"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionRouteV1InnerServiceSMO;
import com.java110.po.inspection.InspectionRoutePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "inspectionRoute.saveInspectionRoute")
public class SaveInspectionRouteCmd extends Cmd {

    /**
     * 巡检路线服务接口，用于调用巡检路线的数据操作
     */
    @Autowired
    private IInspectionRouteV1InnerServiceSMO inspectionRouteV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 注释掉的代码：Assert.hasKeyAndValue(reqJson, "xxx", "xxx");
        
        // 验证路线名称参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "routeName", "必填，请填写路线名称，字数100个以内");
        
        // 验证巡点序列参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "seq", "必填，请选择巡点名称");
        
        // 验证小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 处理巡检路线的新增保存业务逻辑，包括生成主键、数据转换和保存操作
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 生成巡检路线ID，使用代码生成工厂生成唯一标识
        reqJson.put("inspectionRouteId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_inspectionRouteId));
        
        // 将JSON对象转换为巡检路线POJO对象
        InspectionRoutePo inspectionRoutePo = BeanConvertUtil.covertBean(reqJson, InspectionRoutePo.class);

        // 调用服务层保存巡检路线数据
        int flag = inspectionRouteV1InnerServiceSMOImpl.saveInspectionRoute(inspectionRoutePo);
        
        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("删除巡检路线失败");
        }
    }
}