/**
 * 巡检任务保存命令类
 * 
 * 该类负责处理巡检任务的保存操作，包括参数验证和业务逻辑执行。
 * 通过注解@Java110Cmd声明服务代码，用于接收前端请求并处理巡检任务的新增功能。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.inspectionTask;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionTaskV1InnerServiceSMO;
import com.java110.po.inspection.InspectionTaskPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "inspectionTask.saveInspectionTask")
public class SaveInspectionTaskCmd extends Cmd {

    /**
     * 巡检任务服务接口，用于调用底层的业务逻辑
     */
    @Autowired
    private IInspectionTaskV1InnerServiceSMO inspectionTaskV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证前端传入的JSON请求参数是否包含必要的字段，确保数据的完整性。
     * 如果缺少必要参数，会抛出CmdException异常。
     * 
     * @param event 命令事件对象，包含请求的相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 前端传入的JSON格式请求参数
     * @throws CmdException 当请求参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证巡检计划ID是否存在
        Assert.hasKeyAndValue(reqJson, "inspectionPlanId", "请求报文中未包含inspectionPlanId");
        // 验证实际巡检时间是否存在
        Assert.hasKeyAndValue(reqJson, "actInsTime", "请求报文中未包含actInsTime");
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证实际执行用户ID是否存在
        Assert.hasKeyAndValue(reqJson, "actUserId", "请求报文中未包含actUserId");
        // 验证实际执行用户姓名是否存在
        Assert.hasKeyAndValue(reqJson, "actUserName", "请求报文中未包含actUserName");
        // 验证签到类型是否存在
        Assert.hasKeyAndValue(reqJson, "signType", "请求报文中未包含signType");
    }

    /**
     * 命令执行方法
     * 
     * 处理巡检任务的保存逻辑，包括生成任务ID、数据转换和调用服务层保存数据。
     * 如果保存失败，会抛出CmdException异常。
     * 
     * @param event 命令事件对象，包含请求的相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 前端传入的JSON格式请求参数
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 生成唯一的任务ID，使用任务ID前缀
        reqJson.put("taskId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_taskId));
        
        // 将JSON对象转换为巡检任务POJO对象
        InspectionTaskPo inspectionTaskPo = BeanConvertUtil.covertBean(reqJson, InspectionTaskPo.class);
        
        // 调用服务层保存巡检任务，返回影响的行数
        int flag = inspectionTaskV1InnerServiceSMOImpl.saveInspectionTask(inspectionTaskPo);
        
        // 检查保存是否成功，如果影响行数小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }
    }
}