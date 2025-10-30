package com.java110.acct.cmd.payment;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.smo.IQrCodePaymentSMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CallApiServiceFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.payment.PaymentPoolDto;
import com.java110.dto.payment.PaymentPoolConfigDto;
import com.java110.intf.acct.IPaymentPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolV1InnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 扫码支付命令类
 * 负责处理二维码支付请求，包括支付参数验证、支付适配器选择、支付执行和结果处理
 * 支持支付宝和微信支付的二维码支付功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "payment.qrCodePayment")
public class QrCodePaymentCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(QrCodePaymentCmd.class);

    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    @Autowired
    private IPaymentPoolConfigV1InnerServiceSMO paymentPoolConfigV1InnerServiceSMOImpl;

    @Autowired
    private IPaymentPoolV1InnerServiceSMO paymentPoolV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    private IQrCodePaymentSMO qrCodePaymentSMOImpl;

    /**
     * 验证支付请求参数
     * 检查必要的支付参数是否存在且有效
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "authCode", "未包含授权码");
        Assert.hasKeyAndValue(reqJson, "receivedAmount", "未包含支付金额");
        Assert.hasKeyAndValue(reqJson, "subServiceCode", "未包含支付接口");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行扫码支付命令
     * 处理二维码支付的核心业务逻辑，包括支付适配器选择、支付执行和结果处理
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 生成订单ID
        String orderId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_orderId);
        // 获取支付金额
        double receivedAmount = Double.parseDouble(reqJson.getString("receivedAmount"));
        // 获取授权码
        String authCode = reqJson.getString("authCode");
        
        // 验证授权码格式
        if (StringUtil.isEmpty(authCode) || authCode.length() < 2) {
            throw new IllegalArgumentException("授权码错误");
        }

        // 计算支付适配器
        String payQrAdapt = computeAdapt(reqJson.getString("subServiceCode"), reqJson);

        // 根据支付适配器配置选择具体的支付实现
        if (StringUtil.isEmpty(payQrAdapt)) {
            // 根据授权码前两位判断支付类型
            int pre = Integer.parseInt(authCode.substring(0, 2));
            if (pre > 24 && pre < 31) { 
                // 支付宝支付适配器
                qrCodePaymentSMOImpl = ApplicationContextFactory.getBean("qrCodeAliPaymentAdapt", IQrCodePaymentSMO.class);
                reqJson.put("primeRate", FeeDetailDto.PRIME_REATE_ALI_QRCODE);
            } else {
                // 微信支付适配器
                qrCodePaymentSMOImpl = ApplicationContextFactory.getBean("qrCodeWechatPaymentAdapt", IQrCodePaymentSMO.class);
                reqJson.put("primeRate", FeeDetailDto.PRIME_REATE_WECHAT_QRCODE);
            }
        } else {
            // 使用配置的支付适配器
            qrCodePaymentSMOImpl = ApplicationContextFactory.getBean(payQrAdapt, IQrCodePaymentSMO.class);
        }

        // 验证小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(reqJson.getString("communityId"));
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        Assert.listOnlyOne(communityDtos, "小区不存在");

        // 构建费用名称
        String feeName = communityDtos.get(0).getName();
        if (!StringUtil.isEmpty(reqJson.getString("payerObjName"))) {
            feeName += ("-" + reqJson.getString("payerObjName"));
        }

        if (!StringUtil.isEmpty(reqJson.getString("feeName"))) {
            feeName += ("-" + reqJson.getString("feeName"));
        }

        // 限制费用名称长度
        if (feeName.length() > 120) {
            feeName = feeName.substring(0, 120);
        }

        // 缓存订单ID，防止重复支付
        CommonCache.setValue("qrCode_order"+orderId, orderId, CommonCache.TOKEN_EXPIRE_TIME);

        ResultVo resultVo = null;
        try {
            // 执行支付操作
            resultVo = qrCodePaymentSMOImpl.pay(reqJson.getString("communityId"), orderId,
                    receivedAmount, authCode, feeName, reqJson.getString("paymentPoolId"));
        } catch (Exception e) {
            logger.error("异常了", e);
            cmdDataFlowContext.setResponseEntity(ResultVo.error(e.getLocalizedMessage()));
            return;
        }
        
        logger.debug("适配器返回结果:" + resultVo.toString());
        
        // 检查支付结果
        if (ResultVo.CODE_OK != resultVo.getCode()) {
            reqJson.put("orderId", orderId);
            cmdDataFlowContext.setResponseEntity(ResultVo.error(resultVo.getMsg(), reqJson));
            return;
        }
        
        // 获取应用ID和用户ID
        String appId = cmdDataFlowContext.getReqHeaders().get(CommonConstant.APP_ID);
        String userId = cmdDataFlowContext.getReqHeaders().get(CommonConstant.USER_ID);
        
        // 设置支付订单ID
        reqJson.put("payOrderId", orderId);

        // 从缓存中获取并移除订单ID，防止重复处理
        orderId = CommonCache.getAndRemoveValue("qrCode_order"+orderId);
        if (StringUtil.isEmpty(orderId)) {
            throw new CmdException("订单已经处理过");
        }

        // 调用API服务处理支付结果
        JSONObject paramOut = CallApiServiceFactory.postForApi(appId, reqJson, reqJson.getString("subServiceCode"), JSONObject.class, userId);
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(paramOut));
    }

    /**
     * 计算支付适配器
     * 根据业务类型和请求参数确定使用哪个支付适配器
     *
     * @param business 业务类型代码
     * @param reqJson 请求参数JSON对象
     * @return 支付适配器Bean名称
     */
    private String computeAdapt(String business, JSONObject reqJson) {
        String communityId = reqJson.getString("communityId");
        
        // 如果是单个费用缴费业务，查询对应的支付池配置
        PaymentPoolDto paymentPoolDto = ifPayFeeBusiness(business, reqJson);
        if (paymentPoolDto != null) {
            reqJson.put("paymentPoolId", paymentPoolDto.getPpId());
            return paymentPoolDto.getBeanQrcode();
        }

        // 按小区查询支付信息
        paymentPoolDto = new PaymentPoolDto();
        paymentPoolDto.setCommunityId(communityId);
        paymentPoolDto.setPayType(PaymentPoolDto.PAY_TYPE_COMMUNITY);
        paymentPoolDto.setState("Y");
        List<PaymentPoolDto> paymentPoolDtos = paymentPoolV1InnerServiceSMOImpl.queryPaymentPools(paymentPoolDto);
        
        // 验证支付配置是否存在
        if (paymentPoolDtos == null || paymentPoolDtos.isEmpty()) {
            throw new IllegalArgumentException("小区未配置支付信息");
        }

        reqJson.put("paymentPoolId", paymentPoolDtos.get(0).getPpId());
        return paymentPoolDtos.get(0).getBeanQrcode();
    }

    /**
     * 检查是否为费用支付业务并获取对应的支付池配置
     * 如果是费用支付业务，查询对应的费用配置和支付池信息
     *
     * @param business 业务类型代码
     * @param reqJson 请求参数JSON对象
     * @return 支付池数据传输对象，如果不是费用支付业务则返回null
     */
    private PaymentPoolDto ifPayFeeBusiness(String business, JSONObject reqJson) {
        String feeId = "";
        
        // 检查是否为费用支付业务且包含费用ID
        if (!"fee.payFee".equals(business) || !reqJson.containsKey("feeId")) {
            return null;
        }

        feeId = reqJson.getString("feeId");
        // 检查费用ID是否为数字格式
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

        // 查询支付池配置信息
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