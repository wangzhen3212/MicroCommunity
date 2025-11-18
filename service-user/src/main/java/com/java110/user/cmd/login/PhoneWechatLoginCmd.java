package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.client.RestTemplate;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.system.SystemInfoDto;
import com.java110.dto.user.LoginOwnerResDto;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.common.ISystemInfoV1InnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.job.IIotInnerServiceSMO;
import com.java110.intf.job.IMallInnerServiceSMO;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.po.user.UserPo;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.apache.commons.net.util.Base64;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 微信一键登录命令类
 * 处理微信小程序手机号一键登录功能，包括获取微信session_key、解密手机号、
 * 用户注册/登录、自动绑定业主信息等业务流程
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode login.phoneWechatLogin
 */
@Java110Cmd(serviceCode = "login.phoneWechatLogin")
public class PhoneWechatLoginCmd extends Cmd {
    private final static Logger logger = LoggerFactory.getLogger(PhoneWechatLoginCmd.class);

    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    @Autowired
    private RestTemplate outRestTemplate;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IMallInnerServiceSMO mallInnerServiceSMOImpl;

    @Autowired
    private IIotInnerServiceSMO iotInnerServiceSMOImpl;

    @Autowired
    private ISystemInfoV1InnerServiceSMO systemInfoV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查必要的微信登录参数是否完整
     *
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "encryptedData", "未包含encryptedData");
        Assert.hasKeyAndValue(reqJson, "iv", "未包含iv");
        Assert.hasKeyAndValue(reqJson, "code", "未包含code");
        Assert.hasKeyAndValue(reqJson, "appId", "未包含appId");
    }

    /**
     * 执行微信一键登录命令
     * 完整的登录流程：获取session_key -> 解密手机号 -> 用户注册/登录 -> 同步用户信息 -> 生成登录凭证
     *
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 登录过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求中获取微信登录相关参数
        String code = reqJson.getString("code");
        String encryptedData = reqJson.getString("encryptedData");
        String iv = reqJson.getString("iv");
        String appId = reqJson.getString("appId");

        // 根据appId查询小程序配置信息
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setAppId(appId);
        List<SmallWeChatDto> smallWeChatDtos = smallWechatV1InnerServiceSMOImpl.querySmallWechats(smallWeChatDto);
        if (ListUtil.isNull(smallWeChatDtos)) {
            throw new CmdException("未配置小程序信息");
        }

        // 调用微信接口获取session_key
        String sessionKey = getSessionKey(code, smallWeChatDtos.get(0));
        if (sessionKey == null) {
            throw new CmdException("获取session_key失败");
        }

        // 使用session_key解密获取手机号
        String phoneNumber = "";
        try {
            phoneNumber = decryptData(encryptedData, iv, sessionKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CmdException(e.getMessage());
        }

        if (StringUtil.isEmpty(phoneNumber)) {
            throw new CmdException("获取手机号失败");
        }

        // 根据手机号查询用户信息
        UserDto userDto = new UserDto();
        userDto.setTel(phoneNumber);
        userDto.setLevelCd(UserDto.LEVEL_CD_USER);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        
        // 如果用户不存在，则自动注册新用户
        if (ListUtil.isNull(userDtos)) {
            registerUser(phoneNumber);
            userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        }
        
        // 同步用户信息到商城系统
        mallInnerServiceSMOImpl.sendUserInfo(userDtos.get(0));

        // 同步用户信息到物联网系统
        iotInnerServiceSMOImpl.sendUserInfo(userDtos.get(0));

        String communityId = "";
        // 查询用户是否已经认证绑定业主信息
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userDtos.get(0).getUserId());
        ownerAppUserDto.setLink(userDtos.get(0).getTel());
        ownerAppUserDto.setState(OwnerAppUserDto.STATE_AUDIT_SUCCESS);
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        // 如果未绑定业主，则自动绑定
        if (ListUtil.isNull(ownerAppUserDtos)) {
            autoBindUserToOwner(userDtos.get(0), phoneNumber);
            communityId = smallWeChatDtos.get(0).getObjId();
        } else {
            communityId = ownerAppUserDtos.get(0).getCommunityId();
        }

        // 验证小区信息是否存在
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(communityId);
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        Assert.listOnlyOne(communityDtos, "小区不存在，确保开发者账户配置默认小区" + communityId);

        // 生成用户登录凭证
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
     * 自动绑定用户到业主信息
     * 根据手机号查询业主信息，并建立用户与业主的关联关系
     *
     * @param userDto 用户信息
     * @param phoneNumber 手机号码
     */
    private void autoBindUserToOwner(UserDto userDto, String phoneNumber) {
        // 根据手机号查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(phoneNumber);
        ownerDto.setPage(1);
        ownerDto.setRow(1);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 如果业主不存在，直接返回
        if (ListUtil.isNull(ownerDtos)) {
            return;
        }
        
        // 查询小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(ownerDtos.get(0).getCommunityId());
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        if (ListUtil.isNull(communityDtos)) {
            return;
        }
        CommunityDto tmpCommunityDto = communityDtos.get(0);

        // 创建业主应用用户关联记录
        OwnerAppUserPo ownerAppUserPo = new OwnerAppUserPo();
        // 状态类型：10000 审核中，12000 审核成功，13000 审核失败
        ownerAppUserPo.setState("12000");
        ownerAppUserPo.setAppTypeCd(OwnerAppUserDto.APP_TYPE_WECHAT_MINA);
        ownerAppUserPo.setAppUserId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_appUserId));
        ownerAppUserPo.setMemberId(ownerDtos.get(0).getMemberId());
        ownerAppUserPo.setCommunityName(tmpCommunityDto.getName());
        ownerAppUserPo.setCommunityId(ownerDtos.get(0).getCommunityId());
        ownerAppUserPo.setAppUserName(ownerDtos.get(0).getName());
        ownerAppUserPo.setIdCard(ownerDtos.get(0).getIdCard());
        ownerAppUserPo.setAppType("WECHAT");
        ownerAppUserPo.setLink(ownerDtos.get(0).getLink());
        ownerAppUserPo.setUserId(userDto.getUserId());
        ownerAppUserPo.setOpenId("-1");
        
        // 保存关联关系
        ownerAppUserV1InnerServiceSMOImpl.saveOwnerAppUser(ownerAppUserPo);
    }

    /**
     * 注册新用户
     * 根据手机号创建新的用户账户
     *
     * @param phoneNumber 手机号码
     * @throws CmdException 注册失败时抛出异常
     */
    private void registerUser(String phoneNumber) {
        // 生成随机密码
        String userPassword = AuthenticationFactory.passwdMd5(GenerateCodeFactory.getRandomCode(6));

        // 创建用户信息
        UserPo userPo = new UserPo();
        userPo.setUserId(GenerateCodeFactory.getGeneratorId("30"));
        userPo.setName(phoneNumber);
        userPo.setTel(phoneNumber);
        userPo.setLevelCd(UserDto.LEVEL_CD_USER);
        userPo.setPassword(AuthenticationFactory.passwdMd5(userPassword));
        
        // 保存用户信息
        int flag = userV1InnerServiceSMOImpl.saveUser(userPo);
        if (flag < 1) {
            throw new CmdException("注册失败");
        }
    }

    /**
     * 获取微信session_key
     * 调用微信接口根据code获取session_key
     *
     * @param code 微信登录code
     * @param smallWeChatDto 小程序配置信息
     * @return session_key字符串
     * @throws CmdException 获取session_key失败时抛出异常
     */
    private String getSessionKey(String code, SmallWeChatDto smallWeChatDto) {
        String secret = smallWeChatDto.getAppSecret();
        // 构建微信接口URL
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                smallWeChatDto.getAppId(), secret, code
        );
        
        // 调用微信接口
        String responseStr = outRestTemplate.getForObject(url, String.class);
        JSONObject response = JSONObject.parseObject(responseStr);

        // 检查接口返回结果
        if (response == null || response.containsKey("errcode")) {
            throw new CmdException("微信接口调用失败");
        }

        return response.getString("session_key");
    }

    /**
     * 解密微信加密数据
     * 使用AES算法解密微信返回的加密手机号数据
     *
     * @param encryptedData 加密数据
     * @param iv 加密算法的初始向量
     * @param sessionKey 会话密钥
     * @return 解密后的手机号码
     * @throws Exception 解密过程中出现异常
     */
    public String decryptData(String encryptedData, String iv, String sessionKey) throws Exception {
        // Base64解码相关参数
        byte[] keyBytes = Base64.decodeBase64(sessionKey);
        byte[] ivBytes = Base64.decodeBase64(iv);
        byte[] encryptedDataBytes = Base64.decodeBase64(encryptedData);

        // 初始化AES cipher
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        // 解密数据
        byte[] decryptedBytes = cipher.doFinal(encryptedDataBytes);
        String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);

        // 解析JSON获取手机号
        JSONObject data = JSONObject.parseObject(decryptedText);
        return data.getString("purePhoneNumber");
    }

    /**
     * 生成用户登录密钥
     * 为用户生成唯一的登录密钥，用于后续的身份验证
     *
     * @param tmpUserDto 用户信息
     * @return 生成的登录密钥
     */
    private String generatorLoginKey(UserDto tmpUserDto) {
        List<UserAttrDto> userAttrDtos = tmpUserDto.getUserAttrs();
        UserAttrDto userAttrDto = getCurrentUserAttrDto(userAttrDtos, UserAttrDto.SPEC_KEY);
        String newKey = UUID.randomUUID().toString();
        
        // 如果用户已有密钥则更新，否则创建新的密钥
        if (userAttrDto != null) {
            UserAttrPo userAttrPo = BeanConvertUtil.covertBean(userAttrDto, UserAttrPo.class);
            userAttrPo.setValue(newKey);
            userAttrPo.setStatusCd("0");
            userAttrV1InnerServiceSMOImpl.updateUserAttr(userAttrPo);
        } else {
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setAttrId(GenerateCodeFactory.getAttrId());
            userAttrPo.setUserId(tmpUserDto.getUserId());
            userAttrPo.setSpecCd(UserAttrDto.SPEC_KEY);
            userAttrPo.setValue(newKey);
            userAttrPo.setStatusCd("0");
            userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
        }
        return newKey;
    }

    /**
     * 获取用户属性信息
     * 根据属性编码从用户属性列表中查找对应的属性
     *
     * @param userAttrDtos 用户属性列表
     * @param specCd 属性编码
     * @return 匹配的用户属性对象，未找到时返回null
     */
    private UserAttrDto getCurrentUserAttrDto(List<UserAttrDto> userAttrDtos, String specCd) {
        if (userAttrDtos == null) {
            return null;
        }
        // 遍历查找指定编码的属性
        for (UserAttrDto userAttrDto : userAttrDtos) {
            if (specCd.equals(userAttrDto.getSpecCd())) {
                return userAttrDto;
            }
        }

        return null;
    }

    /**
     * 生成登录token
     * 为用户生成JWT token，用于后续的接口身份验证
     *
     * @param tmpUserDto 用户信息
     * @return 生成的登录token
     * @throws CmdException token生成失败时抛出异常
     */
    private static String generatorLoginToken(UserDto tmpUserDto) {
        String token;
        try {
            // 构建token包含的用户信息
            Map userMap = new HashMap();
            userMap.put(CommonConstant.LOGIN_USER_ID, tmpUserDto.getUserId());
            userMap.put(CommonConstant.LOGIN_USER_NAME, tmpUserDto.getUserName());
            // 创建并保存token
            token = AuthenticationFactory.createAndSaveToken(userMap);
        } catch (Exception e) {
            logger.error("登录异常：", e);
            throw new CmdException("系统内部错误，请联系管理员");
        }
        return token;
    }
}