package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IInspectionRouteV1InnerServiceSMO;
import com.java110.po.inspection.InspectionRoutePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 更新巡检路线命令类
 * 
 * 该类负责处理更新巡检路线的业务逻辑，包括参数验证和数据更新操作
 * 通过注解@Java110Cmd声明服务代码，与前端请求进行映射
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspectionRoute.updateInspectionRoute")
public class UpdateInspectionRouteCmd extends Cmd {

    /**
     * 巡检路线服务接口，用于调用巡检路线的业务逻辑
     */
    @Autowired
    private IInspectionRouteV1InnerServiceSMO inspectionRouteV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 对请求参数进行校验，确保必填参数不为空且符合业务规则
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 校验巡检路线ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "路线ID不能为空");
        // 校验路线名称不能为空，且字数限制在100个以内
        Assert.hasKeyAndValue(reqJson, "routeName", "必填，请填写路线名称，字数100个以内");
        // 校验巡点名称不能为空
        Assert.hasKeyAndValue(reqJson, "seq", "必填，请选择巡点名称");
        // 校验小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 将请求参数转换为业务对象，并调用服务层更新巡检路线信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为巡检路线POJO对象
        InspectionRoutePo inspectionRoutePo = BeanConvertUtil.covertBean(reqJson, InspectionRoutePo.class);

        // 调用服务层更新巡检路线信息，返回受影响的行数
        int flag = inspectionRouteV1InnerServiceSMOImpl.updateInspectionRoute(inspectionRoutePo);
        
        // 检查更新操作是否成功，如果受影响行数小于1则认为更新失败
        if (flag < 1) {
            throw new CmdException("删除巡检路线失败");
        }
    }
}