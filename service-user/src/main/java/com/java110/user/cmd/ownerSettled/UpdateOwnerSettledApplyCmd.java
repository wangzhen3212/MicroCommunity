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
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.user.IOwnerSettledApplyV1InnerServiceSMO;
import com.java110.intf.user.IOwnerSettledRoomsV1InnerServiceSMO;
import com.java110.po.owner.OwnerSettledApplyPo;
import com.java110.po.owner.OwnerSettledRoomsPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：ownerSettledApply.updateOwnerSettledApply
 * 请求路劲：/app/ownerSettledApply.UpdateOwnerSettledApply
 * add by 吴学文 at 2023-01-26 00:52:26 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 业主入驻申请更新命令类
 * 负责处理业主入驻申请信息的更新操作，包括申请基本信息更新和关联房屋信息的更新
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-01-26
 */
@Java110Cmd(serviceCode = "ownerSettled.updateOwnerSettledApply")
public class UpdateOwnerSettledApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateOwnerSettledApplyCmd.class);

    /**
     * ID生成前缀常量
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 业主入驻申请服务接口
     */
    @Autowired
    private IOwnerSettledApplyV1InnerServiceSMO ownerSettledApplyV1InnerServiceSMOImpl;

    /**
     * 业主入驻房屋关联服务接口
     */
    @Autowired
    private IOwnerSettledRoomsV1InnerServiceSMO ownerSettledRoomsV1InnerServiceSMOImpl;

    /**
     * 房屋信息服务接口
     */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "applyId", "applyId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

        // 检查是否包含房屋信息，如果没有则直接返回
        if (!reqJson.containsKey("rooms")) {
            return;
        }

        JSONArray rooms = reqJson.getJSONArray("rooms");

        // 检查房屋信息数组是否为空
        if (rooms == null || rooms.size() < 1) {
            return;
        }

        // 遍历房屋信息数组，验证每个房屋信息的必需字段
        JSONObject room = null;
        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            room = rooms.getJSONObject(roomIndex);
            Assert.hasKeyAndValue(room, "roomId", "请求报文中未包含roomId");
            Assert.hasKeyAndValue(room, "startTime", "请求报文中未包含startTime");
            Assert.hasKeyAndValue(room, "endTime", "请求报文中未包含endTime");
        }
    }

    /**
     * 执行更新业主入驻申请命令
     * 更新业主入驻申请信息，并处理关联的房屋信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为业主入驻申请PO对象
        OwnerSettledApplyPo ownerSettledApplyPo = BeanConvertUtil.covertBean(reqJson, OwnerSettledApplyPo.class);
        // 调用服务更新业主入驻申请信息
        int flag = ownerSettledApplyV1InnerServiceSMOImpl.updateOwnerSettledApply(ownerSettledApplyPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 检查是否包含房屋信息，如果没有则直接返回成功
        if (!reqJson.containsKey("rooms")) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        JSONArray rooms = reqJson.getJSONArray("rooms");

        // 检查房屋信息数组是否为空
        if (rooms == null || rooms.size() < 1) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        // 删除原有的房屋关联信息
        OwnerSettledRoomsPo ownerSettledRoomsPo = new OwnerSettledRoomsPo();
        ownerSettledRoomsPo.setApplyId(reqJson.getString("applyId"));
        ownerSettledApplyPo.setCommunityId(reqJson.getString("communityId"));
        flag = ownerSettledRoomsV1InnerServiceSMOImpl.deleteOwnerSettledRooms(ownerSettledRoomsPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 重新添加新的房屋关联信息
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
            Assert.listOnlyOne(roomDtos,"房屋不存在"+roomDto.getRoomId());
            
            // 将房屋信息转换为PO对象
            ownerSettledRoomsPo = BeanConvertUtil.covertBean(room, OwnerSettledRoomsPo.class);
            ownerSettledRoomsPo.setApplyId(ownerSettledApplyPo.getApplyId());
            ownerSettledRoomsPo.setCommunityId(ownerSettledApplyPo.getCommunityId());
            // 生成唯一ID
            ownerSettledRoomsPo.setOsrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 构建房屋完整名称（楼层-单元-房间号）
            ownerSettledRoomsPo.setRoomName(roomDtos.get(0).getFloorNum()+"-"+roomDtos.get(0).getUnitNum()+"-"+roomDtos.get(0).getRoomNum());
            
            // 保存房屋关联信息
            flag = ownerSettledRoomsV1InnerServiceSMOImpl.saveOwnerSettledRooms(ownerSettledRoomsPo);
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}