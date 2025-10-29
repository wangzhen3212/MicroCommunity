package com.java110.fee.cmd.payFeeQrcode;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.payFee.PayFeeQrcodeDto;
import com.java110.intf.fee.IPayFeeQrcodeV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 查询支付二维码配置命令类
 * 
 * 该类用于处理获取支付二维码配置信息的业务逻辑，通过服务码"payFeeQrcode.getQrcodeConfig"被调用
 * 主要功能包括参数验证和查询二维码配置信息
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "payFeeQrcode.getQrcodeConfig")
public class GetQrcodeConfigCmd extends Cmd {

    /**
     * 支付二维码服务接口，用于查询二维码配置信息
     */
    @Autowired
    private IPayFeeQrcodeV1InnerServiceSMO payFeeQrcodeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 验证请求参数中必须包含二维码ID
        Assert.hasKeyAndValue(reqJson, "pfqId", "未包含二维码信息");
    }

    /**
     * 执行命令方法
     * 
     * 根据验证通过的参数查询二维码配置信息，并将查询结果返回给调用方
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建二维码数据传输对象
        PayFeeQrcodeDto payFeeQrcodeDto = new PayFeeQrcodeDto();
        // 设置小区ID到查询对象
        payFeeQrcodeDto.setCommunityId(reqJson.getString("communityId"));
        // 设置二维码ID到查询对象
        payFeeQrcodeDto.setPfqId(reqJson.getString("pfqId"));
        
        // 调用服务接口查询二维码配置信息
        List<PayFeeQrcodeDto> payFeeQrcodeDtos = payFeeQrcodeV1InnerServiceSMOImpl.queryPayFeeQrcodes(payFeeQrcodeDto);

        // 验证查询结果有且仅有一条记录，确保数据准确性
        Assert.listOnlyOne(payFeeQrcodeDtos, "二维码配置错误");

        // 将查询到的第一条记录封装成响应结果并设置到上下文中
        context.setResponseEntity(ResultVo.createResponseEntity(payFeeQrcodeDtos.get(0)));
    }
}