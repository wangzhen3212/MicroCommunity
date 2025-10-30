package com.java110.store.cmd.shop;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.shop.StoreShopDto;
import com.java110.intf.store.IStoreShopV1InnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 申请商户临时票据命令类
 * 
 * 该命令用于为指定商铺生成临时的访问票据，主要用于商铺相关操作的临时授权验证
 * 服务代码：shop.applyShopCode
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "shop.applyShopCode")
public class ApplyShopCodeCmd extends Cmd {

    /**
     * 商铺信息服务接口，用于查询商铺相关信息
     */
    @Autowired
    private IStoreShopV1InnerServiceSMO storeShopV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的商铺信息，并检查当前用户是否有权限操作该商铺
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头信息和设置响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含shopId字段
        Assert.hasKeyAndValue(reqJson, "shopId", "未包含商铺信息");

        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");

        // 构建商铺查询条件
        StoreShopDto storeShopDto = new StoreShopDto();
        storeShopDto.setShopId(reqJson.getString("shopId"));  // 设置要查询的商铺ID
        storeShopDto.setStoreId(storeId);  // 设置当前店铺ID，用于权限验证
        
        // 查询商铺信息，验证当前用户是否有权限操作该商铺
        List<ShopDto> storeShopDtos = storeShopV1InnerServiceSMOImpl.queryStoreShops(storeShopDto);

        // 验证查询结果必须且只能有一条记录，确保商铺存在且用户有操作权限
        Assert.listOnlyOne(storeShopDtos, "您没有权限操作");
    }

    /**
     * 执行命令的主方法
     * 
     * 为指定商铺生成临时票据，并将票据存入缓存，设置过期时间
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 生成唯一的UUID作为临时票据
        String code = GenerateCodeFactory.getUUID();

        // 将票据存入缓存，key格式为：shopId+code，设置默认的过期时间
        // 这样设计可以确保每个商铺的票据都是唯一的，并且可以设置过期时间自动清理
        CommonCache.setValue(reqJson.getString("shopId") + code, code, CommonCache.PAY_DEFAULT_EXPIRE_TIME);

        // 设置响应结果，返回生成的临时票据
        context.setResponseEntity(ResultVo.createResponseEntity(code));
    }
}