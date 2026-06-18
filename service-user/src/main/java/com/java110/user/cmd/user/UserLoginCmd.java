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
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.ISmsInnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.user.UserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.constant.UserLevelConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户登录命令类
 * 处理用户登录相关的业务逻辑，支持用户名密码登录、手机号验证码登录、临时票据登录等多种登录方式
 * 同时支持业主自动注册和绑定功能
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode user.userLogin
 */
@Java110Cmd(serviceCode = "user.userLogin")
public class UserLoginCmd extends Cmd {

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

    /**
     * 参数验证方法
     * 验证请求参数是否包含必要的登录信息
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必须包含userName或key参数
        if (!reqJson.containsKey("userName")) {
            Assert.hasKeyAndValue(reqJson, "key", "未包含key");
        }

        if (!reqJson.containsKey("key")) {
            Assert.hasKeyAndValue(reqJson, "userName", "未包含userName");
            Assert.hasKeyAndValue(reqJson, "password", "未包含password");
        }
    }

    /**
     * 执行登录命令的主要业务逻辑
     * 处理多种登录方式并返回登录结果
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当登录过程中出现业务异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 1.0 优先用手机号登录
        UserDto userDto = new UserDto();
        String errorInfo = "";
        
        // 设置用户等级
        if (reqJson.containsKey("levelCd")) {
            userDto.setLevelCd(reqJson.getString("levelCd"));
        }
        
        // 根据不同的登录方式设置不同的查询条件和错误信息
        if (reqJson.containsKey("userName")) {
            if (ValidatorUtil.isMobile(reqJson.getString("userName"))) {
                // 用户临时秘钥登录
                userDto.setTel(reqJson.getString("userName"));
            } else {
                userDto.setUserName(reqJson.getString("userName"));
            }
            
            // 验证码登录逻辑
            if (reqJson.containsKey("loginByPhone") && reqJson.getBoolean("loginByPhone")) {
                SmsDto smsDto = new SmsDto();
                smsDto.setTel(reqJson.getString("userName"));
                smsDto.setCode(reqJson.getString("password"));
                smsDto = smsInnerServiceSMOImpl.validateCode(smsDto);
                if (!smsDto.isSuccess()) {
                    throw new SMOException("验证码错误");
                }
            } else {
                // 密码登录，对密码进行MD5加密
                userDto.setPassword(AuthenticationFactory.passwdMd5(reqJson.getString("password")));
            }
            errorInfo = "用户名或密码错误";
        } else {
            // 临时票据登录
            userDto.setKey(reqJson.getString("key"));
            errorInfo = "临时票据错误";
        }

        // 查询用户信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // todo 验证码登录，判断是否是否业主，并且是否绑定关系 如果没有 自动绑定关系
        // 如果用户不存在，尝试通过业主手机号登录
        if (ListUtil.isNull(userDtos)) {
            userDtos = ifOwnerLoginByPhone(reqJson, context);
        }
        
        // 如果仍然找不到用户，抛出异常
        if (ListUtil.isNull(userDtos)) {
            throw new SMOException(errorInfo);
        }

        // 表名登录成功，获取第一个用户信息
        UserDto tmpUserDto = userDtos.get(0);

        // 获取用户属性列表
        List<UserAttrDto> userAttrDtos = tmpUserDto.getUserAttrs();

        // 获取或更新用户的临时密钥
        UserAttrDto userAttrDto = getCurrentUserAttrDto(userAttrDtos, UserAttrDto.SPEC_KEY);
        String newKey = UUID.randomUUID().toString();
        if (userAttrDto != null) {
            // 更新已存在的密钥
            UserAttrPo userAttrPo = BeanConvertUtil.covertBean(userAttrDto, UserAttrPo.class);
            userAttrPo.setValue(newKey);
            userAttrPo.setStatusCd("0");
            userAttrV1InnerServiceSMOImpl.updateUserAttr(userAttrPo);
        } else {
            // 创建新的密钥属性
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setAttrId("-1");
            userAttrPo.setUserId(tmpUserDto.getUserId());
            userAttrPo.setSpecCd(UserAttrDto.SPEC_KEY);
            userAttrPo.setValue(newKey);
            userAttrPo.setStatusCd("0");
            userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
        }

        try {
            // 创建登录令牌和返回结果
            Map userMap = new HashMap();
            userMap.put(CommonConstant.LOGIN_USER_ID, tmpUserDto.getUserId());
            userMap.put(CommonConstant.LOGIN_USER_NAME, tmpUserDto.getUserName());
            String token = AuthenticationFactory.createAndSaveToken(userMap);
            
            // 清除敏感信息并设置返回数据
            tmpUserDto.setPassword("");
            tmpUserDto.setToken(token);
            tmpUserDto.setKey(newKey);
            context.setResponseEntity(ResultVo.createResponseEntity(tmpUserDto));
        } catch (Exception e) {
            logger.error("登录异常：", e);
            throw new SMOException(ResponseConstant.RESULT_CODE_INNER_ERROR, "系统内部错误，请联系管理员");
        }
    }

    /**
     * 验证码登录时判断是否为业主并自动绑定关系
     * 如果用户不存在但手机号对应业主信息，则自动创建用户并绑定关系
     *
     * @param reqJson 请求参数JSON对象
     * @param context 数据流上下文对象
     * @return 用户信息列表，如果处理成功返回用户列表，否则返回null
     */
    private List<UserDto> ifOwnerLoginByPhone(JSONObject reqJson, ICmdDataFlowContext context) {
        // 检查是否为验证码登录
        if (!reqJson.containsKey("loginByPhone") || !reqJson.getBoolean("loginByPhone")) {
            return null;
        }
        
        // 获取应用ID，判断是否为业主端应用
        String appId = context.getReqHeaders().get("app-id");
        if (!AppDto.WECHAT_OWNER_APP_ID.equals(appId)
                && !AppDto.WECHAT_MINA_OWNER_APP_ID.equals(appId)
                && !AppDto.OWNER_APP_APP_ID.equals(appId)) {
            return null;
        }

        // 检查用户名（手机号）是否为空
        if (StringUtil.isEmpty(reqJson.getString("userName"))) {
            return null;
        }

        // todo 查询业主或成员信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(reqJson.getString("userName"));
        ownerDto.setPage(1);
        ownerDto.setRow(1);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 说明业主不存在，直接返回null
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
        userPo.setUserId(GenerateCodeFactory.getUserId());
        userPo.setName(ownerDtos.get(0).getName());
        userPo.setTel(ownerDtos.get(0).getLink());
        userPo.setPassword(AuthenticationFactory.passwdMd5(reqJson.getString("password")));
        userPo.setLevelCd(UserLevelConstant.USER_LEVEL_ORDINARY);
        userPo.setAge(ownerDtos.get(0).getAge());
        userPo.setAddress(ownerDtos.get(0).getAddress());
        userPo.setSex(ownerDtos.get(0).getSex());
        int flag = userV1InnerServiceSMOImpl.saveUser(userPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }

        // 创建业主应用用户关系
        OwnerAppUserPo ownerAppUserPo = new OwnerAppUserPo();
        // 状态类型，10000 审核中，12000 审核成功，13000 审核失败
        ownerAppUserPo.setState("12000");
        ownerAppUserPo.setAppTypeCd("10010");
        ownerAppUserPo.setAppUserId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_appUserId));
        ownerAppUserPo.setMemberId(ownerDtos.get(0).getMemberId());
        ownerAppUserPo.setCommunityName(tmpCommunityDto.getName());
        ownerAppUserPo.setCommunityId(ownerDtos.get(0).getCommunityId());
        ownerAppUserPo.setAppUserName(ownerDtos.get(0).getName());
        ownerAppUserPo.setIdCard(ownerDtos.get(0).getIdCard());
        ownerAppUserPo.setAppType("WECHAT");
        ownerAppUserPo.setLink(ownerDtos.get(0).getLink());
        ownerAppUserPo.setUserId(userPo.getUserId());
        ownerAppUserPo.setOpenId("-1");

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
     * 根据规格代码从用户属性列表中获取对应的用户属性
     *
     * @param userAttrDtos 用户属性列表
     * @param specCd 规格代码
     * @return 匹配的用户属性对象，如果未找到返回null
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