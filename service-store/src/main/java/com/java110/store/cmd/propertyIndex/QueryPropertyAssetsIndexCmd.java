/**
 * 物业资产指标查询命令类
 * 
 * 该类用于查询物业资产相关的各项指标数据，包括楼栋数量、房屋数量、商铺数量、
 * 车位数量、业主数量和车辆数量等，为物业管理系统提供资产统计功能。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.propertyIndex;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 物业资产指标查询命令实现类
 * 通过服务码"propertyIndex.queryPropertyAssetsIndex"标识，用于查询小区各项资产统计信息
 */
@Java110Cmd(serviceCode = "propertyIndex.queryPropertyAssetsIndex")
public class QueryPropertyAssetsIndexCmd extends Cmd {

    /** 楼栋服务接口，用于查询楼栋相关信息 */
    @Autowired
    private IFloorInnerServiceSMO floorV1InnerServiceSMOImpl;

    /** 房屋服务接口，用于查询房屋和商铺信息 */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /** 车位服务接口，用于查询车位信息 */
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /** 业主服务接口，用于查询业主信息 */
    @Autowired
    private IOwnerV1InnerServiceSMO  ownerV1InnerServiceSMOImpl;

    /** 业主车辆服务接口，用于查询车辆信息 */
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区ID信息
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
     * 查询小区的各项资产指标数据，包括楼栋、房屋、商铺、车位、业主和车辆数量，
     * 并将结果封装到响应对象中返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建返回结果对象
        JSONObject paramOut = new JSONObject();
        
        // 查询楼栋数量
        FloorDto floorDto = new FloorDto();
        floorDto.setCommunityId(reqJson.getString("communityId"));
        int floorCount = floorV1InnerServiceSMOImpl.queryFloorsCount(floorDto);
        paramOut.put("floorCount", floorCount);

        // 查询房屋数量 - 设置房屋类型为普通房间
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(reqJson.getString("communityId"));
        roomDto.setRoomType(RoomDto.ROOM_TYPE_ROOM);
        int roomCount = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        paramOut.put("roomCount", roomCount);

        // 查询商铺数量 - 设置房屋类型为商铺
        roomDto = new RoomDto();
        roomDto.setCommunityId(reqJson.getString("communityId"));
        roomDto.setRoomType(RoomDto.ROOM_TYPE_SHOPS);
        int shopCount = roomV1InnerServiceSMOImpl.queryRoomsCount(roomDto);
        paramOut.put("shopCount", shopCount);

        // 查询车位数量
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        int spaceCount = parkingSpaceV1InnerServiceSMOImpl.queryParkingSpacesCount(parkingSpaceDto);
        paramOut.put("spaceCount", spaceCount);

        // 查询业主数量 - 设置业主类型为普通业主
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);
        int ownerCount = ownerV1InnerServiceSMOImpl.queryOwnersCount(ownerDto);
        paramOut.put("ownerCount", ownerCount);

        // 查询车辆数量 - 只查询主车辆和副车辆类型
        OwnerCarDto ownerCarDto  =  new OwnerCarDto();
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        // 设置查询的车辆类型为主车辆和副车辆
        ownerCarDto.setCarTypeCds(new String[]{OwnerCarDto.CAR_TYPE_PRIMARY, OwnerCarDto.CAR_TYPE_MEMBER});
        int carCount = ownerCarV1InnerServiceSMOImpl.queryOwnerCarsCount(ownerCarDto);
        paramOut.put("carCount", carCount);

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(paramOut));
    }
}