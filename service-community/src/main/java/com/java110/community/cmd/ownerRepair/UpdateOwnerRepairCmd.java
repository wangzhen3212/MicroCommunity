/**
 * 业主报修更新命令类
 * 
 * 该类负责处理业主报修信息的更新操作，包括参数验证和业务逻辑执行。
 * 通过注解@Java110Cmd声明服务代码为"ownerRepair.updateOwnerRepair"，
 * 当接收到对应服务请求时会自动调用该类进行处理。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IRepairPoolV1InnerServiceSMO;
import com.java110.po.owner.RepairPoolPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

@Java110Cmd(serviceCode = "ownerRepair.updateOwnerRepair")
public class UpdateOwnerRepairCmd extends Cmd {

    /**
     * 报修池服务接口，用于操作报修数据
     */
    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 
     * 对传入的请求参数进行完整性验证，确保更新操作所需的关键字段都存在且不为空。
     * 如果验证失败会抛出CmdException异常。
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证报修ID不能为空
        Assert.hasKeyAndValue(reqJson, "repairId", "报修ID不能为空");
        // 验证报修类型不能为空
        Assert.hasKeyAndValue(reqJson, "repairType", "必填，请选择报修类型");
        // 验证报修人名称不能为空
        Assert.hasKeyAndValue(reqJson, "repairName", "必填，请填写报修人名称");
        // 验证报修人手机号不能为空
        Assert.hasKeyAndValue(reqJson, "tel", "必填，请填写报修人手机号");
        // 验证报修对象类型不能为空
        Assert.hasKeyAndValue(reqJson, "repairObjType", "必填，请填写报修对象类型");
        // 验证报修对象ID不能为空
        Assert.hasKeyAndValue(reqJson, "repairObjId", "必填，请填写报修对象ID");
        // 验证报修对象名称不能为空
        Assert.hasKeyAndValue(reqJson, "repairObjName", "必填，请填写报修对象名称");
        // 验证预约时间不能为空
        Assert.hasKeyAndValue(reqJson, "appointmentTime", "必填，请填写预约时间");
        // 验证报修内容不能为空
        Assert.hasKeyAndValue(reqJson, "context", "必填，请填写报修内容");
    }

    /**
     * 执行命令方法
     * 
     * 处理业主报修信息的更新操作，将请求数据转换为业务对象并调用服务层进行数据更新。
     * 如果更新失败会抛出CmdException异常。
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建业务对象，复制请求参数
        JSONObject businessOwnerRepair = new JSONObject();
        businessOwnerRepair.putAll(reqJson);

        // 将JSON对象转换为报修池PO对象
        RepairPoolPo repairPoolPo = BeanConvertUtil.covertBean(businessOwnerRepair, RepairPoolPo.class);
        
        // 调用服务层更新报修信息，返回影响的行数
        int flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
        
        // 检查更新结果，如果影响行数小于1说明更新失败
        if (flag < 1) {
            throw new CmdException("删除工单");
        }
    }
}