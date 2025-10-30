package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.account.AccountDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.store.StoreDto;
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
import com.java110.po.org.OrgPo;
import com.java110.po.org.OrgStaffRelPo;
import com.java110.po.privilege.PrivilegeUserPo;
import com.java110.po.store.StorePo;
import com.java110.po.store.StoreUserPo;
import com.java110.po.shop.StoreShopPo;
import com.java110.po.shop.StoreShopCommunityPo;
import com.java110.po.user.UserPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.StoreUserRelConstant;
import com.java110.utils.constant.UserLevelConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Java110CmdDoc(title = "物业公司添加商户和商铺功能",
        description = "此接口目前主要计划用于停车卷相关使用",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/store.propertySaveStoreAndShop",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "store.propertySaveStoreAndShop",
        seq = 5
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "shopName", length = 30, remark = "商铺名称 商家名称和商铺名称一致，如果需求需要不一致"),
        @Java110ParamDoc(name = "link", length = 30, remark = "管理员手机号"),
        @Java110ParamDoc(name = "password", length = 30, remark = "登录密码"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\"shopName\":\"测试商家\",\"link\":\"18909714444\",\"password\":\"123456\",\"communityId\":\"2022081539020475\"}",
        resBody="{'code':0,'msg':'成功'}"
)

/**
 * 物业公司添加商户和商铺功能命令类
 * 
 * <p>此接口主要用于物业公司为停车券相关功能添加商户和商铺信息，
 * 包括创建商户、用户、组织架构、权限、商铺和账户等完整业务流程</p>
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "store.propertySaveStoreAndShop")
public class PropertySaveStoreAndShopCmd extends Cmd {
    
    /** ID生成前缀 */
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
     * <p>验证请求参数是否完整，确保必要的参数都存在</p>
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证商铺名称参数
        Assert.hasKeyAndValue(reqJson, "shopName", "未包含商铺名称");
        // 验证手机号参数
        Assert.hasKeyAndValue(reqJson, "link", "未包含手机号");
        // 验证密码参数
        Assert.hasKeyAndValue(reqJson, "password", "未包含密码");
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行命令方法
     * 
     * <p>完整的商户和商铺创建流程，包括：<br>
     * 1. 验证小区信息<br>
     * 2. 创建商户信息<br>
     * 3. 创建用户信息<br>
     * 4. 建立商户用户关系<br>
     * 5. 创建组织架构<br>
     * 6. 建立组织员工关系<br>
     * 7. 分配权限<br>
     * 8. 创建商铺信息<br>
     * 9. 关联小区和商铺<br>
     * 10. 创建账户信息</p>
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 查询小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(reqJson.getString("communityId"));
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);

        // 验证小区是否存在且唯一
        Assert.listOnlyOne(communityDtos, "小区不存在");
        // 将小区名称和区域编码添加到请求参数中
        reqJson.put("communityName", communityDtos.get(0).getName());
        reqJson.put("areaCode", communityDtos.get(0).getCityCode());

        // 检查手机号是否已被使用
        StoreDto storeDto = new StoreDto();
        storeDto.setTel(reqJson.getString("link"));
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        if (storeDtos != null && storeDtos.size() > 0) {
            throw new IllegalArgumentException(reqJson.getString("link") + "，手机号已经被占用 请更换或者联系管理员");
        }

        // 创建商户信息
        StorePo storePo = BeanConvertUtil.covertBean(reqJson, StorePo.class);
        storePo.setStoreId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        storePo.setStoreTypeCd(StoreDto.STORE_TYPE_MALL); // 设置商户类型为商城
        storePo.setState(StoreDto.STATE_NORMAL); // 设置商户状态为正常
        storePo.setName(reqJson.getString("shopName"));
        storePo.setUserId("-1"); // 默认用户ID
        storePo.setAddress("无"); // 默认地址
        storePo.setTel(reqJson.getString("link"));
        storePo.setNearByLandmarks("无"); // 默认地标信息
        // 设置默认坐标
        if (!reqJson.containsKey("mapY")) {
            storePo.setMapY("1");
        }
        if (!reqJson.containsKey("mapX")) {
            storePo.setMapX("1");
        }
        int flag = storeV1InnerServiceSMOImpl.saveStore(storePo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建用户信息
        UserPo userPo = new UserPo();
        userPo.setTel(reqJson.getString("link"));
        userPo.setName(reqJson.getString("shopName"));
        userPo.setAddress("无");
        userPo.setUserId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_userId));
        userPo.setScore("0"); // 初始积分
        userPo.setAge("1"); // 默认年龄
        userPo.setEmail("无");
        userPo.setLevelCd(UserLevelConstant.USER_LEVEL_ADMIN); // 设置用户级别为管理员
        userPo.setSex("1"); // 默认性别
        userPo.setPassword(AuthenticationFactory.passwdMd5(reqJson.getString("password"))); // 密码加密
        userPo.setbId("-1"); // 默认业务ID
        flag = userV1InnerServiceSMOImpl.saveUser(userPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 保存商户和用户的关系
        StoreUserPo storeUserPo = new StoreUserPo();
        storeUserPo.setStoreUserId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        storeUserPo.setRelCd(StoreUserRelConstant.REL_ADMIN); // 设置关系类型为管理员
        storeUserPo.setStoreId(storePo.getStoreId());
        storeUserPo.setUserId(userPo.getUserId());
        flag = storeUserV1InnerServiceSMOImpl.saveStoreUser(storeUserPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 创建公司级组织
        OrgPo orgPo = new OrgPo();
        orgPo.setOrgName(storePo.getName());
        orgPo.setOrgLevel("1"); // 一级组织
        orgPo.setOrgId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_orgId));
        orgPo.setAllowOperation("F"); // 不允许操作
        orgPo.setBelongCommunityId("9999"); // 默认小区ID
        orgPo.setParentOrgId(orgPo.getOrgId()); // 父组织ID指向自己
        orgPo.setStoreId(storePo.getStoreId());

        flag = orgV1InnerServiceSMOImpl.saveOrg(orgPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 创建分公司组织
        OrgPo orgHeadPo = new OrgPo();
        orgHeadPo.setOrgName("公司总部");
        orgHeadPo.setOrgLevel("2"); // 二级组织
        orgHeadPo.setOrgId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_orgId));
        orgHeadPo.setAllowOperation("F");
        orgHeadPo.setBelongCommunityId("9999");
        orgHeadPo.setParentOrgId(orgPo.getOrgId()); // 父组织为一级组织
        orgHeadPo.setStoreId(storePo.getStoreId());
        flag = orgV1InnerServiceSMOImpl.saveOrg(orgHeadPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 创建部门组织
        OrgPo orgHeadPartPo = new OrgPo();
        orgHeadPartPo.setOrgName("总部办公室");
        orgHeadPartPo.setOrgLevel("3"); // 三级组织
        orgHeadPartPo.setOrgId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_orgId));
        orgHeadPartPo.setAllowOperation("F");
        orgHeadPartPo.setBelongCommunityId("9999");
        orgHeadPartPo.setParentOrgId(orgHeadPo.getOrgId()); // 父组织为二级组织
        orgHeadPartPo.setStoreId(storePo.getStoreId());
        flag = orgV1InnerServiceSMOImpl.saveOrg(orgHeadPartPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 添加组织员工关系
        OrgStaffRelPo orgStaffRelPo = new OrgStaffRelPo();
        orgStaffRelPo.setOrgId(orgHeadPartPo.getOrgId());
        orgStaffRelPo.setStaffId(userPo.getUserId());
        orgStaffRelPo.setRelId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        orgStaffRelPo.setRelCd(StoreUserRelConstant.REL_ADMIN); // 关系类型为管理员
        orgStaffRelPo.setStoreId(storePo.getStoreId());
        flag = orgStaffRelV1InnerServiceSMOImpl.saveOrgStaffRel(orgStaffRelPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 分配默认权限
        String defaultPrivilege = MappingCache.getValue(MappingConstant.DOMAIN_DEFAULT_PRIVILEGE_ADMIN, StoreDto.STORE_TYPE_MALL);

        Assert.hasLength(defaultPrivilege, "未配置商城默认权限");
        PrivilegeUserPo privilegeUserPo = new PrivilegeUserPo();
        privilegeUserPo.setPrivilegeFlag("1"); // 权限标识
        privilegeUserPo.setStoreId(storePo.getStoreId());
        privilegeUserPo.setUserId(userPo.getUserId());
        privilegeUserPo.setpId(defaultPrivilege); // 权限ID
        privilegeUserPo.setPuId(GenerateCodeFactory.getGeneratorId("10"));

        flag = privilegeUserV1InnerServiceSMOImpl.savePrivilegeUser(privilegeUserPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 保存商铺信息
        saveShop(storePo, reqJson);

        // 关联小区和商铺
        saveShopCommunity(reqJson);

        // 创建账户信息
        addAccountDto(storePo, reqJson);
    }

    /**
     * 保存商铺小区关联关系
     * 
     * <p>建立商铺和小区之间的关联关系，支持两种不同的服务实现</p>
     * 
     * @param reqJson 请求参数JSON对象，包含小区信息和商铺ID
     * @throws IllegalArgumentException 当关联关系保存失败时抛出异常
     */
    private void saveShopCommunity(JSONObject reqJson) {

        StoreShopCommunityPo storeShopCommunityPo = new StoreShopCommunityPo();
        storeShopCommunityPo.setAddress("无");
        storeShopCommunityPo.setCityCode(reqJson.getString("areaCode")); // 城市编码
        storeShopCommunityPo.setCodeName("无");
        storeShopCommunityPo.setCommunityId(reqJson.getString("communityId")); // 小区ID
        storeShopCommunityPo.setCommunityName(reqJson.getString("communityName")); // 小区名称
        storeShopCommunityPo.setEndTime("2050-01-01"); // 结束时间
        storeShopCommunityPo.setMessage("物业添加"); // 备注信息
        storeShopCommunityPo.setScId(GenerateCodeFactory.getGeneratorId("10")); // 关联ID
        storeShopCommunityPo.setShopId(reqJson.getString("shopId")); // 商铺ID
        storeShopCommunityPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 开始时间为当前时间
        storeShopCommunityPo.setState(StoreShopCommunityDto.STATE_SUCCESS); // 状态为成功
        
        int flag = 0;
        // 根据配置选择不同的服务实现
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            flag = shopCommunityInnerServiceSMOImpl.saveShopCommunity(storeShopCommunityPo);
        } else {
            flag = storeShopCommunityV1InnerServiceSMOImpl.saveStoreShopCommunity(storeShopCommunityPo);
        }
        if (flag < 1) {
            throw new IllegalArgumentException("小区关联商铺失败");
        }
    }

    /**
     * 创建账户信息
     * 
     * <p>为商户创建现金账户，用于资金管理</p>
     * 
     * @param storePo 商户信息对象
     * @param reqJson 请求参数JSON对象，包含联系方式和商铺ID
     */
    private void addAccountDto(StorePo storePo, JSONObject reqJson) {

        AccountPo accountPo = new AccountPo();
        accountPo.setAmount("0"); // 初始金额为0
        accountPo.setAcctId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_acctId)); // 账户ID
        accountPo.setObjId(storePo.getStoreId()); // 对象ID为商户ID
        accountPo.setObjType(AccountDto.OBJ_TYPE_STORE); // 对象类型为商户
        accountPo.setAcctType(AccountDto.ACCT_TYPE_CASH); // 账户类型为现金账户
        accountPo.setAcctName(storePo.getName()); // 账户名称
        accountPo.setPartId(reqJson.getString("shopId")); // 参与方ID为商铺ID
        accountPo.setLink(reqJson.getString("link")); // 联系方式
        accountInnerServiceSMOImpl.saveAccount(accountPo);
    }

    /**
     * 保存商铺信息
     * 
     * <p>创建商铺基本信息，支持两种不同的服务实现</p>
     * 
     * @param storePo 商户信息对象
     * @param reqJson 请求参数JSON对象，包含商铺相关信息
     * @throws CmdException 当商铺信息保存失败时抛出异常
     */
    private void saveShop(StorePo storePo, JSONObject reqJson) {

        StoreShopPo shopPo = new StoreShopPo();
        shopPo.setShopName(reqJson.getString("shopName")); // 商铺名称
        shopPo.setShopDesc("无"); // 商铺描述
        shopPo.setReturnPerson("无"); // 退货联系人
        shopPo.setReturnLink(reqJson.getString("link")); // 退货联系方式
        shopPo.setStoreId(storePo.getStoreId()); // 商户ID
        shopPo.setSendAddress("无"); // 发货地址
        shopPo.setReturnAddress("无"); // 退货地址
        shopPo.setShopType("1012021070202890002"); // 商铺类型
        shopPo.setOpenType(ShopDto.OPEN_TYPE_SHOP); // 开放类型
        shopPo.setAreaCode(reqJson.getString("areaCode")); // 区域编码
        shopPo.setShopId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_shopId)); // 商铺ID
        shopPo.setShopLogo(""); // 商铺Logo
        shopPo.setMapX(storePo.getMapX()); // 经度坐标
        shopPo.setMapY(storePo.getMapY()); // 纬度坐标

        shopPo.setState(ShopDto.STATE_Y); // 商铺状态为启用
        int flag = 0;
        // 根据配置选择不同的服务实现
        if ("ON".equals(MappingCache.getValue("HAS_HC_MALL"))) {
            flag = shopInnerServiceSMOImpl.saveShop(shopPo);
        } else {
            flag = storeShopV1InnerServiceSMOImpl.saveStoreShop(shopPo);
        }
        if (flag < 1) {
            throw new CmdException("保存商铺失败");
        }
        // 将商铺ID保存到请求参数中，供后续使用
        reqJson.put("shopId", shopPo.getShopId());
    }
}