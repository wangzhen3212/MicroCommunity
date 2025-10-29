/**
 * 业主用户通过微信OpenID登录命令类
 * 
 * 该命令类专门处理业主用户通过微信OpenID进行登录的业务逻辑，包括：
 * 1. 验证OpenID的有效性
 * 2. 根据OpenID查询用户信息
 * 3. 校验业主用户绑定关系
 * 4. 生成登录token和key
 * 5. 返回登录结果信息
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode user.ownerUserLoginByOpenId
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
import com.java110.dto.system.SystemInfoDto;
import com.java110.dto.user.LoginOwnerResDto;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.wechat.WechatSubscribeDto;
import com.java110.intf.common.ISmsInnerServiceSMO;
import com.java110.intf.common.ISystemInfoV1InnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.po.user.UserPo;
import com.java110.user.smo.impl.WechatSubscribeV1InnerServiceSMOImpl;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.CommonConstant;
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
 * 微信openId登录
 */
@Java110Cmd(serviceCode = "user.ownerUserLoginByOpenId")
public class OwnerUserLoginByOpenIdCmd extends Cmd {

    /** 日志记录器 */
    private final static Logger logger = LoggerFactory.getLogger(UserLoginCmd.class);

    /** 用户内部服务 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /** 用户属性内部服务 */
    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    /** 短信内部服务 */
    @Autowired
    private ISmsInnerServiceSMO smsInnerServiceSMOImpl;

    /** 业主应用用户内部服务 */
    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    /** 业主内部服务 */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /** 小区内部服务 */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /** 用户V1内部服务 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /** 系统信息V1内部服务 */
    @Autowired
    private ISystemInfoV1InnerServiceSMO systemInfoV1InnerServiceSMOImpl;

    /** 微信订阅内部服务 */
    @Autowired
    private WechatSubscribeV1InnerServiceSMOImpl wechatSubscribeV1InnerServiceSMOImpl;


    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含有效的OpenID
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求报文中必须包含openId参数
        Assert.hasKeyAndValue(reqJson, "openId", "请求报文中未包含openId");

        // todo openId转换
        String openId = reqJson.getString("openId");
        // 检查openId是否为null或undefined等非法值
        if ("null".equalsIgnoreCase(openId) || "undefined".equals(openId)) {
            throw new CmdException("openId非法");
        }
    }

    /**
     * 执行命令方法
     * 
     * 处理业主用户通过OpenID登录的主要业务逻辑
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 根据OpenID查询用户属性信息
        UserAttrDto userAttrDto = new UserAttrDto();
        userAttrDto.setSpecCd(UserAttrDto.SPEC_OPEN_ID); // 设置查询条件为OpenID类型
        userAttrDto.setValue(reqJson.getString("openId")); // 设置OpenID值
        List<UserAttrDto> userAttrDtos = userAttrV1InnerServiceSMOImpl.queryUserAttrs(userAttrDto);

        // 如果未找到对应的用户属性记录，抛出异常
        if (ListUtil.isNull(userAttrDtos)) {
            throw new CmdException("未找到用户信息");
        }

        // todo 2.0 校验业主用户绑定表是否存在记录
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userAttrDtos.get(0).getUserId()); // 设置用户ID
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        String communityId = "";
        // 如果存在业主用户绑定记录
        if (!ListUtil.isNull(ownerAppUserDtos)) {
            // todo 4.0 查询小区是否存在
            communityId = ownerAppUserDtos.get(0).getCommunityId(); // 获取小区ID
            // 更新微信业主应用用户的OpenID
            updateWechatOwnerAppUserOpenId(ownerAppUserDtos.get(0), reqJson.getString("openId"));
        } else {
            // 如果不存在绑定记录，获取系统默认小区ID
            SystemInfoDto systemInfoDto = new SystemInfoDto();
            List<SystemInfoDto> systemInfoDtos = systemInfoV1InnerServiceSMOImpl.querySystemInfos(systemInfoDto);
            communityId = systemInfoDtos.get(0).getDefaultCommunityId(); // 获取默认小区ID
        }
        
        // 验证小区信息是否存在
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(communityId);
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        Assert.listOnlyOne(communityDtos, "小区不存在，确保开发者账户配置默认小区" + communityId);

        // 查询用户基本信息
        UserDto userDto = new UserDto();
        userDto.setLevelCd(UserDto.LEVEL_CD_USER); // 设置用户级别为普通用户
        userDto.setUserId(userAttrDtos.get(0).getUserId()); // 设置用户ID

        // todo 1.0 查询用户是否存在
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 如果用户不存在，抛出异常
        if (ListUtil.isNull(userDtos)) {
            throw new CmdException("业主不存在，请先注册");
        }

        UserDto tmpUserDto = userDtos.get(0);
        // 生成登录key
        String newKey = generatorLoginKey(tmpUserDto);

        // todo 生成登录token
        String token = generatorLoginToken(tmpUserDto);
        // 构建登录响应对象
        LoginOwnerResDto loginOwnerResDto = new LoginOwnerResDto();

        // 设置响应信息
        loginOwnerResDto.setCommunityId(communityDtos.get(0).getCommunityId());
        loginOwnerResDto.setCommunityName(communityDtos.get(0).getName());
        loginOwnerResDto.setCommunityTel(communityDtos.get(0).getTel());
        loginOwnerResDto.setUserId(tmpUserDto.getUserId());
        loginOwnerResDto.setUserName(tmpUserDto.getName());
        loginOwnerResDto.setOwnerTel(tmpUserDto.getTel());
        loginOwnerResDto.setToken(token);
        loginOwnerResDto.setKey(newKey);
        
        // 设置响应实体
        context.setResponseEntity(ResultVo.createResponseEntity(loginOwnerResDto));
    }

    /**
     * 更新微信业主应用用户的OpenID
     * 
     * 根据OpenID获取UnionID，并更新或创建微信类型的业主应用用户记录
     * 
     * @param ownerAppUserDto 业主应用用户数据传输对象
     * @param openId 微信OpenID
     */
    private void updateWechatOwnerAppUserOpenId(OwnerAppUserDto ownerAppUserDto, String openId) {
        // 从缓存中获取code2Session信息
        String openUser = CommonCache.getValue("code2Session" + openId);
        if (StringUtil.isEmpty(openUser)) {
            return; // 如果缓存中不存在，直接返回
        }

        // 解析缓存中的响应信息，获取UnionID
        JSONObject responseObj = JSONObject.parseObject(openUser);
        String unionId = responseObj.getString("unionid");

        // 根据UnionID查询微信订阅信息
        WechatSubscribeDto wechatSubscribeDto = new WechatSubscribeDto();
        wechatSubscribeDto.setUnionId(unionId);
        List<WechatSubscribeDto> wechatSubscribeDtos = wechatSubscribeV1InnerServiceSMOImpl.queryWechatSubscribes(wechatSubscribeDto);
        if (ListUtil.isNull(wechatSubscribeDtos)) {
            return; // 如果未找到微信订阅信息，直接返回
        }

        // 查询是否已存在微信类型的业主应用用户记录
        OwnerAppUserDto tOwnerAppUserDto = new OwnerAppUserDto();
        tOwnerAppUserDto.setUserId(ownerAppUserDto.getUserId());
        tOwnerAppUserDto.setCommunityId(ownerAppUserDto.getCommunityId());
        tOwnerAppUserDto.setAppTypeCd(OwnerAppUserDto.APP_TYPE_WECHAT); // 设置应用类型为微信
        List<OwnerAppUserDto> wechatOwnerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(tOwnerAppUserDto);

        // 转换对象为PO
        OwnerAppUserPo wechatOwnerAppUserPo = BeanConvertUtil.covertBean(ownerAppUserDto, OwnerAppUserPo.class);
        wechatOwnerAppUserPo.setAppTypeCd(OwnerAppUserDto.APP_TYPE_WECHAT); // 设置应用类型为微信
        wechatOwnerAppUserPo.setOpenId(wechatSubscribeDtos.get(0).getOpenId()); // 设置OpenID
        
        // 如果不存在微信类型的记录，则创建新记录
        if (ListUtil.isNull(wechatOwnerAppUserDtos)) {
            wechatOwnerAppUserPo.setAppUserId(GenerateCodeFactory.getGeneratorId("11")); // 生成应用用户ID
            ownerAppUserV1InnerServiceSMOImpl.saveOwnerAppUser(wechatOwnerAppUserPo);
            return;
        }
        
        // 如果已存在记录，则更新记录
        wechatOwnerAppUserPo.setAppUserId(wechatOwnerAppUserDtos.get(0).getAppUserId());
        ownerAppUserV1InnerServiceSMOImpl.updateOwnerAppUser(wechatOwnerAppUserPo);
    }

    /**
     * 生成登录token
     * 
     * 根据用户信息生成JWT token并保存到缓存中
     * 
     * @param tmpUserDto 用户数据传输对象
     * @return 生成的token字符串
     */
    private static String generatorLoginToken(UserDto tmpUserDto) {
        String token;
        try {
            // 构建用户信息Map
            Map userMap = new HashMap();
            userMap.put(CommonConstant.LOGIN_USER_ID, tmpUserDto.getUserId()); // 用户ID
            userMap.put(CommonConstant.LOGIN_USER_NAME, tmpUserDto.getUserName()); // 用户名
            
            // 创建并保存token
            token = AuthenticationFactory.createAndSaveToken(userMap);
        } catch (Exception e) {
            logger.error("登录异常：", e);
            throw new CmdException("系统内部错误，请联系管理员");
        }
        return token;
    }

    /**
     * 生成登录key
     * 
     * 为用户生成唯一的登录key，并保存到用户属性中
     * 
     * @param tmpUserDto 用户数据传输对象
     * @return 生成的登录key
     */
    private String generatorLoginKey(UserDto tmpUserDto) {
        // 获取用户属性列表
        List<UserAttrDto> userAttrDtos = tmpUserDto.getUserAttrs();
        // 查找当前用户的key属性
        UserAttrDto userAttrDto = getCurrentUserAttrDto(userAttrDtos, UserAttrDto.SPEC_KEY);
        
        // 生成新的UUID作为key
        String newKey = UUID.randomUUID().toString();
        
        // 如果已存在key属性，则更新
        if (userAttrDto != null) {
            UserAttrPo userAttrPo = BeanConvertUtil.covertBean(userAttrDto, UserAttrPo.class);
            userAttrPo.setValue(newKey); // 设置新的key值
            userAttrPo.setStatusCd("0"); // 设置状态为有效
            userAttrV1InnerServiceSMOImpl.updateUserAttr(userAttrPo);
        } else {
            // 如果不存在key属性，则创建新记录
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setAttrId(GenerateCodeFactory.getAttrId()); // 生成属性ID
            userAttrPo.setUserId(tmpUserDto.getUserId()); // 设置用户ID
            userAttrPo.setSpecCd(UserAttrDto.SPEC_KEY); // 设置属性类型为key
            userAttrPo.setValue(newKey); // 设置key值
            userAttrPo.setStatusCd("0"); // 设置状态为有效
            userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
        }
        return newKey;
    }

    /**
     * 获取当前用户的指定属性
     * 
     * 从用户属性列表中查找指定类型的属性
     * 
     * @param userAttrDtos 用户属性列表
     * @param specCd 属性类型代码
     * @return 匹配的用户属性对象，如果未找到则返回null
     */
    private UserAttrDto getCurrentUserAttrDto(List<UserAttrDto> userAttrDtos, String specCd) {
        // 如果属性列表为空，直接返回null
        if (userAttrDtos == null) {
            return null;
        }
        
        // 遍历属性列表，查找匹配的属性类型
        for (UserAttrDto userAttrDto : userAttrDtos) {
            if (specCd.equals(userAttrDto.getSpecCd())) {
                return userAttrDto; // 返回匹配的属性对象
            }
        }

        return null; // 未找到匹配的属性
    }
}