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
package com.java110.community.cmd.inspectionTask;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.community.IInspectionTaskInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.inspectionTask.ApiInspectionTaskDataVo;
import com.java110.vo.api.inspectionTask.ApiInspectionTaskVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡检任务管理命令类
 * 
 * 该类负责处理管理员巡检任务的查询请求，包括任务列表查询、分页处理、
 * 权限验证以及社区信息补充等功能。
 * 
 * 服务编码：inspectionTask.listAdminInspectionTasks
 * 请求路径：/app/inspectionTask.ListInspectionTask
 * 
 * @author 吴学文 at 2022-03-16 09:35:12 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 * @see InspectionTaskDto
 * @see ApiInspectionTaskVo
 */
@Java110Cmd(serviceCode = "inspectionTask.listAdminInspectionTasks")
public class ListAdminInspectionTasksCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListAdminInspectionTasksCmd.class);

    /**
     * 巡检任务内部服务接口
     */
    @Autowired
    private IInspectionTaskInnerServiceSMO inspectionTaskInnerServiceSMOImpl;

    /**
     * 社区内部服务接口
     */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 员工社区关联内部服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证分页参数和管理员权限，确保请求的合法性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 执行巡检任务查询命令
     * 
     * 处理巡检任务的查询请求，包括参数转换、权限过滤、数据查询和结果封装
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求参数转换为巡检任务DTO对象
        InspectionTaskDto inspectionTaskDto = BeanConvertUtil.covertBean(reqJson, InspectionTaskDto.class);
        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 查询当前用户有权限的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有权限的社区不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            inspectionTaskDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }
        
        // 处理多状态查询条件
        if (reqJson.containsKey("moreState") && reqJson.getString("moreState").contains(",")) {
            inspectionTaskDto.setStates(reqJson.getString("moreState").split(","));
        }

        // 查询当天巡检任务标识
        if (!StringUtil.isEmpty(reqJson.getString("isToday"))) {
            inspectionTaskDto.setDayTask(reqJson.getString("isToday"));
        }
        
        // 查询巡检任务总数
        int count = inspectionTaskInnerServiceSMOImpl.queryInspectionTasksCount(inspectionTaskDto);

        List<ApiInspectionTaskDataVo> inspectionTasks = null;

        // 根据查询结果数量处理数据
        if (count > 0) {
            // 查询巡检任务列表并转换为VO对象
            inspectionTasks = BeanConvertUtil.covertBeanList(inspectionTaskInnerServiceSMOImpl.queryInspectionTasks(inspectionTaskDto), ApiInspectionTaskDataVo.class);
        } else {
            // 如果没有数据，返回空列表
            inspectionTasks = new ArrayList<>();
        }

        // 刷新社区名称信息
        refreshCommunityName(inspectionTasks);

        // 构建返回结果VO对象
        ApiInspectionTaskVo apiInspectionTaskVo = new ApiInspectionTaskVo();

        apiInspectionTaskVo.setTotal(count);
        // 计算总页数
        apiInspectionTaskVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiInspectionTaskVo.setInspectionTasks(inspectionTasks);

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiInspectionTaskVo), HttpStatus.OK);

        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(responseEntity);

    }

    /**
     * 刷新巡检任务列表中的社区名称
     * 
     * 根据社区ID查询社区名称并设置到巡检任务数据中
     * 
     * @param inspectionTasks 巡检任务数据列表
     */
    private void refreshCommunityName(List<ApiInspectionTaskDataVo> inspectionTasks) {

        // 如果巡检任务列表为空，直接返回
        if(ListUtil.isNull(inspectionTasks)){
            return;
        }

        // 收集所有社区ID
        List<String> communityIds = new ArrayList<>();
        for (ApiInspectionTaskDataVo apiInspectionTaskDataVo : inspectionTasks) {
            communityIds.add(apiInspectionTaskDataVo.getCommunityId());
        }

        // 如果社区ID列表为空，直接返回
        if(ListUtil.isNull(communityIds)){
            return ;
        }
        
        // 构建社区查询条件
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        // 查询社区信息
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 如果社区信息为空，直接返回
        if(ListUtil.isNull(communityDtos)){
            return;
        }
        
        // 遍历巡检任务，设置社区名称
        for (ApiInspectionTaskDataVo apiInspectionTaskDataVo : inspectionTasks) {
            for (CommunityDto tCommunityDto : communityDtos) {
                // 匹配社区ID
                if (!apiInspectionTaskDataVo.getCommunityId().equals(tCommunityDto.getCommunityId())) {
                    continue;
                }
                // 设置社区名称
                apiInspectionTaskDataVo.setCommunityName(tCommunityDto.getName());
            }
        }
    }
}