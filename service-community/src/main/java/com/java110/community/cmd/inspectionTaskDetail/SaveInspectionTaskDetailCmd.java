/**
 * 巡检任务详情保存命令类
 * 
 * 该类负责处理巡检任务详情的保存操作，包括参数验证和业务逻辑处理。
 * 通过注解@Java110Cmd声明服务代码，与前端请求进行映射。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.inspectionTaskDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionTaskDetailV1InnerServiceSMO;
import com.java110.po.inspection.InspectionTaskDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "inspectionTaskDetail.saveInspectionTaskDetail")
public class SaveInspectionTaskDetailCmd extends Cmd {

    /**
     * 巡检任务详情服务接口，用于执行数据库操作
     */
    @Autowired
    private IInspectionTaskDetailV1InnerServiceSMO inspectionTaskDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求报文中是否包含必要的参数，确保数据的完整性。
     * 如果缺少必要参数，将抛出CmdException异常。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中是否包含taskDetailId参数
        Assert.hasKeyAndValue(reqJson, "taskDetailId", "请求报文中未包含taskDetailId");
        // 验证请求报文中是否包含taskId参数
        Assert.hasKeyAndValue(reqJson, "taskId", "请求报文中未包含taskId");
        // 验证请求报文中是否包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中是否包含inspectionId参数
        Assert.hasKeyAndValue(reqJson, "inspectionId", "请求报文中未包含inspectionId");
        // 验证请求报文中是否包含inspectionName参数
        Assert.hasKeyAndValue(reqJson, "inspectionName", "请求报文中未包含inspectionName");
        // 验证请求报文中是否包含signType参数
        Assert.hasKeyAndValue(reqJson, "signType", "请求报文中未包含signType");
    }

    /**
     * 执行命令方法
     * 
     * 主要业务逻辑处理方法，生成任务详情ID，转换数据对象并保存到数据库。
     * 如果保存失败，将抛出CmdException异常。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 生成任务详情ID，使用代码生成工厂生成唯一标识
        reqJson.put("taskDetailId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_taskDetailId));
        
        // 将JSON对象转换为巡检任务详情PO对象
        InspectionTaskDetailPo inspectionTaskPo = BeanConvertUtil.covertBean(reqJson, InspectionTaskDetailPo.class);
        
        // 调用服务层方法保存巡检任务详情数据
        int flag = inspectionTaskDetailV1InnerServiceSMOImpl.saveInspectionTaskDetail(inspectionTaskPo);
        
        // 检查保存结果，如果影响行数小于1，说明保存失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }
    }
}