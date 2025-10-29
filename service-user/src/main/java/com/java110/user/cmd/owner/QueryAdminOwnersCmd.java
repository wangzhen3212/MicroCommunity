/**
 * 业主管理查询命令类
 * 
 * 该命令类用于处理管理员查询业主信息的请求，支持分页查询和房屋信息关联查询。
 * 主要功能包括：
 * 1. 验证管理员权限和分页参数
 * 2. 根据房屋信息查询对应的业主ID
 * 3. 查询当前管理员有权限的小区业主信息
 * 4. 查询业主相关的统计数据
 * 5. 返回分页格式的查询结果
 * 
 * @serviceCode owner.queryAdminOwners
 * @version 1.0
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.user.bmo.owner.IQueryOwnerStatisticsBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "owner.queryAdminOwners")
public class QueryAdminOwnersCmd extends Cmd {

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IQueryOwnerStatisticsBMO queryOwnerStatisticsBMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证管理员权限和分页参数的有效性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
        // 验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行业主查询命令
     * 
     * 主要流程：
     * 1. 根据房屋信息刷新房间ID
     * 2. 构建业主查询条件
     * 3. 获取管理员有权限的小区列表
     * 4. 查询业主总数和分页数据
     * 5. 查询业主统计数据
     * 6. 构建响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 根据房屋名称查询对应的房间ID
        freshRoomId(reqJson);
        
        // 将请求参数转换为业主查询DTO对象
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);

        // 获取当前登录用户ID
        String staffId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 查询当前用户有权限管理的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有权限管理的小区不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            ownerDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 获取每页显示条数
        int row = reqJson.getInteger("row");
        // 查询符合条件的业主总数
        int total = ownerInnerServiceSMOImpl.queryOwnersCount(ownerDto);
        
        List<OwnerDto> ownerDtos = null;
        if (total > 0) {
            // 查询业主分页数据
            ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
            // 查询业主相关的统计数据并合并到业主信息中
            ownerDtos = queryOwnerStatisticsBMOImpl.queryAdminData(ownerDtos);
        } else {
            // 如果没有数据，返回空列表
            ownerDtos = new ArrayList<>();
        }
        
        // 计算总页数并构建响应实体
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, ownerDtos);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 根据房屋名称刷新房间ID
     * 
     * 该方法用于解析房屋名称格式（楼栋-单元-房屋），查询对应的房间ID并设置到请求参数中
     * 
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当房屋名称格式不正确时抛出异常
     */
    private void freshRoomId(JSONObject reqJson) {
        // 检查请求参数中是否包含房屋名称
        if (!reqJson.containsKey("roomName")) {
            return;
        }
        
        String roomName = reqJson.getString("roomName");
        // 如果房屋名称为空，直接返回
        if (StringUtil.isEmpty(roomName)) {
            return;
        }
        
        // 验证房屋名称格式必须包含"-"分隔符
        if (!roomName.contains("-")) {
            throw new IllegalArgumentException("房屋格式错误,请写入如 楼栋-单元-房屋 格式");
        }
        
        // 按"-"分隔符分割房屋名称，最多分割3部分
        String[] params = roomName.split("-", 3);
        // 验证分割后的参数数量必须为3（楼栋、单元、房屋）
        if (params.length != 3) {
            throw new IllegalArgumentException("房屋格式错误,请写入如 楼栋-单元-房屋 格式");
        }
        
        // 构建房屋查询条件
        RoomDto roomDto = new RoomDto();
        roomDto.setFloorNum(params[0]);    // 设置楼栋号
        roomDto.setUnitNum(params[1]);     // 设置单元号
        roomDto.setRoomNum(params[2]);     // 设置房间号
        roomDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        
        // 查询符合条件的房屋信息
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        
        // 验证查询结果必须且只能有一条记录
        Assert.listOnlyOne(roomDtos, "未查询到房屋下业主信息");
        
        // 将查询到的房间ID设置到请求参数中
        reqJson.put("roomId", roomDtos.get(0).getRoomId());
    }
}