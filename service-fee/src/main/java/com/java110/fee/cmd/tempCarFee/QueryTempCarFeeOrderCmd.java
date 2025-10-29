package com.java110.fee.cmd.tempCarFee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.ITempCarFeeCreateOrderV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 临时停车费订单查询命令类
 * 
 * 该类用于处理临时停车费订单查询请求，负责验证请求参数并调用相应的服务接口
 * 服务编码：tempCarFee.queryTempCarFeeOrder
 * 
 * @author 吴学文 at 2021-09-16 22:26:04 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "tempCarFee.queryTempCarFeeOrder")
public class QueryTempCarFeeOrderCmd extends Cmd {
    
    /**
     * 临时停车费创建订单服务接口
     */
    @Autowired
    private ITempCarFeeCreateOrderV1InnerServiceSMO tempCarFeeCreateOrderV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求JSON中是否包含必要的参数，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理数据流
     * @param reqJson 请求的JSON数据对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含paId字段
        Assert.hasKeyAndValue(reqJson, "paId", "paId不能为空");
        // 验证请求参数中必须包含carNum字段
        Assert.hasKeyAndValue(reqJson, "carNum", "carNum不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 处理临时停车费订单查询请求，调用服务接口创建订单并设置响应结果
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理数据流和设置响应
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用临时停车费创建订单服务接口，并将返回结果设置到响应实体中
        cmdDataFlowContext.setResponseEntity(tempCarFeeCreateOrderV1InnerServiceSMOImpl.createOrder(reqJson));
    }
}