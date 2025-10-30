/**
 * 根据小区查询商户列表命令类
 * 
 * 该命令处理通过小区ID查询关联商户信息的业务逻辑，主要用于展示小区内所有商户的列表
 * 支持分页查询，返回商户基本信息及分页数据
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.store.ApiStoreDataVo;
import com.java110.vo.api.store.ApiStoreVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "store.listStoresByCommunity")
public class ListStoresByCommunityCmd extends Cmd {

    /**
     * 小区内部服务接口，用于查询小区成员信息
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 商户内部服务接口，用于查询商户详细信息
     */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否完整有效，包括分页参数和必要的小区ID参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行命令的主方法
     * 
     * 根据小区ID查询关联的商户信息，支持分页查询，返回商户列表和分页信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 命令执行过程中出现错误时抛出
     * @throws ParseException 数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为小区成员DTO对象
        CommunityMemberDto communityMemberDto = BeanConvertUtil.covertBean(reqJson, CommunityMemberDto.class);
        
        // 查询小区成员总数，用于分页
        int storeCount = communityInnerServiceSMOImpl.getCommunityMemberCount(communityMemberDto);
        
        List<CommunityMemberDto> communityMemberDtos = null;
        List<ApiStoreDataVo> stores = null;
        
        // 如果存在商户数据，则查询详细信息
        if (storeCount > 0) {
            // 分页查询小区成员列表
            communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);
            
            // 构建商户查询条件
            StoreDto storeDto = new StoreDto();
            // 设置要查询的商户ID数组
            storeDto.setStoreIds(getStoreIds(communityMemberDtos));
            
            // 根据商户ID查询商户详细信息
            List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);
            
            // 将商户DTO转换为API返回VO对象
            stores = BeanConvertUtil.covertBeanList(storeDtos, ApiStoreDataVo.class);
        } else {
            // 没有数据时返回空列表
            stores = new ArrayList<>();
        }

        // 构建API返回对象
        ApiStoreVo apiStoreVo = new ApiStoreVo();
        
        // 设置总记录数
        apiStoreVo.setTotal(storeCount);
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiStoreVo.setRecords((int) Math.ceil((double) storeCount / (double) reqJson.getInteger("row")));
        // 设置商户数据列表
        apiStoreVo.setStores(stores);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiStoreVo),  // 将返回对象转换为JSON字符串
            HttpStatus.OK  // 设置HTTP状态码为200
        );

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }

    /**
     * 从小区成员列表中提取商户ID数组
     * 
     * 遍历小区成员列表，提取每个成员的商户ID，组装成字符串数组返回
     * 
     * @param communityMemberDtos 小区成员数据对象列表
     * @return 商户ID字符串数组，如果没有数据则返回空数组
     */
    private String[] getStoreIds(List<CommunityMemberDto> communityMemberDtos) {
        List<String> storeIds = new ArrayList<>();
        
        // 遍历小区成员列表，提取商户ID
        for (CommunityMemberDto communityMemberDto : communityMemberDtos) {
            storeIds.add(communityMemberDto.getMemberId());
        }

        // 将List转换为String数组返回
        return storeIds.toArray(new String[storeIds.size()]);
    }
}