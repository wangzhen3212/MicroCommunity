package com.java110.community.cmd.community;

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
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.community.ApiCommunityDataVo;
import com.java110.vo.api.community.ApiCommunityVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询入驻审核小区命令类
 * 负责处理查询待审核入驻小区信息的业务逻辑，支持通过商户信息或小区信息两种方式查询
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-18
 */
@Java110Cmd(serviceCode = "community.listAuditEnterCommunitys")
public class ListAuditEnterCommunitysCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(ListAuditEnterCommunitysCmd.class);

    /**
     * 小区内部服务接口
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 商户内部服务接口
     */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 当前实现为空，可根据业务需求添加参数校验逻辑
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 参数验证逻辑，可根据需要添加具体的校验规则
    }

    /**
     * 命令执行方法
     * 根据请求参数决定查询方式：如果包含商户名称或电话则按商户查询，否则按小区查询
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 根据请求参数决定查询方式
        if (StringUtil.jsonHasKayAndValue(reqJson, "name") || StringUtil.jsonHasKayAndValue(reqJson, "tel")) {
            // 如果请求中包含商户名称或电话，则按商户信息查询
            getInfoByStore(cmdDataFlowContext, reqJson);
        } else {
            // 否则按小区信息查询
            getInfoByCommunity(cmdDataFlowContext, reqJson);
        }
    }

    /**
     * 通过商户信息查询入驻审核小区
     * 根据商户名称和电话查询商户信息，再根据商户ID查询相关的入驻审核小区
     *
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    private void getInfoByStore(ICmdDataFlowContext context, JSONObject reqJson) {
        // 构建商户查询条件
        StoreDto storeDto = new StoreDto();
        storeDto.setName(reqJson.getString("name"));
        storeDto.setTel(reqJson.getString("tel"));
        
        // 查询商户信息
        List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);

        List<ApiCommunityDataVo> communitys = null;
        int count = 0;
        
        // 如果查询到商户信息
        if (storeDtos.size() > 0) {
            // 构建小区成员查询条件
            CommunityMemberDto communityMemberDto = BeanConvertUtil.covertBean(reqJson, CommunityMemberDto.class);
            communityMemberDto.setNeedCommunityInfo(true); // 需要小区详细信息
            communityMemberDto.setNoAuditEnterCommunity(true); // 查询待审核的入驻小区
            communityMemberDto.setMemberId(reqJson.getString("memberId")); // 设置成员ID
            communityMemberDto.setSubMemberId(storeDtos.get(0).getStoreId()); // 设置子成员ID为商户ID

            // 查询小区成员数量
            count = communityInnerServiceSMOImpl.getCommunityMemberCount(communityMemberDto);

            // 根据查询结果构建返回数据
            if (count > 0) {
                // 查询小区成员详情并转换为VO对象
                communitys = BeanConvertUtil.covertBeanList(communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto), ApiCommunityDataVo.class);
            } else {
                communitys = new ArrayList<>();
            }
        } else {
            // 未查询到商户信息，返回空列表
            communitys = new ArrayList<>();
        }

        // 如果查询到小区信息，刷新商户信息
        if (communitys.size() > 0) {
            freshCommunityInfo(communitys);
        }

        // 构建返回VO对象
        ApiCommunityVo apiCommunityVo = new ApiCommunityVo();
        apiCommunityVo.setTotal(count); // 设置总记录数
        // 计算总页数：总记录数/每页记录数，向上取整
        apiCommunityVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiCommunityVo.setCommunitys(communitys); // 设置小区数据列表

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiCommunityVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新小区商户信息
     * 为小区数据列表中的每个小区补充商户详细信息
     *
     * @param communityDataVos 小区数据VO列表
     */
    private void freshCommunityInfo(List<ApiCommunityDataVo> communityDataVos) {
        // 遍历所有小区数据
        for (ApiCommunityDataVo apiCommunityDataVo : communityDataVos) {
            // 根据商户ID查询商户信息
            StoreDto storeDto = new StoreDto();
            storeDto.setStoreId(apiCommunityDataVo.getMemberId());
            List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);

            // 如果查询到的商户信息不唯一，跳过当前小区
            if (storeDtos.size() != 1) {
                continue;
            }
            
            // 将商户信息设置到小区VO对象中
            apiCommunityDataVo.setStoreName(storeDtos.get(0).getStoreName());
            apiCommunityDataVo.setStoreTypeCd(storeDtos.get(0).getStoreTypeCd());
            apiCommunityDataVo.setStoreTypeName(storeDtos.get(0).getStoreTypeName());
            apiCommunityDataVo.setTel(storeDtos.get(0).getTel());
            apiCommunityDataVo.setAddress(storeDtos.get(0).getAddress());
        }
    }

    /**
     * 通过小区信息查询入驻审核小区
     * 直接根据小区查询条件查询待审核的入驻小区信息
     *
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    private void getInfoByCommunity(ICmdDataFlowContext context, JSONObject reqJson) {
        // 构建小区成员查询条件
        CommunityMemberDto communityMemberDto = BeanConvertUtil.covertBean(reqJson, CommunityMemberDto.class);
        communityMemberDto.setNeedCommunityInfo(true); // 需要小区详细信息
        communityMemberDto.setNoAuditEnterCommunity(true); // 查询待审核的入驻小区

        // 查询小区成员数量
        int count = communityInnerServiceSMOImpl.getCommunityMemberCount(communityMemberDto);

        List<ApiCommunityDataVo> communitys = null;

        // 根据查询结果构建返回数据
        if (count > 0) {
            // 查询小区成员详情并转换为VO对象
            communitys = BeanConvertUtil.covertBeanList(communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto), ApiCommunityDataVo.class);
        } else {
            communitys = new ArrayList<>();
        }

        // 如果查询到小区信息，刷新商户信息
        if (communitys.size() > 0) {
            freshCommunityInfo(communitys);
        }

        // 构建返回VO对象
        ApiCommunityVo apiCommunityVo = new ApiCommunityVo();
        apiCommunityVo.setTotal(count); // 设置总记录数
        // 计算总页数：总记录数/每页记录数，向上取整
        apiCommunityVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiCommunityVo.setCommunitys(communitys); // 设置小区数据列表

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiCommunityVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}