package com.java110.fee.cmd.returnPayFee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.onlinePayRefund.OnlinePayRefundDto;
import com.java110.dto.wechat.OnlinePayDto;
import com.java110.intf.acct.IOnlinePayRefundV1InnerServiceSMO;
import com.java110.intf.acct.IOnlinePayV1InnerServiceSMO;
import com.java110.po.onlinePayRefund.OnlinePayRefundPo;
import com.java110.po.wechat.OnlinePayPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 尝试退款命令类
 * 
 * 该命令用于处理退款失败后的重试操作，主要功能包括：
 * 1. 验证退款请求参数
 * 2. 查询退款失败订单信息
 * 3. 更新退款状态为待退款状态
 * 4. 更新支付订单状态为待退款状态
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "returnPayFee.tryRefundMoney")
public class TryRefundMoneyCmd extends Cmd {

    /**
     * 在线支付退款服务接口
     */
    @Autowired
    private IOnlinePayRefundV1InnerServiceSMO onlinePayRefundV1InnerServiceSMOImpl;

    /**
     * 在线支付服务接口
     */
    @Autowired
    private IOnlinePayV1InnerServiceSMO onlinePayV1InnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 
     * 该方法用于验证传入的请求参数是否完整有效
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含detailId字段
        Assert.hasKeyAndValue(reqJson,"detailId","未包含缴费明细");
    }

    /**
     * 执行退款重试命令方法
     * 
     * 该方法主要执行以下操作：
     * 1. 查询退款失败的订单信息
     * 2. 根据支付ID查询对应的支付订单
     * 3. 更新退款记录状态为待退款
     * 4. 更新支付订单状态为待退款
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当执行过程中出现业务异常时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建退款查询条件对象
        OnlinePayRefundDto onlinePayRefundDto = new OnlinePayRefundDto();
        // 设置业务ID为缴费明细ID
        onlinePayRefundDto.setBusiId(reqJson.getString("detailId"));
        // 设置查询状态为退款失败状态
        onlinePayRefundDto.setState(OnlinePayDto.STATE_FT);
        // 查询退款失败的订单列表
        List<OnlinePayRefundDto> onlinePayRefundDtos = onlinePayRefundV1InnerServiceSMOImpl.queryOnlinePayRefunds(onlinePayRefundDto);
        
        // 检查查询结果是否为空，如果为空则抛出异常
        if(ListUtil.isNull(onlinePayRefundDtos)){
            throw new CmdException("未查到退款失败订单");
        }

        // 创建支付订单查询条件对象
        OnlinePayDto onlinePayDto = new OnlinePayDto();
        // 设置支付ID为退款记录中的支付ID
        onlinePayDto.setPayId(onlinePayRefundDtos.get(0).getPayId());
        // 查询对应的支付订单信息
        List<OnlinePayDto> onlinePayDtos = onlinePayV1InnerServiceSMOImpl.queryOnlinePays(onlinePayDto);
        
        // 如果未查询到支付订单，则直接返回
        if (ListUtil.isNull(onlinePayDtos)) {
            return;
        }

        // TODO: 保存退费明细 - 待实现的具体业务逻辑

        // 创建退款更新对象
        OnlinePayRefundPo onlinePayRefundPo = new OnlinePayRefundPo();
        // 设置退款ID
        onlinePayRefundPo.setRefundId(onlinePayRefundDtos.get(0).getRefundId());
        // 更新退款状态为待退款
        onlinePayRefundPo.setState(OnlinePayDto.STATE_WT);
        // 设置退款消息
        onlinePayRefundPo.setMessage("待退费");
        // 设置小区ID
        onlinePayRefundPo.setCommunityId(onlinePayRefundDtos.get(0).getCommunityId());
        // 执行退款记录更新操作
        onlinePayRefundV1InnerServiceSMOImpl.updateOnlinePayRefund(onlinePayRefundPo);

        // 创建支付订单更新对象
        OnlinePayPo onlinePayPo = new OnlinePayPo();
        // 设置订单ID
        onlinePayPo.setOrderId(onlinePayDtos.get(0).getOrderId());
        // 设置支付ID
        onlinePayPo.setPayId(onlinePayDtos.get(0).getPayId());
        // 更新支付订单状态为待退款
        onlinePayPo.setState(OnlinePayDto.STATE_WT);
        // 设置退款金额
        onlinePayPo.setRefundFee(onlinePayRefundDtos.get(0).getRefundFee());
        // 执行支付订单更新操作
        onlinePayV1InnerServiceSMOImpl.updateOnlinePay(onlinePayPo);
    }
}