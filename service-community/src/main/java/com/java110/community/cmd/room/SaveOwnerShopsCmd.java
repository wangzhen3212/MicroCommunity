/**
 * 商铺业主关系保存命令类
 * 
 * 该命令类用于处理商铺与业主关系的保存操作，主要功能包括：
 * 1. 验证请求参数的完整性
 * 2. 创建或更新业主信息
 * 3. 校验商铺状态是否允许操作
 * 4. 处理商铺与业主的关系绑定
 * 5. 更新商铺状态信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.room;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.room.RoomDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.po.owner.OwnerPo;
import com.java110.po.owner.OwnerRoomRelPo;
import com.java110.po.room.RoomPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "room.saveOwnerShops")
public class SaveOwnerShopsCmd extends Cmd {

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 
     * 检查请求报文中是否包含必要的参数节点，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "roomId", "请求报文中未包含roomId节点");
        Assert.jsonObjectHaveKey(reqJson, "ownerName", "请求报文中未包含ownerName节点");
        Assert.jsonObjectHaveKey(reqJson, "tel", "请求报文中未包含tel节点");
        Assert.jsonObjectHaveKey(reqJson, "startTime", "请求报文中未包含tel节点");
        Assert.jsonObjectHaveKey(reqJson, "endTime", "请求报文中未包含tel节点");
        Assert.jsonObjectHaveKey(reqJson, "shopsState", "请求报文中未包含状态节点");
    }

    /**
     * 执行命令方法
     * 
     * 主要业务流程：
     * 1. 创建或更新业主信息
     * 2. 验证商铺状态是否允许操作
     * 3. 处理已有的业主商铺关系
     * 4. 创建新的业主商铺关系
     * 5. 更新商铺状态
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);
        int flag = 0; // 操作结果标志
        
        // 检查业主ID是否存在或有效，如果不存在则创建新业主
        if (!reqJson.containsKey("ownerId")
                || reqJson.getString("ownerId").startsWith("-")
                || StringUtil.isEmpty(reqJson.getString("ownerId"))
        ) {
            // 创建业主信息对象
            OwnerPo ownerPo = new OwnerPo();
            ownerPo.setUserId(userId);
            ownerPo.setAge("1"); // 默认年龄
            ownerPo.setCommunityId(reqJson.getString("communityId"));
            ownerPo.setIdCard(""); // 身份证号，默认为空
            ownerPo.setLink(reqJson.getString("tel")); // 联系电话
            ownerPo.setSex("1"); // 默认性别
            ownerPo.setMemberId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ownerId)); // 生成成员ID
            ownerPo.setName(reqJson.getString("ownerName")); // 业主姓名
            ownerPo.setOwnerId(ownerPo.getMemberId()); // 业主ID和成员ID保持一致
            ownerPo.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER); // 业主类型编码
            ownerPo.setRemark(reqJson.getString("remark")); // 备注信息
            ownerPo.setState("2000"); // 业主状态
            ownerPo.setPersonType(OwnerDto.PERSON_TYPE_PERSON); // 人员类型
            ownerPo.setPersonRole(OwnerDto.PERSON_ROLE_OWNER); // 人员角色
            
            // 保存业主信息
            flag = ownerV1InnerServiceSMOImpl.saveOwner(ownerPo);
            if (flag < 1) {
                throw new IllegalArgumentException("保存业主失败");
            }
            // 将新创建的业主ID设置到请求参数中
            reqJson.put("ownerId", ownerPo.getOwnerId());
        }

        // 查询商铺信息，验证商铺是否存在且状态允许操作
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(reqJson.getString("roomId")); // 设置商铺ID
        roomDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 验证商铺存在且唯一
        Assert.listOnlyOne(roomDtos, "商铺不存在");

        // 验证商铺状态是否为可操作状态（2006:空闲, 2008:出租）
        if (!"2006,2008".contains(roomDtos.get(0).getState())) {
            throw new IllegalArgumentException("当前商铺状态不允许操作");
        }

        // 检查商铺是否已有租客关系
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setRoomId(reqJson.getString("roomId")); // 设置商铺ID
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        // 如果存在已有的业主商铺关系，则先删除
        if (!ListUtil.isNull(ownerRoomRelDtos)) {
            JSONObject businessUnit = new JSONObject();
            businessUnit.put("relId", ownerRoomRelDtos.get(0).getRelId()); // 设置关系ID
            OwnerRoomRelPo roomPo = BeanConvertUtil.covertBean(businessUnit, OwnerRoomRelPo.class); // 转换为PO对象
            flag = ownerRoomRelV1InnerServiceSMOImpl.deleteOwnerRoomRel(roomPo); // 删除关系
            if (flag < 1) {
                throw new IllegalArgumentException("删除已有房屋关系失败");
            }
        }

        // 创建新的业主商铺关系
        reqJson.put("state", "2001"); // 设置关系状态
        JSONObject businessUnit = new JSONObject();
        businessUnit.putAll(reqJson); // 复制所有请求参数
        businessUnit.put("relId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId)); // 生成关系ID
        businessUnit.put("userId", "-1"); // 设置用户ID
        OwnerRoomRelPo ownerRoomRelPo = BeanConvertUtil.covertBean(businessUnit, OwnerRoomRelPo.class); // 转换为PO对象
        ownerRoomRelV1InnerServiceSMOImpl.saveOwnerRoomRel(ownerRoomRelPo); // 保存关系
        
        // 更新商铺状态信息
        reqJson.put("state", reqJson.getString("shopsState")); // 设置商铺状态
        businessUnit = new JSONObject();
        businessUnit.putAll(reqJson); // 复制所有请求参数
        businessUnit.put("userId", "-1"); // 设置用户ID
        RoomPo roomPo = BeanConvertUtil.covertBean(businessUnit, RoomPo.class); // 转换为PO对象
        flag = roomV1InnerServiceSMOImpl.updateRoom(roomPo); // 更新商铺信息
        if (flag < 1) {
            throw new IllegalArgumentException("删除已有房屋关系失败");
        }
    }
}