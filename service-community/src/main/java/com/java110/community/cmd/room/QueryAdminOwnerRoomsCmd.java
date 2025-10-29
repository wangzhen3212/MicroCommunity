package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.community.bmo.room.IQueryRoomStatisticsBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.ApiRoomDataVo;
import com.java110.vo.api.ApiRoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 查询管理员业主房屋信息命令类
 * 
 * 该命令类用于处理管理员查询业主名下房屋信息的请求，
 * 包括房屋基本信息统计和欠费信息查询等功能
 * 
 * @serviceCode room.queryAdminOwnerRooms
 */
@Java110Cmd(serviceCode = "room.queryAdminOwnerRooms")
public class QueryAdminOwnerRoomsCmd extends Cmd {

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IQueryRoomStatisticsBMO queryRoomStatisticsBMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证管理员权限、分页信息和必要参数
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证业主编号参数是否存在
        Assert.hasKeyAndValue(reqJson,"ownerId","未包含业主编号");
    }

    /**
     * 执行命令方法
     * 
     * 查询业主名下房屋信息，包括房屋基本信息和欠费统计
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求参数转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        // 处理房屋编号参数
        hasRoomNum(reqJson, roomDto);

        // 创建返回对象
        ApiRoomVo apiRoomVo = new ApiRoomVo();
        // 查询业主名下的房屋列表
        List<RoomDto> roomDtoList = roomInnerServiceSMOImpl.queryRoomsByOwner(roomDto);
        // 查询房屋欠费信息
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
     * 判断并处理房屋编号参数
     * 
     * 如果请求中包含房屋编号参数，将其拆分为楼层号、单元号和房间号
     * 房屋编号格式为：楼层号-单元号-房间号
     * 
     * @param reqJson 请求参数JSON对象
     * @param roomDto 房屋数据传输对象
     */
    private void hasRoomNum(JSONObject reqJson, RoomDto roomDto) {
        // 检查请求中是否包含房屋编号参数
        if (!reqJson.containsKey("roomNum")) {
            return;
        }
        String roomNum = reqJson.getString("roomNum");
        // 检查房屋编号是否为空
        if (StringUtil.isEmpty(roomNum)) {
            return;
        }
        // 按"-"分割房屋编号
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