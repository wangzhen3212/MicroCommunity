/**
 * 组织未关联店铺查询命令类
 * 
 * 功能描述：该命令类用于查询指定角色下未关联的店铺列表。
 * 通过角色社区关联信息过滤出已关联的社区，然后查询不在这些社区中的店铺信息。
 * 主要用于组织管理中的权限分配和店铺关联管理。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.org;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.dto.shop.ShopDto;
import com.java110.intf.mall.IShopInnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "org.listOrgNoShops")
public class ListOrgNoShopsCmd extends Cmd {

    /**
     * 角色社区关联服务接口
     * 用于查询角色与社区的关联关系
     */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMO;

    /**
     * 店铺信息服务接口
     * required = false 表示该依赖不是必须的
     */
    @Autowired(required = false)
    private IShopInnerServiceSMO shopInnerServiceSMO;

    /**
     * 请求参数验证方法
     * 
     * 功能描述：验证请求参数的有效性和完整性，确保必要的参数都存在且有效
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头等信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
        
        // 如果请求参数中没有storeId或者storeId为空，则从请求头中获取
        if(!reqJson.containsKey("storeId") || StringUtil.isEmpty(reqJson.getString("storeId"))) {
            String storeId = context.getReqHeaders().get("store-id");
            reqJson.put("storeId",storeId);
        }
        
        // 验证storeId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户ID");
        
        // 验证roleId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "roleId", "必填，请填写角色");
    }

    /**
     * 命令执行方法
     * 
     * 功能描述：执行主要的业务逻辑，查询指定角色下未关联的店铺列表
     * 1. 查询角色已关联的社区列表
     * 2. 根据已关联社区过滤出未关联的店铺
     * 3. 返回分页查询结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为RoleCommunityDto对象
        RoleCommunityDto roleCommunityDto = BeanConvertUtil.covertBean(reqJson, RoleCommunityDto.class);

        // 查询角色已关联的社区列表
        List<RoleCommunityDto> orgCommunityDtos = roleCommunityV1InnerServiceSMO.queryRoleCommunitys(roleCommunityDto);
        
        // 提取所有已关联社区的ID
        List<String> communityIds = new ArrayList<>();
        for(RoleCommunityDto tmpOrgCommunityDto : orgCommunityDtos){
            communityIds.add(tmpOrgCommunityDto.getCommunityId());
        }
        
        // 将请求参数转换为ShopDto对象
        ShopDto shopDto = BeanConvertUtil.covertBean(reqJson, ShopDto.class);
        
        // 如果存在已关联的社区，则设置排除这些社区的店铺
        if(communityIds.size()>0) {
            shopDto.setNotInShopId(communityIds.toArray(new String[communityIds.size()]));
        }
        
        // 设置店铺状态为有效状态
        shopDto.setState(ShopDto.STATE_Y);
        // 设置商户ID
        shopDto.setStoreId(reqJson.getString("storeId"));
        
        // 查询符合条件的店铺总数
        int count = shopInnerServiceSMO.queryShopsCount(shopDto);

        List<ShopDto> communitys = null;

        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            // 查询店铺详细列表并转换为ShopDto对象列表
            communitys = BeanConvertUtil.covertBeanList(shopInnerServiceSMO.queryShops(shopDto), ShopDto.class);
        } else {
            // 如果没有数据，返回空列表
            communitys = new ArrayList<>();
        }

        // 设置响应结果，包含分页信息和数据列表
        context.setResponseEntity(ResultVo.createResponseEntity(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 计算总页数
            count, // 总记录数
            communitys // 数据列表
        ));
    }
}