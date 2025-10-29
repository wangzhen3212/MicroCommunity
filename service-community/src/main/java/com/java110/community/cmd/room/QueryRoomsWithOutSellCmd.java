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
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.ApiRoomDataVo;
import com.java110.vo.api.ApiRoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 查询未售房屋信息命令类
 * 
 * 该命令用于查询小区中未售出的房屋信息，支持分页查询和按楼层号筛选
 * 服务编码：room.queryRoomsWithOutSell
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "room.queryRoomsWithOutSell")
public class QueryRoomsWithOutSellCmd extends Cmd {

    /** 楼层内部服务接口 */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /** 房屋内部服务接口 */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /** 最大查询行数限制 */
    private static final int MAX_ROW = 50;

    /**
     * 验证请求参数的有效性
     * 
     * 验证请求中必须包含的字段：communityId、page、row，并检查其格式和取值范围
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必须包含的字段
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        Assert.jsonObjectHaveKey(reqJson, "page", "请求报文中未包含page节点");
        Assert.jsonObjectHaveKey(reqJson, "row", "请求报文中未包含row节点");

        // 验证字段格式
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
     * 执行查询未售房屋的主要业务逻辑
     * 
     * 处理查询请求，返回分页的未售房屋信息，支持按楼层筛选
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将楼层号转换为楼层ID并刷入请求参数
        freshFloorIdToParam(reqJson);

        // 将请求参数转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);

        // 创建返回结果对象
        ApiRoomVo apiRoomVo = new ApiRoomVo();
        
        // 查询总记录数
        int total = roomInnerServiceSMOImpl.queryRoomsWithOutSellCount(BeanConvertUtil.covertBean(reqJson, RoomDto.class));
        apiRoomVo.setTotal(total);
        
        // 如果有数据，则查询详细房屋信息
        if (total > 0) {
            List<RoomDto> roomDtoList = roomInnerServiceSMOImpl.queryRoomsWithOutSell(roomDto);
            // 将RoomDto列表转换为ApiRoomDataVo列表
            apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(roomDtoList, ApiRoomDataVo.class));
        }
        
        // 计算总页数
        int row = reqJson.getInteger("row");
        apiRoomVo.setRecords((int) Math.ceil((double) total / (double) row));

        // 构建并设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 将楼层号转换为楼层ID并刷入请求参数
     * 
     * 如果请求中包含floorNum参数，则根据楼层号查询对应的楼层ID，
     * 并将楼层ID设置到请求参数中供后续查询使用
     * 
     * @param reqJson 请求参数的JSON对象
     */
    private void freshFloorIdToParam(JSONObject reqJson) {
        // 检查是否包含楼层号参数
        if (!reqJson.containsKey("floorNum")) {
            return;
        }
        
        // 检查楼层号是否为空
        if (StringUtil.isEmpty(reqJson.getString("floorNum"))) {
            return;
        }
        
        // 构建楼层查询条件
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorNum(reqJson.getString("floorNum"));
        floorDto.setCommunityId(reqJson.getString("communityId"));
        floorDto.setPage(1);
        floorDto.setRow(1);
        
        // 查询楼层信息
        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);

        // 如果查询到楼层信息，则将楼层ID刷入请求参数
        if (ListUtil.isNull(floorDtos)) {
            return;
        }
        
        String floorId = floorDtos.get(0).getFloorId();
        reqJson.put("floorId", floorId);
    }
}