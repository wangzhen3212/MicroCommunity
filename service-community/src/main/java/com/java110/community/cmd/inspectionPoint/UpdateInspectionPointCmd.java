/**
 * 巡检点更新命令类
 * 
 * 该类负责处理巡检点信息的更新操作，包括参数验证和业务逻辑执行。
 * 通过注解@Java110Cmd声明服务代码，与前端请求进行映射。
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
import com.java110.intf.community.IInspectionPointV1InnerServiceSMO;
import com.java110.po.inspection.InspectionPointPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "inspectionPoint.updateInspectionPoint")
public class UpdateInspectionPointCmd extends Cmd {
    
    /**
     * 巡检点服务接口，用于调用巡检点相关的业务方法
     */
    @Autowired
    private IInspectionPointV1InnerServiceSMO inspectionPointV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保更新操作的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证巡检点ID不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionId", "巡检点ID不能为空");
        // 验证巡检点名称不能为空
        Assert.hasKeyAndValue(reqJson, "inspectionName", "必填，请填写巡检点名称");
        // 验证位置信息ID不能为空
        Assert.hasKeyAndValue(reqJson, "pointObjId", "必填，请填写位置信息");
        // 验证巡检类型不能为空
        Assert.hasKeyAndValue(reqJson, "pointObjType", "必填，请填写巡检类型");
        // 验证位置信息名称不能为空
        Assert.hasKeyAndValue(reqJson, "pointObjName", "必填，请填写位置信息");
    }

    /**
     * 命令执行方法
     * 
     * 将请求参数转换为巡检点PO对象，并调用服务层进行数据更新
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为巡检点PO对象
        InspectionPointPo inspectionPointPo = BeanConvertUtil.covertBean(reqJson, InspectionPointPo.class);
        
        // 调用服务层更新巡检点信息，返回受影响的行数
        int flag = inspectionPointV1InnerServiceSMOImpl.updateInspectionPoint(inspectionPointPo);
        
        // 检查更新操作是否成功，如果受影响行数小于1，则认为更新失败
        if (flag < 1) {
            throw new CmdException("保存巡检点失败");
        }
    }
}