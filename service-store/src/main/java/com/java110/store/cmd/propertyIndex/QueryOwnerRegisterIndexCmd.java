/**
 * 业主注册指标查询命令类
 * 
 * 该类用于查询小区业主注册相关的统计指标，包括：
 * - 未注册业主数量
 * - 已注册业主数量  
 * - 空闲房屋数量
 * - 已绑定房屋数量
 * 
 * 通过调用业主服务和房屋服务获取相关统计数据，用于物业管理系统中的指标展示
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.propertyIndex;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "propertyIndex.queryOwnerRegisterIndex")
public class QueryOwnerRegisterIndexCmd extends Cmd {

    /**
     * 业主服务接口，用于查询业主相关信息
     */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;
    
    /**
     * 房屋服务接口，用于查询房屋相关信息
     */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 
     * 查询业主注册相关的统计指标，包括：
     * - 未注册业主数量
     * - 已注册业主数量
     * - 空闲房屋数量
     * - 已绑定房屋数量
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建返回结果对象
        JSONObject paramOut = new JSONObject();

        // 查询未注册业主数量
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setIsBind(OwnerDto.IS_BIND_N); // 设置查询条件为未绑定状态
        int unbindCount = ownerV1InnerServiceSMOImpl.queryOwnersBindCount(ownerDto);
        paramOut.put("unbindCount", unbindCount);

        // 查询已注册业主数量
        ownerDto = new OwnerDto();
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setIsBind(OwnerDto.IS_BIND_Y); // 设置查询条件为已绑定状态
        int bindCount = ownerV1InnerServiceSMOImpl.queryOwnersBindCount(ownerDto);
        paramOut.put("bindCount", bindCount);

        // 查询空闲房屋数量（未绑定的房屋）
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(reqJson.getString("communityId"));
        roomDto.setRoomType(RoomDto.ROOM_TYPE_ROOM); // 设置房屋类型为住宅
        roomDto.setState(RoomDto.STATE_FREE); // 设置房屋状态为空闲
        int unbindRoomCount = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        paramOut.put("unbindRoomCount", unbindRoomCount);

        // 查询所有房屋数量
        roomDto = new RoomDto();
        roomDto.setCommunityId(reqJson.getString("communityId"));
        roomDto.setRoomType(RoomDto.ROOM_TYPE_ROOM); // 设置房屋类型为住宅
        int allRoomCount = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        
        // 计算已绑定房屋数量 = 总房屋数量 - 空闲房屋数量
        paramOut.put("bindRoomCount", allRoomCount - unbindRoomCount);

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(paramOut));
    }
}