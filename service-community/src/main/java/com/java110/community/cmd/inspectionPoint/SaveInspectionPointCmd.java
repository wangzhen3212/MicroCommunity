/**
 * 巡检点保存命令类
 * 
 * 该类负责处理巡检点的新增保存操作，包括参数验证和业务逻辑处理
 * 通过注解 @Java110Cmd 声明服务代码为 "inspectionPoint.saveInspectionPoint"
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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionPointV1InnerServiceSMO;
import com.java110.po.inspection.InspectionPointPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "inspectionPoint.saveInspectionPoint")
public class SaveInspectionPointCmd extends Cmd {

    /**
     * 巡检点服务接口
     */
    @Autowired
    private IInspectionPointV1InnerServiceSMO inspectionPointV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保数据完整性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证巡检点名称参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "inspectionName", "必填，请填写巡检点名称");
        // 验证位置信息ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "pointObjId", "必填，请填写位置信息");
        // 验证巡检类型参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "pointObjType", "必填，请填写巡检类型");
        // 验证位置信息名称参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "pointObjName", "必填，请填写位置信息");
        // 验证经度参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "lng", "必填，请填写经度");
        // 验证纬度参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "lat", "必填，请填写纬度");
    }

    /**
     * 执行命令方法
     * 
     * 处理巡检点的保存逻辑，包括生成巡检点ID、数据转换和调用服务保存
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存巡检点失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 生成巡检点ID，使用代码前缀 CODE_PREFIX_inspectionId
        reqJson.put("inspectionId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_inspectionId));
        
        // 将JSON对象转换为巡检点POJO对象
        InspectionPointPo inspectionPointPo = BeanConvertUtil.covertBean(reqJson, InspectionPointPo.class);
        
        // 调用服务接口保存巡检点数据
        int flag = inspectionPointV1InnerServiceSMOImpl.saveInspectionPoint(inspectionPointPo);
        
        // 检查保存结果，如果影响行数小于1则保存失败
        if(flag < 1){
            throw new CmdException("保存巡检点失败");
        }
    }
}