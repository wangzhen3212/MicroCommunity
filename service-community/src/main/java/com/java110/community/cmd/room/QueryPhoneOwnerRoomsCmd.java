package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.community.bmo.room.IQueryRoomStatisticsBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.user.IBuildingOwnerV1InnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.ApiRoomDataVo;
import com.java110.vo.api.ApiRoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 查询手机端业主房屋信息命令类
 * 
 * 该命令用于处理手机端查询业主房屋信息的请求，包括验证用户身份、查询业主信息、
 * 查询房屋信息以及统计房屋欠费情况等功能
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode room.queryPhoneOwnerRooms
 */
@Java110Cmd(serviceCode = "room.queryPhoneOwnerRooms")
public class QueryPhoneOwnerRoomsCmd extends Cmd {

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IQueryRoomStatisticsBMO queryRoomStatisticsBMOImpl;
    
    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IBuildingOwnerV1InnerServiceSMO buildingOwnerV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的社区ID信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含communityId
        Assert.hasKeyAndValue(reqJson, "communityId", "请求中未包含communityId信息");
    }

    /**
     * 命令执行方法
     * 
     * 主要业务流程：
     * 1. 获取当前用户ID
     * 2. 查询用户与业主的绑定关系
     * 3. 验证业主身份和房屋认证状态
     * 4. 查询业主详细信息
     * 5. 查询业主名下的房屋信息
     * 6. 统计房屋欠费情况
     * 7. 构建并返回响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);

        // 构建业主应用用户查询条件
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId);
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询用户与业主的绑定关系
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        // 检查是否已绑定业主身份
        if (ListUtil.isNull(ownerAppUserDtos)) {
            throw new CmdException("未绑定业主");
        }

        // 遍历绑定关系，获取有效的成员ID
        String memberId = "";
        for (OwnerAppUserDto tmpOwnerAppUserDto : ownerAppUserDtos) {
            // 跳过无效的成员ID（-1表示未认证）
            if ("-1".equals(tmpOwnerAppUserDto.getMemberId())) {
                continue;
            }
            memberId = tmpOwnerAppUserDto.getMemberId();
        }
        
        // 检查是否已认证房屋
        if (StringUtil.isEmpty(memberId)) {
            throw new CmdException("未认证房屋");
        }

        // 构建业主查询条件
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(memberId);
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询业主详细信息
        List<OwnerDto> ownerDtos = buildingOwnerV1InnerServiceSMOImpl.queryBuildingOwners(ownerDto);
        
        // 检查业主是否存在
        if(ListUtil.isNull(ownerDtos)){
            throw new CmdException("业主或成员不存在");
        }

        // 将请求参数转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        // 设置业主ID
        roomDto.setOwnerId(ownerDtos.get(0).getOwnerId());
        // 处理房屋编号参数
        hasRoomNum(reqJson, roomDto);

        // 创建返回对象
        ApiRoomVo apiRoomVo = new ApiRoomVo();
        
        // 查询业主名下的房屋信息
        List<RoomDto> roomDtoList = roomInnerServiceSMOImpl.queryRoomsByOwner(roomDto);
        
        // 统计房屋欠费情况
        roomDtoList = queryRoomStatisticsBMOImpl.queryRoomOweFee(roomDtoList);

        // 设置返回数据
        apiRoomVo.setTotal(roomDtoList.size());
        apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(roomDtoList, ApiRoomDataVo.class));
        apiRoomVo.setRecords(1);

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 处理房屋编号参数的方法
     * 
     * 解析请求中的房屋编号参数，将其拆分为楼层号、单元号和房间号，
     * 并设置到RoomDto对象中
     * 
     * @param reqJson 请求参数的JSON对象
     * @param roomDto 房屋数据传输对象，用于设置解析后的房屋信息
     */
    private void hasRoomNum(JSONObject reqJson, RoomDto roomDto) {
        // 检查请求参数中是否包含roomNum
        if (!reqJson.containsKey("roomNum")) {
            return;
        }
        
        String roomNum = reqJson.getString("roomNum");
        
        // 检查roomNum是否为空
        if (StringUtil.isEmpty(roomNum)) {
            return;
        }
        
        // 按照"-"分割房屋编号，格式为：楼层号-单元号-房间号
        String[] roomNums = reqJson.getString("roomNum").split("-");

        // 如果分割后的数组长度为3，说明格式正确
        if (roomNums != null && roomNums.length == 3) {
            // 分别设置楼层号、单元号和房间号
            roomDto.setFloorNum(roomNums[0]);
            roomDto.setUnitNum(roomNums[1]);
            roomDto.setRoomNum(roomNums[2]);
        }
    }
}