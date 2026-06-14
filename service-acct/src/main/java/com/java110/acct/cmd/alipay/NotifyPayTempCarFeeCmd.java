/**
 * 支付宝临时停车费支付通知处理类
 * 
 * 该类负责处理支付宝支付临时停车费后的异步通知回调，包括签名验证和订单状态更新等业务逻辑
 * 通过验证支付宝签名确保请求的安全性，并调用相关服务更新订单状态
 * 
 * @serviceCode alipay.notifyPayTempCarFee 服务编码，用于标识该命令处理器
 */
package com.java110.acct.cmd.alipay;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.dto.fee.FeeDto;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.intf.fee.ITempCarFeeCreateOrderV1InnerServiceSMO;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarOpenUserV1InnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.LinkedHashMap;

/**
 * 支付宝临时停车费支付通知处理命令类
 * 
 * 该类继承自Cmd基类，专门处理支付宝临时停车费支付成功后的异步通知回调
 * 主要职责包括：
 * 1. 验证支付宝回调请求的签名安全性
 * 2. 解析和处理支付通知参数
 * 3. 调用订单服务更新订单支付状态
 * 4. 返回处理结果给支付宝
 * 
 * 使用@Java110Cmd注解标识服务编码为alipay.notifyPayTempCarFee
 */
@Java110Cmd(serviceCode = "alipay.notifyPayTempCarFee")
public class NotifyPayTempCarFeeCmd extends Cmd {

    /**
     * 车主车辆开通用户服务接口
     * 用于处理与车主车辆开通相关的业务逻辑
     */
    @Autowired
    private IOwnerCarOpenUserV1InnerServiceSMO ownerCarOpenUserV1InnerServiceSMOImpl;

    /**
     * 停车区域信息服务接口
     * 用于获取停车区域相关信息
     */
    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    /**
     * 小程序微信服务接口
     * 用于处理与微信小程序相关的业务逻辑
     */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /**
     * 临时停车费订单创建服务接口
     * 用于创建和管理临时停车费订单
     */
    @Autowired
    private ITempCarFeeCreateOrderV1InnerServiceSMO tempCarFeeCreateOrderV1InnerServiceSMOImpl;

    /**
     * 验证支付宝支付通知请求
     * 
     * 该方法主要完成以下功能：
     * 1. 验证请求参数完整性
     * 2. 解析支付宝回调参数
     * 3. 验证支付宝签名确保请求安全性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求的JSON数据对象，包含支付宝回调的所有参数
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文必须包含订单号和签名信息
        Assert.jsonObjectHaveKey(reqJson, "out_trade_no", "请求报文中未包含订单信息");
        Assert.jsonObjectHaveKey(reqJson, "sign", "请求报文中未包含签名信息");

        // 从缓存中获取并移除小区ID，使用订单号作为key
        // 格式：FeeDto.REDIS_PAY_TEMP_CAR_FEE_COMMUNITY + 订单号
        String communityId = CommonCache.getAndRemoveValue(FeeDto.REDIS_PAY_TEMP_CAR_FEE_COMMUNITY + reqJson.getString("out_trade_no"));

        // 获取支付宝回调的结果信息
        String resultInfo = reqJson.getString("resultInfo");
        // 按"&"符号分割参数对，将参数字符串转换为数组
        String[] temp = resultInfo.split("&");
        // 使用LinkedHashMap保持参数顺序，用于签名验证
        // LinkedHashMap可以保持参数插入顺序，确保签名验证的一致性
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        
        // 遍历分割后的参数数组，解析键值对
        for (int i = 0; i < temp.length; i++) {
            // 按"="符号分割键值对，限制分割为2部分
            // 使用limit=2确保值中包含的"="不会被错误分割
            String[] arr = temp[i].split("=", 2);
            // 创建新数组存储分割后的数据
            String[] tempAagin = new String[arr.length];
            // 复制数组元素，确保数据完整性
            for (int j = 0; j < arr.length; j++) {
                tempAagin[j] = arr[j];
            }
            // 将解析出的键值对存入Map，key为参数名，value为参数值
            map.put(tempAagin[0], tempAagin[1]);
        }
        
        // 打印解析后的参数Map，用于调试和日志记录
        System.out.println(map);
        
        // 签名验证标志，初始化为false
        boolean signVerified = false;
        try {
            // 使用支付宝公钥验证签名
            // 参数说明：
            // map - 包含所有待验证参数的Map
            // CommunitySettingFactory.getRemark(communityId, "ALIPAY_PUBLIC_KEY") - 获取对应小区的支付宝公钥
            // "UTF-8" - 字符编码
            // "RSA2" - 签名算法类型
            signVerified = AlipaySignature.rsaCheckV1(map,
                    CommunitySettingFactory.getRemark(communityId, "ALIPAY_PUBLIC_KEY")
                    , "UTF-8", "RSA2");
        } catch (AlipayApiException e) {
            // 支付宝API异常时抛出运行时异常
            // 通常是由于公钥格式错误或网络问题导致
            throw new RuntimeException(e);
        }
        
        // 签名验证失败时抛出命令异常
        // 表示请求可能被篡改或来自非法来源
        if (!signVerified) {
            throw new CmdException("签名失败");
        }
    }

    /**
     * 执行临时停车费支付通知处理
     * 
     * 该方法处理支付宝支付成功后的业务逻辑，主要功能：
     * 1. 构建通知参数
     * 2. 调用订单服务更新订单状态
     * 3. 设置响应结果
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求的JSON数据对象，包含支付宝回调的所有参数
     * @throws CmdException 当命令执行失败时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 初始化响应实体，用于封装返回给支付宝的结果
        ResponseEntity responseEntity = null;

        // 构建通知订单服务的参数
        JSONObject paramIn = new JSONObject();
        // 设置订单ID参数，使用支付宝返回的商户订单号(out_trade_no)
        // 该订单号与创建订单时传入的订单号一致
        paramIn.put("oId", reqJson.getString("out_trade_no"));
        
        // 调用临时停车费订单服务，通知订单支付结果
        // 订单服务会根据订单号更新订单状态为已支付
        responseEntity = tempCarFeeCreateOrderV1InnerServiceSMOImpl.notifyOrder(paramIn);
        
        // 设置响应实体到上下文，框架会自动将响应返回给支付宝
        context.setResponseEntity(responseEntity);
    }
}