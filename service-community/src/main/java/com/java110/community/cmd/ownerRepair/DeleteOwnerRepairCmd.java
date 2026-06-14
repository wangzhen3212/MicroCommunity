/**
 * 删除业主报修工单命令类
 * 
 * 该类负责处理删除业主报修工单的业务逻辑，包括参数验证和工单删除操作。
 * 通过注解@Java110Cmd声明服务代码为"ownerRepair.deleteOwnerRepair"，
 * 当接收到对应服务请求时，Spring框架会自动调用该命令类。
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

@Java110Cmd(serviceCode = "ownerRepair.deleteOwnerRepair")
public class DeleteOwnerRepairCmd extends Cmd {

    /**
     * 报修工单服务接口，用于调用删除工单的业务方法
     */
    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必需的报修ID字段，如果缺少必要参数则抛出异常。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求JSON中必须包含repairId字段
        Assert.hasKeyAndValue(reqJson, "repairId", "报修ID不能为空");
    }

    /**
     * 执行删除业主报修工单命令
     * 
     * 将请求参数转换为报修工单PO对象，并调用服务层方法删除工单。
     * 如果删除失败（影响行数小于1），则抛出命令异常。
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出命令异常
     * @throws ParseException 当数据转换出现解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建业务处理用的JSON对象，复制请求参数
        JSONObject businessOwnerRepair = new JSONObject();
        businessOwnerRepair.putAll(reqJson);

        // 将JSON对象转换为报修工单PO对象
        RepairPoolPo repairPoolPo = BeanConvertUtil.covertBean(businessOwnerRepair, RepairPoolPo.class);
        
        // 调用服务层方法删除报修工单，返回影响的行数
        int flag = repairPoolV1InnerServiceSMOImpl.deleteRepairPoolNew(repairPoolPo);
        
        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除工单");
        }
    }
}