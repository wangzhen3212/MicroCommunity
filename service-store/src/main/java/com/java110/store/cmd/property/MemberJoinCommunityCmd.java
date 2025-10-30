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
package com.java110.store.cmd.property;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.menu.MenuGroupDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.oaWorkflow.WorkflowDto;
import com.java110.intf.common.IWorkflowV1InnerServiceSMO;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.intf.store.IStoreAttrV1InnerServiceSMO;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.community.CommunityMemberPo;
import com.java110.po.menu.MenuGroupCommunityPo;
import com.java110.po.oaWorkflow.WorkflowPo;
import com.java110.utils.constant.StateConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 类表述：保存
 * 服务编码：store.saveStore
 * 请求路劲：/app/store.SaveStore
 * add by 吴学文 at 2022-02-28 10:46:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 商户入驻小区命令处理类
 * 负责处理商户加入小区的业务逻辑，包括商户信息验证、小区入驻、功能权限分配等工作流配置
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-02-28
 */
@Java110Cmd(serviceCode = "member.join.community")
public class MemberJoinCommunityCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(MemberJoinCommunityCmd.class);

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

    /** 工作流服务接口 */
    @Autowired
    private IWorkflowV1InnerServiceSMO workflowV1InnerServiceSMOImpl;

    /** 权限用户服务接口 */
    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    /** 小区成员关系服务接口 */
    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    /** 小区信息服务接口 */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /** 菜单组服务接口 */
    @Autowired
    private IMenuGroupV1InnerServiceSMO menuGroupV1InnerServiceSMOImpl;

    /** 菜单组小区关系服务接口 */
    @Autowired
    private IMenuGroupCommunityV1InnerServiceSMO menuGroupCommunityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整有效
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证商户ID参数
        Assert.hasKeyAndValue(reqJson, "memberId", "请求报文中未包含商户");

        // 验证小区ID列表参数
        if (!reqJson.containsKey("communityIds") || reqJson.getJSONArray("communityIds").size() < 1) {
            throw new CmdException("未包含小区");
        }

        // 验证功能组ID列表参数
        if (!reqJson.containsKey("groupIds") || reqJson.getJSONArray("groupIds").size() < 1) {
            throw new CmdException("未包含小区开通功能");
        }
    }

    /**
     * 执行商户入驻小区命令
     * 处理商户入驻小区的核心业务逻辑，包括商户验证、小区入驻、功能权限分配等工作流配置
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 根据商户ID查询商户信息
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("memberId"));
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 验证商户存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");

        // 获取小区ID列表
        JSONArray communityIds = reqJson.getJSONArray("communityIds");

        String communityId;
        int flag = 0;
        // 遍历处理每个小区的入驻
        for (int communityIndex = 0; communityIndex < communityIds.size(); communityIndex++) {
            communityId = communityIds.getString(communityIndex);
            
            // 检查小区是否已被入驻
            CommunityMemberDto communityMemberDto = new CommunityMemberDto();
            communityMemberDto.setCommunityId(communityId);
            communityMemberDto.setMemberTypeCd(CommunityMemberDto.MEMBER_TYPE_PROPERTY);
            List<CommunityMemberDto> communityMemberDtos = communityMemberV1InnerServiceSMOImpl.queryCommunityMembers(communityMemberDto);

            if (communityMemberDtos != null && communityMemberDtos.size() > 0) {
                throw new CmdException("小区已经入驻" + communityId);
            }

            // 验证小区是否存在
            CommunityDto communityDto = new CommunityDto();
            communityDto.setCommunityId(communityId);
            List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);

            Assert.listOnlyOne(communityDtos, "小区不存在");

            // 计算入驻结束时间（根据小区付费月数）
            Calendar endTime = Calendar.getInstance();
            endTime.add(Calendar.MONTH, Integer.parseInt(communityDtos.get(0).getPayFeeMonth()));
            
            // 创建小区成员关系记录
            CommunityMemberPo communityMemberPo = new CommunityMemberPo();
            communityMemberPo.setCommunityMemberId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            communityMemberPo.setEndTime(DateUtil.getFormatTimeString(endTime.getTime(), DateUtil.DATE_FORMATE_STRING_A));
            communityMemberPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            communityMemberPo.setAuditStatusCd(StateConstant.AGREE_AUDIT);
            communityMemberPo.setCommunityId(communityId);
            communityMemberPo.setMemberId(storeDtos.get(0).getStoreId());
            communityMemberPo.setMemberTypeCd(CommunityMemberDto.MEMBER_TYPE_PROPERTY);
            
            // 保存小区成员关系
            flag = communityMemberV1InnerServiceSMOImpl.saveCommunityMember(communityMemberPo);
            if (flag < 1) {
                throw new CmdException("注册失败");
            }

            // 保存小区开放功能权限
            saveMenuGroupCommunity(reqJson, communityId, communityDtos.get(0).getName());

            // 更新投诉建议工作流配置
            flag = updateWorkflow(WorkflowDto.FLOW_TYPE_COMPLAINT, communityId, storeDtos.get(0).getStoreId());
            if (flag < 1) {
                continue;
            }

            // 更新物品领用工作流配置
            flag = updateWorkflow(WorkflowDto.FLOW_TYPE_COLLECTION, communityId, storeDtos.get(0).getStoreId());
            if (flag < 1) {
                continue;
            }

            // 更新仓库调拨工作流配置
            flag = updateWorkflow(WorkflowDto.FLOW_TYPE_ALLOCATION_STOREHOUSE_GO, communityId, storeDtos.get(0).getStoreId());
            if (flag < 1) {
                continue;
            }
        }
    }

    /**
     * 保存菜单组小区关系
     * 为入驻的小区分配功能权限
     *
     * @param reqJson 请求参数JSON对象
     * @param communityId 小区ID
     * @param communityName 小区名称
     * @throws CmdException 当保存失败时抛出异常
     */
    private void saveMenuGroupCommunity(JSONObject reqJson, String communityId, String communityName) {
        List<MenuGroupDto> menuGroupDtos = null;
        MenuGroupDto menuGroupDto = null;
        
        // 根据是否指定功能组ID来查询菜单组
        if (!reqJson.containsKey("groupIds") || reqJson.getJSONArray("groupIds").size() < 1) {
            // 未指定功能组，查询物业类型的所有菜单组
            menuGroupDto = new MenuGroupDto();
            menuGroupDto.setStoreType(StoreDto.STORE_TYPE_PROPERTY);
            menuGroupDtos = menuGroupV1InnerServiceSMOImpl.queryMenuGroups(menuGroupDto);
        } else {
            // 指定了功能组，查询指定的菜单组
            menuGroupDto = new MenuGroupDto();
            JSONArray groupIds = reqJson.getJSONArray("groupIds");
            String groupId;
            List<String> gIds = new ArrayList<>();
            // 收集所有功能组ID
            for (int groupIndex = 0; groupIndex < groupIds.size(); groupIndex++) {
                groupId = groupIds.getString(groupIndex);
                gIds.add(groupId);
            }
            menuGroupDto.setgIds(gIds.toArray(new String[gIds.size()]));
            menuGroupDtos = menuGroupV1InnerServiceSMOImpl.queryMenuGroups(menuGroupDto);
        }

        // 验证菜单组存在
        if (menuGroupDtos == null || menuGroupDtos.size() < 1) {
            throw new IllegalArgumentException("没有分配任何功能");
        }

        // 创建菜单组小区关系记录
        List<MenuGroupCommunityPo> menuGroupCommunityPos = new ArrayList<>();
        MenuGroupCommunityPo tmpMenuGroupCommunityPo = null;
        for (MenuGroupDto menuGroupDto1 : menuGroupDtos) {
            tmpMenuGroupCommunityPo = new MenuGroupCommunityPo();
            tmpMenuGroupCommunityPo.setCommunityId(communityId);
            tmpMenuGroupCommunityPo.setCommunityName(communityName);
            tmpMenuGroupCommunityPo.setGcId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            tmpMenuGroupCommunityPo.setgId(menuGroupDto1.getgId());
            tmpMenuGroupCommunityPo.setName(menuGroupDto1.getName());
            menuGroupCommunityPos.add(tmpMenuGroupCommunityPo);
        }
        
        // 批量保存菜单组小区关系
        int flag = menuGroupCommunityV1InnerServiceSMOImpl.saveMenuGroupCommunitys(menuGroupCommunityPos);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }
    }

    /**
     * 更新工作流配置
     * 为入驻的小区配置相应的工作流
     *
     * @param flowType 工作流类型
     * @param communityId 小区ID
     * @param storeId 商户ID
     * @return 更新影响的行数，0表示未找到对应工作流或更新失败
     */
    public int updateWorkflow(String flowType, String communityId, String storeId) {
        // 查询指定类型的工作流
        WorkflowDto workflowDto = new WorkflowDto();
        workflowDto.setCommunityId(communityId);
        workflowDto.setFlowType(flowType);
        List<WorkflowDto> workflowDtos = workflowV1InnerServiceSMOImpl.queryWorkflows(workflowDto);

        // 如果未找到对应工作流，返回0
        if (workflowDtos == null || workflowDtos.size() < 1) {
            return 0;
        }
        
        // 更新工作流的小区ID和商户ID
        WorkflowPo workflowPo = new WorkflowPo();
        workflowPo.setFlowId(workflowDtos.get(0).getFlowId());
        workflowPo.setCommunityId(communityId);
        workflowPo.setStoreId(storeId);
        return workflowV1InnerServiceSMOImpl.updateWorkflow(workflowPo);
    }
}