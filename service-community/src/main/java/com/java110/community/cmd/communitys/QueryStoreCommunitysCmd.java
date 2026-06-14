/**
 * 查询商户小区命令类
 * 
 * 该命令类用于处理查询商户关联小区的业务逻辑，通过商户ID获取商户信息，
 * 并根据商户类型映射到对应的成员类型，最终查询该商户关联的所有小区信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-18
 */
package com.java110.community.cmd.communitys;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 查询商户小区
 * add by 吴学文 2021-09-18
 */
@Java110Cmd(serviceCode = "/communitys/queryStoreCommunitys")
public class QueryStoreCommunitysCmd extends Cmd {

    /**
     * 商户服务接口，用于查询商户信息
     */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(QueryStoreCommunitysCmd.class);

    /**
     * 小区服务接口，用于查询小区相关信息
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，当前实现为空，可根据业务需求添加验证逻辑
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 参数验证逻辑，可根据业务需求添加具体的验证规则
        // 例如：验证必填字段、数据格式、业务规则等
    }

    /**
     * 命令执行方法
     * 
     * 主要业务逻辑：根据商户ID查询商户信息，然后查询该商户关联的所有小区信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取商户ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        // 如果请求头中没有商户ID，则从请求参数中获取
        if(StringUtil.isEmpty(storeId)){
            storeId = reqJson.getString("memberId");
        }
        
        // 构建商户查询条件
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);    // 设置商户ID
        storeDto.setPage(1);             // 设置查询页码
        storeDto.setRow(1);              // 设置每页记录数
        
        // 调用商户服务查询商户信息
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 验证查询结果，确保只返回一条商户记录
        Assert.listOnlyOne(storeDtos, "商户不存在");
        
        // 将请求参数转换为社区成员DTO对象
        CommunityMemberDto communityMemberDto = BeanConvertUtil.covertBean(reqJson, CommunityMemberDto.class);
        communityMemberDto.setMemberId(storeId);  // 设置成员ID为商户ID
        
        // 根据商户类型映射对应的成员类型编码
        String memberTypeCd = MappingCache.getValue(
            MappingConstant.DOMAIN_STORE_TYPE_2_COMMUNITY_MEMBER_TYPE, 
            storeDtos.get(0).getStoreTypeCd()
        );
        communityMemberDto.setMemberTypeCd(memberTypeCd);  // 设置成员类型编码
        
        // 查询商户关联的小区列表
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.getStoreCommunitys(communityMemberDto);
        
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(communityDtos));
    }
}