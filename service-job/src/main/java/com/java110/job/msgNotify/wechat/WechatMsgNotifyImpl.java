package com.java110.job.msgNotify.wechat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.client.RestTemplate;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.mapping.Mapping;
import com.java110.dto.notice.NoticeStaffDto;
import com.java110.dto.notice.NoticeWechatDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.dto.user.StaffAppAuthDto;
import com.java110.dto.wechat.Content;
import com.java110.dto.wechat.PropertyFeeTemplateMessage;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.intf.user.IStaffAppAuthInnerServiceSMO;
import com.java110.job.msgNotify.IMsgNotify;
import com.java110.job.msgNotify.IWechatTemplate;
import com.java110.job.msgNotify.MsgNotifyFactory;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.cache.UrlCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("wechatMsgNotifyImpl")
public class WechatMsgNotifyImpl implements IMsgNotify {
    private static final Logger logger = LoggerFactory.getLogger(WechatMsgNotifyImpl.class);

    private static final String sendMsgUrl = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=";

    private static final int THING_MAX_LENGTH = 20;

    public static final String SPEC_CD_TOKEN = "33001";
    public static final String SPEC_CD_OWE_FEE_TEMPLATE = "33002";
    public static final String SPEC_CD_WECHAT_TEMPLATE = "33003";
    public static final String SPEC_CD_WECHAT_SUCCESS_TEMPLATE = "33004";
    public static final String SPEC_CD_WECHAT_EXPIRE_TEMPLATE = "33005";
    public static final String SPEC_CD_WECHAT_PROCESS_TEMPLATE = "33006";
    public static final String SPEC_CD_WECHAT_ROOM_STATE_TEMPLATE = "33007";
    public static final String SPEC_CD_WECHAT_WORK_ORDER_REMIND_TEMPLATE = "33008";
    public static final String SPEC_CD_WECHAT_DISPATCH_REMIND_TEMPLATE = "33009";
    public static final String SPEC_CD_WECHAT_SCHEDULE_TEMPLATE = "33010";
    public static final String SPEC_CD_WECHAT_WORK_ORDER_END_TEMPLATE = "33011";
    public static final String SPEC_CD_WECHAT_HOUSE_DECORATION_APPLY_TEMPLATE = "33012";
    public static final String SPEC_CD_WECHAT_HOUSE_DECORATION_CHECK_TEMPLATE = "33013";
    public static final String SPEC_CD_WECHAT_HOUSE_DECORATION_CHECK_RESULT_TEMPLATE = "33014";
    public static final String SPEC_CD_WECHAT_HOUSE_DECORATION_COMPLETED_TEMPLATE = "33015";
    public static final String SPEC_CD_WECHAT_REPAIR_CHARGE_SCENE_TEMPLATE = "33016";
    public static final String SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_TEMPLATE = "33017";
    public static final String SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_FINISH_TEMPLATE = "33018";

    private static final Map<String, String[]> templateKeys = new HashMap<>();

    static {
        templateKeys.put(SPEC_CD_OWE_FEE_TEMPLATE, new String[]{"缴费类型", "房号", "总金额", "缴费周期"});
        templateKeys.put(SPEC_CD_WECHAT_PROCESS_TEMPLATE, new String[]{"流程名称", "发起时间", "发起人"});
        templateKeys.put(SPEC_CD_WECHAT_SUCCESS_TEMPLATE, new String[]{"缴费房间", "费用类型", "费用周期", "缴费金额"});
        templateKeys.put(SPEC_CD_WECHAT_WORK_ORDER_REMIND_TEMPLATE, new String[]{"报修类型", "报修地址", "报修问题"});
        templateKeys.put(SPEC_CD_WECHAT_DISPATCH_REMIND_TEMPLATE, new String[]{"联系人", "手机号", "报修时间", "维修地址"});
        templateKeys.put(SPEC_CD_WECHAT_SCHEDULE_TEMPLATE, new String[]{"平台受理人", "联系电话", "受理时间"});
        templateKeys.put(SPEC_CD_WECHAT_WORK_ORDER_END_TEMPLATE, new String[]{"房屋地址", "维修工程师", "维修完成时间"});
        templateKeys.put(SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_TEMPLATE, new String[]{"流程名称", "发起人", "发起时间"});
        templateKeys.put(SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_FINISH_TEMPLATE, new String[]{"流程名称", "流程状态"});
    }

    /**
     * 截断 thing 类型字段值到20个字符以内（微信模板消息 thing 类型限制）
     */
    private static String truncateThing(String value) {
        if (!StringUtil.isEmpty(value) && value.length() > THING_MAX_LENGTH) {
            return value.substring(0, THING_MAX_LENGTH);
        }
        return value;
    }

    /**
     * 处理微信模板消息发送响应
     * 43101(用户拒收)时返回 CODE_OK，避免后续不断重试
     */
    private ResultVo handleSendResponse(JSONObject paramOut, String openId, String extraInfo) {
        int errcode = paramOut.getIntValue("errcode");
        String errmsg = paramOut.getString("errmsg");
        if (errcode == 0) {
            logger.info("模板消息发送成功, openId={}, extra={}", openId, extraInfo);
            return new ResultVo(ResultVo.CODE_OK, errmsg);
        }
        if (errcode == 43101) {
            logger.warn("用户拒收模板消息, openId={}, extra={}, 标记为已推送避免重复发送", openId, extraInfo);
            return new ResultVo(ResultVo.CODE_OK, errmsg);
        }
        logger.warn("模板消息发送失败, openId={}, extra={}, errcode={}, errmsg={}", openId, extraInfo, errcode, errmsg);
        return new ResultVo(errcode, errmsg);
    }

    @Autowired
    private IWechatTemplate wechatTemplateImpl;

    @Autowired
    private IStaffAppAuthInnerServiceSMO staffAppAuthInnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;


    @Autowired
    private RestTemplate outRestTemplate;

    @Override
    public ResultVo sendApplyReturnFeeMsg(String communityId, String userId, JSONObject content) {

        String accessToken = wechatTemplateImpl.getAccessToken(communityId);

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(userId);
        staffAppAuthDto.setAppType("WECHAT");
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        if (ListUtil.isNull(staffAppAuthDtos)) {
            logger.info("员工未认证，没有获取到微信openId，staffId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
        }
        String openId = staffAppAuthDtos.get(0).getOpenId();
        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_PROCESS_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_PROCESS_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_PROCESS_TEMPLATE));

        String url = sendMsgUrl + accessToken;

        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing2", new Content("退费申请审批"));
        data.put("time10", new Content(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B)));
        data.put("thing9", new Content(truncateThing(content.getString("name"))));
        templateMessage.setData(data);
        String wechatUrl = MappingCache.getValue(MappingConstant.URL_DOMAIN, "STAFF_WECHAT_URL");
        templateMessage.setUrl(wechatUrl);
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("name"));
    }

    @Override
    public ResultVo sendOweFeeMsg(String communityId, String userId, String ownerId, List<JSONObject> contents) {

        if (StringUtil.isEmpty(userId) || userId.startsWith("-")) {
            logger.info("业主未绑定，没有获取到微信openId，userId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "业主未绑定，没有获取到微信openId");
        }

        String accessToken = wechatTemplateImpl.getAccessToken(communityId);

        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setCommunityId(communityId);
        ownerAppUserDto.setAppType(OwnerAppUserDto.APP_TYPE_WECHAT);
        ownerAppUserDto.setUserId(userId);
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        if (ListUtil.isNull(ownerAppUserDtos)) {
            logger.info("业主未绑定，没有获取到微信openId，userId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "业主未绑定，没有获取到微信openId");
        }

        String openId = ownerAppUserDtos.get(0).getOpenId();

        if (StringUtil.isEmpty(openId) || openId.startsWith("-")) {
            throw new IllegalArgumentException("没有获取到微信openId");
        }
        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_OWE_FEE_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_OWE_FEE_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_OWE_FEE_TEMPLATE));

        String url = sendMsgUrl + accessToken;
        for (JSONObject content : contents) {
            JSONObject data = new JSONObject();
            PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
            templateMessage.setTemplate_id(templateId);
            templateMessage.setTouser(openId);
            data.put("thing2", new Content(truncateThing(content.getString("feeTypeName"))));
            data.put("thing12", new Content(truncateThing(content.getString("payerObjName"))));
            data.put("amount3", new Content(content.getString("billAmountOwed")));
            data.put("time19", new Content(content.getString("date")));
            templateMessage.setData(data);
            templateMessage.setUrl(content.getString("url") + "&wAppId=" + wechatTemplateImpl.getAppId(communityId));
            logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
            ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
            JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
            ResultVo resultVo = handleSendResponse(paramOut, openId, content.getString("payerObjName"));
            if (resultVo.getCode() != ResultVo.CODE_OK) {
                return resultVo;
            }
        }
        return new ResultVo(ResultVo.CODE_OK, "成功");
    }

    @Override
    public ResultVo sendPayFeeMsg(String communityId, String userId, JSONObject content, String role) {

        String accessToken = wechatTemplateImpl.getAccessToken(communityId);
        String url = sendMsgUrl + accessToken;
        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_SUCCESS_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_SUCCESS_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_SUCCESS_TEMPLATE));
        String openId = "";
        if (MsgNotifyFactory.ROLE_OWNER.equals(role)) {
            OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
            ownerAppUserDto.setCommunityId(communityId);
            ownerAppUserDto.setAppType(OwnerAppUserDto.APP_TYPE_WECHAT);
            ownerAppUserDto.setUserId(userId);
            List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
            if (ListUtil.isNull(ownerAppUserDtos)) {
                logger.info("业主未绑定，没有获取到微信openId，userId={}", userId);
                return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "业主未绑定，没有获取到微信openId");
            }
            openId = ownerAppUserDtos.get(0).getOpenId();
        } else {
            StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
            staffAppAuthDto.setStaffId(userId);
            staffAppAuthDto.setAppType("WECHAT");
            List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
            if (ListUtil.isNull(staffAppAuthDtos)) {
                logger.info("员工未认证，没有获取到微信openId，staffId={}", userId);
                return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
            }
            openId = staffAppAuthDtos.get(0).getOpenId();
        }
        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing2", new Content(truncateThing(content.getString("feeName"))));
        data.put("thing10", new Content(truncateThing(content.getString("payFeeRoom"))));
        data.put("time18", new Content(content.getString("payFeeTime")));
        data.put("amount6", new Content(content.getString("receivedAmount")));
        templateMessage.setData(data);
        templateMessage.setUrl(content.getString("url"));
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("feeName"));
    }

    @Override
    public ResultVo sendAddOwnerRepairMsg(String communityId, String userId, JSONObject content) {
        String accessToken = wechatTemplateImpl.getAccessToken(communityId);

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(userId);
        staffAppAuthDto.setAppType("WECHAT");
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        if (ListUtil.isNull(staffAppAuthDtos)) {
            logger.info("员工未认证，没有获取到微信openId，staffId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
        }
        String openId = staffAppAuthDtos.get(0).getOpenId();

        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_WORK_ORDER_REMIND_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_WORK_ORDER_REMIND_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_WORK_ORDER_REMIND_TEMPLATE));

        String url = sendMsgUrl + accessToken;
        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing8", new Content(truncateThing(content.getString("repairTypeName"))));
        data.put("thing11", new Content(truncateThing(content.getString("repairObjName"))));
        data.put("thing10", new Content(truncateThing(content.getString("context"))));
        templateMessage.setData(data);
        templateMessage.setUrl(content.getString("url"));
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("repairObjName"));
    }

    @Override
    public ResultVo sendDistributeRepairStaffMsg(String communityId, String userId, JSONObject content) {
        String accessToken = wechatTemplateImpl.getAccessToken(communityId);

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(userId);
        staffAppAuthDto.setAppType("WECHAT");
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        if (ListUtil.isNull(staffAppAuthDtos)) {
            logger.info("员工未认证，没有获取到微信openId，staffId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
        }
        String openId = staffAppAuthDtos.get(0).getOpenId();

        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_DISPATCH_REMIND_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_DISPATCH_REMIND_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_DISPATCH_REMIND_TEMPLATE));

        String url = sendMsgUrl + accessToken;
        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing7", new Content(truncateThing(content.getString("repairName"))));
        data.put("phone_number3", new Content(content.getString("tel")));
        data.put("time13", new Content(content.getString("time")));
        data.put("thing9", new Content(truncateThing(content.getString("address"))));
        templateMessage.setData(data);
        templateMessage.setUrl(content.getString("url"));
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("repairName"));
    }

    @Override
    public ResultVo sendStaffMsg(NoticeStaffDto noticeStaffDto) {

        String accessToken = wechatTemplateImpl.getAccessToken(noticeStaffDto.getCommunityId());

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(noticeStaffDto.getStaffId());
        staffAppAuthDto.setAppType("WECHAT");
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        if (ListUtil.isNull(staffAppAuthDtos)) {
            logger.info("员工未认证，没有获取到微信openId，staffId={}", noticeStaffDto.getStaffId());
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
        }
        String openId = staffAppAuthDtos.get(0).getOpenId();
        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_PROCESS_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_PROCESS_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(noticeStaffDto.getCommunityId(), mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_PROCESS_TEMPLATE));

        String url = sendMsgUrl + accessToken;

        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing2", new Content(truncateThing(noticeStaffDto.getTitle())));
        data.put("time10", new Content(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B)));
        data.put("thing9", new Content(truncateThing(noticeStaffDto.getNotifyUserName())));
        templateMessage.setData(data);

        String wechatUrl = MappingCache.getValue(MappingConstant.URL_DOMAIN, "STAFF_WECHAT_URL");
        templateMessage.setUrl(wechatUrl);
        if (!StringUtil.isEmpty(noticeStaffDto.getUrl())) {
            templateMessage.setUrl(noticeStaffDto.getUrl());
        }
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, noticeStaffDto.getStaffId());
    }

    @Override
    public ResultVo sendWechatMsg(NoticeWechatDto noticeWechatDto) {
        String accessToken = wechatTemplateImpl.getAccessToken(noticeWechatDto.getCommunityId());


        String openId = noticeWechatDto.getOpenId();
        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_PROCESS_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_PROCESS_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(noticeWechatDto.getCommunityId(), mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_PROCESS_TEMPLATE));

        String url = sendMsgUrl + accessToken;

        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing2", new Content(truncateThing(noticeWechatDto.getTitle())));
        data.put("time10", new Content(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B)));
        data.put("thing9", new Content(truncateThing(noticeWechatDto.getNotifyUserName())));
        templateMessage.setData(data);

        String wechatUrl = UrlCache.getOwnerUrl();
        templateMessage.setUrl(wechatUrl);
        if (!StringUtil.isEmpty(noticeWechatDto.getUrl())) {
            templateMessage.setUrl(noticeWechatDto.getUrl());
        }
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, noticeWechatDto.getTitle());
    }

    @Override
    public ResultVo sendDistributeRepairOwnerMsg(String communityId, String userId, JSONObject content) {
        String accessToken = wechatTemplateImpl.getAccessToken(communityId);
        String url = sendMsgUrl + accessToken;
        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_SCHEDULE_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_SCHEDULE_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_SCHEDULE_TEMPLATE));
        String openId = "";

        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setCommunityId(communityId);
        ownerAppUserDto.setAppType(OwnerAppUserDto.APP_TYPE_WECHAT);
        ownerAppUserDto.setUserId(userId);
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        if (ListUtil.isNull(ownerAppUserDtos)) {
            logger.info("业主未绑定，没有获取到微信openId，userId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "业主未绑定，没有获取到微信openId");
        }
        openId = ownerAppUserDtos.get(0).getOpenId();

        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing6", new Content(content.getString("name")));
        data.put("phone_number9", new Content(content.getString("tel")));
        data.put("time3", new Content(content.getString("time")));
        templateMessage.setData(data);
        templateMessage.setUrl(content.getString("url"));
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("name"));
    }

    @Override
    public ResultVo sendFinishRepairOwnerMsg(String communityId, String userId, JSONObject content) {
        String accessToken = wechatTemplateImpl.getAccessToken(communityId);
        String url = sendMsgUrl + accessToken;
        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_WORK_ORDER_END_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_WORK_ORDER_END_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_WORK_ORDER_END_TEMPLATE));
        String openId = "";

        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setCommunityId(communityId);
        ownerAppUserDto.setAppType(OwnerAppUserDto.APP_TYPE_WECHAT);
        ownerAppUserDto.setUserId(userId);
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        if (ListUtil.isNull(ownerAppUserDtos)) {
            logger.info("业主未绑定，没有获取到微信openId，userId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "业主未绑定，没有获取到微信openId");
        }
        openId = ownerAppUserDtos.get(0).getOpenId();

        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing9", new Content(truncateThing(content.getString("repairObjName"))));
        data.put("thing10", new Content(truncateThing(content.getString("staffName"))));
        data.put("time5", new Content(content.getString("time")));
        templateMessage.setData(data);
        templateMessage.setUrl(content.getString("url"));
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("repairObjName"));
    }

    @Override
    public ResultVo sendReturnRepairMsg(String communityId, String userId, JSONObject content) {
        String accessToken = wechatTemplateImpl.getAccessToken(communityId);

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(userId);
        staffAppAuthDto.setAppType("WECHAT");
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        if (staffAppAuthDtos == null || staffAppAuthDtos.size() < 1) {
            logger.info("员工未认证，没有获取到微信openId，staffId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
        }
        String openId = staffAppAuthDtos.get(0).getOpenId();

        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_WORK_ORDER_REMIND_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_WORK_ORDER_REMIND_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_WORK_ORDER_REMIND_TEMPLATE));

        String url = sendMsgUrl + accessToken;
        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing8", new Content(truncateThing(content.getString("repairTypeName"))));
        data.put("thing11", new Content(truncateThing(content.getString("repairObjName"))));
        data.put("thing10", new Content(truncateThing(content.getString("repairName"))));
        templateMessage.setData(data);
        templateMessage.setUrl(content.getString("url"));
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("repairName"));
    }

    @Override
    public ResultVo sendOaDistributeMsg(String communityId, String userId, JSONObject content) {
        String accessToken = wechatTemplateImpl.getAccessToken(communityId);

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(userId);
        staffAppAuthDto.setAppType("WECHAT");
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        if (ListUtil.isNull(staffAppAuthDtos)) {
            logger.info("员工未认证，没有获取到微信openId，staffId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
        }
        String openId = staffAppAuthDtos.get(0).getOpenId();

        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_TEMPLATE));

        String url = sendMsgUrl + accessToken;
        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing2", new Content(truncateThing(content.getString("flowName"))));
        data.put("thing9", new Content(truncateThing(content.getString("create_user_name"))));
        data.put("time10", new Content(content.getString("create_time")));
        templateMessage.setData(data);
        templateMessage.setUrl(content.getString("url"));
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("flowName"));
    }

    @Override
    public ResultVo sendOaCreateStaffMsg(String communityId, String userId, JSONObject content) {
        String accessToken = wechatTemplateImpl.getAccessToken(communityId);

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(userId);
        staffAppAuthDto.setAppType("WECHAT");
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        if (staffAppAuthDtos == null || staffAppAuthDtos.size() < 1) {
            logger.info("员工未认证，没有获取到微信openId，staffId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
        }
        String openId = staffAppAuthDtos.get(0).getOpenId();

        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_FINISH_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_FINISH_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_OA_WORKFLOW_AUDIT_FINISH_TEMPLATE));

        String url = sendMsgUrl + accessToken;
        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing2", new Content(content.getString("flowName")));
        data.put("thing6", new Content(content.getString("staffName")));
        templateMessage.setData(data);
        templateMessage.setUrl(content.getString("url"));
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("flowName"));
    }

    @Override
    public ResultVo sendComplaintMsg(String communityId, String userId, JSONObject content) {
        String accessToken = wechatTemplateImpl.getAccessToken(communityId);

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(userId);
        staffAppAuthDto.setAppType("WECHAT");
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        if (ListUtil.isNull(staffAppAuthDtos)) {
            logger.info("员工未认证，没有获取到微信openId，staffId={}", userId);
            return new ResultVo(ResultVo.CODE_BUSINESS_VERIFICATION, "员工未认证，没有获取到微信openId");
        }
        String openId = staffAppAuthDtos.get(0).getOpenId();
        Mapping mapping = MappingCache.getMapping(MappingConstant.WECHAT_DOMAIN, SPEC_CD_WECHAT_PROCESS_TEMPLATE);

        if (mapping == null) {
            throw new IllegalArgumentException("开发者账户编码映射未配置域为=" + MappingConstant.WECHAT_DOMAIN + ",键为=" + SPEC_CD_WECHAT_PROCESS_TEMPLATE);
        }
        String templateId = wechatTemplateImpl.getTemplateId(communityId, mapping.getValue(), mapping.getName(), templateKeys.get(SPEC_CD_WECHAT_PROCESS_TEMPLATE));

        String url = sendMsgUrl + accessToken;

        JSONObject data = new JSONObject();
        PropertyFeeTemplateMessage templateMessage = new PropertyFeeTemplateMessage();
        templateMessage.setTemplate_id(templateId);
        templateMessage.setTouser(openId);
        data.put("thing2", new Content("投诉单处理流程"));
        data.put("time10", new Content(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B)));
        data.put("thing9", new Content(truncateThing(content.getString("complaintName"))));
        templateMessage.setData(data);
        String wechatUrl = MappingCache.getValue(MappingConstant.URL_DOMAIN, "STAFF_WECHAT_URL");
        templateMessage.setUrl(wechatUrl);
        logger.info("发送模板消息内容:{}", JSON.toJSONString(templateMessage));
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, JSON.toJSONString(templateMessage), String.class);
        JSONObject paramOut = JSONObject.parseObject(responseEntity.getBody());
        return handleSendResponse(paramOut, openId, content.getString("complaintName"));
    }
}
