package com.java110.acct.cmd.alipay;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCreateRequest;
import com.alipay.api.response.AlipayTradeCreateResponse;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarOpenUserDto;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.intf.fee.ITempCarFeeCreateOrderV1InnerServiceSMO;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarOpenUserV1InnerServiceSMO;
import com.java110.po.owner.OwnerCarOpenUserPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付宝临时停车费支付命令类
 * 处理临时车辆停车费的支付宝支付流程，包括订单创建、支付处理、回调通知等
 * 主要功能包括：
 * 1. 验证支付请求参数
 * 2. 创建停车费订单
 * 3. 调用支付宝支付接口
 * 4. 处理支付结果和回调
 * 5. 记录用户车辆关联信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "alipay.payTempCarFee")
public class PayTempCarFeeCmd extends Cmd {

    /** 车主开放用户信息服务接口 */
    @Autowired
    private IOwnerCarOpenUserV1InnerServiceSMO ownerCarOpenUserV1InnerServiceSMOImpl;

    /** 停车场区域信息服务接口 */
    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    /** 微信小程序信息服务接口 */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /** 临时车辆费用创建订单服务接口 */
    @Autowired
    private ITempCarFeeCreateOrderV1InnerServiceSMO tempCarFeeCreateOrderV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查请求报文中是否包含必要的参数信息
     *
     * @param event   命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求JSON对象，包含客户端请求的所有参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含的关键参数
        Assert.jsonObjectHaveKey(reqJson, "carNum", "请求报文中未包含房屋信息节点");
        Assert.jsonObjectHaveKey(reqJson, "appId", "请求报文中未包含appId节点");
        Assert.jsonObjectHaveKey(reqJson, "openId", "请求报文中未包含openId节点");
        Assert.jsonObjectHaveKey(reqJson, "paId", "请求报文中未包含paId节点");
        Assert.jsonObjectHaveKey(reqJson, "inoutId", "请求报文中未包含inoutId节点");
        Assert.jsonObjectHaveKey(reqJson, "couponList", "请求报文中未包含couponList节点");
    }

    /**
     * 执行临时停车费支付命令
     * 处理停车费订单创建、支付流程、结果回调等完整业务流程
     *
     * @param event   命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求JSON对象，包含客户端请求的所有参数
     * @throws CmdException    命令执行异常，当业务逻辑执行失败时抛出
     * @throws ParseException  数据解析异常，当数据格式不正确时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        ResponseEntity responseEntity = null;

        // 根据停车场ID查询小区ID
        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
        parkingAreaDto.setPaId(reqJson.getString("paId"));
        List<ParkingAreaDto> parkingAreaDtos = parkingAreaV1InnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);

        // 验证停车场存在且唯一
        Assert.listOnlyOne(parkingAreaDtos, "停车场不存在");
        // 将小区ID添加到请求参数中，供后续使用
        reqJson.put("communityId", parkingAreaDtos.get(0).getCommunityId());

        // 处理优惠券列表
        JSONArray couponList = reqJson.getJSONArray("couponList");
        List<String> couponIds = new ArrayList<String>();
        if (couponList != null && couponList.size() > 0) {
            // 遍历优惠券列表，提取所有优惠券ID
            for (int couponIndex = 0; couponIndex < couponList.size(); couponIndex++) {
                couponIds.add(couponList.getJSONObject(couponIndex).getString("couponId"));
            }
        }

        // 设置默认用户ID，用于订单创建
        reqJson.put("userId", "-1");

        // 构建订单创建参数
        JSONObject paramIn = new JSONObject();
        paramIn.put("paId", reqJson.getString("paId"));
        paramIn.put("carNum", reqJson.getString("carNum"));
        paramIn.put("machineId", reqJson.getString("machineId"));
        paramIn.put("couponIds", StringUtils.join(couponIds, ","));
        
        // 调用订单创建服务，生成停车费订单
        responseEntity = tempCarFeeCreateOrderV1InnerServiceSMOImpl.createOrder(paramIn);

        // 检查订单创建结果，如果创建失败则直接返回错误响应
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 解析订单信息，获取订单详情
        JSONObject orderInfo = JSONObject.parseObject(responseEntity.getBody().toString());
        if (orderInfo.getIntValue("code") != 0) {
            throw new IllegalArgumentException("缴费失败");
        }
        JSONObject fee = orderInfo.getJSONObject("data");

        // 获取实际支付金额和订单ID
        double money = fee.getDouble("receivedAmount");
        String orderId = fee.getString("oId");
        
        // 处理金额为0的情况（免费订单）
        if (money <= 0) {
            JSONObject paramOut = new JSONObject();
            paramOut.put("oId", orderId);
            // 对于免费订单，直接调用回调通知接口
            responseEntity = tempCarFeeCreateOrderV1InnerServiceSMOImpl.notifyOrder(paramIn);
            JSONObject param = new JSONObject();
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                param.put("code", "101");
                param.put("msg", "扣费为0回调失败");
                context.setResponseEntity(new ResponseEntity(JSONObject.toJSONString(param), HttpStatus.OK));
                return;
            }
            param.put("code", "100");
            param.put("msg", "扣费为0回调成功");
            context.setResponseEntity(new ResponseEntity(JSONObject.toJSONString(param), HttpStatus.OK));
            return;
        }
        
        // 调用支付宝支付接口进行实际支付
        String openId = reqJson.getString("openId");
        ResultVo result = doAlipay(reqJson, paramIn.getString("feeName"), orderId, money, openId);

        responseEntity = new ResponseEntity(JSONObject.toJSONString(result), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
        if (!"0".equals(result.getCode())) {
            return;
        }
        
        // 保存支付信息到缓存，用于后续查询和回调处理
        JSONObject saveFees = new JSONObject();
        saveFees.put("orderId", paramIn.getString("inoutId"));
        saveFees.put("carNum", paramIn.getString("carNum"));
        saveFees.put("amount", money);
        saveFees.put("paId", paramIn.getString("paId"));
        saveFees.put("payTime", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        saveFees.put("payType", "2"); // 支付类型：2表示支付宝支付
        CommonCache.setValue(FeeDto.REDIS_PAY_TEMP_CAR_FEE + orderId, saveFees.toJSONString(), CommonCache.PAY_DEFAULT_EXPIRE_TIME);
        CommonCache.setValue(FeeDto.REDIS_PAY_TEMP_CAR_FEE_COMMUNITY + orderId, reqJson.getString("communityId"), CommonCache.PAY_DEFAULT_EXPIRE_TIME);
        
        // 记录用户与车辆的关联关系，方便下次使用
        OwnerCarOpenUserPo ownerCarOpenUserPo = new OwnerCarOpenUserPo();
        ownerCarOpenUserPo.setCarNum(reqJson.getString("carNum"));
        ownerCarOpenUserPo.setNickname("未获取");
        ownerCarOpenUserPo.setHeadimgurl("为获取");
        ownerCarOpenUserPo.setOpenId(openId);
        ownerCarOpenUserPo.setOpenType(OwnerCarOpenUserDto.OPEN_TYPE_ALIPAY);
        ownerCarOpenUserPo.setOpenUserId(GenerateCodeFactory.getGeneratorId("10"));
        ownerCarOpenUserV1InnerServiceSMOImpl.saveOwnerCarOpenUser(ownerCarOpenUserPo);
    }

    /**
     * 执行支付宝支付流程
     * 创建支付宝交易订单并调用支付宝API进行支付
     *
     * @param reqJson  请求JSON对象，包含社区ID等配置信息
     * @param feeName  费用名称，用于支付宝订单标题
     * @param orderId  订单ID，作为支付宝商户订单号
     * @param money    支付金额，单位元
     * @param openId   用户支付宝openId，用于标识支付用户
     * @return ResultVo 支付结果对象，包含支付状态码和消息
     */
    private ResultVo doAlipay(JSONObject reqJson, String feeName, String orderId, double money, String openId) {
        // 初始化支付宝客户端，使用社区配置的支付宝参数
        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                CommunitySettingFactory.getValue(reqJson.getString("communityId"), "APP_ID"),
                CommunitySettingFactory.getRemark(reqJson.getString("communityId"), "APP_PRIVATE_KEY"),
                "json",
                "UTF-8",
                CommunitySettingFactory.getRemark(reqJson.getString("communityId"), "ALIPAY_PUBLIC_KEY"),
                "RSA2");
        
        // 创建支付宝交易请求
        AlipayTradeCreateRequest request = new AlipayTradeCreateRequest();
        // 设置支付宝回调通知地址
        request.setNotifyUrl(MappingCache.getValue("ALIPAY", "temp"));
        
        // 构建业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderId);      // 商户订单号，使用系统生成的订单ID
        bizContent.put("total_amount", money);        // 订单总金额，单位元
        bizContent.put("subject", feeName);           // 订单标题，显示给用户的费用名称
        bizContent.put("buyer_id", openId);           // 买家支付宝用户ID
        bizContent.put("timeout_express", "10m");     // 订单超时时间，10分钟
        request.setBizContent(bizContent.toString());
        
        AlipayTradeCreateResponse response = null;
        try {
            // 执行支付宝API调用，创建交易订单
            response = alipayClient.execute(request);
            if (response.isSuccess()) {
                // 支付成功，返回成功结果
                return new ResultVo(ResultVo.CODE_OK, ResultVo.MSG_OK, orderId);
            } else {
                // 支付失败，返回错误信息
                return new ResultVo(ResultVo.CODE_ERROR, response.getMsg());
            }
        } catch (AlipayApiException e) {
            throw new CmdException("支付宝下单失败" + e);
        }
    }

    /**
     * 获取小程序微信配置信息
     * 根据小区ID和应用ID查询对应的微信小程序配置
     *
     * @param paramIn 输入参数对象，包含小区ID和应用ID
     * @return SmallWeChatDto 微信小程序配置信息，未找到时返回null
     */
    private SmallWeChatDto getSmallWechat(JSONObject paramIn) {
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setAppId(paramIn.getString("appId"));
        smallWeChatDto.setObjId(paramIn.getString("communityId"));
        smallWeChatDto.setRow(1);
        smallWeChatDto.setPage(1);
        
        // 查询微信小程序配置
        List<SmallWeChatDto> smallWeChatDtos = smallWechatV1InnerServiceSMOImpl.querySmallWechats(smallWeChatDto);

        if (smallWeChatDtos == null || smallWeChatDtos.size() < 1) {
            return null;
        }

        return smallWeChatDtos.get(0);
    }
}