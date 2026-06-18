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
package com.java110.user.cmd.examine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.room.RoomDto;
import com.java110.dto.examine.ExamineStaffDto;
import com.java110.dto.examine.ExamineStaffValueDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.examine.ExamineStaffValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 考核评分保存命令类
 * 
 * 该类负责处理员工考核评分的保存操作，包括数据验证和业务逻辑处理。
 * 主要功能包括：验证考核数据完整性、查询相关业务数据、保存或更新考核评分记录。
 * 
 * 服务编码：examineStaffValue.saveExamineStaffValue
 * 请求路径：/app/examineStaffValue.SaveExamineStaffValue
 * 
 * @author 吴学文 at 2023-03-07 16:34:48 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 * @see ICmdDataFlowContext
 * @see CmdEvent
 */
@Java110Cmd(serviceCode = "examine.saveExamineStaffValue")
public class SaveExamineStaffValueCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveExamineStaffValueCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 考核评分服务接口 */
    @Autowired
    private IExamineStaffValueV1InnerServiceSMO examineStaffValueV1InnerServiceSMOImpl;

    /** 考核员工服务接口 */
    @Autowired
    private IExamineStaffV1InnerServiceSMO examineStaffV1InnerServiceSMOImpl;

    /** 业主应用用户服务接口 */
    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /** 业主服务接口 */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /** 业主房间关系服务接口 */
    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    /** 房间服务接口 */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证考核评分保存请求的参数合法性，包括：
     * 1. 必填字段验证
     * 2. 考核项目数据验证
     * 3. 用户登录状态验证
     * 4. 业主信息验证
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "esId", "请求报文中未包含esId");
        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        
        // 验证考核项目数据
        if (!reqJson.containsKey("projects")) {
            throw new CmdException("未包含考核项目");
        }

        JSONArray projects = reqJson.getJSONArray("projects");

        if (projects == null || projects.size() < 1) {
            throw new CmdException("未包含考核项目");
        }
        
        // 验证每个考核项目的评分数据
        JSONObject param = null;
        String value = "";
        for (int proIndex = 0; proIndex < projects.size(); proIndex++) {
            param = projects.getJSONObject(proIndex);
            Assert.hasKeyAndValue(param, "value", "未包含评分");
            value = param.getString("value");
            if (!StringUtil.isNumber(value)) {
                throw new CmdException("不是有效数字");
            }
        }

        // 验证用户登录状态
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");
        Assert.hasLength(userId, "用户未登陆");

        // 查询业主信息
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId);
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        if (ownerAppUserDtos == null || ownerAppUserDtos.size() < 1) {
            throw new CmdException("未找到业主信息");
        }

        // 将业主ID设置到请求参数中供后续使用
        reqJson.put("ownerId", ownerAppUserDtos.get(0).getOwnerId());
    }

    /**
     * 执行考核评分保存命令
     * 
     * 该方法负责处理考核评分的保存逻辑，包括：
     * 1. 查询用户和考核员工信息
     * 2. 查询业主和房间信息
     * 3. 遍历考核项目，保存或更新评分记录
     * 4. 返回操作结果
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 查询用户信息
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "业主不存在");

        // 查询考核员工信息
        ExamineStaffDto examineStaffDto = new ExamineStaffDto();
        examineStaffDto.setEsId(reqJson.getString("esId"));
        List<ExamineStaffDto> examineStaffDtos = examineStaffV1InnerServiceSMOImpl.queryExamineStaffs(examineStaffDto);
        Assert.listOnlyOne(examineStaffDtos, "考核员工不存在");

        // 查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(reqJson.getString("ownerId"));
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
        String ownerName = "未知";
        if (ownerDtos != null && ownerDtos.size() > 0) {
            ownerName = ownerDtos.get(0).getName();
        }

        // 查询房间信息
        String roomId = "";
        String roomName = "";
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerId(reqJson.getString("ownerId"));
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        if (ownerRoomRelDtos != null && ownerRoomRelDtos.size() > 0) {
            RoomDto roomDto = new RoomDto();
            roomDto.setRoomId(ownerRoomRelDtos.get(0).getRoomId());
            List<RoomDto> roomDtos = roomV1InnerServiceSMOImpl.queryRooms(roomDto);
            if (roomDtos != null && roomDtos.size() > 0) {
                // 构建房间名称：楼层-单元-房间号
                roomName = roomDtos.get(0).getFloorNum() + "-" + roomDtos.get(0).getUnitNum() + "-" + roomDtos.get(0).getRoomNum();
            }
        }

        // 处理考核项目数据
        JSONArray projects = reqJson.getJSONArray("projects");
        ExamineStaffValuePo examineStaffValuePo = null;
        for (int proIndex = 0; proIndex < projects.size(); proIndex++) {
            // 检查是否已存在评分记录
            ExamineStaffValueDto examineStaffValueDto = new ExamineStaffValueDto();
            examineStaffValueDto.setEsYear(DateUtil.getYear()); // 设置考核年份为当前年份
            examineStaffValueDto.setOwnerId(reqJson.getString("ownerId"));
            examineStaffValueDto.setStaffId(examineStaffDtos.get(0).getStaffId());
            examineStaffValueDto.setProjectId(projects.getJSONObject(proIndex).getString("projectId"));
            List<ExamineStaffValueDto> examineStaffValueDtos = examineStaffValueV1InnerServiceSMOImpl.queryExamineStaffValues(examineStaffValueDto);
            
            if (examineStaffValueDtos == null || examineStaffValueDtos.size() < 1) {
                // 新增考核评分记录
                examineStaffValuePo = new ExamineStaffValuePo();
                examineStaffValuePo.setExamineValue(projects.getJSONObject(proIndex).getString("value"));
                examineStaffValuePo.setStaffId(examineStaffDtos.get(0).getStaffId());
                examineStaffValuePo.setStaffName(examineStaffDtos.get(0).getStaffName());
                examineStaffValuePo.setEsvId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID)); // 生成唯一ID
                examineStaffValuePo.setEsId(reqJson.getString("esId"));
                examineStaffValuePo.setCommunityId(examineStaffDtos.get(0).getCommunityId());
                examineStaffValuePo.setOwnerId(reqJson.getString("ownerId"));
                examineStaffValuePo.setOwnerName(ownerName);
                examineStaffValuePo.setProjectId(projects.getJSONObject(proIndex).getString("projectId"));
                examineStaffValuePo.setRoomId(roomId);
                examineStaffValuePo.setRoomName(roomName);
                examineStaffValuePo.setEsYear(DateUtil.getYear());
                
                int flag = examineStaffValueV1InnerServiceSMOImpl.saveExamineStaffValue(examineStaffValuePo);

                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }
            } else {
                // 更新已存在的考核评分记录
                examineStaffValuePo = new ExamineStaffValuePo();
                examineStaffValuePo.setExamineValue(projects.getJSONObject(proIndex).getString("value"));
                examineStaffValuePo.setEsvId(examineStaffValueDtos.get(0).getEsvId());
                examineStaffValuePo.setCommunityId(examineStaffDtos.get(0).getCommunityId());
                examineStaffValueV1InnerServiceSMOImpl.updateExamineStaffValue(examineStaffValuePo);
            }
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}