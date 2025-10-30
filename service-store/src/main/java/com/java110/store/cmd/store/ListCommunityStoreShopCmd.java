/**
 * 查询周围店铺命令类
 * 
 * 该命令类用于根据小区ID查询周边店铺信息，主要用于停车券商家购买停车券时查询小区周边的商铺（如饭店等）
 * 支持分页查询，并根据系统配置决定使用商城模块还是店铺模块进行数据查询
 * 同时会为店铺信息刷入停车券数量统计信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.parking.ParkingCouponShopDto;
import com.java110.dto.shop.ShopCommunityDto;
import com.java110.dto.shop.StoreShopCommunityDto;
import com.java110.intf.acct.IParkingCouponShopV1InnerServiceSMO;
import com.java110.intf.mall.IShopCommunityInnerServiceSMO;
import com.java110.intf.store.IStoreShopCommunityV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110CmdDoc(title = "查询周围店铺",
        description = "根据小区ID查询周围店铺，设计此接口主要 用于停车卷 商家购买 停车卷时 查询小区周边 商铺，比如 饭店等",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/store.listCommunityStoreShop",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "store.listCommunityStoreShop",
        seq = 7
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/store.listCommunityStoreShop?communityId=123123&page=1&row=10",
        resBody = "{'code':0,'msg':'成功'}"
)

@Java110Cmd(serviceCode = "store.listCommunityStoreShop")
public class ListCommunityStoreShopCmd extends Cmd {

    /**
     * 商城小区信息服务接口（可选注入）
     * 当系统配置启用商城模块时使用
     */
    @Autowired(required = false)
    private IShopCommunityInnerServiceSMO shopCommunityInnerServiceSMOImpl;

    /**
     * 店铺小区关联信息服务接口
     * 当系统配置未启用商城模块时使用
     */
    @Autowired
    private IStoreShopCommunityV1InnerServiceSMO storeShopCommunityV1InnerServiceSMOImpl;

    /**
     * 停车券店铺信息服务接口
     * 用于查询店铺的停车券统计信息
     */
    @Autowired
    private IParkingCouponShopV1InnerServiceSMO parkingCouponShopV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必要参数
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 命令执行方法
     * 
     * 根据小区ID查询周边店铺信息，支持分页查询
     * 根据系统配置决定使用商城模块还是店铺模块进行查询
     * 同时为查询结果刷入停车券数量信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     * @throws ParseException 当数据解析错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为DTO对象
        StoreShopCommunityDto storeShopCommunityDto = BeanConvertUtil.covertBean(reqJson, StoreShopCommunityDto.class);
        List<ShopCommunityDto> storeShopCommunityDtos = null;
        int count = 0;
        
        // 根据系统配置决定使用哪个服务进行查询
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            // 使用商城模块服务查询
            count = shopCommunityInnerServiceSMOImpl.queryShopCommunitysCount(storeShopCommunityDto);
            if (count > 0) {
                storeShopCommunityDtos = shopCommunityInnerServiceSMOImpl.queryShopCommunitys(storeShopCommunityDto);
            } else {
                storeShopCommunityDtos = new ArrayList<>();
            }
        } else {
            // 使用店铺模块服务查询
            count = storeShopCommunityV1InnerServiceSMOImpl.queryStoreShopCommunitysCount(storeShopCommunityDto);
            if (count > 0) {
                storeShopCommunityDtos = storeShopCommunityV1InnerServiceSMOImpl.queryStoreShopCommunitys(storeShopCommunityDto);
            } else {
                storeShopCommunityDtos = new ArrayList<>();
            }
        }
        
        // 为查询结果刷入停车券数量信息
        freshParkingCoupon(storeShopCommunityDtos);
        
        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, storeShopCommunityDtos);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷入停车券数量信息
     * 
     * 为店铺列表中的每个店铺查询并设置停车券数量
     * 只处理数量不超过20个的店铺列表，避免性能问题
     * 
     * @param storeShopCommunityDtos 店铺小区关联数据列表
     */
    private void freshParkingCoupon(List<ShopCommunityDto> storeShopCommunityDtos) {
        /**
         * 刷入停车卷数
         */
        // 检查参数有效性
        if (storeShopCommunityDtos == null || storeShopCommunityDtos.size() < 1) {
            return;
        }
        // 限制处理数量，避免性能问题
        if (storeShopCommunityDtos.size() > 20) {
            return;
        }
        
        // 提取所有店铺ID
        List<String> shopIds = new ArrayList<>();
        for (ShopCommunityDto shopCommunityDto : storeShopCommunityDtos) {
            shopIds.add(shopCommunityDto.getShopId());
        }
        
        // 查询停车券统计信息
        ParkingCouponShopDto parkingCouponShopDto = new ParkingCouponShopDto();
        parkingCouponShopDto.setShopIds(shopIds.toArray(new String[shopIds.size()]));
        List<ParkingCouponShopDto> parkingCouponShopDtos = parkingCouponShopV1InnerServiceSMOImpl.queryParkingCouponShopStatistics(parkingCouponShopDto);
        
        // 将停车券数量设置到对应的店铺对象中
        for (ShopCommunityDto shopCommunityDto : storeShopCommunityDtos) {
            for (ParkingCouponShopDto parkingCouponShopDto1 : parkingCouponShopDtos) {
                // 匹配店铺ID
                if (!parkingCouponShopDto1.getShopId().equals(shopCommunityDto.getShopId())) {
                    continue;
                }
                // 设置停车券数量
                shopCommunityDto.setParkingCouponCount(parkingCouponShopDto1.getQuantity());
            }
        }
    }
}