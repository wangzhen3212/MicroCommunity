/**
 * 巡检计划工作人员保存命令类
 * 
 * 该类用于处理巡检计划工作人员的保存操作，包括参数验证和业务逻辑执行
 * 通过注解@Java110Cmd声明服务代码，作为命令模式的具体实现
 * 
 * @author Java110
 * @version 1.0
 */
package com.java110.community.cmd.inspectionPlanStaff;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.po.inspection.InspectionPlanStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "inspectionPlanStaff.saveInspectionPlanStaff")
public class SaveInspectionPlanStaffCmd extends Cmd {

    /**
     * 巡检计划工作人员服务接口
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求报文中的必填字段是否完整，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求报文JSON对象，包含前端传入的参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中是否包含staffId字段
        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        // 验证请求报文中是否包含staffName字段
        Assert.hasKeyAndValue(reqJson, "staffName", "请求报文中未包含staffName");
        // 验证请求报文中是否包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中是否包含startTime字段
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含startTime");
        // 验证请求报文中是否包含endTime字段
        Assert.hasKeyAndValue(reqJson, "endTime", "请求报文中未包含endTime");
        // 验证请求报文中是否包含inspectionPlanId字段
        Assert.hasKeyAndValue(reqJson, "inspectionPlanId", "请求报文中未包含inspectionPlanId");
    }

    /**
     * 命令执行方法
     * 
     * 执行巡检计划工作人员的实际保存操作，包括生成主键、数据转换和调用服务层保存
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求报文JSON对象，包含前端传入的参数
     * @throws CmdException 当保存操作失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 生成巡检计划工作人员主键ID
        reqJson.put("ipStaffId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ipStaffId));

        // 将JSON对象转换为巡检计划工作人员PO对象
        InspectionPlanStaffPo inspectionPlanStaffPo = BeanConvertUtil.covertBean(reqJson, InspectionPlanStaffPo.class);

        // 调用服务层保存巡检计划工作人员信息
        int flag = inspectionPlanStaffV1InnerServiceSMOImpl.saveInspectionPlanStaff(inspectionPlanStaffPo);
        
        // 检查保存结果，如果影响行数小于1则抛出异常
        if (flag < 1) {
            throw new CmdException("保存巡检师傅失败");
        }
    }
}