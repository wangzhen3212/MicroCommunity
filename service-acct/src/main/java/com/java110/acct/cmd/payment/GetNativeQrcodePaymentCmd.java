package com.java110.acct.cmd.payment;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;

import java.text.ParseException;

/**
 * 查询native支付二维码支付信息命令类
 * 
 * 该类负责处理获取原生二维码支付信息的业务逻辑，主要功能包括：
 * 1. 验证请求参数中是否包含必要的token信息
 * 2. 根据token从缓存中获取支付信息
 * 3. 返回支付信息或抛出相应的异常信息
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode payment.getNativeQrcodePayment
 */
@Java110Cmd(serviceCode = "payment.getNativeQrcodePayment")
public class GetNativeQrcodePaymentCmd extends Cmd {
    
    /**
     * 验证请求参数
     * 
     * 该方法用于验证传入的请求JSON对象是否包含必要的参数，
     * 主要检查qrToken字段是否存在且不为空
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求的JSON对象，包含客户端传入的参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求JSON中是否包含qrToken键值对
        Assert.hasKeyAndValue(reqJson, "qrToken", "未包含token信息");
    }

    /**
     * 执行获取原生二维码支付信息命令
     * 
     * 该方法主要逻辑：
     * 1. 根据qrToken从缓存中获取支付参数
     * 2. 检查支付参数是否有效（是否过期）
     * 3. 将支付参数设置到响应中返回给客户端
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求的JSON对象，包含客户端传入的参数
     * @throws CmdException 当支付码过期或获取支付信息失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建缓存key，格式为：nativeQrcodePayment_ + qrToken
        String cacheKey = "nativeQrcodePayment_" + reqJson.getString("qrToken");
        // 从缓存中获取并移除支付参数（一次性使用）
        String param = CommonCache.getAndRemoveValue(cacheKey);

        // 检查获取到的支付参数是否为空或空字符串
        if (StringUtil.isEmpty(param)) {
            // 如果支付参数为空，说明支付码已过期或不存在
            throw new CmdException("支付码已经过期");
        }

        // 将字符串类型的支付参数解析为JSON对象，并设置到响应实体中返回
        context.setResponseEntity(ResultVo.createResponseEntity(JSONObject.parseObject(param)));
    }
}