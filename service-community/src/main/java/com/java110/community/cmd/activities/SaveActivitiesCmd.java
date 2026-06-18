/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.community.cmd.activities;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.activities.ActivitiesTypeDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IActivitiesTypeInnerServiceSMO;
import com.java110.intf.community.IActivitiesV1InnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.activities.ActivitiesPo;
import com.java110.po.file.FileRelPo;
import com.java110.utils.constant.StateConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.community.ApiCommunityDataVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 活动保存命令类
 * 
 * 该类负责处理活动信息的保存操作，包括单社区和多社区的活动创建。
 * 主要功能包括：参数验证、用户信息获取、社区权限校验、活动类型验证、活动数据保存等。
 * 
 * @author 吴学文 at 2022-06-19 10:49:17 mail: 928255095@qq.com
 * @version 1.0
 * @serviceCode activities.saveActivities
 * @requestPath /app/activities.SaveActivities
 * @openSourceAddress https://gitee.com/wuxw7/MicroCommunity
 * @officialWebsite http://www.homecommunity.cn
 */
@Java110Cmd(serviceCode = "activities.saveActivities")
public class SaveActivitiesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveActivitiesCmd.class);

    /** 代码前缀ID常量 */
    public static final String CODE_PREFIX_ID = "10";

    /** 活动服务接口 */
    @Autowired
    private IActivitiesV1InnerServiceSMO activitiesV1InnerServiceSMOImpl;

    /** 活动类型服务接口 */
    @Autowired
    private IActivitiesTypeInnerServiceSMO activitiesTypeInnerServiceSMOImpl;

    /** 社区服务接口 */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /** 文件关联服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /** 角色社区服务接口 */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    /** 商户服务接口 */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /** 用户V1服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "title", "必填，请填写业活动标题");
        Assert.hasKeyAndValue(reqJson, "typeCd", "必填，请选择活动类型");
        Assert.hasKeyAndValue(reqJson, "headerImg", "必填，请选择头部照片");
        Assert.hasKeyAndValue(reqJson, "context", "必填，请填写活动内容");
        Assert.hasKeyAndValue(reqJson, "startTime", "必填，请选择开始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "必填，请选择结束时间");
    }

    /**
     * 执行保存活动命令
     * 
     * 处理活动保存的核心业务逻辑，包括用户信息获取、社区权限判断、活动类型验证和活动数据保存
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID和商户ID
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");
        reqJson.put("userId", userDtos.get(0).getUserId());
        reqJson.put("userName", userDtos.get(0).getName());

        // 判断是否为多社区发布，如果不是则直接保存单个活动
        if (!reqJson.containsKey("isMoreCommunity") || "N".equals(reqJson.getString("isMoreCommunity"))) {
            addActivities(cmdDataFlowContext, reqJson);
            return;
        }

        // 多社区发布逻辑
        reqJson.put("storeId", storeId);
        List<ApiCommunityDataVo> communityDataVos = getCommunitys(reqJson);

        // 如果没有可操作的社区则直接返回
        if (communityDataVos == null || communityDataVos.size() < 1) {
            return;
        }

        // 验证原始活动类型是否存在
        ActivitiesTypeDto activitiesTypeDto = new ActivitiesTypeDto();
        activitiesTypeDto.setCommunityId(reqJson.getString("communityId"));
        activitiesTypeDto.setTypeCd(reqJson.getString("typeCd"));
        List<ActivitiesTypeDto> oneActivitiesTypeDtos = activitiesTypeInnerServiceSMOImpl.queryActivitiesTypes(activitiesTypeDto);

        Assert.listOnlyOne(oneActivitiesTypeDtos, "通知类型不存在");
        List<ActivitiesTypeDto> activitiesTypeDtos = null;
        
        // 遍历所有社区，为每个社区创建活动
        for (ApiCommunityDataVo apiCommunityDataVo : communityDataVos) {
            reqJson.put("communityId", apiCommunityDataVo.getCommunityId());
            activitiesTypeDto = new ActivitiesTypeDto();
            activitiesTypeDto.setCommunityId(reqJson.getString("communityId"));
            activitiesTypeDto.setTypeName(oneActivitiesTypeDtos.get(0).getTypeName());
            
            // 查询当前社区对应的活动类型
            activitiesTypeDtos = activitiesTypeInnerServiceSMOImpl.queryActivitiesTypes(activitiesTypeDto);
            if (activitiesTypeDtos == null || activitiesTypeDtos.size() < 1) {
                continue; // 如果当前社区没有对应的活动类型，跳过
            }
            
            reqJson.put("typeCd", activitiesTypeDtos.get(0).getTypeCd());
            addActivities(cmdDataFlowContext, reqJson);
        }
    }

    /**
     * 获取用户有权限的社区列表
     * 
     * 根据用户权限级别（管理员或普通用户）获取对应的社区列表
     * 
     * @param reqJson 请求参数JSON对象，包含用户ID、商户ID等信息
     * @return 社区数据视图对象列表
     */
    public List<ApiCommunityDataVo> getCommunitys(JSONObject reqJson) {
        // 1.0 先查询员工对应的部门
        List<ApiCommunityDataVo> communitys = null;
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");

        // 校验商户是否存在
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("storeId"));
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        Assert.listOnlyOne(storeDtos, "商户不存在");

        int count = 0;
        
        // 根据用户权限级别处理不同的社区获取逻辑
        if (UserDto.LEVEL_CD_ADMIN.equals(userDtos.get(0).getLevelCd())) {
            // 管理员权限：可以获取所有社区
            CommunityDto communityDto = BeanConvertUtil.covertBean(reqJson, CommunityDto.class);
            communityDto.setMemberId(reqJson.getString("storeId"));
            communityDto.setAuditStatusCd(StateConstant.AGREE_AUDIT);
            if (reqJson.containsKey("communityName")) {
                communityDto.setName(reqJson.getString("communityName"));
            }
            count = communityInnerServiceSMOImpl.queryCommunitysCount(communityDto);
            if (count > 0) {
                communitys = BeanConvertUtil.covertBeanList(communityInnerServiceSMOImpl.queryCommunitys(communityDto), ApiCommunityDataVo.class);
            } else {
                communitys = new ArrayList<>();
            }
        } else {
            // 普通用户权限：只能获取有权限的社区
            RoleCommunityDto orgCommunityDto = BeanConvertUtil.covertBean(reqJson, RoleCommunityDto.class);
            orgCommunityDto.setStaffId(userDtos.get(0).getStaffId());
            count = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitysCount(orgCommunityDto);
            if (count > 0) {
                List<RoleCommunityDto> roleCommunityDtos = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitys(orgCommunityDto);
                communitys = BeanConvertUtil.covertBeanList(roleCommunityDtos, ApiCommunityDataVo.class);
                
                // 设置社区名称
                for (RoleCommunityDto tmpOrgCommunityDto : roleCommunityDtos) {
                    for (ApiCommunityDataVo tmpApiCommunityDataVo : communitys) {
                        if (tmpOrgCommunityDto.getCommunityId().equals(tmpApiCommunityDataVo.getCommunityId())) {
                            tmpApiCommunityDataVo.setName(tmpOrgCommunityDto.getCommunityName());
                        }
                    }
                }
            } else {
                communitys = new ArrayList<>();
            }
        }
        return communitys;
    }

    /**
     * 添加活动信息
     * 
     * 保存活动数据到数据库，包括活动基本信息和关联的头像文件
     * 
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    public void addActivities(ICmdDataFlowContext context, JSONObject reqJson) {
        // 生成活动ID
        reqJson.put("activitiesId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_activitiesId));
        
        // 处理活动头像文件关联
        if (reqJson.containsKey("headerImg") && !StringUtils.isEmpty(reqJson.getString("headerImg"))) {
            FileRelPo fileRelPo = new FileRelPo();
            fileRelPo.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fileRelId));
            fileRelPo.setFileRealName(reqJson.getString("headerImg"));
            fileRelPo.setFileSaveName(reqJson.getString("headerImg"));
            fileRelPo.setObjId(reqJson.getString("activitiesId"));
            fileRelPo.setSaveWay("table");
            fileRelPo.setRelTypeCd("70000"); // 文件关联类型码
            int flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            if (flag < 1) {
                throw new CmdException("保存广告失败");
            }
        }
        
        // 转换并保存活动信息
        ActivitiesPo activitiesPo = BeanConvertUtil.covertBean(reqJson, ActivitiesPo.class);
        // 设置活动初始统计信息
        activitiesPo.setReadCount("0");
        activitiesPo.setLikeCount("0");
        activitiesPo.setCollectCount("0");
        activitiesPo.setState("11000"); // 活动状态码
        int flag = activitiesV1InnerServiceSMOImpl.saveActivities(activitiesPo);
        if (flag < 1) {
            throw new CmdException("保存广告失败");
        }
    }
}