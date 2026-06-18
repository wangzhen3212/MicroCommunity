/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.java110.dto.parking.ParkingCouponCarDto;
import com.java110.dto.parking.ParkingCouponShopDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.shop.StoreShopDto;
import com.java110.intf.acct.IParkingCouponCarV1InnerServiceSMO;
import com.java110.intf.acct.IParkingCouponShopV1InnerServiceSMO;
import com.java110.intf.acct.IParkingCouponV1InnerServiceSMO;
import com.java110.intf.mall.IShopInnerServiceSMO;
import com.java110.intf.shore.IShoreShopV1InnerServiceSMO;
import com.java110.intf.store.IStoreShopV1InnerServiceSMO;
import com.java110.po.parking.ParkingCouponCarPo;
import com.java110.po.parking.ParkingCouponShopPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Java110CmdDoc(title = "商家赠送停车劵",
        description = "商家通过此接口赠送停车劵",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/parkingCoupon.saveParkingCouponCar",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "parkingCoupon.saveParkingCouponCar"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "couponShopId", length = 30, remark = "优惠劵ID"),
        @Java110ParamDoc(name = "giveWay", length = 30, remark = "赠送方式 1001 扫码获取 2002 商家添加 3003 购物自动赠送"),
        @Java110ParamDoc(name = "carNum", length = 30, remark = "车牌号"),
        @Java110ParamDoc(name = "shopId", length = 30, remark = "店铺ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),

        }
)

@Java110ExampleDoc(
        reqBody = "{\"shopId\":\"502022101140520018\",\"giveWay\":\"2002\",\"carNum\":\"青A88888\",\"couponShopId\":\"102022101112890007\"}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)

/**
 * 停车券赠送命令类
 * 
 * 该类负责处理商家向车辆赠送停车券的业务逻辑，包括参数验证、库存检查、并发控制、
 * 库存扣减和停车券发放等操作。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-12
 */
@Java110Cmd(serviceCode = "parkingCoupon.saveParkingCouponCar")
public class SaveParkingCouponCarCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveParkingCouponCarCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 停车券车辆服务接口 */
    @Autowired
    private IParkingCouponCarV1InnerServiceSMO parkingCouponCarV1InnerServiceSMOImpl;

    /** 停车券服务接口 */
    @Autowired
    private IParkingCouponV1InnerServiceSMO parkingCouponV1InnerServiceSMOImpl;

    /** 停车券店铺服务接口 */
    @Autowired
    private IParkingCouponShopV1InnerServiceSMO parkingCouponShopV1InnerServiceSMOImpl;

    /** 店铺服务接口 */
    @Autowired
    private IStoreShopV1InnerServiceSMO shopInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、店铺存在性验证和临时票据验证
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "couponShopId", "请求报文中未包含couponShopId");
        Assert.hasKeyAndValue(reqJson, "shopId", "请求报文中未包含shopId");
        Assert.hasKeyAndValue(reqJson, "carNum", "请求报文中未包含carNum");
        Assert.hasKeyAndValue(reqJson, "giveWay", "请求报文中未包含giveWay");
        Assert.hasKeyAndValue(reqJson, "code", "请求报文中未包含临时票据");

        // 验证店铺是否存在
        StoreShopDto shopDto = new StoreShopDto();
        shopDto.setShopId(reqJson.getString("shopId"));
        List<ShopDto> shopDtos = shopInnerServiceSMOImpl.queryStoreShops(shopDto);

        if (ListUtil.isNull(shopDtos)) {
            throw new CmdException("店铺不存在");
        }

        // 构建缓存键
        String codeKey = reqJson.getString("shopId") + reqJson.getString("code");

        // 检查是否开启停车券二维码验证
        String checkCode = MappingCache.getValue(MappingConstant.DOMAIN_SYSTEM_SWITCH, "CHECK_PARKING_COUPON_QRCODE_CODE");

        // 如果验证功能关闭，直接返回
        if ("OFF".equals(checkCode)) {
            return;
        }
        
        // 验证临时票据是否有效
        if (!reqJson.getString("code").equals(CommonCache.getAndRemoveValue(codeKey))) {
            throw new CmdException("非法操作");
        }
    }

    /**
     * 执行停车券赠送命令
     * 
     * 处理停车券赠送的核心业务逻辑，包括库存检查、并发控制、库存扣减和停车券发放
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 查询停车券店铺信息
        ParkingCouponShopDto parkingCouponShopDto = new ParkingCouponShopDto();
        parkingCouponShopDto.setCouponShopId(reqJson.getString("couponShopId"));
        parkingCouponShopDto.setShopId(reqJson.getString("shopId"));
        List<ParkingCouponShopDto> parkingCouponShopDtos = parkingCouponShopV1InnerServiceSMOImpl.queryParkingCouponShops(parkingCouponShopDto);

        // 验证停车券是否存在且唯一
        Assert.listOnlyOne(parkingCouponShopDtos, "停车劵不存在");

        // 检查停车券库存
        int quantity = Integer.parseInt(parkingCouponShopDtos.get(0).getQuantity());
        if (quantity < 1) {
            throw new CmdException("停车劵不足，请购买");
        }
        
        int flag = 0;
        // 使用分布式锁防止并发问题
        String requestId = DistributedLock.getLockUUID();
        String key = this.getClass().getSimpleName() + reqJson.getString("couponShopId");
        try {
            // 获取分布式锁
            DistributedLock.waitGetDistributedLock(key, requestId);
            
            // 在锁内重新查询库存，防止库存被其他线程修改
            parkingCouponShopDto = new ParkingCouponShopDto();
            parkingCouponShopDto.setCouponShopId(reqJson.getString("couponShopId"));
            parkingCouponShopDto.setShopId(reqJson.getString("shopId"));
            parkingCouponShopDtos = parkingCouponShopV1InnerServiceSMOImpl.queryParkingCouponShops(parkingCouponShopDto);
            quantity = Integer.parseInt(parkingCouponShopDtos.get(0).getQuantity());
            
            // 再次检查库存
            if (quantity < 1) {
                throw new CmdException("停车劵不足，请购买");
            }
            
            // 扣减停车券库存
            ParkingCouponShopPo parkingCouponShopPo = new ParkingCouponShopPo();
            parkingCouponShopPo.setCouponShopId(parkingCouponShopDtos.get(0).getCouponShopId());
            parkingCouponShopPo.setQuantity((quantity - 1) + "");
            flag = parkingCouponShopV1InnerServiceSMOImpl.updateParkingCouponShop(parkingCouponShopPo);
            
            if (flag < 1) {
                throw new CmdException("优惠券递减失败");
            }
        } finally {
            // 释放分布式锁
            DistributedLock.releaseDistributedLock(key, requestId);
        }

        // 创建停车券车辆记录
        ParkingCouponCarPo parkingCouponCarPo = BeanConvertUtil.covertBean(reqJson, ParkingCouponCarPo.class);
        parkingCouponCarPo.setPccId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        parkingCouponCarPo.setCouponId(parkingCouponShopDtos.get(0).getCouponId());
        parkingCouponCarPo.setCommunityId(parkingCouponShopDtos.get(0).getCommunityId());
        // 设置开始时间为当前时间
        parkingCouponCarPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        // 设置结束时间为第二天
        parkingCouponCarPo.setEndTime(DateUtil.getAddDayString(DateUtil.getCurrentDate(), DateUtil.DATE_FORMATE_STRING_A, 1));
        parkingCouponCarPo.setPaId(parkingCouponShopDtos.get(0).getPaId());
        // 设置状态为待使用
        parkingCouponCarPo.setState(ParkingCouponCarDto.STATE_WAIT);
        parkingCouponCarPo.setTypeCd(parkingCouponShopDtos.get(0).getTypeCd());
        parkingCouponCarPo.setValue(parkingCouponShopDtos.get(0).getValue());

        // 保存停车券车辆记录
        flag = parkingCouponCarV1InnerServiceSMOImpl.saveParkingCouponCar(parkingCouponCarPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 返回成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}