/**
 * 管理员房屋查询命令类
 * 
 * 该类负责处理管理员对房屋信息的查询请求，支持按业主信息、楼栋单元房屋编号等条件进行查询，
 * 并返回包含统计信息的房屋数据列表。实现了Cmd接口，通过注解@Java110Cmd声明服务代码。
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 */
package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.community.bmo.room.IQueryRoomStatisticsBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "room.queryAdminRoom")
public class QueryAdminRoomCmd extends Cmd {

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IQueryRoomStatisticsBMO queryRoomStatisticsBMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证管理员权限和分页信息，确保请求的合法性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理房屋查询请求，根据条件查询房屋信息并返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 将请求参数转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);

        // 根据业主信息计算房屋ID条件
        computeOwnerRoomIds(reqJson, roomDto);

        // 解析楼栋单元房屋编号条件
        computeFloorUnitRoomNum(reqJson, roomDto);

        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询用户管理的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有管理的社区，设置社区ID条件
        if (!ListUtil.isNull(communityIds)) {
            roomDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 查询符合条件的房屋总数
        int count = roomInnerServiceSMOImpl.queryRoomsCount(roomDto);
        List<RoomDto> roomDtos = null;
        
        // 如果有符合条件的房屋，查询详细数据并添加统计信息
        if (count > 0) {
            roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            // 查询房屋统计数据并合并到结果中
            roomDtos = queryRoomStatisticsBMOImpl.querySimple(roomDtos);
        } else {
            // 没有符合条件的房屋，返回空列表
            roomDtos = new ArrayList<>();
        }
        
        // 构造返回结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, roomDtos);

        // 构造HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应数据
        context.setResponseEntity(responseEntity);
    }

    /**
     * 根据业主信息计算房屋ID条件
     * 
     * 根据业主姓名和电话查询关联的房屋ID，并设置到查询条件中
     * 
     * @param reqJson 请求参数的JSON对象
     * @param roomDto 房屋查询条件对象
     */
    private void computeOwnerRoomIds(JSONObject reqJson, RoomDto roomDto) {
        // 获取业主姓名和电话查询条件
        String ownerNameLike = reqJson.getString("ownerNameLike");
        String ownerTel = reqJson.getString("ownerTel");

        // 如果业主姓名和电话都为空，则不进行业主关联查询
        if (StringUtil.isEmpty(ownerNameLike) && StringUtil.isEmpty(ownerTel)) {
            return;
        }

        // 构造业主房屋关联查询条件
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerNameLike(reqJson.getString("ownerNameLike"));
        ownerRoomRelDto.setLink(ownerTel);
        
        // 查询业主房屋关联信息
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
        
        // 如果没有找到关联的房屋，设置roomId为-1确保查询无结果
        if (ListUtil.isNull(ownerRoomRelDtos)) {
            roomDto.setRoomId("-1");
            return;
        }
        
        // 提取所有关联的房屋ID
        List<String> roomIds = new ArrayList<>();
        for (OwnerRoomRelDto tOwnerRoomRelDto : ownerRoomRelDtos) {
            roomIds.add(tOwnerRoomRelDto.getRoomId());
        }
        
        // 设置房屋ID查询条件
        roomDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
    }

    /**
     * 计算楼栋单元房屋编号
     * 
     * 解析房间编号字符串，将其拆分为楼栋号、单元号和房间号
     * 支持格式：楼栋号-单元号-房间号
     * 
     * @param reqJson 请求参数的JSON对象
     * @param roomDto 房屋查询条件对象
     */
    private static void computeFloorUnitRoomNum(JSONObject reqJson, RoomDto roomDto) {

        // 检查是否包含房间编号参数
        if (!reqJson.containsKey("roomNum") || StringUtil.isEmpty(reqJson.getString("roomNum"))) {
            // 如果没有房间编号参数，清空相关查询条件
            roomDto.setUnitNum("");
            roomDto.setFloorNum("");
            roomDto.setRoomNum("");
            return;
        }
        
        // 按"-"分割房间编号字符串
        String[] roomNums = reqJson.getString("roomNum").split("-", 3);
        
        // 如果分割后有三部分，分别设置为楼栋号、单元号、房间号
        if (roomNums.length == 3) {
            roomDto.setFloorNum(roomNums[0]);
            roomDto.setUnitNum(roomNums[1]);
            roomDto.setRoomNum(roomNums[2]);
            return;
        }
        
        // 如果分割后不是三部分，直接设置为房间号
        roomDto.setRoomNum(reqJson.getString("roomNum"));
    }
}