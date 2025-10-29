/**
 * 业主应用用户绑定业主命令类
 * 
 * 功能：处理移动端用户绑定业主信息的业务逻辑，包括参数验证、业主信息查询、绑定关系创建等
 * 用途：用于移动端应用用户提交业主绑定申请，验证身份信息后建立用户与业主的关联关系
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.factory.SendSmsFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.msg.SmsDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.ISmsInnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "owner.appUserBindingOwner")
public class AppUserBindingOwnerCmd extends Cmd {

    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    @Autowired
    private ISmsInnerServiceSMO smsInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能：验证请求参数的有效性，包括必填字段检查、短信验证码验证等
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 数据流上下文，用于获取请求头等信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "communityName", "未包含小区名称");
        Assert.hasKeyAndValue(reqJson, "areaCode", "未包含小区地区");
        Assert.hasKeyAndValue(reqJson, "appUserName", "未包含用户名称");
        Assert.hasKeyAndValue(reqJson, "idCard", "未包含身份证号");
        Assert.hasKeyAndValue(reqJson, "link", "未包含联系电话");
        Assert.hasKeyAndValue(reqJson, "msgCode", "未包含联系电话验证码");

        // 从请求头中获取用户ID并验证
        Map<String, String> headers = context.getReqHeaders();
        Assert.hasKeyAndValue(headers, "user_id", "请求头中未包含用户信息");
        
        // 创建短信验证DTO并验证验证码
        SmsDto smsDto = new SmsDto();
        smsDto.setTel(reqJson.getString("link")); // 设置手机号码
        smsDto.setCode(reqJson.getString("msgCode")); // 设置验证码
        smsDto = smsInnerServiceSMOImpl.validateCode(smsDto); // 调用短信服务验证验证码

        // 如果短信验证失败且短信发送开关为开启状态，则抛出异常
        if (!smsDto.isSuccess() && "ON".equals(MappingCache.getValue(MappingConstant.SMS_DOMAIN,SendSmsFactory.SMS_SEND_SWITCH))) {
            throw new IllegalArgumentException(smsDto.getMsg());
        }
    }

    /**
     * 命令执行方法
     * 
     * 功能：执行业主绑定业务逻辑，包括用户信息查询、业主信息验证、绑定关系创建等
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID
        Map<String, String> headers = context.getReqHeaders();
        String userId = headers.get("user_id");
        
        // 根据用户ID查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);
        
        // 验证用户信息唯一性
        Assert.listOnlyOne(userDtos, "未找到相应用户信息，或查询到多条");
        
        // 获取用户的openId并验证
        String openId = userDtos.get(0).getOpenId();
        Assert.hasLength(openId, "该用户不是能力开放用户");

        // 检查是否已经申请过绑定业主
        OwnerAppUserDto ownerAppUserDto = BeanConvertUtil.covertBean(reqJson, OwnerAppUserDto.class);
        ownerAppUserDto.setStates(new String[]{"10000", "12000"}); // 设置查询状态：审核中和审核成功
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        
        // 如果已经存在绑定申请，则抛出异常
        if (ownerAppUserDtos != null && ownerAppUserDtos.size() > 0) {
            throw new IllegalArgumentException("已经申请过绑定业主");
        }

        // 查询小区信息是否存在
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCityCode(reqJson.getString("areaCode")); // 设置地区编码
        communityDto.setName(reqJson.getString("communityName")); // 设置小区名称
        communityDto.setState("1100"); // 设置小区状态为有效
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 验证小区信息唯一性
        Assert.listOnlyOne(communityDtos, "填写小区信息错误");
        CommunityDto tmpCommunityDto = communityDtos.get(0); // 获取查询到的小区信息

        // 查询业主信息是否存在
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(tmpCommunityDto.getCommunityId()); // 设置小区ID
        ownerDto.setIdCard(reqJson.getString("idCard")); // 设置身份证号
        ownerDto.setName(reqJson.getString("appUserName")); // 设置业主姓名
        ownerDto.setLink(reqJson.getString("link")); // 设置联系电话
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnerMembers(ownerDto);
        
        // 验证业主信息唯一性
        Assert.listOnlyOne(ownerDtos, "填写业主信息错误");
        OwnerDto tmpOwnerDto = ownerDtos.get(0); // 获取查询到的业主信息

        // 准备绑定数据
        reqJson.put("openId", openId); // 设置openId
        reqJson.put("userId", userId); // 设置用户ID

        // 构建业主应用用户绑定信息
        JSONObject businessOwnerAppUser = new JSONObject();
        businessOwnerAppUser.putAll(reqJson); // 复制所有请求参数
        // 设置绑定状态和类型
        businessOwnerAppUser.put("state", "12000"); // 状态：审核成功
        businessOwnerAppUser.put("appTypeCd", "10010"); // 应用类型代码
        businessOwnerAppUser.put("appUserId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_appUserId)); // 生成应用用户ID
        businessOwnerAppUser.put("memberId", ownerDto.getMemberId()); // 设置成员ID
        businessOwnerAppUser.put("communityName", communityDto.getName()); // 设置小区名称
        businessOwnerAppUser.put("communityId", communityDto.getCommunityId()); // 设置小区ID
        businessOwnerAppUser.put("appUserName", ownerDto.getName()); // 设置应用用户名称
        businessOwnerAppUser.put("idCard", ownerDto.getIdCard()); // 设置身份证号
        businessOwnerAppUser.put("link", ownerDto.getLink()); // 设置联系电话
        businessOwnerAppUser.put("userId", reqJson.getString("userId")); // 设置用户ID
        
        // 转换为PO对象并保存绑定关系
        OwnerAppUserPo ownerAppUserPo = BeanConvertUtil.covertBean(businessOwnerAppUser, OwnerAppUserPo.class);
        int flag = ownerAppUserV1InnerServiceSMOImpl.saveOwnerAppUser(ownerAppUserPo);
        
        // 验证保存结果
        if (flag < 1) {
            throw new IllegalArgumentException("保存业主失败");
        }
    }
}