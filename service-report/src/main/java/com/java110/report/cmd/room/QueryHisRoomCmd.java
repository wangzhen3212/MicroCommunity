package com.java110.report.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 房屋变更记录查询命令类
 * 负责处理房屋历史变更记录的查询请求，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd声明服务代码为"room.queryHisRoom"
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "room.queryHisRoom")
public class QueryHisRoomCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询房屋变更记录相关数据
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否包含必要的字段
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
    }

    /**
     * 命令执行方法
     * 处理房屋变更记录的查询请求，包括参数处理、数据查询和响应构建
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页行数参数
        int row = reqJson.getInteger("row");
        // 将请求参数转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);

        // 处理房间名称参数，如果存在则解析为楼层、单元和房间号
        String roomName = reqJson.getString("roomName");
        if(!StringUtil.isEmpty(roomName)){
            // 按照"-"分割房间名称，格式通常为"楼层-单元-房间号"
            String[] roomNames = roomName.split("-");
            // 只有当分割后的数组长度为3时才进行赋值
            if(roomNames.length == 3) {
                roomDto.setFloorNum(roomNames[0]);  // 设置楼层号
                roomDto.setUnitNum(roomNames[1]);   // 设置单元号
                roomDto.setRoomNum(roomNames[2]);   // 设置房间号
            }
        }

        // 查询房屋变更记录总数
        int total = reportCommunityInnerServiceSMOImpl.queryHisRoomCount(roomDto);
        
        List<RoomDto> roomDtos = null;
        // 根据查询结果决定是否查询详细数据
        if (total > 0) {
            // 如果总数大于0，则查询具体的房屋变更记录列表
            roomDtos = reportCommunityInnerServiceSMOImpl.queryHisRooms(roomDto);
        } else {
            // 如果总数为0，返回空列表
            roomDtos = new ArrayList<>();
        }

        // 计算总页数并构建响应实体
        // 使用Math.ceil确保向上取整，正确处理分页
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) total / (double) row),  // 计算总页数
            total,      // 总记录数
            roomDtos    // 房屋变更记录列表
        );
        
        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}