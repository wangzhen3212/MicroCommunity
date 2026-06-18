/**
 * 业主房屋关系绑定命令类
 * 
 * 该类负责处理业主与房屋的绑定关系操作，对应后台的业主入驻房屋功能。
 * 主要功能包括：验证请求参数、绑定业主房屋关系、更新房屋状态、更新业主信息等。
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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.po.owner.OwnerPo;
import com.java110.po.owner.OwnerRoomRelPo;
import com.java110.po.room.RoomPo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.ListenerUtils;

import java.text.ParseException;
import java.util.List;

@Java110CmdDoc(title = "业主房屋关系绑定",
        description = "对应后台 业主入驻房屋功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/room.sellRoom",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "room.sellRoom",
        seq = 18
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "roomId", length = 30, remark = "房屋ID"),
        @Java110ParamDoc(name = "state", length = 12, remark = "状态 2001\t已入住\t\n" +
                "2003\t已交房\t\n" +
                "2005\t已装修\t\n" +
                "2004\t未入住\t\n" +
                "2008\t空闲\n" +
                "2009\t装修中\t"),
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
                "\t\"state\": \"2001\",\n" +
                "\t\"roomId\": \"123123\",\n" +
                "\t\"communityId\": \"2022121921870161\"\n" +
                "}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "room.sellRoom")
public class SellRoomCmd extends Cmd {

    // 单元内部服务接口
    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    // 小区内部服务接口
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    // 业主房屋关系内部服务接口
    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    // 房屋内部服务接口
    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    // 业主内部服务接口
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证请求参数的有效性，包括必填字段检查、字段非空检查以及房屋是否已绑定业主的检查。
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查必填字段是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "ownerId", "请求报文中未包含ownerId节点");
        Assert.jsonObjectHaveKey(reqJson, "roomId", "请求报文中未包含roomId节点");
        Assert.jsonObjectHaveKey(reqJson, "state", "请求报文中未包含state节点");
        //  Assert.jsonObjectHaveKey(reqJson, "storeId", "请求报文中未包含storeId节点");

        // 检查字段值是否为空
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        Assert.hasLength(reqJson.getString("ownerId"), "ownerId不能为空");
        Assert.hasLength(reqJson.getString("roomId"), "roomId不能为空");
        Assert.hasLength(reqJson.getString("state"), "state不能为空");

        // 检查房屋是否已经绑定业主
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setRoomId(reqJson.getString("roomId"));
        ownerRoomRelDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
        
        // 如果房屋已经绑定业主，则抛出异常
        if(!ListUtil.isNull(ownerRoomRelDtos)){
            throw new CmdException("房屋已经绑定业主");
        }
    }

    /**
     * 执行业主房屋绑定命令
     * 
     * 该方法负责处理业主房屋绑定的主要业务逻辑，包括设置默认时间、绑定房屋关系、更新房屋状态和更新业主信息。
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行失败时抛出异常
     * @throws ParseException 当时间解析失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 设置默认开始时间（当前时间）
        if (!reqJson.containsKey("startTime")) {
            reqJson.put("startTime", DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        }
        // 设置默认结束时间（未来时间）
        if (!reqJson.containsKey("endTime")) {
            reqJson.put("endTime", "2037-01-01 00:00:00");
        }
        
        // 绑定业主房屋关系
        sellRoom(reqJson);

        // 更新房屋状态为已售出
        updateShellRoom(reqJson);

        // 更新业主信息，触发databus同步，确保业主房屋上费用的业主名称正确
        updateOwner(reqJson);
    }

    /**
     * 绑定业主房屋关系
     * 
     * 该方法用于创建业主与房屋的绑定关系，生成关系ID并保存到数据库。
     * 
     * @param paramInJson 接口调用方传入的参数
     */
    public void sellRoom(JSONObject paramInJson) {
        // 创建业务数据对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.putAll(paramInJson);
        
        // 生成关系ID并设置默认用户ID
        businessUnit.put("relId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
        businessUnit.put("userId", "-1");
        
        // 转换为PO对象并保存
        OwnerRoomRelPo ownerRoomRelPo = BeanConvertUtil.covertBean(businessUnit, OwnerRoomRelPo.class);
        int flag = ownerRoomRelV1InnerServiceSMOImpl.saveOwnerRoomRel(ownerRoomRelPo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("添加业主房屋关系");
        }
    }

    /**
     * 更新房屋状态
     * 
     * 该方法用于更新房屋的状态信息，将房屋状态标记为已售出。
     * 
     * @param paramInJson 接口调用方传入的参数
     */
    public void updateShellRoom(JSONObject paramInJson) {
        // 创建业务数据对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.putAll(paramInJson);
        businessUnit.put("userId", "-1");
        
        // 转换为PO对象并更新
        RoomPo roomPo = BeanConvertUtil.covertBean(businessUnit, RoomPo.class);
        int flag = roomV1InnerServiceSMOImpl.updateRoom(roomPo);
        
        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("添加业主房屋关系");
        }
    }

    /**
     * 更新业主信息
     * 
     * 该方法用于更新业主信息，主要目的是触发databus同步机制，
     * 确保业主房屋上相关费用的业主名称信息正确同步。
     * 
     * @param reqJson 请求的JSON数据
     */
    private void updateOwner(JSONObject reqJson) {
        // 查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(reqJson.getString("ownerId"));
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
        
        // 验证业主存在且唯一
        Assert.listOnlyOne(ownerDtos, "业主不存在");
        
        // 转换为PO对象并更新
        OwnerPo ownerPo = BeanConvertUtil.covertBean(ownerDtos.get(0), OwnerPo.class);
        ownerV1InnerServiceSMOImpl.updateOwner(ownerPo);
    }

}