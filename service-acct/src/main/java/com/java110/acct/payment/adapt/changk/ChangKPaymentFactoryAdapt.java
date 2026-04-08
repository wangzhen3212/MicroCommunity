package com.java110.acct.payment.adapt.changk;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.payment.IPaymentFactoryAdapt;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.payment.PaymentPoolValueDto;
import com.java110.dto.wechat.OnlinePayDto;
import com.java110.dto.payment.NotifyPaymentOrderDto;
import com.java110.dto.payment.PaymentOrderDto;
import com.java110.intf.acct.IOnlinePayV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolValueV1InnerServiceSMO;
import com.java110.po.wechat.OnlinePayPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.cache.UrlCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.PayUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.java110.utils.util.ListUtil;

import java.util.*;

/**
 * -- payment_adapt 表
 * INSERT INTO `payment_adapt` (`adapt_id`, `payment_type`, `name`, `bean_jsapi`, `bean_qrcode`, `bean_native`, `create_time`, `status_cd`, `bean_refund`)
 * VALUES ('CHANGK', 'CHANGK', '畅快聚合支付', 'changKPaymentFactoryAdapt', 'changKQrCodePaymentAdapt', NULL, NOW(), '0', 'changKRefundAdapt');
 *
 * -- payment_key 表
 * INSERT INTO `payment_key` (`key_id`, `payment_type`, `name`, `column_key`, `remark`, `create_time`, `status_cd`)
 * VALUES ('CHANGK_001', 'CHANGK', '商户号', 'CHANGK_MERCHANT_NO', '畅快聚合支付商户号', NOW(), '0');
 *
 * INSERT INTO `payment_key` (`key_id`, `payment_type`, `name`, `column_key`, `remark`, `create_time`, `status_cd`)
 * VALUES ('CHANGK_002', 'CHANGK', '终端号', 'CHANGK_TERMINAL_ID', '畅快聚合支付终端号', NOW(), '0');
 *
 * INSERT INTO `payment_key` (`key_id`, `payment_type`, `name`, `column_key`, `remark`, `create_time`, `status_cd`)
 * VALUES ('CHANGK_003', 'CHANGK', '密钥', 'CHANGK_ACCESS_TOKEN', '畅快聚合支付签名密钥', NOW(), '0');
 */
@Service("changKPaymentFactoryAdapt")
public class ChangKPaymentFactoryAdapt implements IPaymentFactoryAdapt {

    private static final Logger logger = LoggerFactory.getLogger(ChangKPaymentFactoryAdapt.class);

    public static final String TRADE_TYPE_JSAPI = "JSAPI";
    public static final String TRADE_TYPE_NATIVE = "NATIVE";

    @Autowired
    private IOnlinePayV1InnerServiceSMO onlinePayV1InnerServiceSMOImpl;

    @Autowired
    private IPaymentPoolValueV1InnerServiceSMO paymentPoolValueV1InnerServiceSMOImpl;

    @Autowired
    private RestTemplate outRestTemplate;

    @Override
    public Map java110Payment(PaymentOrderDto paymentOrderDto, JSONObject reqJson, ICmdDataFlowContext context) throws Exception {
        String paymentPoolId = reqJson.getString("paymentPoolId");
        String tradeType = reqJson.getString("tradeType");
        String notifyUrl = UrlCache.getOwnerUrl() + "/app/payment/notify/changk/992020011134400001/" + paymentPoolId;

        double payAmount = PayUtil.getPayAmountByEnv(
                MappingCache.getValue(MappingConstant.ENV_DOMAIN, "HC_ENV"),
                paymentOrderDto.getMoney()
        );

        PaymentPoolValueDto paymentPoolValueDto = new PaymentPoolValueDto();
        paymentPoolValueDto.setPpId(paymentPoolId);
        List<PaymentPoolValueDto> paymentPoolValueDtos = paymentPoolValueV1InnerServiceSMOImpl.queryPaymentPoolValues(paymentPoolValueDto);

        if (ListUtil.isNull(paymentPoolValueDtos)) {
            throw new IllegalArgumentException("配置错误,未配置参数");
        }

        String merchantNo = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_MERCHANT_NO");
        String terminalId = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_TERMINAL_ID");
        String accessToken = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_ACCESS_TOKEN");

        String payType = determinePayType(reqJson);
        String authCode = reqJson.getString("authCode");
        String openId = reqJson.getString("openId");

        Map<String, String> resultMap = new TreeMap<>();

        if (TRADE_TYPE_NATIVE.equals(tradeType) || StringUtil.isEmpty(authCode)) {
            resultMap = doQrPay(paymentOrderDto, merchantNo, terminalId, accessToken,
                    payType, payAmount, notifyUrl);
        } else {
            resultMap = doBarCodePay(paymentOrderDto, merchantNo, terminalId, accessToken,
                    payType, authCode, payAmount, notifyUrl);
        }

        return resultMap;
    }

    private Map<String, String> doBarCodePay(PaymentOrderDto paymentOrderDto, String merchantNo,
                                              String terminalId, String accessToken,
                                              String payType, String authCode, double payAmount,
                                              String notifyUrl) throws Exception {
        TreeMap<String, String> params = new TreeMap<>();
        String orderId = paymentOrderDto.getOrderId();
        String terminalTrace = generateTraceNo();
        String terminalTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
//        params.put("terminal_time", terminalTime);
//        params.put("merchant_no", merchantNo);
//        params.put("terminal_id", terminalId);
//        params.put("terminal_trace", terminalTrace);
////        params.put("terminal_time", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
//        params.put("pay_type", payType);
//        params.put("total_fee", String.valueOf((int) (payAmount * 100)));
//        params.put("out_trade_no", orderId);
//        params.put("auth_no", authCode);
////        params.put("notify_url", notifyUrl);


        params.put("merchant_no", merchantNo);
        params.put("terminal_id", terminalId);
        params.put("terminal_trace", terminalTrace);
        params.put("terminal_time", terminalTime);   // 已改为 yyyyMMddHHmmss
        params.put("pay_type", payType);
        params.put("total_fee", String.valueOf((int) (payAmount * 100)));
        params.put("out_trade_no", orderId);
        params.put("auth_no", authCode);
// 补充缺失字段
        params.put("pay_ver", "110");
        params.put("service_id", "010");
        params.put("attach", "");   // 可选，与文档示例保持一致

        String sign = ChangKSignature.generateSign(params, accessToken);
        params.put("key_sign", sign);

        logger.info("【畅快条码支付】请求参数: " + params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONObject.toJSONString(params), headers);

        ResponseEntity<String> response = outRestTemplate.postForEntity(
                ChangKConfig.BARCODE_PAY_URL, requestEntity, String.class);

        logger.info("【畅快条码支付】响应: " + response.getBody());

        return parseResponse(response.getBody(), orderId);
    }

    private Map<String, String> doQrPay(PaymentOrderDto paymentOrderDto, String merchantNo,
                                         String terminalId, String accessToken,
                                         String payType, double payAmount,
                                         String notifyUrl) throws Exception {
        TreeMap<String, String> params = new TreeMap<>();
        String orderId = paymentOrderDto.getOrderId();
        String terminalTrace = generateTraceNo();
        String terminalTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        params.put("terminal_time", terminalTime);
        params.put("merchant_no", merchantNo);
        params.put("terminal_id", terminalId);
        params.put("terminal_trace", terminalTrace);
//        params.put("terminal_time", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        params.put("pay_type", payType);
        params.put("total_fee", String.valueOf((int) (payAmount * 100)));
        params.put("out_trade_no", orderId);
        params.put("notify_url", notifyUrl);

        String sign = ChangKSignature.generateSign(params, accessToken);
        params.put("key_sign", sign);

        logger.info("【畅快二维码支付】请求参数: " + params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONObject.toJSONString(params), headers);

        ResponseEntity<String> response = outRestTemplate.postForEntity(
                ChangKConfig.QR_PAY_URL, requestEntity, String.class);

        logger.info("【畅快二维码支付】响应: " + response.getBody());

        return parseQrResponse(response.getBody(), orderId);
    }

    private Map<String, String> parseResponse(String responseBody, String orderId) throws Exception {
        Map<String, String> resultMap = new TreeMap<>();
        JSONObject result = JSONObject.parseObject(responseBody);

        String returnCode = result.getString("return_code");
        String resultCode = result.getString("result_code");
        String returnMsg = result.getString("return_msg");

        if (ChangKConfig.RESULT_CODE_SUCCESS.equals(returnCode)
                && ChangKConfig.RESULT_CODE_SUCCESS.equals(resultCode)) {
            resultMap.put("code", "0");
            resultMap.put("msg", "支付成功");
            resultMap.put("transactionId", result.getString("channel_trade_no"));
            resultMap.put("orderId", orderId);
        } else {
            resultMap.put("code", "1");
            resultMap.put("msg", returnMsg);
        }

        return resultMap;
    }

    private Map<String, String> parseQrResponse(String responseBody, String orderId) throws Exception {
        Map<String, String> resultMap = new TreeMap<>();
        JSONObject result = JSONObject.parseObject(responseBody);

        String returnCode = result.getString("return_code");
        String resultCode = result.getString("result_code");
        String returnMsg = result.getString("return_msg");

        if (ChangKConfig.RESULT_CODE_SUCCESS.equals(returnCode)
                && ChangKConfig.RESULT_CODE_SUCCESS.equals(resultCode)) {
            resultMap.put("code", "0");
            resultMap.put("msg", "下单成功");
            resultMap.put("codeUrl", result.getString("qrcode_url"));
            resultMap.put("prepayId", result.getString("terminal_trace"));
            resultMap.put("orderId", orderId);
        } else {
            resultMap.put("code", "1");
            resultMap.put("msg", returnMsg);
        }

        return resultMap;
    }

    private String determinePayType(JSONObject reqJson) {
        String payType = reqJson.getString("payType");
        if (StringUtil.isEmpty(payType)) {
            return ChangKConfig.PAY_TYPE_AUTO;
        }
        return payType;
    }

    private String generateTraceNo() {
        return System.currentTimeMillis() + "" + new Random().nextInt(10000);
    }

    @Override
    public PaymentOrderDto java110NotifyPayment(NotifyPaymentOrderDto notifyPaymentOrderDto) {
        PaymentPoolValueDto paymentPoolValueDto = new PaymentPoolValueDto();
        paymentPoolValueDto.setPpId(notifyPaymentOrderDto.getPaymentPoolId());
        paymentPoolValueDto.setCommunityId(notifyPaymentOrderDto.getCommunityId());
        List<PaymentPoolValueDto> paymentPoolValueDtos = paymentPoolValueV1InnerServiceSMOImpl.queryPaymentPoolValues(paymentPoolValueDto);

        if (ListUtil.isNull(paymentPoolValueDtos)) {
            throw new IllegalArgumentException("配置错误,未配置参数");
        }

        String accessToken = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_ACCESS_TOKEN");

        String param = notifyPaymentOrderDto.getParam();
        logger.info("【畅快支付回调】回调参数: " + param);

        JSONObject params = JSONObject.parseObject(param);

        if (!ChangKSignature.verifyNotifySign(params, accessToken)) {
            throw new IllegalArgumentException("签名验证失败");
        }

        String returnCode = params.getString("return_code");
        String resultCode = params.getString("result_code");

        PaymentOrderDto paymentOrderDto = new PaymentOrderDto();
        paymentOrderDto.setOrderId(params.getString("out_trade_no"));
        paymentOrderDto.setTransactionId(params.getString("channel_trade_no"));

        if (ChangKConfig.RESULT_CODE_SUCCESS.equals(returnCode)
                && ChangKConfig.RESULT_CODE_SUCCESS.equals(resultCode)) {
            doUpdateOnlinePay(paymentOrderDto.getOrderId(), OnlinePayDto.STATE_COMPILE, "支付成功");
        }

        JSONObject resJson = new JSONObject();
        resJson.put("return_code", ChangKConfig.RETURN_CODE_SUCCESS);
        resJson.put("return_msg", "成功");

        paymentOrderDto.setResponseEntity(new ResponseEntity<String>(resJson.toJSONString(), HttpStatus.OK));
        return paymentOrderDto;
    }

    private void doUpdateOnlinePay(String orderId, String state, String message) {
        OnlinePayPo onlinePayPo = new OnlinePayPo();
        onlinePayPo.setMessage(message.length() > 1000 ? message.substring(0, 1000) : message);
        onlinePayPo.setOrderId(orderId);
        onlinePayPo.setState(state);
        onlinePayV1InnerServiceSMOImpl.updateOnlinePay(onlinePayPo);
    }
}
