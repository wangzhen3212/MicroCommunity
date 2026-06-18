/**
 * 查询带销售状态的房屋信息命令类
 * 
 * 该类负责处理查询房屋信息的请求，支持分页查询，并返回包含销售状态的房屋数据
 * 主要功能包括参数验证、数据查询和结果封装
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiRoomDataVo;
import com.java110.vo.api.ApiRoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询带销售状态的房屋信息命令实现类
 * 服务编码：room.queryRoomsWithSell
 */
@Java110Cmd(serviceCode = "room.queryRoomsWithSell")
public class QueryRoomsWithSellCmd extends Cmd {

    /** 楼层内部服务接口 */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /** 房屋内部服务接口 */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;
    
    /** 最大行数限制 */
    private static final int MAX_ROW = 50;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否完整和有效，包括必填字段检查、数据类型验证和业务规则校验
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        Assert.jsonObjectHaveKey(reqJson, "page", "请求报文中未包含page节点");
        Assert.jsonObjectHaveKey(reqJson, "row", "请求报文中未包含row节点");

        // 验证数据类型
        Assert.isInteger(reqJson.getString("page"), "page不是数字");
        Assert.isInteger(reqJson.getString("row"), "row不是数字");
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        
        // 验证行数限制
        int row = Integer.parseInt(reqJson.getString("row"));
        if (row > MAX_ROW) {
            throw new SMOException(ResponseConstant.RESULT_CODE_ERROR, "row 数量不能大于50");
        }
    }

    /**
     * 执行命令主方法
     * 
     * 处理房屋查询业务逻辑，包括楼层信息处理、数据查询、结果封装和响应返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将小区楼编号转换为楼层ID并刷入请求参数
        freshFloorIdToParam(reqJson);

        // 将请求参数转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);

        // 创建API响应对象
        ApiRoomVo apiRoomVo = new ApiRoomVo();
        
        // 查询总记录数
        int total = roomInnerServiceSMOImpl.queryRoomsWithSellCount(BeanConvertUtil.covertBean(reqJson, RoomDto.class));
        apiRoomVo.setTotal(total);
        
        // 如果有数据，则查询详细房屋信息
        if (total > 0) {
            List<RoomDto> roomDtoList = roomInnerServiceSMOImpl.queryRoomsWithSell(roomDto);
            List<RoomDto> rooms = new ArrayList<>();
            
            // 处理查询结果，补充楼层信息
            for (RoomDto room : roomDtoList) {
                // 设置楼层ID和楼层编号
                room.setFloorId(reqJson.getString("floorId"));
                room.setFloorNum(reqJson.getString("floorNum"));
                rooms.add(room);
            }
            
            // 转换数据格式并设置到响应对象
            apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(rooms, ApiRoomDataVo.class));
        }
        
        // 计算总页数
        int row = reqJson.getInteger("row");
        apiRoomVo.setRecords((int) Math.ceil((double) total / (double) row));

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 将楼层编号转换为楼层ID并刷入请求参数
     * 
     * 如果请求参数中没有楼层编号，则查询默认楼层信息并设置楼层ID
     * 
     * @param reqJson 请求参数JSON对象，方法会修改此对象的floorId字段
     */
    private void freshFloorIdToParam(JSONObject reqJson) {
        // 将请求参数转换为FloorDto对象
        FloorDto floorDto = BeanConvertUtil.covertBean(reqJson, FloorDto.class);
        String floorId = "001"; // 默认楼层ID
        
        // 设置分页参数为1，避免分页影响查询结果
        floorDto.setPage(1);
        
        try {
            // 查询楼层信息
            List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);

            // 如果有查询结果，使用第一个楼层的ID
            if (floorDtos.size() > 0) {
                floorId = floorDtos.get(0).getFloorId();
            }
        } finally {
            // 无论是否成功查询到楼层信息，都设置楼层ID到请求参数中
            reqJson.put("floorId", floorId);
        }
    }
}