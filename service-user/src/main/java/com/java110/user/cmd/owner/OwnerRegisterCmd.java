package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.factory.SendSmsFactory;
import com.java110.dto.app.AppDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.msg.SmsDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.ISmsInnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.user.UserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.UserLevelConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 业主注册命令类
 * 负责处理业主用户注册业务逻辑，包括用户信息验证、用户创建、业主关联等操作
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode owner.ownerRegister
 */
@Java110Cmd(serviceCode = "owner.ownerRegister")
public class OwnerRegisterCmd extends Cmd {
    private final static Logger logger = LoggerFactory.getLogger(OwnerRegisterCmd.class);
    
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private ISmsInnerServiceSMO smsInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查必要的请求参数是否存在，验证手机号是否已注册，验证短信验证码是否正确
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "link", "未包含联系电话");
        Assert.hasKeyAndValue(reqJson, "msgCode", "未包含联系电话验证码");
        Assert.hasKeyAndValue(reqJson, "password", "未包含密码");

        // 检查手机号是否已注册
        UserDto userDto = new UserDto();
        userDto.setTel(reqJson.getString("link"));
        userDto.setLevelCd(UserDto.LEVEL_CD_USER);
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        if (!ListUtil.isNull(userDtos)) {
            throw new CmdException("手机号已存在，请登陆");
        }

        // 验证短信验证码
        SmsDto smsDto = new SmsDto();
        smsDto.setTel(reqJson.getString("link"));
        smsDto.setCode(reqJson.getString("msgCode"));
        smsDto = smsInnerServiceSMOImpl.validateCode(smsDto);

        // 如果短信验证失败且短信发送开关为开启状态，则抛出异常
        if (!smsDto.isSuccess() && "ON".equals(MappingCache.getValue(MappingConstant.SMS_DOMAIN, SendSmsFactory.SMS_SEND_SWITCH))) {
            throw new IllegalArgumentException(smsDto.getMsg());
        }
    }

    /**
     * 执行业主注册命令
     * 创建用户账号，保存用户属性，关联业主信息，并建立业主应用用户关系
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当注册过程中出现错误时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 根据手机号查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(reqJson.getString("link"));
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnerMembers(ownerDto);

        // 设置用户密码并进行MD5加密
        String userPassword = reqJson.getString("password");
        userPassword = AuthenticationFactory.passwdMd5(userPassword);
        
        // 设置用户名称，默认为手机号，如果存在业主信息则使用业主姓名
        String name = reqJson.getString("link");
        if (!ListUtil.isNull(ownerDtos)) {
            name = ownerDtos.get(0).getName();
        }
        
        // 创建用户信息
        UserPo userPo = new UserPo();
        userPo.setAddress("无");
        userPo.setUserId(GenerateCodeFactory.getUserId());
        userPo.setLevelCd(UserLevelConstant.USER_LEVEL_ORDINARY);
        userPo.setName(name);
        userPo.setTel(reqJson.getString("link"));
        userPo.setPassword(userPassword);

        // 保存用户信息
        int flag = userV1InnerServiceSMOImpl.saveUser(userPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }
        
        // 保存用户OpenID（如果存在）
        String openId = reqJson.getString("openId");
        if (!StringUtil.isEmpty(openId)) {
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
            userAttrPo.setSpecCd(UserAttrDto.SPEC_OPEN_ID);
            userAttrPo.setUserId(userPo.getUserId());
            userAttrPo.setValue(openId);
            flag = userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
            if (flag < 1) {
                throw new CmdException("注册失败");
            }
        }

        // 如果没有找到关联的业主信息，直接返回成功
        if (ListUtil.isNull(ownerDtos)) {
            return;
        }
        
        // 根据应用ID确定应用类型
        String appId = cmdDataFlowContext.getReqHeaders().get("app-id");
        String appType = "";
        if (AppDto.WECHAT_OWNER_APP_ID.equals(appId)) { // 公众号
            appType = OwnerAppUserDto.APP_TYPE_WECHAT;
        } else if (AppDto.WECHAT_MINA_OWNER_APP_ID.equals(appId)) { // 小程序
            appType = OwnerAppUserDto.APP_TYPE_WECHAT_MINA;
        } else {// APP应用
            appType = OwnerAppUserDto.APP_TYPE_APP;
        }

        OwnerAppUserPo ownerAppUserPo = null;
        List<CommunityDto> communityDtos = null;
        
        // 遍历所有业主信息，为每个业主创建应用用户关系
        for (OwnerDto tmpOwnerDto : ownerDtos) {
            // 查询小区信息
            CommunityDto communityDto = new CommunityDto();
            communityDto.setState("1100");
            communityDto.setCommunityId(tmpOwnerDto.getCommunityId());
            communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
            if (ListUtil.isNull(communityDtos)) {
                continue; // 如果小区不存在，跳过当前业主
            }
            
            communityDto = communityDtos.get(0);
            
            // 创建业主应用用户关系对象
            ownerAppUserPo = new OwnerAppUserPo();
            ownerAppUserPo.setAppUserId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_appUserId));
            ownerAppUserPo.setMemberId(tmpOwnerDto.getMemberId());
            ownerAppUserPo.setCommunityId(tmpOwnerDto.getCommunityId());
            ownerAppUserPo.setCommunityName(communityDto.getName());
            ownerAppUserPo.setAppUserName(tmpOwnerDto.getName());
            ownerAppUserPo.setIdCard(tmpOwnerDto.getIdCard());
            ownerAppUserPo.setLink(tmpOwnerDto.getLink());
            ownerAppUserPo.setOpenId("-1");
            ownerAppUserPo.setAppTypeCd("10010");
            ownerAppUserPo.setState(OwnerAppUserDto.STATE_AUDIT_SUCCESS);
            ownerAppUserPo.setRemark("注册自动关联");
            ownerAppUserPo.setUserId(userPo.getUserId());
            ownerAppUserPo.setAppType(appType);
            ownerAppUserPo.setOwnerTypeCd(tmpOwnerDto.getOwnerTypeCd());
            
            // 查询业主房间信息并设置到应用用户关系中
            queryOwnerRoom(tmpOwnerDto, ownerAppUserPo);
            
            // 保存业主应用用户关系
            flag = ownerAppUserV1InnerServiceSMOImpl.saveOwnerAppUser(ownerAppUserPo);
            if (flag < 1) {
                throw new CmdException("添加用户业主关系失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 查询业主房间信息
     * 根据业主信息查询关联的房间信息，并将房间ID和房间名称设置到业主应用用户对象中
     *
     * @param ownerDto 业主数据传输对象
     * @param ownerAppUserPo 业主应用用户持久化对象
     */
    private void queryOwnerRoom(OwnerDto ownerDto, OwnerAppUserPo ownerAppUserPo) {
        // 查询业主房间关联关系
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerId(ownerDto.getOwnerId());

        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        // 如果没有房间关联关系，直接返回
        if (ListUtil.isNull(ownerRoomRelDtos)) {
            return;
        }

        // 查询房间详细信息
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(ownerRoomRelDtos.get(0).getRoomId());
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        if (ListUtil.isNull(roomDtos)) {
            return;
        }

        // 设置房间信息到业主应用用户对象中
        ownerAppUserPo.setRoomId(roomDtos.get(0).getRoomId());
        ownerAppUserPo.setRoomName(roomDtos.get(0).getFloorNum() + "-" + roomDtos.get(0).getUnitNum() + "-" + roomDtos.get(0).getRoomNum());
    }
}