package com.java110.community.cmd.inspectionPlanStaff;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.po.inspection.InspectionPlanStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 删除巡检计划工作人员命令类
 * 
 * 该类负责处理删除巡检计划工作人员的请求，包括参数验证和执行删除操作
 * 通过注解@Java110Cmd声明服务代码为"inspectionPlanStaff.deleteInspectionPlanStaff"
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspectionPlanStaff.deleteInspectionPlanStaff")
public class DeleteInspectionPlanStaffCmd extends Cmd {

    /**
     * 巡检计划工作人员服务接口
     * 用于执行巡检计划工作人员的相关数据库操作
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含ipStaffId字段
        Assert.hasKeyAndValue(reqJson, "ipStaffId", "ipStaffId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "小区信息不能为空");
    }

    /**
     * 执行删除巡检计划工作人员命令
     * 
     * 将请求参数转换为实体对象，并调用服务层执行删除操作
     * 如果删除失败则抛出异常
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为InspectionPlanStaffPo实体对象
        InspectionPlanStaffPo inspectionPlanStaffPo = BeanConvertUtil.covertBean(reqJson, InspectionPlanStaffPo.class);
        
        // 调用服务层执行删除操作，返回受影响的行数
        int flag = inspectionPlanStaffV1InnerServiceSMOImpl.deleteInspectionPlanStaff(inspectionPlanStaffPo);
        
        // 检查删除操作是否成功，如果受影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除巡检师傅失败");
        }
    }
}