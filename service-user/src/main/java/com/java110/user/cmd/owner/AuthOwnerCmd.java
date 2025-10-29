package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 业主认证命令类
 * 处理业主在小程序或APP端的身份认证申请，包括业主、家庭成员等身份类型的认证
 * 主要功能包括参数验证、业务逻辑校验和认证信息保存
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode owner.authOwner
 */
@Java110Cmd(serviceCode = "owner.authOwner")
public class AuthOwnerCmd extends Cmd {

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IRoomV1InnerServiceSMO roomV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的完整性和业务逻辑的合法性，包括：
     * 1. 必填参数验证
     * 2. 用户身份验证
     * 3. 认证关系重复性检查
     * 4. 房屋信息验证
     * 5. 业主身份特殊校验
     *
     * @param event   命令事件对象，包含事件相关信息
     * @param context 数据流上下文对象，包含请求上下文信息
     * @param reqJson 请求参数JSON对象，包含认证所需的所有参数
     * @throws CmdException    当参数验证失败或业务逻辑不满足时抛出
     * @throws ParseException  当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "roomName", "未包含房屋");
        Assert.hasKeyAndValue(reqJson, "roomId", "未包含房屋");
        Assert.hasKeyAndValue(reqJson, "link", "未包含手机号");
        Assert.hasKeyAndValue(reqJson, "ownerName", "未包含人员名称");
        Assert.hasKeyAndValue(reqJson, "ownerTypeCd", "未包含人员类型");

        // 获取当前登录用户ID并验证用户信息
        String userId = CmdContextUtils.getUserId(context);
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 确保用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户未登录");
        // 验证手机号是否与注册时一致
        if (!userDtos.get(0).getTel().equals(reqJson.getString("link"))) {
            throw new CmdException("手机号错误，不是注册时的手机号");
        }

        // 检查是否已存在认证关系（审核中或审核成功状态）
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setLink(reqJson.getString("link"));
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        ownerAppUserDto.setStates(new String[]{
                OwnerAppUserDto.STATE_AUDITING,
                OwnerAppUserDto.STATE_AUDIT_SUCCESS
        });
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        if (!ListUtil.isNull(ownerAppUserDtos)) {
            throw new CmdException("已存在认证关系，请勿重复认证");
        }

        // 验证房屋信息是否存在
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(reqJson.getString("roomId"));
        roomDto.setCommunityId(reqJson.getString("communityId"));
        List<RoomDto> roomDtos = roomV1InnerServiceSMOImpl.queryRooms(roomDto);
        Assert.listOnlyOne(roomDtos, "房屋不存在");

        // 检查房屋是否已销售（存在业主房屋关系）
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setRoomId(roomDtos.get(0).getRoomId());
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        if (ListUtil.isNull(ownerRoomRelDtos)) {
            throw new CmdException("房屋未销售，请联系物业");
        }

        // 如果是业主身份认证，需要额外检查业主是否已被认证
        if (!OwnerDto.OWNER_TYPE_CD_OWNER.equals(reqJson.getString("ownerTypeCd"))) {
            return;
        }

        // 检查业主是否已被其他用户认证
        ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setMemberId(ownerRoomRelDtos.get(0).getOwnerId());
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        ownerAppUserDto.setStates(new String[]{
                OwnerAppUserDto.STATE_AUDITING,
                OwnerAppUserDto.STATE_AUDIT_SUCCESS
        });
        ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        if (!ListUtil.isNull(ownerAppUserDtos)) {
            throw new CmdException("业主已经被认证");
        }
    }

    /**
     * 执行业主认证命令
     * 创建业主认证申请记录，包括用户信息、房屋信息、认证状态等
     * 认证初始状态为"审核中"，需要物业后台审核通过后方可生效
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象，包含认证信息
     * @throws CmdException    当命令执行失败时抛出
     * @throws ParseException  当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取当前登录用户ID
        String userId = CmdContextUtils.getUserId(context);

        // 查询小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(reqJson.getString("communityId"));
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        Assert.listNotNull(communityDtos, "未包含小区信息");
        CommunityDto tmpCommunityDto = communityDtos.get(0);

        // 构建业主认证申请对象
        OwnerAppUserPo ownerAppUserPo = new OwnerAppUserPo();
        // 设置认证状态为审核中
        ownerAppUserPo.setState(OwnerAppUserDto.STATE_AUDITING);
        // 设置应用类型编码
        ownerAppUserPo.setAppTypeCd("10010");
        // 生成唯一应用用户ID
        ownerAppUserPo.setAppUserId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_appUserId));
        // 初始成员ID设为-1，审核通过后更新为实际业主ID
        ownerAppUserPo.setMemberId("-1");
        // 设置小区信息
        ownerAppUserPo.setCommunityName(tmpCommunityDto.getName());
        ownerAppUserPo.setCommunityId(tmpCommunityDto.getCommunityId());
        // 设置申请人信息
        ownerAppUserPo.setAppUserName(reqJson.getString("ownerName"));
        ownerAppUserPo.setAppType("WECHAT");
        ownerAppUserPo.setLink(reqJson.getString("link"));
        ownerAppUserPo.setUserId(userId);
        // 初始openId设为-1，后续从用户属性中获取
        ownerAppUserPo.setOpenId("-1");
        // 设置房屋信息
        ownerAppUserPo.setRoomId(reqJson.getString("roomId"));
        ownerAppUserPo.setRoomName(reqJson.getString("roomName"));
        // 设置业主类型
        ownerAppUserPo.setOwnerTypeCd(reqJson.getString("ownerTypeCd"));

        // 查询用户OpenID属性
        UserAttrDto userAttrDto = new UserAttrDto();
        userAttrDto.setUserId(userId);
        userAttrDto.setSpecCd(UserAttrDto.SPEC_OPEN_ID);
        List<UserAttrDto> userAttrDtos = userAttrV1InnerServiceSMOImpl.queryUserAttrs(userAttrDto);
        // 如果存在OpenID，则设置到认证记录中
        if (!ListUtil.isNull(userAttrDtos)) {
            ownerAppUserPo.setOpenId(userAttrDtos.get(0).getValue());
        }

        // 保存业主认证申请记录
        ownerAppUserV1InnerServiceSMOImpl.saveOwnerAppUser(ownerAppUserPo);
    }
}