/**
 * 业主房屋关系解绑命令类
 * 
 * 该命令类实现了业主退房功能，主要处理业主与房屋关系的解绑操作。
 * 包括验证请求参数、查询业主房屋关系、删除关系记录、更新房屋状态等业务逻辑。
 * 对应后台服务的"room.exitRoom"功能。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.room.RoomDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.po.owner.OwnerRoomRelPo;
import com.java110.po.room.RoomPo;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110CmdDoc(title = "业主房屋关系解绑",
        description = "对应后台 业主退房房屋功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/room.exitRoom",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "room.exitRoom",
        seq = 19
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "roomId", length = 30, remark = "房屋ID"),
        @Java110ParamDoc(name = "ownerId", length = 30, remark = "业主ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\n" +
                "\t\"ownerId\": 121231,\n" +
                "\t\"roomId\": \"123123\",\n" +
                "\t\"communityId\": \"2022121921870161\"\n" +
                "}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "room.exitRoom")
public class ExitRoomCmd extends Cmd {

    /**
     * 业主房屋关系内部服务接口
     */
    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    /**
     * 房屋内部服务接口
     */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 费用内部服务接口
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 小区内部服务接口
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 业主房屋关系V1版本内部服务接口
     */
    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    /**
     * 房屋V1版本内部服务接口
     */
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整且有效，确保必要的参数都存在且不为空
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含的关键字段
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "ownerId", "请求报文中未包含ownerId节点");
        Assert.jsonObjectHaveKey(reqJson, "roomId", "请求报文中未包含roomId节点");
        // Assert.jsonObjectHaveKey(reqJson, "storeId", "请求报文中未包含storeId节点");

        // 验证关键字段的值不能为空
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        Assert.hasLength(reqJson.getString("ownerId"), "ownerId不能为空");
        Assert.hasLength(reqJson.getString("roomId"), "roomId不能为空");
        // Assert.hasLength(reqJson.getString("storeId"), "storeId不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 处理业主退房的核心业务逻辑，包括：
     * 1. 查询业主房屋关系
     * 2. 删除业主房屋关系
     * 3. 更新房屋状态
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务逻辑执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 根据ownerId和roomId查询业主房屋关系
        OwnerRoomRelDto ownerRoomRelDto = BeanConvertUtil.covertBean(reqJson, OwnerRoomRelDto.class);
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        // 验证查询结果的有效性
        if (ownerRoomRelDtos == null || ownerRoomRelDtos.size() < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "数据存在问题，业主和房屋对应关系不是一条");
        }

        // 验证关系ID是否存在
        if (StringUtil.isEmpty(ownerRoomRelDtos.get(0).getRelId())) {
            throw new CmdException("未包含关系");
        }

        // 准备删除业主房屋关系的业务数据
        JSONObject businessUnit = new JSONObject();
        // businessUnit.putAll(paramInJson);
        businessUnit.put("relId", ownerRoomRelDtos.get(0).getRelId());
        // businessUnit.put("userId", dataFlowContext.getRequestCurrentHeaders().get(CommonConstant.HTTP_USER_ID));
        
        // 转换业务数据为PO对象并执行删除操作
        OwnerRoomRelPo roomPo = BeanConvertUtil.covertBean(businessUnit, OwnerRoomRelPo.class);
        roomPo.setOwnerId(ownerRoomRelDtos.get(0).getOwnerId());
        int flag = ownerRoomRelV1InnerServiceSMOImpl.deleteOwnerRoomRel(roomPo);

        // 验证删除操作是否成功
        if (flag < 1) {
            throw new IllegalArgumentException("删除业主房屋关系失败");
        }

        // 查询房屋信息以更新房屋状态
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(reqJson.getString("roomId"));
        roomDto.setCommunityId(reqJson.getString("communityId"));
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 验证房屋信息唯一性
        Assert.listOnlyOne(roomDtos, "房屋或商铺不存在");
        
        // 根据房屋类型设置不同的状态值
        if (RoomDto.ROOM_TYPE_SHOPS.equals(roomDtos.get(0).getRoomType())) {
            // 商铺类型设置为空闲状态
            reqJson.put("state", RoomDto.STATE_SHOP_FREE);
        } else {
            // 普通房屋设置为2002状态（空闲状态）
            reqJson.put("state", "2002");
        }

        // 再次验证房屋信息唯一性
        Assert.listOnlyOne(roomDtos, "存在" + roomDtos.size() + "条房屋信息");

        // 准备更新房屋状态的业务数据
        businessUnit = new JSONObject();
        businessUnit.putAll(BeanConvertUtil.beanCovertMap(roomDtos.get(0)));
        businessUnit.putAll(reqJson);
        
        // 转换业务数据为PO对象并执行更新操作
        RoomPo tmpRoomPo = BeanConvertUtil.covertBean(businessUnit, RoomPo.class);
        flag = roomV1InnerServiceSMOImpl.updateRoom(tmpRoomPo);

        // 验证更新操作是否成功
        if (flag < 1) {
            throw new IllegalArgumentException("更新房屋状态");
        }
    }
}