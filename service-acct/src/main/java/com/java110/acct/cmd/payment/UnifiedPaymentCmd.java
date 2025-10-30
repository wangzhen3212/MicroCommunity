package com.java110.acct.cmd.payment;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.payment.IPaymentBusiness;
import com.java110.acct.payment.IPaymentFactoryAdapt;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.payment.PaymentOrderDto;
import com.java110.dto.payment.PaymentPoolDto;
import com.java110.dto.payment.PaymentPoolConfigDto;
import com.java110.intf.acct.IPaymentPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;
import java.util.Map;


@Java110CmdDoc(title = "统一支付接口",
        description = "系统中的统一支付接口",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/payment.unifiedPayment",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "payment.unifiedPayment"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "business",length = 64, remark = "支付场景，比如场地预约 为 venueReservation"),
        @Java110ParamDoc(name = "payAdapt",length = 64, remark = "支付适配器，非必填"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "tradeType", length = 30, remark = "支付类型 NATIVE JSAPI APP"),
        @Java110ParamDoc(name = "...", length = 30, remark = "其他参数根据相应接口协议传"),

})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 100 成功不需要唤起支付窗口，直接支付成功，可能从账户等做了扣款，其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "....", type = "String", length = 250, remark = "相应支付厂商要求字段，具体参考 不同厂商协议"),

        }
)

@Java110ExampleDoc(
        reqBody="{\"business\":\"venueReservation\",\"communityId\":\"123123\",\"...\":\"...\"}",
        resBody="{\"code\":0,\"msg\":\"成功\",\"...\":\"...\"}"
)

/**
 * 统一支付接口命令类
 * 
 * 该类负责处理系统中的统一支付请求，根据不同的支付场景和业务类型，
 * 调用相应的支付业务处理和支付适配器完成支付流程。
 * 支持多种支付方式（微信支付等）和多种支付场景（费用支付、临时车缴费等）。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
@Java110Cmd(serviceCode = "payment.unifiedPayment")
public class UnifiedPaymentCmd extends Cmd{

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(UnifiedPaymentCmd.class);

    /** 默认支付适配器 - 微信通用支付 */
    protected static final String DEFAULT_PAYMENT_ADAPT = "wechatPaymentFactory";

    /** 支付池配置服务接口 */
    @Autowired
    private IPaymentPoolConfigV1InnerServiceSMO paymentPoolConfigV1InnerServiceSMOImpl;

    /** 支付池服务接口 */
    @Autowired
    private IPaymentPoolV1InnerServiceSMO paymentPoolV1InnerServiceSMOImpl;

    /** 费用服务接口 */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * 验证请求参数中是否包含必要的业务类型字段
     * 
     * @param event   命令事件对象
     * @param context 命令数据流上下文，包含请求报文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 校验请求参数中必须包含business字段
        Assert.hasKeyAndValue(reqJson,"business","未包含业务");
    }

    /**
     * 执行支付命令的主方法
     * 
     * 处理支付请求的完整流程，包括：
     * 1. 获取支付业务处理器
     * 2. 调用业务下单接口
     * 3. 环境判断（开发/测试环境特殊处理）
     * 4. 零金额支付特殊处理
     * 5. 选择支付适配器
     * 6. 调用支付厂商接口
     * 7. 缓存请求参数
     * 
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException    当支付业务处理失败时抛出
     * @throws ParseException  当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        logger.debug(">>>>>>>>>>>>>>>>支付参数报文,{}",reqJson.toJSONString());
        // 从请求头中获取应用ID和用户ID
        String appId = context.getReqHeaders().get("app-id");
        String userId = context.getReqHeaders().get("user-id");

        // 1.0 根据业务类型获取对应的支付业务处理器
        IPaymentBusiness paymentBusiness = ApplicationContextFactory.getBean(reqJson.getString("business"), IPaymentBusiness.class);

        if(paymentBusiness == null){
            throw new CmdException("当前支付业务不支持");
        }

        // 2.0 调用业务处理器的统一支付方法，生成支付订单
        PaymentOrderDto paymentOrderDto =  paymentBusiness.unified(context,reqJson);
        paymentOrderDto.setAppId(appId);
        paymentOrderDto.setUserId(userId);

        logger.debug(">>>>>>>>>>>>>>>>支付业务下单返回,{}",JSONObject.toJSONString(paymentOrderDto));

        // 获取当前环境配置
        String env = MappingCache.getValue(MappingConstant.ENV_DOMAIN,"HC_ENV");

        // 开发环境和测试环境特殊处理：不实际调用支付接口
        if ("DEV".equals(env) || "TEST".equals(env)) {
            // 模拟支付成功回调
            paymentBusiness.notifyPayment(paymentOrderDto,reqJson);
            JSONObject param = new JSONObject();
            param.put("code", "100");
            param.put("msg", "演示环境不触发支付");
            context.setResponseEntity(new ResponseEntity(JSONObject.toJSONString(param), HttpStatus.OK));
            return ;
        }

        // 3.0 如果支付金额为0，直接调用支付完成通知接口
        if (paymentOrderDto.getMoney() <= 0) {
            paymentBusiness.notifyPayment(paymentOrderDto,reqJson);
            JSONObject param = new JSONObject();
            param.put("code", "100");
            param.put("msg", "扣费为0回调成功");
            context.setResponseEntity(new ResponseEntity(JSONObject.toJSONString(param), HttpStatus.OK));
            return ;
        }

        // 4.0 计算并获取支付适配器
        String payAdapt = computeAdapt(reqJson.getString("business"), reqJson);
        payAdapt = StringUtil.isEmpty(payAdapt) ? DEFAULT_PAYMENT_ADAPT : payAdapt;

        // 如果请求中指定了支付适配器，则使用指定的适配器
        if(reqJson.containsKey("payAdapt") && !StringUtil.isEmpty(reqJson.getString("payAdapt"))){
            payAdapt = reqJson.getString("payAdapt");
        }

        // 获取支付适配器实例
        IPaymentFactoryAdapt tPayAdapt = ApplicationContextFactory.getBean(payAdapt, IPaymentFactoryAdapt.class);

        // 5.0 调用支付厂商接口进行实际支付
        Map result = null;
        try {
            result = tPayAdapt.java110Payment(paymentOrderDto,reqJson, context);
        } catch (Exception e) {
            logger.error("支付异常",e);
            throw new CmdException(e.getLocalizedMessage());
        }
        ResponseEntity<String> responseEntity = new ResponseEntity(JSONObject.toJSONString(result), HttpStatus.OK);

        logger.debug("调用支付厂家返回,{}",responseEntity);
        context.setResponseEntity(responseEntity);

        // 6.0 将请求参数缓存到Redis中，用于后续处理
        CommonCache.setValue("unifiedPayment_"+paymentOrderDto.getOrderId(),reqJson.toJSONString(),CommonCache.PAY_DEFAULT_EXPIRE_TIME);
    }

    /**
     * 计算支付适配器
     * 
     * 根据业务类型和请求参数确定使用哪个支付适配器，优先级：
     * 1. 费用支付业务适配器
     * 2. 临时车缴费业务适配器
     * 3. 小区默认支付适配器
     * 
     * @param business  业务类型
     * @param reqJson   请求参数
     * @return String   支付适配器Bean名称
     */
    private String computeAdapt(String business, JSONObject reqJson) {

        String communityId = reqJson.getString("communityId");
        
        // 1. 如果是费用支付业务，获取对应的支付适配器
        PaymentPoolDto paymentPoolDto = ifPayFeeBusiness(business, reqJson);
        if (paymentPoolDto != null) {
            reqJson.put("paymentPoolId", paymentPoolDto.getPpId());
            return paymentPoolDto.getBeanJsapi();
        }

        // 2. 如果是临时车缴费业务，获取对应的支付适配器
        paymentPoolDto = ifTempCarFeeBusiness(business, communityId);
        if (paymentPoolDto != null) {
            reqJson.put("paymentPoolId", paymentPoolDto.getPpId());
            return paymentPoolDto.getBeanJsapi();
        }

        // 3. 按小区查询默认支付信息
        paymentPoolDto = new PaymentPoolDto();
        paymentPoolDto.setCommunityId(communityId);
        paymentPoolDto.setPayType(PaymentPoolDto.PAY_TYPE_COMMUNITY);
        paymentPoolDto.setState("Y");
        List<PaymentPoolDto> paymentPoolDtos = paymentPoolV1InnerServiceSMOImpl.queryPaymentPools(paymentPoolDto);
        if (paymentPoolDtos == null || paymentPoolDtos.isEmpty()) {
            throw new IllegalArgumentException("小区未配置支付信息");
        }

        reqJson.put("paymentPoolId", paymentPoolDtos.get(0).getPpId());
        return paymentPoolDtos.get(0).getBeanJsapi();
    }

    /**
     * 临时车缴费业务处理
     * 
     * 检查是否为临时车缴费业务，并返回对应的支付池配置
     * 
     * @param business    业务类型
     * @param communityId 小区ID
     * @return PaymentPoolDto 支付池数据传输对象，如果不是临时车业务返回null
     */
    private PaymentPoolDto ifTempCarFeeBusiness(String business, String communityId) {
        // 检查业务类型是否为临时车缴费
        if (!"tempCarFee".equals(business)) {
            return null;
        }
        
        // 查询临时车缴费的支付池配置
        PaymentPoolDto paymentPoolDto = new PaymentPoolDto();
        paymentPoolDto.setCommunityId(communityId);
        paymentPoolDto.setPayType(PaymentPoolDto.PAY_TYPE_TEMP_CAT);
        paymentPoolDto.setState("Y");
        List<PaymentPoolDto> paymentPoolDtos = paymentPoolV1InnerServiceSMOImpl.queryPaymentPools(paymentPoolDto);
        if (paymentPoolDtos == null || paymentPoolDtos.isEmpty()) {
            return null;
        }

        return paymentPoolDtos.get(0);
    }

    /**
     * 费用支付业务处理
     * 
     * 检查是否为费用支付业务，并返回对应的支付池配置
     * 
     * @param business 业务类型
     * @param reqJson  请求参数
     * @return PaymentPoolDto 支付池数据传输对象，如果不是费用支付业务返回null
     */
    private PaymentPoolDto ifPayFeeBusiness(String business, JSONObject reqJson) {
        String feeId = "";
        // 检查业务类型是否为费用支付且包含费用ID
        if (!"payFee".equals(business) || !reqJson.containsKey("feeId")) {
            return null;
        }

        feeId = reqJson.getString("feeId");
        // 检查费用ID是否为数字（这里逻辑可能需要调整，通常应该检查是否为有效数字）
        if (StringUtil.isNumber(feeId)) {
            return null;
        }

        // 查询费用信息
        PayFeeDto feeDto = new PayFeeDto();
        feeDto.setFeeId(feeId);
        feeDto.setCommunityId(reqJson.getString("communityId"));
        List<PayFeeDto> feeDtos = payFeeV1InnerServiceSMOImpl.queryPayFees(feeDto);

        if (feeDtos == null || feeDtos.isEmpty()) {
            return null;
        }

        // 根据费用配置查询支付池配置
        PaymentPoolConfigDto paymentPoolConfigDto = new PaymentPoolConfigDto();
        paymentPoolConfigDto.setConfigId(feeDtos.get(0).getConfigId());
        paymentPoolConfigDto.setCommunityId(feeDtos.get(0).getCommunityId());
        List<PaymentPoolConfigDto> paymentPoolConfigDtos = paymentPoolConfigV1InnerServiceSMOImpl.queryPaymentPoolConfigs(paymentPoolConfigDto);
        if (paymentPoolConfigDtos == null || paymentPoolConfigDtos.isEmpty()) {
            return null;
        }

        // 查询支付池信息
        PaymentPoolDto paymentPoolDto = new PaymentPoolDto();
        paymentPoolDto.setPpId(paymentPoolConfigDtos.get(0).getPpId());
        paymentPoolDto.setCommunityId(paymentPoolConfigDtos.get(0).getCommunityId());
        paymentPoolDto.setPayType(PaymentPoolDto.PAY_TYPE_FEE_CONFIG);
        paymentPoolDto.setState("Y");
        List<PaymentPoolDto> paymentPoolDtos = paymentPoolV1InnerServiceSMOImpl.queryPaymentPools(paymentPoolDto);
        if (paymentPoolDtos == null || paymentPoolDtos.isEmpty()) {
            return null;
        }

        return paymentPoolDtos.get(0);
    }
}