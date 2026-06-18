package com.java110.acct.cmd.couponProperty;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.coupon.CouponPropertyUserDto;
import com.java110.dto.coupon.CouponPropertyUserDetailDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.ICouponPropertyUserDetailV1InnerServiceSMO;
import com.java110.intf.acct.ICouponPropertyUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.coupon.CouponPropertyUserPo;
import com.java110.po.coupon.CouponPropertyUserDetailPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;


@Java110CmdDoc(title = "核销优惠券",
        description = "物业手机版 或者第三方平台调用 核销功能核销优惠券",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/couponProperty.writeOffCouponPropertyUser",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "couponProperty.writeOffCouponPropertyUser"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "couponQrcode", length = 30, remark = "核销码"),
        @Java110ParamDoc(name = "giftCount", type = "int", length = 11, remark = "核销数量"),

})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"couponQrcode\":\"70ff0d19c69e490b9e656795811037c5\",\"communityId\":\"2022112555490011\",\"giftCount\":1}",
        resBody = "{\"code\":0,\"data\":[{\"businessKey\":\"302022083031660031\",\"communityId\":\"2022112555490011\",\"couponId\":\"102022112610570082\",\"couponName\":\"购物券\",\"createTime\":\"2022-11-27 15:22:18\",\"detailType\":\"1011\",\"page\":-1,\"records\":0,\"remark\":\"wuxw-18909715555核销\",\"row\":0,\"statusCd\":\"0\",\"tel\":\"18909711443\",\"total\":0,\"uoId\":\"112022112729340004\",\"userName\":\"吴学文\",\"value\":\"面值:10元;\"}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":1}"
)

/**
 * 核销优惠券命令类
 * 负责处理优惠券的核销业务逻辑，包括参数验证、优惠券状态检查、库存扣减和核销记录保存
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022
 */
@Java110Cmd(serviceCode = "couponProperty.writeOffCouponPropertyUser")
public class WriteOffCouponPropertyUserCmd extends Cmd {

    @Autowired
    private ICouponPropertyUserV1InnerServiceSMO couponPropertyUserV1InnerServiceSMOImpl;

    @Autowired
    private ICouponPropertyUserDetailV1InnerServiceSMO couponPropertyUserDetailV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 验证请求参数和优惠券状态
     * 检查必要参数是否存在，验证优惠券二维码有效性，检查优惠券状态和库存数量
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败或优惠券状态不满足条件时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "couponQrcode", "未包含优惠券");
        Assert.hasKeyAndValue(reqJson, "giftCount", "未包含核销数量");

        // 通过二维码从缓存中获取优惠券ID
        String couponId = CommonCache.getAndRemoveValue(reqJson.getString("couponQrcode"));
        if(StringUtil.isEmpty(couponId)){
            throw new CmdException("优惠券不存在");
        }
        reqJson.put("couponId", couponId);

        // 查询优惠券信息
        CouponPropertyUserDto couponPropertyUserDto = new CouponPropertyUserDto();
        couponPropertyUserDto.setCouponId(reqJson.getString("couponId"));
        couponPropertyUserDto.setState(CouponPropertyUserDto.STATE_WAIT); // 只查询待使用的优惠券

        List<CouponPropertyUserDto> couponPropertyUserDtos = couponPropertyUserV1InnerServiceSMOImpl.queryCouponPropertyUsers(couponPropertyUserDto);

        // 验证优惠券是否存在
        if (couponPropertyUserDtos == null || couponPropertyUserDtos.size() < 1) {
            throw new CmdException("优惠券不存在");
        }

        // 验证优惠券是否过期
        if (!"Y".equals(couponPropertyUserDtos.get(0).getIsExpire())) {
            throw new CmdException("优惠券已过期");
        }

        // 验证库存是否足够
        int stock = Integer.parseInt(couponPropertyUserDtos.get(0).getStock());
        int giftCount = Integer.parseInt(reqJson.getString("giftCount"));
        if (stock < giftCount) {
            throw new CmdException("优惠券不够赠送，当前数量为：" + stock);
        }
    }

    /**
     * 执行优惠券核销操作
     * 使用分布式锁保证并发安全，更新优惠券库存，保存核销记录
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当核销操作失败时抛出异常
     * @throws ParseException 当解析异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分布式锁，防止并发操作
        String requestId = DistributedLock.getLockUUID();
        String key = this.getClass().getSimpleName() + reqJson.getString("couponId");
        int flag = 0;
        List<CouponPropertyUserDto> couponPropertyUserDtos = null;
        
        try {
            // 重新查询优惠券信息，确保数据一致性
            CouponPropertyUserDto couponPropertyUserDto = new CouponPropertyUserDto();
            couponPropertyUserDto.setCouponId(reqJson.getString("couponId"));
            couponPropertyUserDto.setState(CouponPropertyUserDto.STATE_WAIT);

            couponPropertyUserDtos = couponPropertyUserV1InnerServiceSMOImpl.queryCouponPropertyUsers(couponPropertyUserDto);

            // 双重验证优惠券状态
            if (couponPropertyUserDtos == null || couponPropertyUserDtos.size() < 1) {
                throw new CmdException("优惠券不存在");
            }

            if (!"Y".equals(couponPropertyUserDtos.get(0).getIsExpire())) {
                throw new CmdException("优惠券已过期");
            }

            int stock = Integer.parseInt(couponPropertyUserDtos.get(0).getStock());
            int giftCount = Integer.parseInt(reqJson.getString("giftCount"));
            if (stock < giftCount) {
                throw new CmdException("优惠券不够赠送，当前数量为：" + stock);
            }
            
            // 更新优惠券库存信息
            CouponPropertyUserPo couponPropertyUserPo = new CouponPropertyUserPo();
            couponPropertyUserPo.setCouponId(couponPropertyUserDtos.get(0).getCouponId());
            couponPropertyUserPo.setCommunityId(couponPropertyUserDtos.get(0).getCommunityId());
            couponPropertyUserPo.setStock((stock - giftCount) + ""); // 扣减库存
            
            // 如果库存扣减完毕，更新优惠券状态为已完成
            if (stock == giftCount) {
                couponPropertyUserPo.setState(CouponPropertyUserDto.STATE_FINISH);
            }
            
            flag = couponPropertyUserV1InnerServiceSMOImpl.updateCouponPropertyUser(couponPropertyUserPo);

            if (flag < 1) {
                throw new CmdException("赠送失败");
            }
        } finally {
            // 释放分布式锁
            DistributedLock.releaseDistributedLock(requestId, key);
        }

        // 保存核销记录
        String userId = context.getReqHeaders().get("user-id");
        String remark = getRemark(userId); // 获取核销备注信息

        CouponPropertyUserDetailPo couponPropertyUserDetailPo = new CouponPropertyUserDetailPo();
        couponPropertyUserDetailPo.setBusinessKey(StringUtil.isEmpty(userId)?"-1":userId); // 设置业务主键
        couponPropertyUserDetailPo.setCommunityId(reqJson.getString("communityId"));
        couponPropertyUserDetailPo.setCouponId(couponPropertyUserDtos.get(0).getCouponId());
        couponPropertyUserDetailPo.setCouponName(couponPropertyUserDtos.get(0).getCouponName());
        couponPropertyUserDetailPo.setUoId(GenerateCodeFactory.getGeneratorId("11")); // 生成唯一ID
        couponPropertyUserDetailPo.setDetailType(couponPropertyUserDtos.get(0).getToType());
        
        // 设置备注信息，优先使用请求中的备注
        if(reqJson.containsKey("remark")) {
            couponPropertyUserDetailPo.setRemark(remark + reqJson.getString("remark"));
        }else{
            couponPropertyUserDetailPo.setRemark(remark);
        }
        
        flag = couponPropertyUserDetailV1InnerServiceSMOImpl.saveCouponPropertyUserDetail(couponPropertyUserDetailPo);
        if (flag < 1) {
            throw new CmdException("赠送失败");
        }

        // 查询并返回核销记录详情
        CouponPropertyUserDetailDto couponPropertyUserDetailDto = new CouponPropertyUserDetailDto();
        couponPropertyUserDetailDto.setUoId(couponPropertyUserDetailPo.getUoId());
        List<CouponPropertyUserDetailDto> couponPropertyUserDetailDtos
                = couponPropertyUserDetailV1InnerServiceSMOImpl.queryCouponPropertyUserDetails(couponPropertyUserDetailDto);

        context.setResponseEntity(ResultVo.createResponseEntity(couponPropertyUserDetailDtos));
    }

    /**
     * 获取核销备注信息
     * 根据用户ID查询用户信息，生成格式化的核销备注
     *
     * @param userId 用户ID
     * @return 格式化后的核销备注信息
     */
    private String getRemark(String userId) {
        // 如果用户ID为空或者是默认值，返回接口核销标识
        if(StringUtil.isEmpty(userId) || "-1".equals(userId)){
            return "接口核销";
        }
        
        // 根据用户ID查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        
        // 如果用户不存在，返回接口核销标识
        if (userDtos == null || userDtos.size() < 1) {
            return "接口核销";
        }

        // 返回格式化的核销备注：用户名-手机号核销
        return userDtos.get(0).getName() + "-" + userDtos.get(0).getTel() + "核销";
    }
}