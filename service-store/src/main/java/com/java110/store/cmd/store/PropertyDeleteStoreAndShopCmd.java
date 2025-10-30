package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.account.AccountDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.shop.ShopCommunityDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.shop.StoreShopDto;
import com.java110.dto.shop.StoreShopCommunityDto;
import com.java110.intf.acct.IAccountBondObjInnerServiceSMO;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.mall.IShopCommunityInnerServiceSMO;
import com.java110.intf.mall.IShopInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.intf.user.IPrivilegeUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.account.AccountPo;
import com.java110.po.store.StorePo;
import com.java110.po.shop.StoreShopPo;
import com.java110.po.shop.StoreShopCommunityPo;
import com.java110.po.user.UserPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Java110CmdDoc(title = "物业公司删除商户和商铺功能",
        description = "此接口目前主要计划用于停车卷相关使用",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/store.propertyDeleteStoreAndShop",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "store.propertyDeleteStoreAndShop",
        seq = 6
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "shopId", length = 30, remark = "商铺编号"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"shopId\":\"123123123\",\"communityId\":\"2022081539020475\"}",
        resBody = "{'code':0,'msg':'成功'}"
)

/**
 * 物业公司删除商户和商铺功能命令类
 * <p>
 * 此接口目前主要计划用于停车卷相关使用，负责删除商户、商铺及其相关关联数据
 * 包括商铺小区关系、商铺信息、商户信息、用户信息等
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "store.propertyDeleteStoreAndShop")
public class PropertyDeleteStoreAndShopCmd extends Cmd {
    /** 代码前缀ID常量 */
    public static final String CODE_PREFIX_ID = "10";

    /** 商户服务接口 */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /** 商户属性服务接口 */
    @Autowired
    private IStoreAttrV1InnerServiceSMO storeAttrV1InnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /** 商户用户关系服务接口 */
    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    /** 组织服务接口 */
    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /** 组织员工关系服务接口 */
    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    /** 权限用户服务接口 */
    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    /** 商户商铺服务接口 */
    @Autowired
    private IStoreShopV1InnerServiceSMO storeShopV1InnerServiceSMOImpl;

    /** 账户服务接口 */
    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    /** 账户绑定对象服务接口 */
    @Autowired
    private IAccountBondObjInnerServiceSMO accountBondObjInnerServiceSMOImpl;

    /** 商户商铺小区关系服务接口 */
    @Autowired
    private IStoreShopCommunityV1InnerServiceSMO storeShopCommunityV1InnerServiceSMOImpl;

    /** 小区服务接口 */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /** 商铺服务接口（可选） */
    @Autowired(required = false)
    private IShopInnerServiceSMO shopInnerServiceSMOImpl;

    /** 商铺小区关系服务接口（可选） */
    @Autowired(required = false)
    private IShopCommunityInnerServiceSMO shopCommunityInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含shopId
        Assert.hasKeyAndValue(reqJson, "shopId", "未包含商家商铺");
        // 验证请求参数中必须包含communityId
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行删除商户和商铺的核心业务逻辑
     * <p>
     * 该方法会按照以下顺序删除相关数据：
     * 1. 删除商铺与小区的关系
     * 2. 删除商铺信息
     * 3. 删除商户信息
     * 4. 删除关联的用户信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 1.0 删除商铺和小区的关系
        StoreShopCommunityDto shopCommunityDto = new StoreShopCommunityDto();
        shopCommunityDto.setCommunityId(reqJson.getString("communityId"));
        shopCommunityDto.setShopId(reqJson.getString("shopId"));

        List<ShopCommunityDto> storeShopCommunityDtos = null;
        // 根据系统配置选择不同的服务实现
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            // 使用商城版的服务
            storeShopCommunityDtos = shopCommunityInnerServiceSMOImpl.queryShopCommunitys(shopCommunityDto);
        } else {
            // 使用标准版的服务
            storeShopCommunityDtos =  storeShopCommunityV1InnerServiceSMOImpl.queryStoreShopCommunitys(shopCommunityDto);
        }

        // 如果不存在商铺小区关系，直接返回
        if (storeShopCommunityDtos == null || storeShopCommunityDtos.size() < 1) {
            return;
        }
        
        StoreShopCommunityPo storeShopCommunityPo = new StoreShopCommunityPo();
        storeShopCommunityPo.setScId(storeShopCommunityDtos.get(0).getScId());
        int flag = 0;

        // 根据系统配置选择不同的删除服务
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            flag = shopCommunityInnerServiceSMOImpl.deleteShopCommunity(storeShopCommunityPo);
        } else {
            flag = storeShopCommunityV1InnerServiceSMOImpl.deleteStoreShopCommunity(storeShopCommunityPo);
        }
        
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 2.0 删除商铺信息
        List<ShopDto> storeShopDtos = null;
        // 根据系统配置选择不同的查询服务
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            ShopDto storeShopDto = new ShopDto();
            storeShopDto.setShopId(reqJson.getString("shopId"));
            storeShopDtos = shopInnerServiceSMOImpl.queryShops(storeShopDto);
        } else {
            StoreShopDto storeShopDto = new StoreShopDto();
            storeShopDto.setShopId(reqJson.getString("shopId"));
            storeShopDtos =  storeShopV1InnerServiceSMOImpl.queryStoreShops(storeShopDto);
        }

        // 如果不存在商铺信息，直接返回
        if (storeShopDtos == null || storeShopDtos.size() < 1) {
            return;
        }
        
        StoreShopPo storeShopPo = new StoreShopPo();
        storeShopPo.setShopId(storeShopDtos.get(0).getShopId());
        flag = storeShopV1InnerServiceSMOImpl.deleteStoreShop(storeShopPo);
        
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 3.0 删除商户信息
        StorePo storePo = BeanConvertUtil.covertBean(reqJson, StorePo.class);
        storePo.setStoreId(storeShopDtos.get(0).getStoreId());
        flag = storeV1InnerServiceSMOImpl.deleteStore(storePo);
        
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 4.0 删除关联的用户信息
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setStoreId(storeShopDtos.get(0).getStoreId());

        List<StoreUserDto> storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);

        // 如果不存在关联用户，直接返回
        if (storeUserDtos == null || storeUserDtos.size() < 1) {
            return;
        }

        // 遍历所有关联用户并删除
        UserPo userPo = null;
        for(StoreUserDto tmpStoreUserDto : storeUserDtos){
            userPo = new UserPo();
            userPo.setUserId(tmpStoreUserDto.getUserId());
            userV1InnerServiceSMOImpl.deleteUser(userPo);
        }
    }

    /**
     * 保存商铺小区关系
     * <p>
     * 该方法用于创建商铺与小区之间的关联关系
     * 
     * @param reqJson 请求参数JSON对象，包含小区ID、商铺ID等信息
     * @throws IllegalArgumentException 当保存失败时抛出异常
     */
    private void saveShopCommunity(JSONObject reqJson) {
        // 创建商铺小区关系对象
        StoreShopCommunityPo storeShopCommunityPo = new StoreShopCommunityPo();
        storeShopCommunityPo.setAddress("无");
        storeShopCommunityPo.setCityCode(reqJson.getString("areaCode"));
        storeShopCommunityPo.setCodeName("无");
        storeShopCommunityPo.setCommunityId(reqJson.getString("communityId"));
        storeShopCommunityPo.setCommunityName(reqJson.getString("communityName"));
        storeShopCommunityPo.setEndTime("2050-01-01"); // 设置结束时间为2050年
        storeShopCommunityPo.setMessage("物业添加");
        storeShopCommunityPo.setScId(GenerateCodeFactory.getGeneratorId("10")); // 生成唯一ID
        storeShopCommunityPo.setShopId(reqJson.getString("shopId"));
        storeShopCommunityPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置开始时间为当前时间
        storeShopCommunityPo.setState(StoreShopCommunityDto.STATE_SUCCESS); // 设置状态为成功
        
        int flag = 0;
        // 根据系统配置选择不同的保存服务
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            flag = shopCommunityInnerServiceSMOImpl.saveShopCommunity(storeShopCommunityPo);
        } else {
            flag = storeShopCommunityV1InnerServiceSMOImpl.saveStoreShopCommunity(storeShopCommunityPo);
        }
        
        // 检查保存操作是否成功
        if (flag < 1) {
            throw new IllegalArgumentException("小区关联商铺失败");
        }
    }

    /**
     * 创建账户信息
     * <p>
     * 为商户创建对应的现金账户
     * 
     * @param storePo 商户信息对象
     * @param reqJson 请求参数JSON对象，包含商铺ID、联系方式等信息
     */
    private void addAccountDto(StorePo storePo, JSONObject reqJson) {
        // 创建账户对象
        AccountPo accountPo = new AccountPo();
        accountPo.setAmount("0"); // 初始金额为0
        accountPo.setAcctId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_acctId)); // 生成账户ID
        accountPo.setObjId(storePo.getStoreId()); // 设置关联对象ID为商户ID
        accountPo.setObjType(AccountDto.OBJ_TYPE_STORE); // 设置对象类型为商户
        accountPo.setAcctType(AccountDto.ACCT_TYPE_CASH); // 设置账户类型为现金账户
        accountPo.setAcctName(storePo.getName()); // 设置账户名称为商户名称
        accountPo.setPartId(reqJson.getString("shopId")); // 设置参与方ID为商铺ID
        accountPo.setLink(reqJson.getString("link")); // 设置联系方式
        
        // 保存账户信息
        accountInnerServiceSMOImpl.saveAccount(accountPo);
    }

    /**
     * 保存商铺信息
     * <p>
     * 创建商铺基本信息记录
     * 
     * @param storePo 商户信息对象
     * @param reqJson 请求参数JSON对象，包含商铺名称、区域代码等信息
     * @throws CmdException 当保存失败时抛出异常
     */
    private void saveShop(StorePo storePo, JSONObject reqJson) {
        // 创建商铺对象
        StoreShopPo shopPo = new StoreShopPo();
        shopPo.setShopName(reqJson.getString("shopName")); // 设置商铺名称
        shopPo.setShopDesc("无"); // 设置商铺描述
        shopPo.setReturnPerson("无"); // 设置退货联系人
        shopPo.setReturnLink(reqJson.getString("link")); // 设置退货联系方式
        shopPo.setStoreId(storePo.getStoreId()); // 设置关联商户ID
        shopPo.setSendAddress("无"); // 设置发货地址
        shopPo.setReturnAddress("无"); // 设置退货地址
        shopPo.setShopType("1012021070202890002"); // 设置商铺类型
        shopPo.setOpenType(ShopDto.OPEN_TYPE_SHOP); // 设置开放类型为商铺
        shopPo.setAreaCode(reqJson.getString("areaCode")); // 设置区域代码
        shopPo.setShopId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_shopId)); // 生成商铺ID
        shopPo.setShopLogo(""); // 设置商铺Logo为空
        shopPo.setMapX(storePo.getMapX()); // 设置地图X坐标
        shopPo.setMapY(storePo.getMapY()); // 设置地图Y坐标
        shopPo.setState(ShopDto.STATE_Y); // 设置状态为启用
        
        int flag = 0;
        // 根据系统配置选择不同的保存服务
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            flag = shopInnerServiceSMOImpl.saveShop(shopPo);
        } else {
            flag = storeShopV1InnerServiceSMOImpl.saveStoreShop(shopPo);
        }
        
        // 检查保存操作是否成功
        if (flag < 1) {
            throw new CmdException("保存商铺失败");
        }
        
        // 将生成的商铺ID设置到请求参数中
        reqJson.put("shopId", shopPo.getShopId());
    }
}