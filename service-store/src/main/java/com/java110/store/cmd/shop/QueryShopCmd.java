package com.java110.store.cmd.shop;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.area.AreaDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.shop.StoreShopDto;
import com.java110.intf.acct.IAccountBondObjInnerServiceSMO;
import com.java110.intf.common.IAreaInnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.store.IStoreShopV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询商铺信息命令类
 * 
 * <p>此类主要用于查询商铺信息，为未购买HC社区商城的用户提供商铺查询功能。
 * 如果用户已购买HC社区商城，建议将接口调整为调用shop-service；
 * 如果未购买，则调用store-service，即使用本类。</p>
 * 
 * <p>主要应用场景：停车劵商家登录后赠送停车劵时，需要查询商铺信息。</p>
 * 
 * <p>HC小区管理系统原则上是开源的，包括：
 * - HC小区管理系统后端
 * - HC小区管理系统前段  
 * - HC智慧家园（业主端）
 * - HC掌上物业（物业手机端）
 * - HC小区物联网
 * - HC社区政务</p>
 * 
 * <p>但HC智慧商城需要购买才能使用，本类为未购买的用户提供替代方案。</p>
 * 
 * @author HC小区管理系统
 * @version 1.0
 * @see Cmd
 * @see StoreShopDto
 * @see ShopDto
 */
@Java110Cmd(serviceCode = "/shop/queryShop")
public class QueryShopCmd extends Cmd {

    /** 商铺信息服务接口 */
    @Autowired
    private IStoreShopV1InnerServiceSMO storeShopV1InnerServiceSMOImpl;

    /** 商户信息服务接口 */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;
    
    /** 账户保证金对象服务接口 */
    @Autowired
    private IAccountBondObjInnerServiceSMO accountBondObjInnerServiceSMOImpl;
    
    /** 区域信息服务接口 */
    @Autowired
    private IAreaInnerServiceSMO areaInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * <p>验证请求参数的有效性，主要调用父类的分页信息验证方法</p>
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行商铺查询命令
     * 
     * <p>主要流程：
     * 1. 从请求头获取商户ID
     * 2. 转换请求参数为商铺查询对象
     * 3. 处理状态参数（支持多状态查询）
     * 4. 查询商铺总数和商铺列表
     * 5. 刷新商铺图片和商户信息
     * 6. 构建并返回查询结果</p>
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取商户ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        
        // 将请求参数转换为商铺查询对象
        StoreShopDto shopDto = BeanConvertUtil.covertBean(reqJson, StoreShopDto.class);
        
        // 处理状态参数，支持多状态查询（用逗号分隔）
        String states = reqJson.getString("states");
        if (states != null && states.contains(",")) {
            shopDto.setStates(states.split(","));
        }
        shopDto.setStoreId(storeId);

        // 查询符合条件的商铺总数
        int count = storeShopV1InnerServiceSMOImpl.queryStoreShopsCount(shopDto);

        List<ShopDto> shopDtos = null;
        if (count > 0) {
            // 查询商铺列表数据
            shopDtos = storeShopV1InnerServiceSMOImpl.queryStoreShops(shopDto);
            List<String> shopIds = new ArrayList<>();
            
            // 收集所有商铺ID
            for (ShopDto prod : shopDtos) {
                shopIds.add(prod.getShopId());
            }
            
            // 刷新商铺图片信息
            freshShopImg(shopDtos);
            // 刷新商户信息
            freshStore(shopDtos);
        } else {
            shopDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) shopDto.getRow()), count, shopDtos);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新商户信息
     * 
     * <p>为商铺列表中的每个商铺设置对应的商户名称、电话和地址信息</p>
     * 
     * @param shopDtos 商铺数据列表
     */
    private void freshStore(List<ShopDto> shopDtos) {
        // 参数校验
        if (shopDtos == null || shopDtos.size() < 1) {
            return;
        }
        
        // 收集所有商户ID
        List<String> storeIds = new ArrayList<>();
        for (ShopDto shopDto : shopDtos) {
            storeIds.add(shopDto.getStoreId());
        }

        // 构建商户查询条件
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreIds(storeIds.toArray(new String[storeIds.size()]));
        
        // 查询商户信息
        List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);

        // 为每个商铺设置商户信息
        for (ShopDto shopDto : shopDtos) {
            for (StoreDto tmpStoreDto : storeDtos) {
                if (shopDto.getStoreId().equals(tmpStoreDto.getStoreId())) {
                    shopDto.setStoreName(tmpStoreDto.getStoreName());
                    shopDto.setStoreTel(tmpStoreDto.getTel());
                    shopDto.setStoreAddress(tmpStoreDto.getAddress());
                }
            }
        }
    }

    /**
     * 刷新商铺图片和区域信息
     * 
     * <p>主要功能：
     * 1. 设置商铺Logo的完整URL
     * 2. 设置商铺手机端首页URL
     * 3. 根据区域代码设置完整的区域名称</p>
     * 
     * @param shopDtos 商铺数据列表
     */
    private void freshShopImg(List<ShopDto> shopDtos) {
        // 从缓存中获取图片域名和商城域名
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        String mallUrl = MappingCache.getValue(MappingConstant.URL_DOMAIN, "MALL_URL");

        // 确保URL以斜杠结尾
        imgUrl += (!StringUtil.isEmpty(imgUrl) && imgUrl.endsWith("/") ? "" : "/");
        mallUrl += (!StringUtil.isEmpty(mallUrl) && mallUrl.endsWith("/") ? "" : "/");

        // 为每个商铺设置图片和区域信息
        for (ShopDto shopDto : shopDtos) {
            // 设置完整的Logo URL
            shopDto.setShopLogo(imgUrl + shopDto.getShopLogo());
            
            // 设置手机端首页URL（包含商铺ID参数）
            shopDto.setPhoneIndexUrl(mallUrl + "#/pages/shopIndex/index?shopId=" + shopDto.getShopId());
            
            // 如果有区域代码，查询并设置完整的区域名称
            if (!StringUtil.isEmpty(shopDto.getAreaCode())) {
                AreaDto areaDto = new AreaDto();
                areaDto.setAreaCode(shopDto.getAreaCode());
                
                // 查询省市区信息
                List<AreaDto> areaDtos = areaInnerServiceSMOImpl.getProvCityArea(areaDto);
                if (areaDtos != null && areaDtos.size() > 0) {
                    // 拼接完整的区域名称（省-市-区）
                    shopDto.setAreaName(areaDtos.get(0).getProvName() + "-" + 
                                       areaDtos.get(0).getCityName() + "-" + 
                                       areaDtos.get(0).getAreaName());
                }
            }
        }
    }
}