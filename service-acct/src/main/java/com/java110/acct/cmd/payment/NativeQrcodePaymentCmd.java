package com.java110.acct.cmd.payment;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.payment.IPaymentBusiness;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.payment.PaymentOrderDto;
import com.java110.intf.acct.IPaymentPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.UrlCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;

/**
 * 原生二维码支付命令类
 * 负责处理原生二维码支付业务，包括参数验证、支付订单创建和支付二维码生成
 * 通过指定的支付业务类型创建支付订单，并生成对应的支付二维码URL
 * 
 * @author wuxw
 * @version 1.0
 * @since 2023-06-25
 */
@Java110Cmd(serviceCode = "payment.nativeQrcodePayment")
public class NativeQrcodePaymentCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(NativeQrcodePaymentCmd.class);

    /**
     * 默认支付适配器名称 - 微信原生二维码支付工厂
     */
    protected static final String DEFAULT_PAYMENT_ADAPT = "wechatNativeQrcodePaymentFactory";

    /**
     * 支付池配置服务接口
     */
    @Autowired
    private IPaymentPoolConfigV1InnerServiceSMO paymentPoolConfigV1InnerServiceSMOImpl;

    /**
     * 支付池服务接口
     */
    @Autowired
    private IPaymentPoolV1InnerServiceSMO paymentPoolV1InnerServiceSMOImpl;

    /**
     * 缴费服务接口
     */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的业务参数和小区信息
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException   当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含business字段
        Assert.hasKeyAndValue(reqJson, "business", "未包含业务");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行支付命令
     * 处理原生二维码支付业务逻辑，包括创建支付订单、生成支付二维码等
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException   当支付业务处理失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 记录支付请求参数日志
        logger.debug(">>>>>>>>>>>>>>>>支付参数报文,{}", reqJson.toJSONString());
        
        // 从请求头中获取应用ID和用户ID
        String appId = context.getReqHeaders().get("app-id");
        String userId = context.getReqHeaders().get("user-id");
        
        // 将用户ID和店铺ID添加到请求参数中
        reqJson.put("createUserId", userId);
        reqJson.put("storeId", CmdContextUtils.getStoreId(context));

        // 1.0 根据业务类型获取对应的支付业务处理器
        IPaymentBusiness paymentBusiness = ApplicationContextFactory.getBean(reqJson.getString("business"), IPaymentBusiness.class);

        // 检查支付业务处理器是否存在
        if (paymentBusiness == null) {
            throw new CmdException("当前支付业务不支持");
        }

        // 2.0 调用支付业务处理器创建统一支付订单
        PaymentOrderDto paymentOrderDto = paymentBusiness.unified(context, reqJson);
        
        // 设置支付订单的应用ID和用户ID
        paymentOrderDto.setAppId(appId);
        paymentOrderDto.setUserId(userId);
        
        // 将支付金额添加到请求参数中
        reqJson.put("money", paymentOrderDto.getMoney());

        // 生成唯一的支付令牌
        String token = GenerateCodeFactory.getUUID();

        // 将支付请求参数缓存到Redis中，使用支付令牌作为key
        CommonCache.setValue("nativeQrcodePayment_" + token, reqJson.toJSONString(), CommonCache.PAY_DEFAULT_EXPIRE_TIME);
        
        // 创建响应结果对象
        JSONObject result = new JSONObject();
        
        // 生成支付二维码URL，包含支付令牌参数
        result.put("codeUrl", UrlCache.getOwnerUrl() + "/#/pages/fee/qrCodeCashier?qrToken=" + token);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(result);

        // 记录支付厂家返回结果日志
        logger.debug("调用支付厂家返回,{}", responseEntity);
        
        // 设置响应实体到上下文中
        context.setResponseEntity(responseEntity);

    }

}