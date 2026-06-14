/**
 * 首页统计查询命令类
 * 
 * 该类用于处理首页统计信息的查询请求，包括业主、房屋、停车位、商铺等各类数据的统计
 * 通过调用不同的内部服务接口获取统计数据，并返回给前端展示
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.index;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiIndexStatisticVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "index.queryIndexStatistic")
public class QueryIndexStatisticCmd extends Cmd {

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;
    
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的communityId参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含communityId参数
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
    }

    /**
     * 执行统计查询命令
     * 
     * 该方法负责查询各类统计信息，包括：
     * 1. 业主统计（总业主数、未入住业主数）
     * 2. 房屋统计（总房屋数、空闲房屋数）
     * 3. 停车位统计（总停车位数、空闲停车位数）
     * 4. 商铺统计（总商铺数、空闲商铺数）
     * 5. 商户信息查询
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 从请求头中获取商户ID
        String storeId = context.getReqHeaders().get("store-id");
        
        // 查询商户信息
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setPage(1);
        storeDto.setRow(1);
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 验证商户存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");
        
        // 初始化统计VO对象
        ApiIndexStatisticVo apiIndexStatisticVo = new ApiIndexStatisticVo();
        
        // 查询业主统计信息
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);
        // 总业主数量
        int ownerCount = ownerInnerServiceSMOImpl.queryOwnersCount(ownerDto);
        // 未入住业主数量
        int noEnterRoomOwnerCount = ownerInnerServiceSMOImpl.queryNoEnterRoomOwnerCount(ownerDto);
        
        // 查询房屋统计信息
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        // 设置房屋类型为普通房屋
        roomDto.setRoomType(RoomDto.ROOM_TYPE_ROOM);
        // 总房屋数量
        int roomCount = roomInnerServiceSMOImpl.queryRoomsCount(roomDto);
        // 空闲房屋数量（未售出）
        int freeRoomCount = roomInnerServiceSMOImpl.queryRoomsWithOutSellCount(roomDto);
        
        // 查询停车位统计信息
        // 总停车位数量
        int parkingSpaceCount = parkingSpaceInnerServiceSMOImpl.queryParkingSpacesCount(BeanConvertUtil.covertBean(reqJson, ParkingSpaceDto.class));
        ParkingSpaceDto parkingSpaceDto = BeanConvertUtil.covertBean(reqJson, ParkingSpaceDto.class);
        // 设置停车位状态为空闲（F）
        parkingSpaceDto.setState("F");
        // 空闲停车位数量
        int freeParkingSpaceCount = parkingSpaceInnerServiceSMOImpl.queryParkingSpacesCount(parkingSpaceDto);
        
        // 查询商铺统计信息
        roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        // 设置房屋类型为商铺
        roomDto.setRoomType(RoomDto.ROOM_TYPE_SHOPS);
        // 总商铺数量
        int shopCount = roomInnerServiceSMOImpl.queryRoomsCount(roomDto);
        // 空闲商铺数量（未售出）
        int freeShopCount = roomInnerServiceSMOImpl.queryRoomsWithOutSellCount(roomDto);

        // 设置统计结果到VO对象
        apiIndexStatisticVo.setOwnerCount(ownerCount + "");
        apiIndexStatisticVo.setNoEnterRoomCount(noEnterRoomOwnerCount + "");
        apiIndexStatisticVo.setRoomCount(roomCount + "");
        apiIndexStatisticVo.setFreeRoomCount(freeRoomCount + "");
        apiIndexStatisticVo.setParkingSpaceCount(parkingSpaceCount + "");
        apiIndexStatisticVo.setFreeParkingSpaceCount(freeParkingSpaceCount + "");
        apiIndexStatisticVo.setShopCount(shopCount + "");
        apiIndexStatisticVo.setFreeShopCount(freeShopCount + "");
        // 设置商户类型编码
        apiIndexStatisticVo.setStoreTypeCd(storeDtos.get(0).getStoreTypeCd());
        
        // 构建响应实体并设置到上下文
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiIndexStatisticVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}