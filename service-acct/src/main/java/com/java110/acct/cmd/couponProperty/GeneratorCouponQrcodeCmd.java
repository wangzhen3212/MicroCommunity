package com.java110.acct.cmd.couponProperty;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.coupon.ICouponAdapt;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.coupon.CouponPropertyUserDto;
import com.java110.dto.coupon.CouponQrCodeDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.ICouponPropertyUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 优惠券核销码生成命令类
 * 
 * 该类负责处理优惠券核销码的生成请求，包括参数验证、用户验证、优惠券状态检查
 * 以及通过适配器模式调用具体的优惠券类型处理器生成二维码信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "优惠券核销码",
        description = "供业主端生成优惠券核销码",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/couponProperty.generatorCouponQrcode",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "couponProperty.generatorCouponQrcode"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "couponId", length = 30, remark = "优惠券ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "qrCode", type = "String", remark = "二维码信息"),
                @Java110ParamDoc(parentNodeName = "data", name = "remark", type = "String", remark = "优惠券核销说明"),
        }
)

@Java110ExampleDoc(
        reqBody = "{'couponId':'123123'}",
        resBody = "{'code':0,'msg':'成功','data':{'qrCode':'123123','remark':'nihao'}}"
)

@Java110Cmd(serviceCode = "couponProperty.generatorCouponQrcode")
public class GeneratorCouponQrcodeCmd extends Cmd {

    /**
     * 优惠券用户关系服务接口
     */
    @Autowired
    private ICouponPropertyUserV1InnerServiceSMO couponPropertyUserV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的优惠券ID参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含couponId字段
        Assert.hasKeyAndValue(reqJson, "couponId", "未包含优惠券ID");
    }

    /**
     * 执行优惠券核销码生成命令
     * 
     * 该方法执行完整的优惠券核销码生成流程：
     * 1. 验证用户身份
     * 2. 检查优惠券是否存在且有效
     * 3. 根据优惠券类型获取对应的适配器
     * 4. 生成二维码信息并返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出异常
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");

        // 校验用户是否存在
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 验证用户信息唯一性
        Assert.listOnlyOne(userDtos,"用户不存在");

        // 构建优惠券查询条件
        CouponPropertyUserDto couponPropertyUserDto = new CouponPropertyUserDto();
        couponPropertyUserDto.setCouponId(reqJson.getString("couponId")); // 设置优惠券ID
        couponPropertyUserDto.setTel(userDtos.get(0).getTel()); // 设置用户手机号
        couponPropertyUserDto.setState(CouponPropertyUserDto.STATE_WAIT); // 设置优惠券状态为待使用

        // 查询用户拥有的优惠券信息
        List<CouponPropertyUserDto> couponPropertyUserDtos = couponPropertyUserV1InnerServiceSMOImpl.queryCouponPropertyUsers(couponPropertyUserDto);

        // 验证优惠券是否存在
        if (couponPropertyUserDtos == null || couponPropertyUserDtos.size() < 1) {
            throw new CmdException("优惠券不存在");
        }

        // 验证优惠券是否已过期
        if (!"Y".equals(couponPropertyUserDtos.get(0).getIsExpire())) {
            throw new CmdException("优惠券已过期");
        }

        // 获取优惠券类型，用于确定使用哪个适配器
        String toType = couponPropertyUserDtos.get(0).getToType();

        // 根据优惠券类型获取对应的适配器
        ICouponAdapt couponAdapt = ApplicationContextFactory.getBean(ICouponAdapt.COUPON_PRE+toType,ICouponAdapt.class);

        // 验证适配器是否存在
        if(couponAdapt == null){
            throw new CmdException("优惠券不支持生成二维码");
        }

        // 调用适配器生成二维码信息
        CouponQrCodeDto couponQrCodeDto = couponAdapt.generatorQrcode(couponPropertyUserDtos.get(0));

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(couponQrCodeDto));
    }
}