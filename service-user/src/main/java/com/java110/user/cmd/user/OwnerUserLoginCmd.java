/**
 * 业主用户登录命令类
 * 
 * 该类专门处理业主用户的登录请求，支持用户名密码登录和手机验证码登录两种方式。
 * 登录成功后生成token和登录key，并返回用户信息和小区信息。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.app.AppDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.msg.SmsDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.system.SystemInfoDto;
import com.java110.dto.user.LoginOwnerResDto;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.ISmsInnerServiceSMO;
import com.java110.intf.common.ISystemInfoV1InnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.job.IIotInnerServiceSMO;
import com.java110.intf.job.IMallInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.po.user.UserPo;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.constant.UserLevelConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * 业主登录，专门提供业主使用
 */
@Java110Cmd(serviceCode = "user.ownerUserLogin")
public class OwnerUserLoginCmd extends Cmd {

    private final static Logger logger = LoggerFactory.getLogger(UserLoginCmd.class);

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    @Autowired
    private ISmsInnerServiceSMO smsInnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private ISystemInfoV1InnerServiceSMO systemInfoV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private IMallInnerServiceSMO mallInnerServiceSMOImpl;

    @Autowired
    private IIotInnerServiceSMO iotInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求报文中必须包含用户名和密码
        Assert.hasKeyAndValue(reqJson, "username", "请求报文中未包含用户名");
        Assert.hasKeyAndValue(reqJson, "password", "请求报文中未包含密码");

        // 如果是验证码登录方式，需要验证验证码是否正确
        if (reqJson.containsKey("loginByPhone") && reqJson.getBoolean("loginByPhone")) {
            SmsDto smsDto = new SmsDto();
            smsDto.setTel(reqJson.getString("username")); // 手机号作为用户名
            smsDto.setCode(reqJson.getString("password")); // 验证码作为密码
            smsDto = smsInnerServiceSMOImpl.validateCode(smsDto);
            if (!smsDto.isSuccess()) {
                throw new SMOException("验证码错误");
            }
        }
    }

    /**
     * 执行登录命令
     * 
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        UserDto userDto = new UserDto();
        userDto.setLevelCd(UserDto.LEVEL_CD_USER);
        // 根据用户名判断是手机号登录还是用户名登录
        if (ValidatorUtil.isMobile(reqJson.getString("username"))) {
            userDto.setTel(reqJson.getString("username")); // 手机号登录
        } else {
            userDto.setUserName(reqJson.getString("username")); // 用户名登录
        }

        // 如果不是验证码登录，需要对密码进行MD5加密
        if (!reqJson.containsKey("loginByPhone") || !reqJson.getBoolean("loginByPhone")) {
            userDto.setPassword(AuthenticationFactory.passwdMd5(reqJson.getString("password")));
        }

        // 查询用户是否存在
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 如果是验证码登录且用户不存在，尝试自动绑定业主关系
        if (ListUtil.isNull(userDtos)) {
            userDtos = ifOwnerLoginByPhone(reqJson, context);
        }
        if (ListUtil.isNull(userDtos)) {
            throw new CmdException("密码错误或者用户不存在");
        }

        // 同步用户信息到商城系统
        mallInnerServiceSMOImpl.sendUserInfo(userDtos.get(0));

        // 同步用户信息到物联网系统
        iotInnerServiceSMOImpl.sendUserInfo(userDtos.get(0));

        // 查询业主用户绑定关系
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userDtos.get(0).getUserId());
        ownerAppUserDto.setLink(userDtos.get(0).getTel());
        ownerAppUserDto.setState(OwnerAppUserDto.STATE_AUDIT_SUCCESS);
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        String communityId = "";
        if (!ListUtil.isNull(ownerAppUserDtos)) {
            // 从绑定关系中获取小区ID
            communityId = ownerAppUserDtos.get(0).getCommunityId();
        } else {
            // 如果没有绑定关系，使用系统默认小区
            SystemInfoDto systemInfoDto = new SystemInfoDto();
            List<SystemInfoDto> systemInfoDtos = systemInfoV1InnerServiceSMOImpl.querySystemInfos(systemInfoDto);
            communityId = systemInfoDtos.get(0).getDefaultCommunityId();
        }
        
        // 查询小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(communityId);
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        if (ListUtil.isNull(communityDtos)) {
            // 如果指定小区不存在，查询所有小区
            communityDto = new CommunityDto();
            communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        }
        if (ListUtil.isNull(communityDtos)) {
            throw new CmdException("系统中未找到任何小区");
        }

        // 生成app永久登录key
        UserDto tmpUserDto = userDtos.get(0);
        String newKey = generatorLoginKey(tmpUserDto);

        // 生成登录token
        String token = generatorLoginToken(tmpUserDto);
        
        // 构建登录响应数据
        LoginOwnerResDto loginOwnerResDto = new LoginOwnerResDto();
        loginOwnerResDto.setCommunityId(communityDtos.get(0).getCommunityId());
        loginOwnerResDto.setCommunityName(communityDtos.get(0).getName());
        loginOwnerResDto.setCommunityTel(communityDtos.get(0).getTel());
        loginOwnerResDto.setCommunityQrCode(communityDtos.get(0).getQrCode());
        loginOwnerResDto.setUserId(tmpUserDto.getUserId());
        loginOwnerResDto.setUserName(tmpUserDto.getName());
        loginOwnerResDto.setOwnerTel(tmpUserDto.getTel());
        loginOwnerResDto.setToken(token);
        loginOwnerResDto.setKey(newKey);
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(loginOwnerResDto));
    }

    /**
     * 生成登录token
     * 
     * @param tmpUserDto 用户数据传输对象
     * @return 生成的token字符串
     */
    private static String generatorLoginToken(UserDto tmpUserDto) {
        String token;
        try {
            Map userMap = new HashMap();
            userMap.put(CommonConstant.LOGIN_USER_ID, tmpUserDto.getUserId()); // 用户ID
            userMap.put(CommonConstant.LOGIN_USER_NAME, tmpUserDto.getUserName()); // 用户名
            token = AuthenticationFactory.createAndSaveToken(userMap); // 创建并保存token
        } catch (Exception e) {
            logger.error("登录异常：", e);
            throw new CmdException("系统内部错误，请联系管理员");
        }
        return token;
    }

    /**
     * 生成登录key
     * 
     * @param tmpUserDto 用户数据传输对象
     * @return 生成的登录key
     */
    private String generatorLoginKey(UserDto tmpUserDto) {
        List<UserAttrDto> userAttrDtos = tmpUserDto.getUserAttrs();
        // 获取当前用户的key属性
        UserAttrDto userAttrDto = getCurrentUserAttrDto(userAttrDtos, UserAttrDto.SPEC_KEY);
        String newKey = UUID.randomUUID().toString(); // 生成随机UUID作为新key
        
        if (userAttrDto != null) {
            // 如果已存在key属性，则更新
            UserAttrPo userAttrPo = BeanConvertUtil.covertBean(userAttrDto, UserAttrPo.class);
            userAttrPo.setValue(newKey);
            userAttrPo.setStatusCd("0"); // 设置状态为有效
            userAttrV1InnerServiceSMOImpl.updateUserAttr(userAttrPo);
        } else {
            // 如果不存在key属性，则新增
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setAttrId(GenerateCodeFactory.getAttrId()); // 生成属性ID
            userAttrPo.setUserId(tmpUserDto.getUserId());
            userAttrPo.setSpecCd(UserAttrDto.SPEC_KEY); // 设置属性规格代码
            userAttrPo.setValue(newKey);
            userAttrPo.setStatusCd("0"); // 设置状态为有效
            userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
        }
        return newKey;
    }

    /**
     * 验证码登录时判断是否为业主并自动绑定关系
     * 
     * @param reqJson 请求参数JSON对象
     * @param context 数据流上下文
     * @return 用户数据传输对象列表
     */
    private List<UserDto> ifOwnerLoginByPhone(JSONObject reqJson, ICmdDataFlowContext context) {
        // 如果不是验证码登录，直接返回null
        if (!reqJson.containsKey("loginByPhone") || !reqJson.getBoolean("loginByPhone")) {
            return null;
        }
        
        String appId = context.getReqHeaders().get("app-id"); // 获取应用ID

        // 只处理业主相关的应用
        if (!AppDto.WECHAT_OWNER_APP_ID.equals(appId)
                && !AppDto.WECHAT_MINA_OWNER_APP_ID.equals(appId)
                && !AppDto.OWNER_APP_APP_ID.equals(appId)) {
            return null;
        }

        // 验证码登录必须包含用户名
        if (StringUtil.isEmpty(reqJson.getString("userName"))) {
            return null;
        }

        // 查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(reqJson.getString("userName")); // 手机号作为联系方式
        ownerDto.setPage(1);
        ownerDto.setRow(1);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 如果业主不存在，直接返回null
        if (ListUtil.isNull(ownerDtos)) {
            return null;
        }

        // 查询小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(ownerDtos.get(0).getCommunityId());
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        Assert.listNotNull(communityDtos, "未包含小区信息");
        CommunityDto tmpCommunityDto = communityDtos.get(0);

        // 创建新用户
        UserPo userPo = new UserPo();
        userPo.setUserId(GenerateCodeFactory.getUserId()); // 生成用户ID
        userPo.setName(ownerDtos.get(0).getName()); // 业主姓名
        userPo.setTel(ownerDtos.get(0).getLink()); // 业主手机号
        userPo.setPassword(AuthenticationFactory.passwdMd5(reqJson.getString("password"))); // 加密密码
        userPo.setLevelCd(UserLevelConstant.USER_LEVEL_ORDINARY); // 普通用户级别
        userPo.setAge(ownerDtos.get(0).getAge()); // 年龄
        userPo.setAddress(ownerDtos.get(0).getAddress()); // 地址
        userPo.setSex(ownerDtos.get(0).getSex()); // 性别
        int flag = userV1InnerServiceSMOImpl.saveUser(userPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 创建业主应用用户关系
        OwnerAppUserPo ownerAppUserPo = new OwnerAppUserPo();
        ownerAppUserPo.setState("12000"); // 审核成功状态
        ownerAppUserPo.setAppTypeCd("10010"); // 应用类型代码
        ownerAppUserPo.setAppUserId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_appUserId)); // 生成应用用户ID
        ownerAppUserPo.setMemberId(ownerDtos.get(0).getMemberId()); // 成员ID
        ownerAppUserPo.setCommunityName(tmpCommunityDto.getName()); // 小区名称
        ownerAppUserPo.setCommunityId(ownerDtos.get(0).getCommunityId()); // 小区ID
        ownerAppUserPo.setAppUserName(ownerDtos.get(0).getName()); // 应用用户名
        ownerAppUserPo.setIdCard(ownerDtos.get(0).getIdCard()); // 身份证号
        ownerAppUserPo.setAppType("WECHAT"); // 应用类型
        ownerAppUserPo.setLink(ownerDtos.get(0).getLink()); // 联系方式
        ownerAppUserPo.setUserId(userPo.getUserId()); // 用户ID
        ownerAppUserPo.setOpenId("-1"); // 默认openId
        ownerAppUserPo.setOwnerTypeCd(ownerDtos.get(0).getOwnerTypeCd()); // 业主类型代码

        // 查询业主房间关系
        queryOwnerRoom(ownerDtos.get(0), ownerAppUserPo);

        // 保存业主应用用户关系
        flag = ownerAppUserV1InnerServiceSMOImpl.saveOwnerAppUser(ownerAppUserPo);
        if (flag < 1) {
            throw new CmdException("添加用户业主关系失败");
        }

        // 返回新创建的用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userPo.getUserId());
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);
        return userDtos;
    }

    /**
     * 查询业主房间关系
     * 
     * @param ownerDto 业主数据传输对象
     * @param ownerAppUserPo 业主应用用户持久化对象
     */
    private void queryOwnerRoom(OwnerDto ownerDto, OwnerAppUserPo ownerAppUserPo) {
        // 查询业主房间关系
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerId(ownerDto.getOwnerId()); // 业主ID

        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        if (ListUtil.isNull(ownerRoomRelDtos)) {
            return; // 没有房间关系直接返回
        }

        // 查询房间信息
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(ownerRoomRelDtos.get(0).getRoomId()); // 房间ID
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        if (ListUtil.isNull(roomDtos)) {
            return; // 房间不存在直接返回
        }

        // 设置房间信息到业主应用用户对象
        ownerAppUserPo.setRoomId(roomDtos.get(0).getRoomId()); // 房间ID
        // 构建房间名称：楼层-单元-房间号
        ownerAppUserPo.setRoomName(roomDtos.get(0).getFloorNum() + "-" + roomDtos.get(0).getUnitNum() + "-" + roomDtos.get(0).getRoomNum());
    }

    /**
     * 根据规格代码获取用户属性
     * 
     * @param userAttrDtos 用户属性数据传输对象列表
     * @param specCd 规格代码
     * @return 匹配的用户属性数据传输对象，未找到返回null
     */
    private UserAttrDto getCurrentUserAttrDto(List<UserAttrDto> userAttrDtos, String specCd) {
        if (userAttrDtos == null) {
            return null;
        }
        // 遍历用户属性列表，查找匹配的规格代码
        for (UserAttrDto userAttrDto : userAttrDtos) {
            if (specCd.equals(userAttrDto.getSpecCd())) {
                return userAttrDto;
            }
        }

        return null;
    }
}