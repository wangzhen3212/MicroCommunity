package com.java110.acct.payment.adapt.changk;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.smo.IQrCodePaymentSMO;
import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import com.java110.dto.payment.PaymentPoolValueDto;
import com.java110.dto.wechat.OnlinePayDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.acct.IOnlinePayV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolValueV1InnerServiceSMO;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.po.wechat.OnlinePayPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.PayUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("changKQrCodePaymentAdapt")
public class ChangKQrCodePaymentAdapt implements IQrCodePaymentSMO {

    private static final Logger logger = LoggerFactory.getLogger(ChangKQrCodePaymentAdapt.class);

    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    @Autowired
    private RestTemplate outRestTemplate;

    @Autowired
    private IPaymentPoolValueV1InnerServiceSMO paymentPoolValueV1InnerServiceSMOImpl;

    @Autowired
    private IOnlinePayV1InnerServiceSMO onlinePayV1InnerServiceSMOImpl;

    @Override
    public ResultVo pay(String communityId, String orderNum, double money, String authCode, String feeName, String paymentPoolId) throws Exception {
        logger.info("【畅快聚合支付-条码支付】 订单编号=" + orderNum + ", 金额=" + money + ", 授权码=" + authCode);

        double payAmount = PayUtil.getPayAmountByEnv(
                MappingCache.getValue(MappingConstant.ENV_DOMAIN, "HC_ENV"), money);

        PaymentPoolValueDto paymentPoolValueDto = new PaymentPoolValueDto();
        paymentPoolValueDto.setPpId(paymentPoolId);
        List<PaymentPoolValueDto> paymentPoolValueDtos = paymentPoolValueV1InnerServiceSMOImpl.queryPaymentPoolValues(paymentPoolValueDto);

        if (ListUtil.isNull(paymentPoolValueDtos)) {
            throw new IllegalArgumentException("配置错误,未配置参数");
        }

        String merchantNo = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_MERCHANT_NO");
        String terminalId = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_TERMINAL_ID");
        String accessToken = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_ACCESS_TOKEN");

        SmallWeChatDto shopSmallWeChatDto = getSmallWechat(communityId);

        String payType = determinePayTypeByAuthCode(authCode);

        TreeMap<String, String> params = new TreeMap<>();
        String terminalTrace = generateTraceNo();

        params.put("merchant_no", merchantNo);
        params.put("terminal_id", terminalId);
        params.put("terminal_trace", terminalTrace);
        params.put("terminal_time", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        params.put("pay_type", payType);
        params.put("total_fee", String.valueOf((int) (payAmount * 100)));
        params.put("out_trade_no", orderNum);
        params.put("auth_code", authCode);
        params.put("notify_url", "");

        String sign = ChangKSignature.generateSign(params, accessToken);
        params.put("key_sign", sign);

        logger.info("【畅快条码支付】请求参数: " + params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONObject.toJSONString(params), headers);

        ResponseEntity<String> response = outRestTemplate.postForEntity(
                ChangKConfig.BARCODE_PAY_URL, requestEntity, String.class);

        logger.info("【畅快条码支付】响应: " + response.getBody());

        return parseBarCodeResponse(response.getBody(), shopSmallWeChatDto, orderNum, feeName, payAmount, paymentPoolValueDtos.get(0).getPpId());
    }

    @Override
    public ResultVo checkPayFinish(String communityId, String orderNum, String paymentPoolId) {
        logger.info("【畅快聚合支付-订单查询】 订单编号=" + orderNum);

        PaymentPoolValueDto paymentPoolValueDto = new PaymentPoolValueDto();
        paymentPoolValueDto.setPpId(paymentPoolId);
        paymentPoolValueDto.setCommunityId(communityId);
        List<PaymentPoolValueDto> paymentPoolValueDtos = paymentPoolValueV1InnerServiceSMOImpl.queryPaymentPoolValues(paymentPoolValueDto);

        if (ListUtil.isNull(paymentPoolValueDtos)) {
            throw new IllegalArgumentException("配置错误,未配置参数");
        }

        String merchantNo = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_MERCHANT_NO");
        String terminalId = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_TERMINAL_ID");
        String accessToken = PaymentPoolValueDto.getValue(paymentPoolValueDtos, "CHANGK_ACCESS_TOKEN");

        OnlinePayDto onlinePayDto = new OnlinePayDto();
        onlinePayDto.setOrderId(orderNum);
        List<OnlinePayDto> onlinePayDtos = onlinePayV1InnerServiceSMOImpl.queryOnlinePays(onlinePayDto);

        String channelTradeNo = "";
        if (!ListUtil.isNull(onlinePayDtos)) {
            channelTradeNo = onlinePayDtos.get(0).getTransactionId();
        }

        TreeMap<String, String> params = new TreeMap<>();
        params.put("merchant_no", merchantNo);
        params.put("terminal_id", terminalId);
        params.put("terminal_trace", generateTraceNo());
        params.put("terminal_time", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        params.put("out_trade_no", orderNum);
        if (!StringUtil.isEmpty(channelTradeNo)) {
            params.put("channel_trade_no", channelTradeNo);
        }

        String sign = ChangKSignature.generateSign(params, accessToken);
        params.put("key_sign", sign);

        logger.info("【畅快订单查询】请求参数: " + params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONObject.toJSONString(params), headers);

        ResponseEntity<String> response = outRestTemplate.postForEntity(
                ChangKConfig.QUERY_URL, requestEntity, String.class);

        logger.info("【畅快订单查询】响应: " + response.getBody());

        return parseQueryResponse(response.getBody(), orderNum);
    }

    private ResultVo parseBarCodeResponse(String responseBody, SmallWeChatDto smallWeChatDto,
                                         String orderNum, String feeName, double payAmount, String ppId) {
        JSONObject result = JSONObject.parseObject(responseBody);

        String returnCode = result.getString("return_code");
        String resultCode = result.getString("result_code");
        String returnMsg = result.getString("return_msg");

        if (ChangKConfig.RESULT_CODE_SUCCESS.equals(returnCode)
                && ChangKConfig.RESULT_CODE_SUCCESS.equals(resultCode)) {
            doSaveOnlinePay(smallWeChatDto, "-1", orderNum, result.getString("channel_trade_no"),
                    feeName, payAmount, OnlinePayDto.STATE_WAIT, "待支付", ppId);
            return new ResultVo(ResultVo.CODE_OK, "支付成功");
        } else {
            doSaveOnlinePay(smallWeChatDto, "-1", orderNum, "",
                    feeName, payAmount, OnlinePayDto.STATE_FAIL, returnMsg, ppId);
            return new ResultVo(ResultVo.CODE_ERROR, returnMsg);
        }
    }

    private ResultVo parseQueryResponse(String responseBody, String orderNum) {
        JSONObject result = JSONObject.parseObject(responseBody);

        String returnCode = result.getString("return_code");
        String resultCode = result.getString("result_code");

        if (ChangKConfig.RESULT_CODE_SUCCESS.equals(returnCode)
                && ChangKConfig.RESULT_CODE_SUCCESS.equals(resultCode)) {
            String tradeStatus = result.getString("trade_status");
            if ("SUCCESS".equalsIgnoreCase(tradeStatus)) {
                doUpdateOnlinePay(orderNum, OnlinePayDto.STATE_COMPILE, "支付成功");
                return new ResultVo(ResultVo.CODE_OK, "支付成功");
            } else {
                return new ResultVo(ResultVo.CODE_WAIT_PAY, "等待用户支付中");
            }
        }

        return new ResultVo(ResultVo.CODE_ERROR, "查询失败");
    }

    private String determinePayTypeByAuthCode(String authCode) {
        if (StringUtil.isEmpty(authCode) || authCode.length() < 2) {
            return ChangKConfig.PAY_TYPE_AUTO;
        }
        String prefix = authCode.substring(0, 2);
        switch (prefix) {
            case "10":
            case "11":
            case "12":
            case "13":
            case "14":
            case "15":
                return ChangKConfig.PAY_TYPE_WECHAT;
            case "25":
            case "26":
            case "27":
            case "28":
            case "29":
            case "30":
                return ChangKConfig.PAY_TYPE_ALIPAY;
            default:
                return ChangKConfig.PAY_TYPE_AUTO;
        }
    }

    private String generateTraceNo() {
        return System.currentTimeMillis() + "" + new Random().nextInt(10000);
    }

    private SmallWeChatDto getSmallWechat(String communityId) {
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setObjId(communityId);
        List<SmallWeChatDto> smallWeChatDtos = smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto);
        if (ListUtil.isNull(smallWeChatDtos)) {
            smallWeChatDto = new SmallWeChatDto();
            smallWeChatDto.setAppId(MappingCache.getValue(WechatConstant.WECHAT_DOMAIN, "appId"));
            smallWeChatDto.setAppSecret(MappingCache.getValue(WechatConstant.WECHAT_DOMAIN, "appSecret"));
            return smallWeChatDto;
        }
        return smallWeChatDtos.get(0);
    }

    private void doSaveOnlinePay(SmallWeChatDto smallWeChatDto, String openId, String orderId,
                                  String transactionId, String feeName, double money,
                                  String state, String message, String ppId) {
        OnlinePayPo onlinePayPo = new OnlinePayPo();
        onlinePayPo.setAppId(smallWeChatDto.getAppId());
        onlinePayPo.setMchId(smallWeChatDto.getMchId());
        onlinePayPo.setMessage(message.length() > 1000 ? message.substring(0, 1000) : message);
        onlinePayPo.setOpenId(openId);
        onlinePayPo.setOrderId(orderId);
        onlinePayPo.setPayId(orderId);
        onlinePayPo.setPayName(feeName);
        onlinePayPo.setRefundFee("0");
        onlinePayPo.setState(state);
        onlinePayPo.setTotalFee(money + "");
        onlinePayPo.setTransactionId(transactionId);
        onlinePayPo.setPaymentPoolId(ppId);
        onlinePayV1InnerServiceSMOImpl.saveOnlinePay(onlinePayPo);
    }

    private void doUpdateOnlinePay(String orderId, String state, String message) {
        OnlinePayPo onlinePayPo = new OnlinePayPo();
        onlinePayPo.setMessage(message.length() > 1000 ? message.substring(0, 1000) : message);
        onlinePayPo.setOrderId(orderId);
        onlinePayPo.setState(state);
        onlinePayV1InnerServiceSMOImpl.updateOnlinePay(onlinePayPo);
    }
}
