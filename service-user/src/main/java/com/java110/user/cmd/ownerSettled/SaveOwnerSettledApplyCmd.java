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
package com.java110.user.cmd.ownerSettled;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.room.RoomDto;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.owner.OwnerSettledApplyDto;
import com.java110.dto.owner.OwnerSettledSettingDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.user.IOwnerSettledApplyV1InnerServiceSMO;
import com.java110.intf.user.IOwnerSettledRoomsV1InnerServiceSMO;
import com.java110.intf.user.IOwnerSettledSettingV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.owner.OwnerSettledApplyPo;
import com.java110.po.owner.OwnerSettledRoomsPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Java110CmdDoc(title = "业主入驻申请",
        description = "业主企业入驻房屋申请",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/ownerSettled.saveOwnerSettledApply",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "ownerSettled.saveOwnerSettledApply",
        seq = 15
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "typeId", length = 30, remark = "放行ID"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "放行小区"),
        @Java110ParamDoc(name = "applyCompany", length = 30, remark = "申请单位"),
        @Java110ParamDoc(name = "applyPerson", length = 30, remark = "申请人"),
        @Java110ParamDoc(name = "idCard", length = 30, remark = "身份证"),
        @Java110ParamDoc(name = "applyTel", length = 30, remark = "申请电话"),
        @Java110ParamDoc(name = "passTime", length = 30, remark = "通信时间"),
        @Java110ParamDoc(name = "amount", length = 30, remark = "数量"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\n" +
                "\t\"irId\": \"\",\n" +
                "\t\"typeId\": \"102023011264340006\",\n" +
                "\t\"applyCompany\": \"123123\",\n" +
                "\t\"applyPerson\": \"12312\",\n" +
                "\t\"idCard\": \"12312\",\n" +
                "\t\"applyTel\": \"18909711443\",\n" +
                "\t\"passTime\": \"2023-01-13 11:55:00\",\n" +
                "\t\"resNames\": [{\n" +
                "\t\t\"resName\": \"123\",\n" +
                "\t\t\"amount\": \"123\"\n" +
                "\t}],\n" +
                "\t\"state\": \"\",\n" +
                "\t\"carNum\": \"123123\",\n" +
                "\t\"remark\": \"123\",\n" +
                "\t\"audit\": {\n" +
                "\t\t\"assignee\": \"-2\",\n" +
                "\t\t\"staffId\": \"\",\n" +
                "\t\t\"staffName\": \"\",\n" +
                "\t\t\"taskId\": \"\"\n" +
                "\t},\n" +
                "\t\"communityId\": \"2022120695590004\"\n" +
                "}",
        resBody="{'code':0,'msg':'成功'}"
)
/**
 * 业主入驻申请命令处理类
 * 负责处理业主企业入驻房屋的申请流程，包括数据验证、申请信息保存、房屋关联信息保存以及审核流程处理
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-01-26
 */
@Java110Cmd(serviceCode = "ownerSettled.saveOwnerSettledApply")
public class SaveOwnerSettledApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveOwnerSettledApplyCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 业主入驻申请服务接口 */
    @Autowired
    private IOwnerSettledApplyV1InnerServiceSMO ownerSettledApplyV1InnerServiceSMOImpl;

    /** 业主入驻房屋关联服务接口 */
    @Autowired
    private IOwnerSettledRoomsV1InnerServiceSMO ownerSettledRoomsV1InnerServiceSMOImpl;

    /** 业主入驻设置服务接口 */
    @Autowired
    private IOwnerSettledSettingV1InnerServiceSMO ownerSettledSettingV1InnerServiceSMOImpl;

    /** 房屋信息服务接口 */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /** 工作流服务接口 */
    @Autowired
    private IOaWorkflowInnerServiceSMO oaWorkflowInnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /** Activiti工作流服务接口 */
    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowActivitiInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查请求中必需的参数是否存在，并对房屋信息进行详细验证
     *
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必需的基本参数
        Assert.hasKeyAndValue(reqJson, "ownerId", "请求报文中未包含ownerId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 验证房屋信息是否存在
        if (!reqJson.containsKey("rooms")) {
            throw new CmdException("房屋不存在");
        }

        JSONArray rooms = reqJson.getJSONArray("rooms");

        if (rooms == null || rooms.size() < 1) {
            throw new CmdException("房屋不存在");
        }

        // 验证每个房屋的必需参数
        JSONObject room = null;
        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            room = rooms.getJSONObject(roomIndex);
            Assert.hasKeyAndValue(room, "roomId", "请求报文中未包含roomId");
            Assert.hasKeyAndValue(room, "startTime", "请求报文中未包含startTime");
            Assert.hasKeyAndValue(room, "endTime", "请求报文中未包含endTime");
        }
    }

    /**
     * 执行业主入驻申请命令
     * 处理业主入驻申请的完整流程，包括保存申请信息、关联房屋信息、处理审核流程
     *
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当数据保存失败或流程处理异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取当前用户ID
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");

        // 转换并保存业主入驻申请信息
        OwnerSettledApplyPo ownerSettledApplyPo = BeanConvertUtil.covertBean(reqJson, OwnerSettledApplyPo.class);
        ownerSettledApplyPo.setApplyId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        ownerSettledApplyPo.setCreateUserId(userId);
        ownerSettledApplyPo.setState(OwnerSettledApplyDto.STATE_WAIT); // 设置初始状态为等待审核
        int flag = ownerSettledApplyV1InnerServiceSMOImpl.saveOwnerSettledApply(ownerSettledApplyPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 处理关联的房屋信息
        JSONArray rooms = reqJson.getJSONArray("rooms");
        JSONObject room = null;
        RoomDto roomDto = null;
        List<RoomDto> roomDtos = null;
        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            room = rooms.getJSONObject(roomIndex);
            // 验证房屋是否存在
            roomDto = new RoomDto();
            roomDto.setCommunityId(ownerSettledApplyPo.getCommunityId());
            roomDto.setRoomId(room.getString("roomId"));
            roomDtos = roomV1InnerServiceSMOImpl.queryRooms(roomDto);
            Assert.listOnlyOne(roomDtos, "房屋不存在" + roomDto.getRoomId());
            
            // 保存业主入驻房屋关联信息
            OwnerSettledRoomsPo ownerSettledRoomsPo = BeanConvertUtil.covertBean(room, OwnerSettledRoomsPo.class);
            ownerSettledRoomsPo.setApplyId(ownerSettledApplyPo.getApplyId());
            ownerSettledRoomsPo.setCommunityId(ownerSettledApplyPo.getCommunityId());
            ownerSettledRoomsPo.setOsrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 构建完整的房间名称（楼层-单元-房间号）
            ownerSettledRoomsPo.setRoomName(roomDtos.get(0).getFloorNum() + "-" + roomDtos.get(0).getUnitNum() + "-" + roomDtos.get(0).getRoomNum());
            flag = ownerSettledRoomsV1InnerServiceSMOImpl.saveOwnerSettledRooms(ownerSettledRoomsPo);
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 检查是否需要走审核流程
        OwnerSettledSettingDto ownerSettledSettingDto = new OwnerSettledSettingDto();
        ownerSettledSettingDto.setCommunityId(reqJson.getString("communityId"));
        ownerSettledSettingDto.setAuditWay("Y"); // 设置审核方式为需要审核
        List<OwnerSettledSettingDto> ownerSettledSettingDtos
                = ownerSettledSettingV1InnerServiceSMOImpl.queryOwnerSettledSettings(ownerSettledSettingDto);
        
        // 如果不需要审核流程，直接设置申请状态为完成
        if (ownerSettledSettingDtos == null || ownerSettledSettingDtos.size() < 1) {
            OwnerSettledApplyPo tmpOwnerSettledApplyPo = new OwnerSettledApplyPo();
            tmpOwnerSettledApplyPo.setApplyId(ownerSettledApplyPo.getApplyId());
            tmpOwnerSettledApplyPo.setState(OwnerSettledApplyDto.STATE_COMPLETE);
            ownerSettledApplyV1InnerServiceSMOImpl.updateOwnerSettledApply(tmpOwnerSettledApplyPo);
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        // 获取并验证工作流配置
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setStoreId(ownerSettledSettingDtos.get(0).getStoreId());
        oaWorkflowDto.setFlowId(ownerSettledSettingDtos.get(0).getFlowId());
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);
        Assert.listOnlyOne(oaWorkflowDtos, "流程不存在");
        
        // 检查工作流状态是否可用
        if (!OaWorkflowDto.STATE_COMPLAINT.equals(oaWorkflowDtos.get(0).getState())) {
            throw new IllegalArgumentException(oaWorkflowDtos.get(0).getFlowName() + "流程未部署");
        }

        // 检查流程定义键是否存在
        if (StringUtil.isEmpty(oaWorkflowDtos.get(0).getProcessDefinitionKey())) {
            throw new IllegalArgumentException(oaWorkflowDtos.get(0).getFlowName() + "流程未部署");
        }

        // 启动工作流流程
        JSONObject flowJson = new JSONObject();
        flowJson.put("processDefinitionKey", oaWorkflowDtos.get(0).getProcessDefinitionKey());
        flowJson.put("createUserId", userId);
        flowJson.put("flowId", oaWorkflowDtos.get(0).getFlowId());
        flowJson.put("id", ownerSettledApplyPo.getApplyId());
        flowJson.put("auditMessage", "提交审核");
        flowJson.put("storeId", ownerSettledSettingDtos.get(0).getStoreId());
        reqJson.put("processDefinitionKey", oaWorkflowDtos.get(0).getProcessDefinitionKey());
        JSONObject result = oaWorkflowActivitiInnerServiceSMOImpl.startProcess(flowJson);

        // 提交者提交审核任务
        flowJson = new JSONObject();
        flowJson.put("processInstanceId", result.getString("processInstanceId"));
        flowJson.put("createUserId", userId);
        flowJson.put("nextUserId", reqJson.getJSONObject("audit").getString("staffId"));
        flowJson.put("storeId", ownerSettledSettingDtos.get(0).getStoreId());
        flowJson.put("id", ownerSettledApplyPo.getApplyId());
        flowJson.put("flowId", oaWorkflowDtos.get(0).getFlowId());

        // 自动完成第一个任务节点
        oaWorkflowActivitiInnerServiceSMOImpl.autoFinishFirstTask(flowJson);

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}