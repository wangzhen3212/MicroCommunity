package com.java110.fee.cmd.tempCarFee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.ITempCarFeeCreateOrderV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 临时车费用订单通知命令类
 * 
 * 该类用于处理临时车费用订单的通知请求，接收支付通知并转发给相应的服务进行处理
 * 服务编码：tempCarFee.notifyTempCarFeeOrder
 * 请求路径：/app/tempCarFee.notifyTempCarFeeOrder
 * 
 * @author 吴学文 at 2021-09-16 22:26:04 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "tempCarFee.notifyTempCarFeeOrder")
public class NotifyTempCarFeeOrderCmd extends Cmd {

    /**
     * 临时车费用创建订单服务接口
     * 用于处理临时车费用订单的相关业务操作
     */
    @Autowired
    private ITempCarFeeCreateOrderV1InnerServiceSMO tempCarFeeCreateOrderV1InnerServiceSMOImpl;
    
    // 请求JSON示例格式说明：
    // {"amount":20.0,"payType":"2","orderId":"19c4321c-b5d5-405f-b2ff-20e86a2e7f3e",
    // "payTime":"2021-10-17 17:29:54","paId":"102021101160020175","carNum":"青A88888","oId":"102021101724760012"}

    /**
     * 参数验证方法
     * 
     * 用于验证请求参数的合法性，当前实现为空，可根据业务需求添加具体的验证逻辑
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求的JSON数据对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 当前验证逻辑为空，可根据具体业务需求添加参数验证
        // 例如：验证必填字段、字段格式、数据范围等
    }

    /**
     * 命令执行方法
     * 
     * 处理临时车费用订单通知的核心业务逻辑，将通知请求转发给订单服务处理
     * 使用@Java110Transactional注解确保方法在事务中执行
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求的JSON数据对象，包含订单通知相关信息
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用临时车费用创建订单服务的通知方法处理订单通知
        // 并将处理结果设置为命令上下文的响应实体
        cmdDataFlowContext.setResponseEntity(tempCarFeeCreateOrderV1InnerServiceSMOImpl.notifyOrder(reqJson));
    }
}