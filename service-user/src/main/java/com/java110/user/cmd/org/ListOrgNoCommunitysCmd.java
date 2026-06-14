/**
 * 组织未关联小区查询命令类
 * 
 * 功能：查询指定角色未关联的小区列表
 * 用途：用于组织机构管理中，为角色分配小区权限时，展示该角色尚未关联的小区信息
 * 服务码：org.listOrgNoCommunitys
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
import com.java110.dto.PageDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.community.ApiCommunityDataVo;
import com.java110.vo.api.community.ApiCommunityVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "org.listOrgNoCommunitys")
public class ListOrgNoCommunitysCmd extends Cmd {

    /**
     * 角色小区关联服务接口
     */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMO;

    /**
     * 小区信息服务接口
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 功能：验证请求参数的有效性和完整性
     * 1. 验证分页信息
     * 2. 验证商户ID是否存在，如果请求参数中不存在则从请求头中获取
     * 3. 验证角色ID是否必填
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头等信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        
        // 如果请求参数中没有storeId或storeId为空，则从请求头中获取
        if(!reqJson.containsKey("storeId") || StringUtil.isEmpty(reqJson.getString("storeId"))) {
            String storeId = context.getReqHeaders().get("store-id");
            reqJson.put("storeId",storeId);
        }
        
        // 验证商户ID是否必填
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户ID");
        
        // 验证角色ID是否必填
        Assert.hasKeyAndValue(reqJson, "roleId", "必填，请填写角色");
    }

    /**
     * 命令执行方法
     * 
     * 功能：查询指定角色未关联的小区列表
     * 主要逻辑：
     * 1. 查询角色已关联的小区列表
     * 2. 排除已关联的小区，查询剩余小区
     * 3. 构建返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为角色小区DTO对象
        RoleCommunityDto roleCommunityDto = BeanConvertUtil.covertBean(reqJson, RoleCommunityDto.class);
        // 设置分页为默认第一页
        roleCommunityDto.setPage(PageDto.DEFAULT_PAGE);
        
        // 查询角色已关联的小区列表
        List<RoleCommunityDto> orgCommunityDtos = roleCommunityV1InnerServiceSMO.queryRoleCommunitys(roleCommunityDto);
        
        // 收集已关联的小区ID
        List<String> communityIds = new ArrayList<>();
        for(RoleCommunityDto tmpOrgCommunityDto : orgCommunityDtos){
            communityIds.add(tmpOrgCommunityDto.getCommunityId());
        }
        
        // 将请求参数转换为小区查询DTO
        CommunityDto communityDto = BeanConvertUtil.covertBean(reqJson, CommunityDto.class);
        
        // 如果存在已关联的小区，设置排除条件
        if(communityIds.size()>0) {
            communityDto.setNotInCommunityId(communityIds.toArray(new String[communityIds.size()]));
        }
        
        // 设置查询条件：审核状态为已通过(1100)，成员ID为商户ID
        communityDto.setAuditStatusCd("1100");
        communityDto.setMemberId(reqJson.getString("storeId"));
        
        // 查询未关联小区的总数
        int count = communityInnerServiceSMOImpl.queryCommunitysCount(communityDto);

        List<ApiCommunityDataVo> communitys = null;

        // 如果有数据，查询小区详情列表
        if (count > 0) {
            communitys = BeanConvertUtil.covertBeanList(communityInnerServiceSMOImpl.queryCommunitys(communityDto), ApiCommunityDataVo.class);
        } else {
            communitys = new ArrayList<>();
        }

        // 构建返回VO对象
        ApiCommunityVo apiCommunityVo = new ApiCommunityVo();

        // 设置总记录数
        apiCommunityVo.setTotal(count);
        // 计算总页数
        apiCommunityVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置小区数据列表
        apiCommunityVo.setCommunitys(communitys);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiCommunityVo), HttpStatus.OK);

        // 设置响应结果
        context.setResponseEntity(responseEntity);
    }
}