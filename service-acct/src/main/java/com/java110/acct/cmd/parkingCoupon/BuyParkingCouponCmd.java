/**
 * 购买停车券命令类
 * 
 * 功能描述：处理停车券购买业务，包括参数验证、优惠券查询、商户信息验证、
 *          停车券库存管理、购买订单记录等完整购买流程
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.acct.cmd.parkingCoupon;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.parking.ParkingCouponDto;
import com.java110.dto.parking.ParkingCouponShopDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.shop.StoreShopDto;
import com.java110.intf.acct.IParkingCouponOrderV1InnerServiceSMO;
import com.java110.intf.acct.IParkingCouponShopV1InnerServiceSMO;
import com.java110.intf.acct.IParkingCouponV1InnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.mall.IShopInnerServiceSMO;
import com.java110.intf.store.IStoreShopV1InnerServiceSMO;
import com.java110.po.parking.ParkingCouponOrderPo;
import com.java110.po.parking.ParkingCouponShopPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

/**
 * API文档注解 - 购买停车券接口
 */
@Java110CmdDoc(title = "购买停车卷",
        description = "可供第三平台购买停车卷使用，费用 需要第三平台 收取",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/parkingCoupon.buyParkingCoupon",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "parkingCoupon.buyParkingCoupon"
)

/**
 * 请求参数文档注解
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "couponId", length = 30, remark = "优惠券ID"),
        @Java110ParamDoc(name = "quantity", length = 30, remark = "优惠券数量"),
        @Java110ParamDoc(name = "shopId", length = 30, remark = "店铺ID"),
        @Java110ParamDoc(name = "payType", length = 30, remark = "支付方式"),
        @Java110ParamDoc(name = "receivedAmount", length = 30, remark = "实收金额"),
})

/**
 * 响应参数文档注解
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),

        }
)

/**
 * 请求响应示例文档注解
 */
@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/parkingCoupon.listParkingCoupon?page=1&row=100&communityId=2022081539020475",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)

/**
 * 购买停车券命令类
 * 处理停车券购买业务逻辑，包括参数验证、数据查询、库存更新和订单记录
 */
@Java110Cmd(serviceCode = "parkingCoupon.buyParkingCoupon")
public class BuyParkingCouponCmd extends Cmd {

    // 停车券服务接口
    @Autowired
    private IParkingCouponV1InnerServiceSMO parkingCouponV1InnerServiceSMOImpl;
    
    // 停车券商户关联服务接口
    @Autowired
    private IParkingCouponShopV1InnerServiceSMO parkingCouponShopV1InnerServiceSMOImpl;

    // 停车券订单服务接口
    @Autowired
    private IParkingCouponOrderV1InnerServiceSMO parkingCouponOrderV1InnerServiceSMOImpl;

    // 商户服务接口（非必需）
    @Autowired(required = false)
    private IShopInnerServiceSMO shopInnerServiceSMOImpl;

    // 店铺服务接口
    @Autowired
    private IStoreShopV1InnerServiceSMO storeShopV1InnerServiceSMOImpl;

    // 小区服务接口
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "couponId", "未包含购买优惠券");
        Assert.hasKeyAndValue(reqJson, "quantity", "未包含购买数量");
        Assert.hasKeyAndValue(reqJson, "shopId", "未包含店铺");
        Assert.hasKeyAndValue(reqJson, "receivedAmount", "未包含购买实收");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "payType", "未包含支付方式");
    }

    /**
     * 执行购买停车券命令
     * 
     * 主要业务流程：
     * 1. 查询优惠券信息
     * 2. 验证商户和小区信息
     * 3. 更新停车券库存
     * 4. 创建购买订单记录
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当业务处理失败时抛出异常
     * @throws ParseException 当日期解析失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 查询优惠券信息
        ParkingCouponDto parkingCouponDto = new ParkingCouponDto();
        parkingCouponDto.setCouponId(reqJson.getString("couponId"));
        parkingCouponDto.setCommunityId(reqJson.getString("communityId"));
        List<ParkingCouponDto> parkingCouponDtos = parkingCouponV1InnerServiceSMOImpl.queryParkingCoupons(parkingCouponDto);

        // 验证优惠券是否存在且唯一
        Assert.listOnlyOne(parkingCouponDtos, "停车卷不存在");

        // 查询商户信息 - 根据系统配置选择不同的商户服务
        List<ShopDto> storeShopDtos = null;
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            // 使用商城商户服务
            ShopDto storeShopDto = new ShopDto();
            storeShopDto.setShopId(reqJson.getString("shopId"));
            storeShopDtos = shopInnerServiceSMOImpl.queryShops(storeShopDto);
        } else {
            // 使用普通店铺服务
            StoreShopDto storeShopDto = new StoreShopDto();
            storeShopDto.setShopId(reqJson.getString("shopId"));
            storeShopDtos = storeShopV1InnerServiceSMOImpl.queryStoreShops(storeShopDto);
        }

        // 验证商户是否存在且唯一
        Assert.listOnlyOne(storeShopDtos, "商家店铺不存在");

        // 查询小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(reqJson.getString("communityId"));
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);

        // 验证小区是否存在且唯一
        Assert.listOnlyOne(communityDtos, "小区不存在");

        // 查询停车券商户关联信息
        ParkingCouponShopDto parkingCouponShopDto = new ParkingCouponShopDto();
        parkingCouponShopDto.setShopId(reqJson.getString("shopId"));
        parkingCouponShopDto.setCommunityId(reqJson.getString("communityId"));
        parkingCouponShopDto.setCouponId(reqJson.getString("couponId"));
        List<ParkingCouponShopDto> parkingCouponShopDtos = parkingCouponShopV1InnerServiceSMOImpl.queryParkingCouponShops(parkingCouponShopDto);
        
        ParkingCouponShopPo parkingCouponShopPo = null;
        int flag = 0;
        
        // 判断是新增还是更新停车券库存
        if (parkingCouponShopDtos == null || parkingCouponShopDtos.size() < 1) {
            // 新增停车券商户关联记录
            parkingCouponShopPo = new ParkingCouponShopPo();
            parkingCouponShopPo.setCommunityId(reqJson.getString("communityId"));
            parkingCouponShopPo.setCouponShopId(GenerateCodeFactory.getGeneratorId("10")); // 生成关联ID
            parkingCouponShopPo.setShopId(reqJson.getString("shopId"));
            parkingCouponShopPo.setShopName(storeShopDtos.get(0).getShopName());
            parkingCouponShopPo.setEndTime("2050-01-01"); // 设置结束时间为2050年
            parkingCouponShopPo.setCommunityName(communityDtos.get(0).getName());
            parkingCouponShopPo.setCouponId(reqJson.getString("couponId"));
            parkingCouponShopPo.setPaId(parkingCouponDtos.get(0).getPaId());
            parkingCouponShopPo.setPaName(parkingCouponDtos.get(0).getPaName());
            parkingCouponShopPo.setQuantity(reqJson.getString("quantity")); // 设置初始数量
            parkingCouponShopPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置开始时间为当前时间
            flag = parkingCouponShopV1InnerServiceSMOImpl.saveParkingCouponShop(parkingCouponShopPo);
        } else {
            // 更新现有停车券库存数量
            String quantityStr = parkingCouponShopDtos.get(0).getQuantity();
            int quantity = Integer.parseInt(quantityStr) + Integer.parseInt(reqJson.getString("quantity")); // 累加数量
            parkingCouponShopPo = new ParkingCouponShopPo();
            parkingCouponShopPo.setCouponShopId(parkingCouponShopDtos.get(0).getCouponShopId());
            parkingCouponShopPo.setQuantity(quantity + ""); // 更新总数量
            flag = parkingCouponShopV1InnerServiceSMOImpl.updateParkingCouponShop(parkingCouponShopPo);
        }

        // 验证库存更新是否成功
        if (flag < 1) {
            throw new CmdException("增加停车卷失败");
        }

        // 创建停车券购买订单记录
        // 计算应收金额：数量 × 单价
        BigDecimal quantityDec = new BigDecimal(reqJson.getString("quantity"));
        quantityDec = quantityDec.multiply(new BigDecimal(parkingCouponDtos.get(0).getValuePrice())).setScale(2, BigDecimal.ROUND_HALF_UP);

        ParkingCouponOrderPo parkingCouponOrderPo = new ParkingCouponOrderPo();
        parkingCouponOrderPo.setCouponId(reqJson.getString("couponId"));
        parkingCouponOrderPo.setCommunityId(reqJson.getString("communityId"));
        parkingCouponOrderPo.setCommunityName(communityDtos.get(0).getName());
        parkingCouponOrderPo.setQuantity(reqJson.getString("quantity"));
        parkingCouponOrderPo.setOrderId(GenerateCodeFactory.getGeneratorId("11")); // 生成订单ID
        parkingCouponOrderPo.setPaId(parkingCouponDtos.get(0).getPaId());
        parkingCouponOrderPo.setPaName(parkingCouponDtos.get(0).getPaName());
        parkingCouponOrderPo.setPrice(parkingCouponDtos.get(0).getValuePrice());
        parkingCouponOrderPo.setReceivableAmount(quantityDec.doubleValue() + ""); // 设置应收金额
        parkingCouponOrderPo.setReceivedAmount(reqJson.getString("receivedAmount")); // 设置实收金额
        parkingCouponOrderPo.setRemark(reqJson.getString("remark"));
        parkingCouponOrderPo.setShopId(reqJson.getString("shopId"));
        parkingCouponOrderPo.setShopName(storeShopDtos.get(0).getShopName());
        parkingCouponOrderPo.setPayType(reqJson.getString("payType")); // 设置支付方式

        // 保存订单记录
        flag = parkingCouponOrderV1InnerServiceSMOImpl.saveParkingCouponOrder(parkingCouponOrderPo);
        if (flag < 1) {
            throw new CmdException("支付订单失败");
        }

    }
}