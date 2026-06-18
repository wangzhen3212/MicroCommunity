/**
 * 查询房屋信息命令类
 * 
 * 该类是一个基于Java110框架的命令处理器，专门用于处理通过APP查询房屋信息的请求。
 * 主要功能包括参数验证、房屋数据查询、数据清理和响应结果封装。
 * 该类仅对微信业主端和小程序业主端开放访问权限。
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 * @see IRoomV1InnerServiceSMO
 */
package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.app.AppDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "room.queryRoomsByApp")
public class QueryRoomsByAppCmd extends Cmd {

    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否完整和有效，包括必填字段检查和APP权限验证。
     * 仅允许微信业主端和小程序业主端调用此接口。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败或权限不足时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        //Assert.jsonObjectHaveKey(reqJson, "floorId", "请求中未包含floorId信息");
        Assert.jsonObjectHaveKey(reqJson, "page", "请求报文中未包含page节点");
        Assert.jsonObjectHaveKey(reqJson, "row", "请求报文中未包含row节点");
        Assert.hasKeyAndValue(reqJson, "unitId", "未包含单元");

        // 验证APP权限，仅允许业主端访问
        String appId = CmdContextUtils.getAppId(context);
        if (!AppDto.WECHAT_OWNER_APP_ID.equals(appId) && !AppDto.WECHAT_MINA_OWNER_APP_ID.equals(appId)) {
            throw new CmdException("此接口用户端专用");
        }
    }

    /**
     * 命令执行方法
     * 
     * 执行房屋查询业务逻辑，包括参数转换、数据查询、数据清理和响应封装。
     * 支持分页查询，返回格式化后的房屋信息列表。
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行异常时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        roomDto.setUserId(""); // 清空用户ID，确保查询条件正确

        // 查询总记录数
        int total = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        List<RoomDto> roomDtoList = null;
        
        // 根据查询结果决定是否查询详细数据
        if (total > 0) {
            roomDtoList = roomV1InnerServiceSMOImpl.queryRooms(roomDto);
        } else {
            roomDtoList = new ArrayList<>();
        }

        // 清理房屋数据，格式化显示信息
        roomDtoList = clearRoomDatas(roomDtoList);

        // 计算总页数并封装返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) total / (double) reqJson.getInteger("row")), total, roomDtoList);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 清理房屋数据方法
     * 
     * 对查询到的房屋数据进行清理和格式化处理，只保留必要的字段信息，
     * 并将楼层号、单元号和房间号组合成完整的房间名称。
     * 
     * @param roomDtoList 原始房屋数据列表
     * @return 清理后的房屋数据列表，包含房间ID和格式化后的房间名称
     */
    private List<RoomDto> clearRoomDatas(List<RoomDto> roomDtoList) {
        // 如果输入列表为空，直接返回
        if(ListUtil.isNull(roomDtoList)){
            return roomDtoList;
        }
        
        List<RoomDto> roomDtos = new ArrayList<>();
        RoomDto tmpRoomDto = null;
        
        // 遍历原始数据，创建新的简化数据对象
        for(RoomDto roomDto:roomDtoList){
            tmpRoomDto = new RoomDto();
            tmpRoomDto.setRoomId(roomDto.getRoomId()); // 保留房间ID
            // 格式化房间名称：楼层号-单元号-房间号
            tmpRoomDto.setRoomName(roomDto.getFloorNum()+"-"+roomDto.getUnitNum()+"-"+roomDto.getRoomNum());
            roomDtos.add(tmpRoomDto);
        }
        return roomDtos;
    }
}