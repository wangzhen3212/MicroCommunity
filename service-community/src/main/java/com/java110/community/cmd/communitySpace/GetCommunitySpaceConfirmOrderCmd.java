/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.community.cmd.communitySpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.community.CommunitySpaceConfirmOrderDto;
import com.java110.dto.community.CommunitySpacePersonDto;
import com.java110.dto.community.CommunitySpacePersonTimeDto;
import com.java110.intf.community.ICommunitySpaceConfirmOrderV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpacePersonTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpacePersonV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类表述：扫码解析预约核销信息（扫码前预校验）
 * 服务编码：communitySpace.getCommunitySpaceConfirmOrder
 * 请求路劲：/app/communitySpace.GetCommunitySpaceConfirmOrder
 * add by 吴学文 at 2026-10-13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 */
@Java110CmdDoc(title = "扫码解析预约核销信息",
        description = "按核销码解析预约订单，返回四级校验结论与预约明细，供 PC 端核销页识别后自动核销",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/communitySpace.getCommunitySpaceConfirmOrder",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.getCommunitySpaceConfirmOrder"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "code", length = 64, remark = "核销码：预约时段ID/订单号（含后6位、后4位）/预约人手机号后4位"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "解析结果"),
                @Java110ParamDoc(parentNodeName = "data", name = "level", type = "String", remark = "校验结论 ok 可核销 warn 待处理 error 不可核销"),
                @Java110ParamDoc(parentNodeName = "data", name = "multiHit", type = "boolean", remark = "模糊码是否命中多笔预约"),
                @Java110ParamDoc(parentNodeName = "data", name = "timeId", type = "String", remark = "命中的预约时段ID"),
                @Java110ParamDoc(parentNodeName = "data", name = "times", type = "Array", remark = "预约订单下所有时段"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/communitySpace.getCommunitySpaceConfirmOrder?communityId=2022081539020475&code=102022100469010004",
        resBody = "{\"code\":0,\"data\":{\"level\":\"ok\",\"multiHit\":false,\"personName\":\"张三\",\"timeId\":\"102022100469010004\"},\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "communitySpace.getCommunitySpaceConfirmOrder")
public class GetCommunitySpaceConfirmOrderCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(GetCommunitySpaceConfirmOrderCmd.class);

    public static final String LEVEL_OK = "ok";

    public static final String LEVEL_WARN = "warn";

    public static final String LEVEL_ERROR = "error";

    private static final int SUFFIX_LENGTH_SIX = 6;

    private static final int SUFFIX_LENGTH_FOUR = 4;

    @Autowired
    private ICommunitySpaceConfirmOrderV1InnerServiceSMO communitySpaceConfirmOrderV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpacePersonTimeV1InnerServiceSMO communitySpacePersonTimeV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpacePersonV1InnerServiceSMO communitySpacePersonV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "code", "请求报文中未包含code");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        String communityId = reqJson.getString("communityId");
        String code = reqJson.getString("code").trim();

        CommunitySpacePersonTimeDto matchedTimeDto = null;
        CommunitySpacePersonDto communitySpacePersonDto = null;
        boolean multiHit = false;
        String matchType = "";

        //1、先按预约时段ID（业主端二维码）精确匹配
        matchedTimeDto = queryPersonTime(code, null, communityId);
        if (matchedTimeDto != null) {
            communitySpacePersonDto = queryPerson(matchedTimeDto.getCspId(), communityId);
            if (communitySpacePersonDto == null) {
                matchedTimeDto = null;
            } else {
                matchType = "timeId";
            }
        }

        //2、按完整订单号匹配
        if (communitySpacePersonDto == null) {
            communitySpacePersonDto = queryPersonByOrderId(code, communityId);
            if (communitySpacePersonDto != null) {
                matchType = "orderId";
            }
        }

        //3、按完整预约ID匹配
        if (communitySpacePersonDto == null) {
            communitySpacePersonDto = queryPerson(code, communityId);
            if (communitySpacePersonDto != null) {
                matchType = "cspId";
            }
        }

        //4、模糊匹配：订单号后 6 位 → 订单号后 4 位 → 手机号后 4 位
        if (communitySpacePersonDto == null) {
            List<CommunitySpacePersonDto> fuzzyPersonDtos = queryPersonByOrderIdSuffix(code, SUFFIX_LENGTH_SIX, communityId);
            if (fuzzyPersonDtos.size() < 1) {
                fuzzyPersonDtos = queryPersonByOrderIdSuffix(code, SUFFIX_LENGTH_FOUR, communityId);
            }
            if (fuzzyPersonDtos.size() < 1) {
                fuzzyPersonDtos = queryPersonByTelSuffix(code, SUFFIX_LENGTH_FOUR, communityId);
            }
            if (fuzzyPersonDtos.size() > 0) {
                multiHit = fuzzyPersonDtos.size() > 1;
                communitySpacePersonDto = fuzzyPersonDtos.get(0);
                matchType = "fuzzy";
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("level", LEVEL_ERROR);
        result.put("multiHit", multiHit);
        result.put("matchType", matchType);
        result.put("code", code);

        //未找到对应预约订单
        if (communitySpacePersonDto == null) {
            result.put("desc", "未找到该预约订单");
            cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(result));
            return;
        }

        result.put("cspId", communitySpacePersonDto.getCspId());
        result.put("orderId", communitySpacePersonDto.getOrderId());
        result.put("spaceId", communitySpacePersonDto.getSpaceId());
        result.put("spaceName", communitySpacePersonDto.getSpaceName());
        result.put("venueId", communitySpacePersonDto.getVenueId());
        result.put("venueName", communitySpacePersonDto.getVenueName());
        result.put("personName", communitySpacePersonDto.getPersonName());
        result.put("personTel", communitySpacePersonDto.getPersonTel());
        result.put("appointmentTime", communitySpacePersonDto.getAppointmentTime());
        result.put("receivableAmount", communitySpacePersonDto.getReceivableAmount());
        result.put("receivedAmount", communitySpacePersonDto.getReceivedAmount());
        result.put("payWay", communitySpacePersonDto.getPayWay());
        result.put("payWayName", communitySpacePersonDto.getPayWayName());
        result.put("state", communitySpacePersonDto.getState());
        result.put("stateName", communitySpacePersonDto.getStateName());
        result.put("remark", communitySpacePersonDto.getRemark());

        List<CommunitySpacePersonTimeDto> communitySpacePersonTimeDtos
                = queryPersonTimes(communitySpacePersonDto.getCspId(), communityId);

        List<Map<String, Object>> times = new ArrayList<>();
        Map<String, Object> timeMap = null;
        List<String> waitTimeIds = new ArrayList<>();
        for (CommunitySpacePersonTimeDto tmpCommunitySpacePersonTimeDto : communitySpacePersonTimeDtos) {
            timeMap = new HashMap<>();
            timeMap.put("timeId", tmpCommunitySpacePersonTimeDto.getTimeId());
            timeMap.put("hours", tmpCommunitySpacePersonTimeDto.getHours());
            timeMap.put("state", tmpCommunitySpacePersonTimeDto.getState());
            times.add(timeMap);
            if (CommunitySpacePersonTimeDto.STATE_WAIT_CONFIRM.equals(tmpCommunitySpacePersonTimeDto.getState())) {
                waitTimeIds.add(tmpCommunitySpacePersonTimeDto.getTimeId());
            }
        }
        result.put("times", times);
        result.put("waitTimeIds", waitTimeIds);

        if (matchedTimeDto == null && waitTimeIds.size() > 0) {
            matchedTimeDto = queryPersonTime(waitTimeIds.get(0), null, communityId);
        }

        //未匹配到时段的扫码（订单号/手机号），默认取第一个待核销时段
        if (matchedTimeDto != null) {
            result.put("timeId", matchedTimeDto.getTimeId());
            result.put("hours", matchedTimeDto.getHours());
            result.put("timeState", matchedTimeDto.getState());
        }

        //预约订单已取消
        if (CommunitySpacePersonDto.STATE_CL.equals(communitySpacePersonDto.getState())) {
            result.put("title", "订单已取消，不可核销");
            result.put("desc", "该预约已于取消时间释放时段，请核对后再试");
            cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(result));
            return;
        }

        //时段已核销或无待核销时段
        if ((matchedTimeDto != null && CommunitySpacePersonTimeDto.STATE_FINISH.equals(matchedTimeDto.getState()))
                || waitTimeIds.size() < 1) {
            result.put("level", LEVEL_ERROR);
            if (matchedTimeDto != null && matchedTimeDto.getTimeId() != null) {
                refreshVerifyInfo(result, matchedTimeDto.getTimeId(), communityId);
            }
            result.put("title", "该预约已核销，请勿重复核销");
            result.put("desc", "该预约时段已核销完成，无需再次核销");
            cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(result));
            return;
        }

        //未支付订单（存量 WP 订单）
        if (CommunitySpacePersonDto.STATE_WP.equals(communitySpacePersonDto.getState())) {
            result.put("level", LEVEL_WARN);
            result.put("title", "订单尚未支付，请先收款后再核销");
            result.put("desc", "该预约订单为待支付状态，请先到预约订单页收款后再核销");
            cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(result));
            return;
        }

        result.put("level", LEVEL_OK);
        result.put("title", "校验通过，可核销入场");
        result.put("desc", "请核对待核销时段与预约人信息后核销入场");

        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(result));
    }

    /**
     * 查询核销留痕（核销方式/核销人/核销时间）
     */
    private void refreshVerifyInfo(Map<String, Object> result, String timeId, String communityId) {
        CommunitySpaceConfirmOrderDto communitySpaceConfirmOrderDto = new CommunitySpaceConfirmOrderDto();
        communitySpaceConfirmOrderDto.setTimeId(timeId);
        communitySpaceConfirmOrderDto.setCommunityId(communityId);
        communitySpaceConfirmOrderDto.setStatusCd("0");
        List<CommunitySpaceConfirmOrderDto> communitySpaceConfirmOrderDtos
                = communitySpaceConfirmOrderV1InnerServiceSMOImpl.queryCommunitySpaceConfirmOrders(communitySpaceConfirmOrderDto);
        if (communitySpaceConfirmOrderDtos == null || communitySpaceConfirmOrderDtos.size() < 1) {
            return;
        }
        CommunitySpaceConfirmOrderDto confirmOrderDto = communitySpaceConfirmOrderDtos.get(0);
        result.put("verifyMethod", confirmOrderDto.getVerifyMethod());
        result.put("verifierName", confirmOrderDto.getVerifierName());
        result.put("verifyTime", confirmOrderDto.getCreateTime());
    }

    private CommunitySpacePersonTimeDto queryPersonTime(String timeId, String cspId, String communityId) {
        CommunitySpacePersonTimeDto communitySpacePersonTimeDto = new CommunitySpacePersonTimeDto();
        communitySpacePersonTimeDto.setTimeId(timeId);
        communitySpacePersonTimeDto.setCspId(cspId);
        communitySpacePersonTimeDto.setCommunityId(communityId);
        communitySpacePersonTimeDto.setStatusCd("0");
        List<CommunitySpacePersonTimeDto> communitySpacePersonTimeDtos
                = communitySpacePersonTimeV1InnerServiceSMOImpl.queryCommunitySpacePersonTimes(communitySpacePersonTimeDto);
        if (communitySpacePersonTimeDtos == null || communitySpacePersonTimeDtos.size() < 1) {
            return null;
        }
        return communitySpacePersonTimeDtos.get(0);
    }

    private List<CommunitySpacePersonTimeDto> queryPersonTimes(String cspId, String communityId) {
        CommunitySpacePersonTimeDto communitySpacePersonTimeDto = new CommunitySpacePersonTimeDto();
        communitySpacePersonTimeDto.setCspId(cspId);
        communitySpacePersonTimeDto.setCommunityId(communityId);
        communitySpacePersonTimeDto.setStatusCd("0");
        List<CommunitySpacePersonTimeDto> communitySpacePersonTimeDtos
                = communitySpacePersonTimeV1InnerServiceSMOImpl.queryCommunitySpacePersonTimes(communitySpacePersonTimeDto);
        if (communitySpacePersonTimeDtos == null) {
            return new ArrayList<>();
        }
        return communitySpacePersonTimeDtos;
    }

    private CommunitySpacePersonDto queryPerson(String cspId, String communityId) {
        if (StringUtil.isEmpty(cspId)) {
            return null;
        }
        CommunitySpacePersonDto communitySpacePersonDto = new CommunitySpacePersonDto();
        communitySpacePersonDto.setCspId(cspId);
        communitySpacePersonDto.setCommunityId(communityId);
        communitySpacePersonDto.setStatusCd("0");
        List<CommunitySpacePersonDto> communitySpacePersonDtos
                = communitySpacePersonV1InnerServiceSMOImpl.queryCommunitySpacePersons(communitySpacePersonDto);
        if (communitySpacePersonDtos == null || communitySpacePersonDtos.size() < 1) {
            return null;
        }
        return communitySpacePersonDtos.get(0);
    }

    private CommunitySpacePersonDto queryPersonByOrderId(String orderId, String communityId) {
        if (StringUtil.isEmpty(orderId)) {
            return null;
        }
        CommunitySpacePersonDto communitySpacePersonDto = new CommunitySpacePersonDto();
        communitySpacePersonDto.setOrderId(orderId);
        communitySpacePersonDto.setCommunityId(communityId);
        communitySpacePersonDto.setStatusCd("0");
        List<CommunitySpacePersonDto> communitySpacePersonDtos
                = communitySpacePersonV1InnerServiceSMOImpl.queryCommunitySpacePersons(communitySpacePersonDto);
        if (communitySpacePersonDtos == null || communitySpacePersonDtos.size() < 1) {
            return null;
        }
        return communitySpacePersonDtos.get(0);
    }

    /**
     * 按订单号后 N 位模糊匹配，命中多笔时优先返回「未核销 + 已预约(S)」的一笔
     */
    private List<CommunitySpacePersonDto> queryPersonByOrderIdSuffix(String code, int suffixLength, String communityId) {
        if (StringUtil.isEmpty(code) || code.length() < suffixLength) {
            return new ArrayList<>();
        }
        String suffix = code.substring(code.length() - suffixLength);
        List<CommunitySpacePersonDto> fuzzyPersonDtos = new ArrayList<>();
        for (CommunitySpacePersonDto tmpCommunitySpacePersonDto : queryPersonByKeyword(suffix, communityId)) {
            if (StringUtil.isEmpty(tmpCommunitySpacePersonDto.getOrderId())
                    || !tmpCommunitySpacePersonDto.getOrderId().endsWith(suffix)) {
                continue;
            }
            fuzzyPersonDtos.add(tmpCommunitySpacePersonDto);
        }
        return sortFuzzyPersonDtos(fuzzyPersonDtos);
    }

    /**
     * 按预约人手机号后 N 位模糊匹配
     */
    private List<CommunitySpacePersonDto> queryPersonByTelSuffix(String code, int suffixLength, String communityId) {
        String digits = code.replaceAll("\\D", "");
        if (digits.length() < suffixLength) {
            return new ArrayList<>();
        }
        String suffix = digits.substring(digits.length() - suffixLength);
        List<CommunitySpacePersonDto> fuzzyPersonDtos = new ArrayList<>();
        for (CommunitySpacePersonDto tmpCommunitySpacePersonDto : queryPersonByKeyword(suffix, communityId)) {
            if (StringUtil.isEmpty(tmpCommunitySpacePersonDto.getPersonTel())
                    || !tmpCommunitySpacePersonDto.getPersonTel().endsWith(suffix)) {
                continue;
            }
            fuzzyPersonDtos.add(tmpCommunitySpacePersonDto);
        }
        return sortFuzzyPersonDtos(fuzzyPersonDtos);
    }

    private List<CommunitySpacePersonDto> queryPersonByKeyword(String keyword, String communityId) {
        CommunitySpacePersonDto communitySpacePersonDto = new CommunitySpacePersonDto();
        communitySpacePersonDto.setKeyword(keyword);
        communitySpacePersonDto.setCommunityId(communityId);
        communitySpacePersonDto.setStatusCd("0");
        List<CommunitySpacePersonDto> communitySpacePersonDtos
                = communitySpacePersonV1InnerServiceSMOImpl.queryCommunitySpacePersons(communitySpacePersonDto);
        if (communitySpacePersonDtos == null) {
            return new ArrayList<>();
        }
        return communitySpacePersonDtos;
    }

    /**
     * 命中多笔时，「已预约(S)」优先于「待支付(WP)」，其次按创建时间倒序（查询已按创建时间倒序）
     */
    private List<CommunitySpacePersonDto> sortFuzzyPersonDtos(List<CommunitySpacePersonDto> fuzzyPersonDtos) {
        if (fuzzyPersonDtos.size() < 2) {
            return fuzzyPersonDtos;
        }
        List<CommunitySpacePersonDto> sortedPersonDtos = new ArrayList<>();
        List<CommunitySpacePersonDto> otherPersonDtos = new ArrayList<>();
        for (CommunitySpacePersonDto tmpCommunitySpacePersonDto : fuzzyPersonDtos) {
            if (CommunitySpacePersonDto.STATE_S.equals(tmpCommunitySpacePersonDto.getState())) {
                sortedPersonDtos.add(tmpCommunitySpacePersonDto);
            } else {
                otherPersonDtos.add(tmpCommunitySpacePersonDto);
            }
        }
        sortedPersonDtos.addAll(otherPersonDtos);
        return sortedPersonDtos;
    }
}
