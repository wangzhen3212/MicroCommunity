/**
 * 优惠券赠送车辆命令类
 * 
 * 功能：处理业主缴费获取的优惠券赠送车辆免费停车的业务逻辑
 * 主要流程包括：
 * 1. 验证请求参数和用户权限
 * 2. 校验优惠券的可用性和数量
 * 3. 使用分布式锁保证并发安全
 * 4. 更新优惠券库存状态
 * 5. 生成优惠券使用记录
 * 6. 创建停车券信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.acct.cmd.couponProperty;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.coupon.CouponPropertyPoolConfigDto;
import com.java110.dto.coupon.CouponPropertyUserDto;
import com.java110.dto.parking.ParkingCouponCarDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.coupon.CouponPropertyUserPo;
import com.java110.po.coupon.CouponPropertyUserDetailPo;
import com.java110.po.parking.ParkingCouponCarPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110CmdDoc(title = "优惠券赠送车辆",
        description = "业主缴费获取的优惠券赠送车辆免费停车",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/couponProperty.couponPropertyUserGiftCar",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "couponProperty.couponPropertyUserGiftCar"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "carNum", length = 12, remark = "赠送车辆"),
        @Java110ParamDoc(name = "couponId", length = 30, remark = "赠送优惠券"),
        @Java110ParamDoc(name = "giftCount", type = "int", length = 11, remark = "赠送数量"),
        @Java110ParamDoc(name = "paId", length = 30, remark = "停车场"),

})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/parkingArea.listParkingAreas?num=&typeCd=&paId=&page=1&row=10&communityId=2022112555490011",
        resBody = "{\"page\":0,\"parkingAreas\":[{\"attrs\":[{\"attrId\":\"112022112796270047\",\"communityId\":\"2022112555490011\",\"listShow\":\"Y\",\"paId\":\"102022112706900045\",\"page\":-1,\"records\":0,\"row\":0,\"specCd\":\"6185-17861\",\"specName\":\"外部编码\",\"specType\":\"2233\",\"statusCd\":\"0\",\"total\":0,\"value\":\"123\"}],\"createTime\":\"2022-11-27 01:48:27\",\"num\":\"A\",\"paId\":\"102022112706900045\",\"remark\":\"\",\"typeCd\":\"1001\"}],\"records\":1,\"rows\":0,\"total\":1}"
)
@Java110Cmd(serviceCode = "couponProperty.couponPropertyUserGiftCar")
public class CouponPropertyUserGiftCarCmd extends Cmd {

    @Autowired
    private ICouponPropertyUserV1InnerServiceSMO couponPropertyUserV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IParkingCouponCarV1InnerServiceSMO parkingCouponCarV1InnerServiceSMOImpl;

    @Autowired
    private ICouponPropertyUserDetailV1InnerServiceSMO couponPropertyUserDetailV1InnerServiceSMOImpl;

    @Autowired
    private ICouponPropertyPoolConfigV1InnerServiceSMO couponPropertyPoolConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的完整性和有效性，包括：
     * - 必填字段验证
     * - 用户身份验证
     * - 优惠券状态和数量验证
     * - 优惠券配置信息验证
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "carNum", "未包含车辆");
        Assert.hasKeyAndValue(reqJson, "couponId", "未包含停车券");
        Assert.hasKeyAndValue(reqJson, "giftCount", "未包含赠送数量");
        Assert.hasKeyAndValue(reqJson, "paId", "未包含停车场");

        // 获取当前用户ID
        String userId = context.getReqHeaders().get("user-id");

        // 车牌号格式化处理：去除空格并转为大写
        reqJson.put("carNum", reqJson.getString("carNum").trim().toUpperCase());

        // 验证用户是否存在
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 查询优惠券信息
        CouponPropertyUserDto couponPropertyUserDto = new CouponPropertyUserDto();
        couponPropertyUserDto.setCouponId(reqJson.getString("couponId"));
        couponPropertyUserDto.setTel(userDtos.get(0).getTel()); // 使用用户手机号查询
        couponPropertyUserDto.setToType(CouponPropertyUserDto.TO_TYPE_PARKING); // 停车类型优惠券
        couponPropertyUserDto.setState(CouponPropertyUserDto.STATE_WAIT); // 等待使用状态

        List<CouponPropertyUserDto> couponPropertyUserDtos = couponPropertyUserV1InnerServiceSMOImpl.queryCouponPropertyUsers(couponPropertyUserDto);

        // 验证优惠券是否存在
        if (ListUtil.isNull(couponPropertyUserDtos)) {
            throw new CmdException("优惠券不存在");
        }

        // 验证优惠券是否过期
        if (!"Y".equals(couponPropertyUserDtos.get(0).getIsExpire())) {
            throw new CmdException("优惠券已过期");
        }

        // 验证优惠券库存是否足够
        int stock = Integer.parseInt(couponPropertyUserDtos.get(0).getStock());
        int giftCount = Integer.parseInt(reqJson.getString("giftCount"));
        if (stock < giftCount) {
            throw new CmdException("优惠券不够赠送，当前数量为：" + stock);
        }

        // 查询优惠券配置信息（停车时长）
        CouponPropertyPoolConfigDto couponPropertyPoolConfigDto = new CouponPropertyPoolConfigDto();
        couponPropertyPoolConfigDto.setCouponId(couponPropertyUserDtos.get(0).getCppId());
        couponPropertyPoolConfigDto.setColumnKey("hours"); // 配置键为小时数
        List<CouponPropertyPoolConfigDto> couponPropertyPoolConfigDtos = couponPropertyPoolConfigV1InnerServiceSMOImpl.queryCouponPropertyPoolConfigs(couponPropertyPoolConfigDto);

        Assert.listOnlyOne(couponPropertyPoolConfigDtos, "未包含优惠券配置信息");

        // 验证配置值是否为有效数字
        Double.parseDouble(couponPropertyPoolConfigDtos.get(0).getColumnValue());
    }

    /**
     * 执行优惠券赠送命令
     * 
     * 主要业务流程：
     * 1. 使用分布式锁保证并发安全
     * 2. 再次验证优惠券状态和数量
     * 3. 更新优惠券库存
     * 4. 生成优惠券使用明细记录
     * 5. 创建停车券信息
     * 6. 返回操作结果
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     * @throws ParseException 当日期解析失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 获取当前用户ID
        String userId = context.getReqHeaders().get("user-id");

        // 验证用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 生成分布式锁相关参数
        String requestId = DistributedLock.getLockUUID();
        String key = this.getClass().getSimpleName() + reqJson.getString("couponId");
        int flag = 0; // 操作结果标志
        List<CouponPropertyUserDto> couponPropertyUserDtos = null;
        int giftCount = Integer.parseInt(reqJson.getString("giftCount"));
        
        try {
            // 获取分布式锁，防止并发操作
            // 查询优惠券信息
            CouponPropertyUserDto couponPropertyUserDto = new CouponPropertyUserDto();
            couponPropertyUserDto.setCouponId(reqJson.getString("couponId"));
            couponPropertyUserDto.setTel(userDtos.get(0).getTel());
            couponPropertyUserDto.setToType(CouponPropertyUserDto.TO_TYPE_PARKING);
            couponPropertyUserDto.setState(CouponPropertyUserDto.STATE_WAIT);

            couponPropertyUserDtos = couponPropertyUserV1InnerServiceSMOImpl.queryCouponPropertyUsers(couponPropertyUserDto);

            // 再次验证优惠券是否存在
            if (ListUtil.isNull(couponPropertyUserDtos)) {
                throw new CmdException("优惠券不存在");
            }

            // 再次验证优惠券是否过期
            if (!"Y".equals(couponPropertyUserDtos.get(0).getIsExpire())) {
                throw new CmdException("优惠券已过期");
            }

            // 再次验证库存数量
            int stock = Integer.parseInt(couponPropertyUserDtos.get(0).getStock());
            if (stock < giftCount) {
                throw new CmdException("优惠券不够赠送，当前数量为：" + stock);
            }
            
            // 更新优惠券库存信息
            CouponPropertyUserPo couponPropertyUserPo = new CouponPropertyUserPo();
            couponPropertyUserPo.setCouponId(couponPropertyUserDtos.get(0).getCouponId());
            couponPropertyUserPo.setCommunityId(couponPropertyUserDtos.get(0).getCommunityId());
            couponPropertyUserPo.setStock((stock - giftCount) + ""); // 扣减库存
            
            // 如果库存扣减为0，更新优惠券状态为已完成
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

        // 循环处理每个赠送的优惠券
        for (int giftIndex = 0; giftIndex < giftCount; giftIndex++) {
            // 生成优惠券明细记录ID
            String pccId = GenerateCodeFactory.getGeneratorId("11");

            // 保存优惠券使用明细记录
            CouponPropertyUserDetailPo couponPropertyUserDetailPo = new CouponPropertyUserDetailPo();
            couponPropertyUserDetailPo.setBusinessKey(pccId); // 关联业务ID
            couponPropertyUserDetailPo.setCommunityId(reqJson.getString("communityId"));
            couponPropertyUserDetailPo.setCouponId(couponPropertyUserDtos.get(0).getCouponId());
            couponPropertyUserDetailPo.setCouponName(couponPropertyUserDtos.get(0).getCouponName());
            couponPropertyUserDetailPo.setUoId(GenerateCodeFactory.getGeneratorId("11")); // 生成唯一ID
            couponPropertyUserDetailPo.setDetailType(couponPropertyUserDtos.get(0).getToType()); // 明细类型
            couponPropertyUserDetailPo.setRemark("赠送" + reqJson.getString("carNum") + "停车券"); // 备注信息
            flag = couponPropertyUserDetailV1InnerServiceSMOImpl.saveCouponPropertyUserDetail(couponPropertyUserDetailPo);
            if (flag < 1) {
                throw new CmdException("赠送失败");
            }

            // 查询优惠券配置信息（停车时长）
            CouponPropertyPoolConfigDto couponPropertyPoolConfigDto = new CouponPropertyPoolConfigDto();
            couponPropertyPoolConfigDto.setCouponId(couponPropertyUserDtos.get(0).getCppId());
            couponPropertyPoolConfigDto.setColumnKey("hours");
            List<CouponPropertyPoolConfigDto> couponPropertyPoolConfigDtos = couponPropertyPoolConfigV1InnerServiceSMOImpl.queryCouponPropertyPoolConfigs(couponPropertyPoolConfigDto);

            Assert.listOnlyOne(couponPropertyPoolConfigDtos, "未包含优惠券配置信息");

            // 计算停车时长（小时转分钟，并向上取整）
            double value = Double.parseDouble(couponPropertyPoolConfigDtos.get(0).getColumnValue()) * 60;
            value = Math.ceil(value);

            // 创建停车券信息
            ParkingCouponCarPo parkingCouponCarPo = new ParkingCouponCarPo();
            parkingCouponCarPo.setPccId(pccId); // 停车券ID
            parkingCouponCarPo.setCouponId(reqJson.getString("couponId")); // 优惠券ID
            parkingCouponCarPo.setCouponShopId(reqJson.getString("couponId")); // 优惠券店铺ID
            parkingCouponCarPo.setCommunityId(reqJson.getString("communityId")); // 小区ID
            parkingCouponCarPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 开始时间（当前时间）
            parkingCouponCarPo.setEndTime(DateUtil.getAddDayString(DateUtil.getCurrentDate(), DateUtil.DATE_FORMATE_STRING_A, 1)); // 结束时间（次日）
            parkingCouponCarPo.setPaId(reqJson.getString("paId")); // 停车场ID
            parkingCouponCarPo.setState(ParkingCouponCarDto.STATE_WAIT); // 等待使用状态
            parkingCouponCarPo.setTypeCd("1001"); // 类型代码：时长赠送
            parkingCouponCarPo.setGiveWay("4004"); // 赠送方式：物业缴费赠送
            parkingCouponCarPo.setValue(value + ""); // 停车时长（分钟）
            parkingCouponCarPo.setCarNum(reqJson.getString("carNum")); // 车牌号
            parkingCouponCarPo.setRemark(userDtos.get(0).getName() + "-" + userDtos.get(0).getTel() + "赠送"); // 备注信息
            parkingCouponCarPo.setShopId(userDtos.get(0).getUserId()); // 店铺ID（用户ID）

            // 保存停车券信息
            flag = parkingCouponCarV1InnerServiceSMOImpl.saveParkingCouponCar(parkingCouponCarPo);

            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }
        
        // 返回成功响应
        context.setResponseEntity(ResultVo.success());
    }
}