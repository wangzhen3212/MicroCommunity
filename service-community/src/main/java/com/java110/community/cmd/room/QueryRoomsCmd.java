package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.community.bmo.room.IQueryRoomStatisticsBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.unit.UnitDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.data.DataPrivilegeStaffDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.community.*;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.*;
import com.java110.vo.api.ApiRoomDataVo;
import com.java110.vo.api.ApiRoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 房屋查询命令类
 * 负责处理房屋信息的查询请求，包括房屋基本信息、业主信息、权限控制等
 * 支持分页查询、条件筛选、数据权限控制等功能
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "查询房屋",
        description = "查询房屋信息",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/room.queryRooms",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "room.queryRooms",
        seq = 16
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page", type = "int", length = 11, remark = "页数"),
        @Java110ParamDoc(name = "row", type = "int", length = 11, remark = "行数"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "roomId", length = 30, remark = "房屋ID"),
        @Java110ParamDoc(name = "floorId", length = 30, remark = "楼栋ID"),
        @Java110ParamDoc(name = "unitId", length = 30, remark = "单元ID"),
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
        reqBody = "http://{ip}:{port}/app/room.queryRooms?floorId=&floorName=&unitId=&roomNum=&roomId=&state=&section=&roomType=1010301&roomSubType=&flag=0&page=1&row=10&communityId=2022081539020475",
        resBody = "{\"page\":0,\"records\":1,\"rooms\":[{\"apartment\":\"10101\",\"apartmentName\":\"一室一厅\",\"builtUpArea\":\"11.00\",\"endTime\":\"2037-01-01 00:00:00\",\"feeCoefficient\":\"1.00\",\"floorId\":\"732022081690440002\",\"floorNum\":\"D\",\"idCard\":\"\",\"layer\":\"1\",\"link\":\"18909711447\",\"ownerId\":\"772022082070860017\",\"ownerName\":\"张杰\",\"remark\":\"11\",\"roomArea\":\"11.00\",\"roomAttrDto\":[{\"attrId\":\"112022082081600012\",\"listShow\":\"Y\",\"page\":-1,\"records\":0,\"roomId\":\"752022082030880010\",\"row\":0,\"specCd\":\"9035007248\",\"specName\":\"精装修\",\"statusCd\":\"0\",\"total\":0,\"value\":\"20\",\"valueName\":\"20\"}],\"roomId\":\"752022082030880010\",\"roomName\":\"D-1-1001\",\"roomNum\":\"1001\",\"roomRent\":\"0.00\",\"roomSubType\":\"110\",\"roomSubTypeName\":\"住宅\",\"roomType\":\"1010301\",\"section\":\"1\",\"startTime\":\"2022-09-03 18:50:53\",\"state\":\"2001\",\"stateName\":\"已入住\",\"unitId\":\"742022082058950007\",\"unitNum\":\"1\"}],\"rows\":0,\"total\":2}"
)
@Java110Cmd(serviceCode = "room.queryRooms")
public class QueryRoomsCmd extends Cmd {

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IDataPrivilegeUnitV1InnerServiceSMO dataPrivilegeUnitV1InnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    @Autowired
    private IQueryRoomStatisticsBMO queryRoomStatisticsBMOImpl;

    /** 最大查询行数限制 */
    protected static final int MAX_ROW = 10000;

    /**
     * 参数验证方法
     * 验证请求参数的合法性，包括必填项检查、分页参数验证、小区楼栋关系验证等
     *
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws SMOException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validateProperty(cmdDataFlowContext);
        // 验证必填参数communityId
        Assert.hasKeyAndValue(reqJson, "communityId", "请求中未包含communityId信息");
        //Assert.jsonObjectHaveKey(reqJson, "floorId", "请求中未包含floorId信息");
        super.validatePageInfo(reqJson);
        int row = Integer.parseInt(reqJson.getString("row"));

        // 验证行数是否超过最大限制
        if (row > MAX_ROW) {
            throw new SMOException(ResponseConstant.RESULT_CODE_ERROR, "row 数量不能大于50");
        }
        // 校验小区楼ID和小区是否有对应关系
        int total = floorInnerServiceSMOImpl.queryFloorsCount(BeanConvertUtil.covertBean(reqJson, FloorDto.class));

        // 如果传入了floorId但查询不到对应楼栋，说明楼栋ID不合法
        if (!StringUtil.isEmpty(reqJson.getString("floorId")) && total < 1) {
            throw new IllegalArgumentException("传入小区楼ID不是该小区的楼");
        }
    }

    /**
     * 执行房屋查询命令
     * 处理房屋查询业务逻辑，包括数据权限控制、房屋信息查询、业主信息关联等
     *
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为RoomDto对象
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class);

        // 获取员工ID并查询数据权限
        String staffId = cmdDataFlowContext.getReqHeaders().get("user-id");
        DataPrivilegeStaffDto dataPrivilegeStaffDto = new DataPrivilegeStaffDto();
        dataPrivilegeStaffDto.setStaffId(staffId);
        // 查询员工有权限访问的单元ID列表
        String[] unitIds = dataPrivilegeUnitV1InnerServiceSMOImpl.queryDataPrivilegeUnitsByStaff(dataPrivilegeStaffDto);

        // 根据不同的查询模式计算房屋ID和单元ID
        computeRoomIdAndUnitId(reqJson, unitIds, roomDto);

        // 处理楼栋-单元-房屋编号格式的查询
        computeFloorUnitRoomNum(reqJson, roomDto);

        // 处理商铺类型的特殊查询格式
        computeRoomShopFloorUnitRoomNum(reqJson, roomDto);

        // 处理房屋状态参数，支持多状态查询
        String roomState = reqJson.getString("roomState");
        if (!StringUtil.isEmpty(roomState)) {
            roomDto.setStates(roomState.split(","));
        }

        ApiRoomVo apiRoomVo = new ApiRoomVo();

        // 如果员工有数据权限限制，设置可查询的单元ID列表
        if (unitIds != null && unitIds.length > 0) {
            roomDto.setUnitIds(unitIds);
        }
        // 查询总记录数
        int total = roomInnerServiceSMOImpl.queryRoomsCount(roomDto);
        apiRoomVo.setTotal(total);
        List<RoomDto> roomDtoList = null;
        if (total > 0) {
            // 查询房屋列表数据
            roomDtoList = roomInnerServiceSMOImpl.queryRooms(roomDto);
            // 刷新房屋业主信息
            refreshRoomOwners(reqJson.getString("loginUserId"), reqJson.getString("communityId"), roomDtoList);
            // 查询房屋统计数据
            roomDtoList = queryRoomStatisticsBMOImpl.query(roomDtoList);
        } else {
            roomDtoList = new ArrayList<>();
        }
        // 转换数据格式并设置返回结果
        apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(roomDtoList, ApiRoomDataVo.class));
        int row = reqJson.getInteger("row");
        // 计算总页数
        apiRoomVo.setRecords((int) Math.ceil((double) total / (double) row));

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 处理商铺类型的房屋编号格式
     * 商铺的房屋编号格式为"楼层-房间号"，需要特殊处理
     *
     * @param reqJson 请求参数JSON对象
     * @param roomDto 房屋数据传输对象
     */
    private static void computeRoomShopFloorUnitRoomNum(JSONObject reqJson, RoomDto roomDto) {
        // 如果不是商铺类型，直接返回
        if (!reqJson.containsKey("roomType") || !RoomDto.ROOM_TYPE_SHOPS.equals(reqJson.getString("roomType"))) {
            return;
        }
        // 如果没有房屋编号参数，清空相关字段
        if (!reqJson.containsKey("roomNum") || StringUtil.isEmpty(reqJson.getString("roomNum"))) {
            roomDto.setUnitNum("");
            roomDto.setFloorNum("");
            roomDto.setRoomNum("");
            return;
        }
        // 按"-"分割房屋编号，处理商铺格式
        String[] roomNums = reqJson.getString("roomNum").split("-", 2);
        if (roomNums != null && roomNums.length == 2) {
            // 商铺格式：楼层-房间号，单元号固定为0
            roomDto.setFloorNum(roomNums[0]);
            roomDto.setUnitNum("0");
            roomDto.setRoomNum(roomNums[1]);
        } else {
            roomDto.setRoomNum(reqJson.getString("roomNum"));
        }
    }

    /**
     * 计算楼栋单元房屋编号
     * 处理标准格式的房屋编号：楼栋号-单元号-房间号
     *
     * @param reqJson 请求参数JSON对象
     * @param roomDto 房屋数据传输对象
     */
    private static void computeFloorUnitRoomNum(JSONObject reqJson, RoomDto roomDto) {

        // 检查是否启用了编号解析模式
        if (!reqJson.containsKey("flag") || !"1".equals(reqJson.getString("flag"))) {
            return;
        }
        // 如果没有房屋编号，清空相关字段
        if (!reqJson.containsKey("roomNum") || StringUtil.isEmpty(reqJson.getString("roomNum"))) {
            roomDto.setUnitNum("");
            roomDto.setFloorNum("");
            roomDto.setRoomNum("");
            return;
        }
        // 按"-"分割房屋编号，处理标准格式
        String[] roomNums = reqJson.getString("roomNum").split("-", 3);
        if (roomNums != null && roomNums.length == 3) {
            // 标准格式：楼栋号-单元号-房间号
            roomDto.setFloorNum(roomNums[0]);
            roomDto.setUnitNum(roomNums[1]);
            roomDto.setRoomNum(roomNums[2]);
            return;
        }
        // 如果不是标准格式，直接使用原始房屋编号
        roomDto.setRoomNum(reqJson.getString("roomNum"));
    }

    /**
     * 根据楼栋号、单元号、房间号计算房屋ID和单元ID
     * 用于通过房屋位置信息反向查找房屋ID的场景
     *
     * @param reqJson 请求参数JSON对象
     * @param unitIds 员工有权限的单元ID数组
     * @param roomDto 房屋数据传输对象
     */
    private void computeRoomIdAndUnitId(JSONObject reqJson, String[] unitIds, RoomDto roomDto) {

        // 检查是否启用了ID计算模式
        if (!reqJson.containsKey("flag") || !"0".equals(reqJson.getString("flag"))) {
            return;
        }

        // 验证必要的参数是否存在
        if (!reqJson.containsKey("floorNum") || StringUtil.isEmpty(reqJson.getString("floorNum"))) {
            return;
        }

        if (!reqJson.containsKey("unitNum") || StringUtil.isEmpty(reqJson.getString("unitNum"))) {
            return;
        }

        if (!reqJson.containsKey("roomNum") || StringUtil.isEmpty(reqJson.getString("roomNum"))) {
            return;
        }

        // 根据楼栋号查询楼栋信息
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorNum(reqJson.getString("floorNum"));
        floorDto.setCommunityId(reqJson.getString("communityId"));
        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);
        if (ListUtil.isNull(floorDtos)) {
            return;
        }

        String roomId = "";
        String unitId = "";
        List<UnitDto> unitDtos = null;
        // 遍历查询到的楼栋，查找对应的单元和房屋
        for (FloorDto floor : floorDtos) {
            UnitDto unitDto = new UnitDto();
            unitDto.setFloorId(floor.getFloorId());
            unitDto.setUnitNum(reqJson.getString("unitNum"));
            // 如果员工有权限限制，设置权限单元ID
            if (unitIds != null && unitIds.length > 0) {
                unitDto.setUnitIds(unitIds);
            }
            unitDtos = unitInnerServiceSMOImpl.queryUnits(unitDto);
            if (unitDtos == null || unitDtos.isEmpty()) {
                continue;
            }
            // 在找到的单元中查找对应的房屋
            for (UnitDto unit : unitDtos) {
                RoomDto room = new RoomDto();
                room.setUnitId(unit.getUnitId());
                room.setRoomNum(reqJson.getString("roomNum"));
                room.setCommunityId(reqJson.getString("communityId"));
                List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(room);
                if (roomDtos == null || roomDtos.isEmpty()) {
                    continue;
                }
                // 找到对应的房屋，记录ID信息
                unitId = roomDtos.get(0).getUnitId();
                roomId = roomDtos.get(0).getRoomId();
            }
        }
        // 设置查询到的房屋ID和单元ID
        roomDto.setRoomId(roomId);
        roomDto.setUnitId(unitId);
    }


    /**
     * 刷新房屋业主信息
     * 为房屋列表关联业主信息，并进行敏感信息脱敏处理
     *
     * @param userId 当前用户ID，用于权限检查
     * @param communityId 小区ID
     * @param roomDtos 房屋数据传输对象列表
     */
    private void refreshRoomOwners(String userId, String communityId, List<RoomDto> roomDtos) {

        /**
         * 性能优化：当房屋数量过多时，不查询业主信息
         * 避免大数据量查询导致的性能问题
         */
        if (roomDtos == null || roomDtos.size() > 20) {
            return;
        }
        // 收集所有房屋ID
        List<String> roomIds = new ArrayList<>();
        for (RoomDto roomDto : roomDtos) {
            roomIds.add(roomDto.getRoomId());
        }
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(communityId);
        ownerDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
        // 检查用户是否有查看完整业主信息的权限
        List<Map> mark = getPrivilegeOwnerList("/roomCreateFee", userId);
        // 查询房屋对应的业主信息
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnersByRoom(ownerDto);
        // 为每个房屋关联业主信息
        for (RoomDto roomDto : roomDtos) {
            for (OwnerDto tmpOwnerDto : ownerDtos) {
                // 匹配房屋ID
                if (!roomDto.getRoomId().equals(tmpOwnerDto.getRoomId())) {
                    continue;
                }
                try {
                    // 转换时间格式
                    roomDto.setStartTime(DateUtil.getDateFromString(tmpOwnerDto.getStartTime(), DateUtil.DATE_FORMATE_STRING_A));
                    roomDto.setEndTime(DateUtil.getDateFromString(tmpOwnerDto.getEndTime(), DateUtil.DATE_FORMATE_STRING_A));
                } catch (Exception e) {
                    // 时间格式转换异常，忽略继续处理
                }
                // 设置业主基本信息
                roomDto.setOwnerId(tmpOwnerDto.getOwnerId());
                roomDto.setOwnerName(tmpOwnerDto.getName());
                // 对业主身份证号进行脱敏处理
                String idCard = tmpOwnerDto.getIdCard();
                // 如果没有查看完整信息的权限，进行脱敏
                if (mark.size() == 0 && idCard != null && !idCard.equals("") && idCard.length() > 15) {
                    idCard = idCard.substring(0, 6) + "**********" + idCard.substring(16);
                }
                // 对业主手机号进行脱敏处理
                String link = tmpOwnerDto.getLink();
                roomDto.setOwnerTel(link); // 程序内部使用完整手机号，用于统计查询
                if (mark.size() == 0 && link != null && !link.equals("") && link.length() > 10) {
                    link = link.substring(0, 3) + "****" + link.substring(7);
                }
                // 设置脱敏后的信息
                roomDto.setIdCard(idCard);
                roomDto.setLink(link);
            }
        }
    }

    /**
     * 检查用户对指定资源是否有权限
     * 用于敏感信息脱敏的权限控制
     *
     * @param resource 资源路径
     * @param userId 用户ID
     * @return 权限列表，如果有权限则返回非空列表
     */
    public List<Map> getPrivilegeOwnerList(String resource, String userId) {
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource(resource);
        basePrivilegeDto.setUserId(userId);
        // 查询用户对指定资源的权限
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        return privileges;
    }
}