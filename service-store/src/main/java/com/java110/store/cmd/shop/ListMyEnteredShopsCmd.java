/**
 * 店铺管理命令类 - 查询用户已入驻的店铺列表
 * 
 * 该命令类用于处理用户查询自己已入驻店铺列表的请求，根据用户角色（管理员/普通用户）
 * 返回不同的店铺数据。管理员可以查看所有店铺，普通用户只能查看自己有权限的店铺。
 * 
 * @serviceCode shop.listMyEnteredShops
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.shop;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.dto.shop.ShopDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.mall.IShopInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IOrgInnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "shop.listMyEnteredShops")
public class ListMyEnteredShopsCmd extends Cmd {

    /** 店铺内部服务接口 */
    @Autowired(required = false)
    private IShopInnerServiceSMO shopInnerServiceSMOImpl;

    /** 组织员工关系内部服务接口 */
    @Autowired
    private IOrgStaffRelInnerServiceSMO orgStaffRelInnerServiceSMOImpl;

    /** 角色社区内部服务接口 */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    /** 组织内部服务接口 */
    @Autowired
    private IOrgInnerServiceSMO orgInnerServiceSMOImpl;

    /** 商户内部服务接口 */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /** 用户内部服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的商户ID和用户ID参数，如果请求报文中没有这些参数，
     * 则从请求头中获取并设置到请求JSON中
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 如果请求报文中没有storeId或storeId为空，从请求头中获取
        if (!reqJson.containsKey("storeId") || StringUtil.isEmpty(reqJson.getString("storeId"))) {
            reqJson.put("storeId", cmdDataFlowContext.getReqHeaders().get("store-id"));
        }

        // 如果请求报文中没有userId或userId为空，从请求头中获取
        if (!reqJson.containsKey("userId") || StringUtil.isEmpty(reqJson.getString("userId"))) {
            reqJson.put("userId", cmdDataFlowContext.getReqHeaders().get("user-id"));
        }

        // 验证必须包含storeId参数
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含商户信息");
        // 验证必须包含userId参数
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含用户信息");
    }

    /**
     * 执行命令的主方法
     * 
     * 根据用户角色查询对应的店铺列表：
     * 1. 管理员用户：查询所有已启用的店铺
     * 2. 普通用户：查询用户有权限的社区/店铺
     * 3. 对于管理员和开发商户类型，如果没有查询到店铺，返回默认小区
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 根据用户ID查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 根据商户ID查询商户信息
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("storeId"));
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 验证商户存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");

        int count = 0; // 查询结果总数
        List<ShopDto> shops = null; // 店铺列表
        ShopDto shopDto = null; // 店铺数据传输对象

        // 根据用户角色类型进行不同的查询逻辑
        if (UserDto.LEVEL_CD_ADMIN.equals(userDtos.get(0).getLevelCd())) {
            // 管理员用户：查询所有已启用的店铺
            shopDto = BeanConvertUtil.covertBean(reqJson, ShopDto.class);
            shopDto.setStoreId(reqJson.getString("storeId"));
            shopDto.setState(ShopDto.STATE_Y); // 设置店铺状态为启用
            
            // 如果请求中包含店铺名称，设置查询条件
            if (reqJson.containsKey("shopName")) {
                shopDto.setShopName(reqJson.getString("shopName"));
            }
            
            // 查询店铺数量
            count = shopInnerServiceSMOImpl.queryShopsCount(shopDto);
            if (count > 0) {
                // 查询店铺列表
                shops = BeanConvertUtil.covertBeanList(shopInnerServiceSMOImpl.queryShops(shopDto), ShopDto.class);
            } else {
                shops = new ArrayList<>();
            }
        } else {
            // 普通用户：查询用户有权限的社区/店铺
            RoleCommunityDto orgCommunityDto = BeanConvertUtil.covertBean(reqJson, RoleCommunityDto.class);
            orgCommunityDto.setStaffId(userDtos.get(0).getUserId());
            
            // 查询用户有权限的社区数量
            count = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitysNameCount(orgCommunityDto);
            if (count > 0) {
                // 查询用户有权限的社区列表
                List<RoleCommunityDto> roleCommunityDtos = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitysName(orgCommunityDto);
                shops = new ArrayList<>();
                
                // 将社区信息转换为店铺信息
                for (RoleCommunityDto tmpOrgCommunityDto : roleCommunityDtos) {
                    shopDto = new ShopDto();
                    shopDto.setShopId(tmpOrgCommunityDto.getCommunityId()); // 社区ID作为店铺ID
                    shopDto.setShopName(tmpOrgCommunityDto.getCommunityName()); // 社区名称作为店铺名称
                    shops.add(shopDto);
                }
            } else {
                shops = new ArrayList<>();
            }
        }

        // 兼容性处理：对于管理员或开发商户类型，如果没有查询到店铺，添加默认小区
        if (shops.size() < 1 && (StoreDto.STORE_TYPE_ADMIN.equals(storeDtos.get(0).getStoreTypeCd()) 
                || StoreDto.STORE_TYPE_DEV.equals(storeDtos.get(0).getStoreTypeCd()))) {
            shopDto = new ShopDto();
            shopDto.setShopId("-1"); // 默认小区ID
            shopDto.setShopName("默认小区"); // 默认小区名称
            shopDto.setStoreTel("18909711234"); // 默认联系电话
            shopDto.setState("1100"); // 默认状态
            shops.add(shopDto);
        }

        // 设置响应结果，包含分页信息和店铺列表
        context.setResponseEntity(ResultVo.createResponseEntity(
                (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 计算总页数
                count, // 总记录数
                shops // 店铺列表数据
        ));
    }
}