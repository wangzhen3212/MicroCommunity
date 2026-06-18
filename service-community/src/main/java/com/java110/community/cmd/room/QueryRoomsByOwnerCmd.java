/**
 * 查询业主房屋信息命令类
 * 
 * 该命令类用于处理查询业主房屋信息的请求，支持通过业主ID或业主姓名模糊查询房屋信息，
 * 并返回包含房屋基本信息、欠费情况等详细数据的响应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
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

@Java110CmdDoc(title = "查询业主房屋",
        description = "查询业主房屋信息",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/room.queryRoomsByOwner",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "room.queryRoomsByOwner",
        seq = 17
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page", type = "int", length = 11, remark = "页数"),
        @Java110ParamDoc(name = "row", type = "int", length = 11, remark = "行数"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "ownerId", length = 30, remark = "业主ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "records", type = "int", length = 11, remark = "总页数"),
                @Java110ParamDoc(name = "total", type = "int", length = 11, remark = "总数据"),
                @Java110ParamDoc(name = "rooms", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "rooms", name = "roomName", type = "String", remark = "房屋名称"),
                @Java110ParamDoc(parentNodeName = "rooms", name = "roomId", type = "String", remark = "房屋编号"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/room.queryRoomsByOwner?ownerId=123123&page=1&row=10&communityId=2022081539020475",
        resBody = "{\"page\":0,\"records\":1,\"rooms\":[{\"apartment\":\"10101\",\"apartmentName\":\"一室一厅\",\"builtUpArea\":\"11.00\",\"endTime\":\"2037-01-01 00:00:00\",\"feeCoefficient\":\"1.00\",\"floorId\":\"732022081690440002\",\"floorNum\":\"D\",\"idCard\":\"\",\"layer\":\"1\",\"link\":\"18909711447\",\"ownerId\":\"772022082070860017\",\"ownerName\":\"张杰\",\"remark\":\"11\",\"roomArea\":\"11.00\",\"roomAttrDto\":[{\"attrId\":\"112022082081600012\",\"listShow\":\"Y\",\"page\":-1,\"records\":0,\"roomId\":\"752022082030880010\",\"row\":0,\"specCd\":\"9035007248\",\"specName\":\"精装修\",\"statusCd\":\"0\",\"total\":0,\"value\":\"20\",\"valueName\":\"20\"}],\"roomId\":\"752022082030880010\",\"roomName\":\"D-1-1001\",\"roomNum\":\"1001\",\"roomRent\":\"0.00\",\"roomSubType\":\"110\",\"roomSubTypeName\":\"住宅\",\"roomType\":\"1010301\",\"section\":\"1\",\"startTime\":\"2022-09-03 18:50:53\",\"state\":\"2001\",\"stateName\":\"已入住\",\"unitId\":\"742022082058950007\",\"unitNum\":\"1\"}],\"rows\":0,\"total\":2}"
)
@Java110Cmd(serviceCode = "room.queryRoomsByOwner")
public class QueryRoomsByOwnerCmd extends Cmd {

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IQueryRoomStatisticsBMO queryRoomStatisticsBMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，确保必要的参数存在且不为空
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "请求中未包含communityId信息");
        //Assert.jsonObjectHaveKey(reqJson, "ownerId", "请求中未包含ownerId信息");
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        //Assert.hasLength(reqJson.getString("ownerId"), "业主ID不能为空");
        
        // 获取业主ID和业主姓名模糊查询参数
        String ownerId = reqJson.getString("ownerId");
        String ownerNameLike = reqJson.getString("ownerNameLike");
        
        // 验证至少提供一种业主查询条件（业主ID或业主姓名模糊查询）
        if (StringUtil.isEmpty(ownerId) && StringUtil.isEmpty(ownerNameLike)) {
            throw new IllegalArgumentException("未包含业主信息");
        }
    }

    /**
     * 执行查询业主房屋命令
     * 
     * 处理查询业主房屋信息的业务逻辑，包括：
     * 1. 转换请求参数为RoomDto对象
     * 2. 处理房屋编号参数
     * 3. 查询房屋信息
     * 4. 查询房屋欠费情况
     * 5. 构建并返回响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);
        
        // 处理房屋编号参数，如果存在则解析为楼层号、单元号和房间号
        hasRoomNum(reqJson, roomDto);

        // 创建响应对象
        ApiRoomVo apiRoomVo = new ApiRoomVo();
        
        // 查询业主房屋信息
        List<RoomDto> roomDtoList = roomInnerServiceSMOImpl.queryRoomsByOwner(roomDto);
        
        // 查询房屋欠费情况
        roomDtoList = queryRoomStatisticsBMOImpl.queryRoomOweFee(roomDtoList);

        // 设置响应数据
        apiRoomVo.setTotal(roomDtoList.size());  // 设置总记录数
        apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(roomDtoList, ApiRoomDataVo.class));  // 转换房屋数据列表
        apiRoomVo.setRecords(1);  // 设置总页数

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 处理房屋编号参数
     * 
     * 如果请求中包含房屋编号参数，将其解析为楼层号、单元号和房间号
     * 房屋编号格式应为：楼层号-单元号-房间号（例如：D-1-1001）
     * 
     * @param reqJson 请求的JSON数据
     * @param roomDto 房屋数据传输对象
     */
    private void hasRoomNum(JSONObject reqJson, RoomDto roomDto) {
        // 检查请求中是否包含房屋编号参数
        if (!reqJson.containsKey("roomNum")) {
            return;
        }
        
        // 获取房屋编号
        String roomNum = reqJson.getString("roomNum");
        if (StringUtil.isEmpty(roomNum)) {
            return;
        }
        
        // 按"-"分割房屋编号，格式应为：楼层号-单元号-房间号
        String[] roomNums = reqJson.getString("roomNum").split("-");

        // 如果分割后得到3个部分，则分别设置到RoomDto对象中
        if (roomNums != null && roomNums.length == 3) {
            roomDto.setFloorNum(roomNums[0]);  // 设置楼层号
            roomDto.setUnitNum(roomNums[1]);   // 设置单元号
            roomDto.setRoomNum(roomNums[2]);   // 设置房间号
        }
    }
}