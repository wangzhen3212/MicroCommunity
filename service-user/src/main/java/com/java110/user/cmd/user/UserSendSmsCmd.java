/**
 * 用户发送短信验证码命令类
 * 
 * 该命令类负责处理用户发送短信验证码的请求，包括手机号验证、业主信息校验、
 * 短信发送频率控制以及验证码生成和发送等功能。
 * 主要用于用户注册、登录、密码重置等需要手机验证码的场景。
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
import com.java110.core.factory.SendSmsFactory;
import com.java110.dto.msg.SmsDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.common.ISmsInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.utils.util.ValidatorUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "user.userSendSms")
public class UserSendSmsCmd extends Cmd {

    // 域常量定义
    /** 公共域常量 */
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    /** 身份证开关配置键 */
    public static final String ID_CARD_SWITCH = "ID_CARD_SWITCH";

    /** 业主信息服务接口 */
    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    /** 用户信息服务接口 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /** 短信服务接口 */
    @Autowired
    private ISmsInnerServiceSMO smsInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括手机号格式验证、业主信息校验等
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证手机号参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "tel", "必填，请填写手机号");

        // 验证手机号格式是否正确
        if (!ValidatorUtil.isMobile(reqJson.getString("tel"))) {
            throw new IllegalArgumentException("手机号格式错误");
        }

        // 获取验证码类型，如果是业主绑定类型，需要验证业主信息
        String captchaType = reqJson.getString("captchaType");
        if (!StringUtil.isEmpty(captchaType) && "ownerBinding".equals(captchaType)) {
            // 创建业主查询对象
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setCommunityId(reqJson.getString("communityId"));
            ownerDto.setName(reqJson.getString("appUserName"));
            ownerDto.setLink(reqJson.getString("tel"));

            // 从配置中获取身份证验证开关状态
            String val = MappingCache.getValue(DOMAIN_COMMON, ID_CARD_SWITCH);
            
            String idCardErrorMsg = "";
            String idCard = reqJson.getString("idCard");
            
            // 如果身份证验证开关开启且身份证号不为空，则验证身份证信息
            if ("1".equals(val) && !StringUtil.isEmpty(idCard)) {
                ownerDto.setIdCard(idCard);
                idCardErrorMsg = "或者身份证号";
            }
            
            // 查询业主信息，验证业主信息是否匹配
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnerMembers(ownerDto);
            
            // 验证查询结果有且仅有一条记录
            Assert.listOnlyOne(ownerDtos, "填写业主信息错误，请确认，预留业主姓名、手机号" + idCardErrorMsg + "信息是否正确！");
        }
    }

    /**
     * 执行发送短信验证码命令
     * 
     * 主要功能包括：
     * 1. 获取分布式锁防止重复发送
     * 2. 验证短信发送频率
     * 3. 生成验证码
     * 4. 发送短信或模拟发送
     * 5. 返回发送结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 生成分布式锁的请求ID
        String requestId = DistributedLock.getLockUUID();
        // 构建锁的key，使用类名+手机号确保唯一性
        String key = this.getClass().getSimpleName() + reqJson.getString("tel");
        
        try {
            // 获取分布式锁，防止同一手机号重复发送
            DistributedLock.waitGetDistributedLock(key, requestId);
            
            // 从缓存中获取已存在的验证码
            String smsCode = CommonCache.getValue(reqJson.getString("tel") + SendSmsFactory.VALIDATE_CODE);

            // 检查验证码是否存在且未过期（2分钟内）
            if (!StringUtil.isEmpty(smsCode) && smsCode.contains("-")) {
                // 解析验证码中的时间戳
                long oldTime = Long.parseLong(smsCode.substring(smsCode.indexOf("-") + 1, smsCode.length()));
                long nowTime = new Date().getTime();
                
                // 如果距离上次发送时间不足2分钟，则不允许重复发送
                if (nowTime - oldTime < 1000 * 60 * 2) {
                    throw new IllegalArgumentException("请不要重复发送验证码");
                }
            }

            // 获取手机号
            String tel = reqJson.getString("tel");
            
            // 生成6位数字验证码
            String msgCode = SendSmsFactory.generateMessageCode(6);
            
            // 创建短信数据传输对象
            SmsDto smsDto = new SmsDto();
            smsDto.setTel(tel);
            smsDto.setCode(msgCode);
            
            // 检查短信发送开关是否开启
            if ("ON".equals(MappingCache.getValue(MappingConstant.SMS_DOMAIN, SendSmsFactory.SMS_SEND_SWITCH))) {
                // 调用短信服务发送真实短信
                smsDto = smsInnerServiceSMOImpl.send(smsDto);
            } else {
                // 短信发送开关关闭，模拟发送，将验证码存入缓存
                CommonCache.setValue(smsDto.getTel() + SendSmsFactory.VALIDATE_CODE, 
                                   smsDto.getCode().toLowerCase() + "-" + new Date().getTime(), 
                                   CommonCache.defaultExpireTime);
                smsDto.setSuccess(true);
                smsDto.setMsg("您的验证码为" + msgCode);
            }

            // 设置响应结果
            context.setResponseEntity(ResultVo.createResponseEntity(smsDto.getMsg()));
        } finally {
            // 释放分布式锁，确保锁一定会被释放
            DistributedLock.releaseDistributedLock(key, requestId);
        }
    }
}