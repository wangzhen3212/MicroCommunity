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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.community.CommunitySpaceDto;
import com.java110.dto.community.CommunitySpaceOpenTimeDto;
import com.java110.dto.community.CommunitySpacePersonDto;
import com.java110.dto.community.CommunitySpacePersonTimeDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.intf.community.ICommunitySpaceOpenTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpacePersonTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpacePersonV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpaceV1InnerServiceSMO;
import com.java110.po.communitySpace.CommunitySpacePersonPo;
import com.java110.po.communitySpace.CommunitySpacePersonTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


@Java110CmdDoc(title = "预约场地",
        description = "系统中的预约场地，建单即收款，订单状态直接为已预约；同时作为扫码支付扣款成功后的回调落地服务码",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/communitySpace.saveCommunitySpacePerson",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.saveCommunitySpacePerson"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "appointmentTime", length = 30, remark = "预约日期 YYYY-MM-DD"),
        @Java110ParamDoc(name = "personName", length = 64, remark = "预约人"),
        @Java110ParamDoc(name = "personTel", length = 30, remark = "预约人电话"),
        @Java110ParamDoc(name = "remark", length = 512, remark = "备注"),
        @Java110ParamDoc(name = "spaceId", length = 30, remark = "场地ID"),
        @Java110ParamDoc(name = "payWay", length = 12, remark = "支付方式 1 现金 2 微信 3 支付宝，为空时按 primeRate 推断"),
        @Java110ParamDoc(name = "receivedAmount", length = 30, remark = "实收金额，与应收不一致时以应收落库"),
        @Java110ParamDoc(name = "primeRate", length = 12, remark = "支付渠道 3 微信二维码 4 支付宝二维码，扫码通道回调时透传"),
        @Java110ParamDoc(name = "openTimes",type="Array", remark = "预约时段"),
        @Java110ParamDoc(parentNodeName = "openTimes",name = "hours", length = 11, remark = "预约小时"),

})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "预约结果"),
                @Java110ParamDoc(parentNodeName = "data", name = "cspId", type = "String", remark = "预约ID"),
                @Java110ParamDoc(parentNodeName = "data", name = "orderId", type = "String", remark = "预约订单号"),
                @Java110ParamDoc(parentNodeName = "data", name = "receivableAmount", type = "String", remark = "应收金额"),
                @Java110ParamDoc(parentNodeName = "data", name = "receivedAmount", type = "String", remark = "实收金额"),
                @Java110ParamDoc(parentNodeName = "data", name = "state", type = "String", remark = "订单状态 S 已预约"),

        }
)

@Java110ExampleDoc(
        reqBody="{\"spaceId\":\"102022093043260007\",\"personName\":\"wuxw\",\"personTel\":\"18909711443\",\"appointmentTime\":\"2022-09-01\",\"remark\":\"123\",\"communityId\":\"2022081539020475\",\"payWay\":\"1\",\"receivedAmount\":\"60.00\",\"openTimes\":[{\"hours\":\"9\"},{\"hours\":\"10\"}]}",
        resBody="{\"code\":0,\"data\":{\"cspId\":\"102022100438263187\",\"orderId\":\"112026102000000001\",\"receivableAmount\":\"60.00\",\"receivedAmount\":\"60.00\",\"state\":\"S\"},\"msg\":\"成功\"}"
)

/**
 * 类表述：保存，建单即收款（只 Save，不 Update）
 * 服务编码：communitySpacePerson.saveCommunitySpacePerson
 * 请求路劲：/app/communitySpacePerson.SaveCommunitySpacePerson
 * add by 吴学文 at 2022-09-30 11:36:52 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * // modify by wuxw at 2026-10-13 场地预约改为支付成功才落库：接收 payWay/receivedAmount/primeRate，
 * // 状态恒为 S 并写 payTime，一次性完成建单与收款，不再出现「先建单后 update 收款」的两状态流转
 */
@Java110Cmd(serviceCode = "communitySpace.saveCommunitySpacePerson")
public class SaveCommunitySpacePersonCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveCommunitySpacePersonCmd.class);

    public static final String CODE_PREFIX_ID = "10";

    public static final String DEFAULT_PAY_WAY = "1";

    public static final String DEFAULT_AMOUNT = "0.00";

    public static final String WEEK_DAY_ALL = "0";

    public static final String PAY_WAY_CASH = "1";

    public static final String PAY_WAY_WECHAT = "2";

    public static final String PAY_WAY_ALIPAY = "3";

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    @Autowired
    private ICommunitySpacePersonV1InnerServiceSMO communitySpacePersonV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpacePersonTimeV1InnerServiceSMO communitySpacePersonTimeV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpaceV1InnerServiceSMO communitySpaceV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpaceOpenTimeV1InnerServiceSMO communitySpaceOpenTimeV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "spaceId", "请求报文中未包含spaceId");
        Assert.hasKeyAndValue(reqJson, "personName", "请求报文中未包含personName");
        Assert.hasKeyAndValue(reqJson, "personTel", "请求报文中未包含personTel");
        Assert.hasKeyAndValue(reqJson, "appointmentTime", "请求报文中未包含appointmentTime");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        //预约日期统一为 YYYY-MM-DD
        String appointmentTime = reqJson.getString("appointmentTime").trim();
        if (appointmentTime.length() > 10) {
            appointmentTime = appointmentTime.substring(0, 10);
        }
        if (!DATE_PATTERN.matcher(appointmentTime).matches()) {
            throw new CmdException("预约日期格式错误，应为 YYYY-MM-DD");
        }
        reqJson.put("appointmentTime", appointmentTime);

        CommunitySpaceDto communitySpaceDto = new CommunitySpaceDto();
        communitySpaceDto.setSpaceId(reqJson.getString("spaceId"));
        communitySpaceDto.setCommunityId(reqJson.getString("communityId"));
        List<CommunitySpaceDto> communitySpaceDtos = communitySpaceV1InnerServiceSMOImpl.queryCommunitySpaces(communitySpaceDto);
        Assert.listOnlyOne(communitySpaceDtos, "场地不存在");

        JSONArray openTimes = reqJson.getJSONArray("openTimes");

        if (openTimes == null || openTimes.size() < 1) {
            throw new CmdException("请选择预约时段");
        }

        List<JSONObject> appointmentTimes = getAppointmentTimes(openTimes);

        if (appointmentTimes.size() < 1) {
            throw new CmdException("请选择预约时段");
        }

        //开放时段校验按场地所属场馆 + 日期星期（week_day 优先，其次全周通用配置）
        String venueId = communitySpaceDtos.get(0).getVenueId();
        String weekDay = getWeekDay(appointmentTime);

        CommunitySpaceOpenTimeDto communitySpaceOpenTimeDto = new CommunitySpaceOpenTimeDto();
        communitySpaceOpenTimeDto.setCommunityId(reqJson.getString("communityId"));
        communitySpaceOpenTimeDto.setVenueId(venueId);
        communitySpaceOpenTimeDto.setWeekDays(new String[]{weekDay, WEEK_DAY_ALL});
        communitySpaceOpenTimeDto.setStatusCd("0");
        List<CommunitySpaceOpenTimeDto> communitySpaceOpenTimeDtos = communitySpaceOpenTimeV1InnerServiceSMOImpl.queryCommunitySpaceOpenTimes(communitySpaceOpenTimeDto);

        Map<String, String> openTimeMap = new HashMap<>();
        for (CommunitySpaceOpenTimeDto tmpCommunitySpaceOpenTimeDto : communitySpaceOpenTimeDtos) {
            if (weekDay.equals(tmpCommunitySpaceOpenTimeDto.getWeekDay()) || !openTimeMap.containsKey(tmpCommunitySpaceOpenTimeDto.getHours())) {
                openTimeMap.put(tmpCommunitySpaceOpenTimeDto.getHours(), tmpCommunitySpaceOpenTimeDto.getIsOpen());
            }
        }

        CommunitySpacePersonTimeDto communitySpacePersonTimeDto = null;
        int flag = 0;
        for (JSONObject appointmentTimeObj : appointmentTimes) {
            String hours = appointmentTimeObj.getString("hours");
            if (!"Y".equals(openTimeMap.get(hours))) {
                throw new CmdException(hours + ":00 时段未开放预约");
            }

            communitySpacePersonTimeDto = new CommunitySpacePersonTimeDto();
            communitySpacePersonTimeDto.setSpaceId(reqJson.getString("spaceId"));
            communitySpacePersonTimeDto.setAppointmentDate(appointmentTime);
            communitySpacePersonTimeDto.setHours(hours);
            communitySpacePersonTimeDto.setStateNotIn(new String[]{CommunitySpacePersonTimeDto.STATE_CL});
            flag = communitySpacePersonTimeV1InnerServiceSMOImpl.queryCommunitySpacePersonTimesCount(communitySpacePersonTimeDto);
            if (flag > 0) {
                throw new CmdException(appointmentTime + " " + hours + ":00 时段已被预约");
            }
        }

        //应收金额由服务端计算，前端金额仅用于展示
        BigDecimal feeMoney = new BigDecimal(communitySpaceDtos.get(0).getFeeMoney());
        BigDecimal receivableAmount = feeMoney.multiply(new BigDecimal(appointmentTimes.size())).setScale(2, BigDecimal.ROUND_HALF_UP);
        reqJson.put("receivableAmount", receivableAmount.toPlainString());

        checkPayWayAndAmount(reqJson, receivableAmount);
    }

    /**
     * 校验收款参数：支付方式取值范围，实收金额为合法金额且不超过应收
     * <p>
     * authCode/subServiceCode/primeRate/paymentPoolId 等由扫码支付通道透传的字段不做校验，通道回调时必然带上
     *
     * @param reqJson          请求报文
     * @param receivableAmount 应收金额
     */
    private void checkPayWayAndAmount(JSONObject reqJson, BigDecimal receivableAmount) {
        String payWay = reqJson.getString("payWay");
        if (!StringUtil.isEmpty(payWay) && !PAY_WAY_CASH.equals(payWay)
                && !PAY_WAY_WECHAT.equals(payWay) && !PAY_WAY_ALIPAY.equals(payWay)) {
            throw new CmdException("支付方式错误，应为 1 现金 2 微信 3 支付宝");
        }

        String receivedAmount = reqJson.getString("receivedAmount");
        if (StringUtil.isEmpty(receivedAmount)) {
            return;
        }

        if (!AMOUNT_PATTERN.matcher(receivedAmount.trim()).matches()) {
            throw new CmdException("实收金额格式错误");
        }

        if (new BigDecimal(receivedAmount.trim()).compareTo(receivableAmount) > 0) {
            throw new CmdException("实收金额不能大于应收金额");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        CommunitySpacePersonPo communitySpacePersonPo = BeanConvertUtil.covertBean(reqJson, CommunitySpacePersonPo.class);
        communitySpacePersonPo.setCspId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        communitySpacePersonPo.setOrderId(GenerateCodeFactory.getGeneratorId("11"));

        //建单即收款：状态恒为已预约，收款时间取当前时间
        communitySpacePersonPo.setState(CommunitySpacePersonDto.STATE_S);
        communitySpacePersonPo.setPayTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));

        //支付方式：优先取请求值，其次按扫码渠道推断，最后默认现金
        communitySpacePersonPo.setPayWay(getPayWay(reqJson));

        //实收金额一律以服务端计算的应收落库，保证账实一致
        BigDecimal receivableAmount = getReceivableAmount(reqJson);
        communitySpacePersonPo.setReceivableAmount(receivableAmount.toPlainString());
        communitySpacePersonPo.setReceivedAmount(receivableAmount.toPlainString());

        int flag = communitySpacePersonV1InnerServiceSMOImpl.saveCommunitySpacePerson(communitySpacePersonPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        List<JSONObject> appointmentTimes = getAppointmentTimes(reqJson.getJSONArray("openTimes"));

        CommunitySpacePersonTimePo communitySpacePersonTimePo = null;
        for (JSONObject appointmentTimeObj : appointmentTimes) {
            communitySpacePersonTimePo = new CommunitySpacePersonTimePo();
            communitySpacePersonTimePo.setCommunityId(communitySpacePersonPo.getCommunityId());
            communitySpacePersonTimePo.setCspId(communitySpacePersonPo.getCspId());
            communitySpacePersonTimePo.setHours(appointmentTimeObj.getString("hours"));
            communitySpacePersonTimePo.setSpaceId(communitySpacePersonPo.getSpaceId());
            communitySpacePersonTimePo.setAppointmentDate(communitySpacePersonPo.getAppointmentTime());
            communitySpacePersonTimePo.setTimeId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            communitySpacePersonTimePo.setState(CommunitySpacePersonTimeDto.STATE_WAIT_CONFIRM);
            communitySpacePersonTimeV1InnerServiceSMOImpl.saveCommunitySpacePersonTime(communitySpacePersonTimePo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("cspId", communitySpacePersonPo.getCspId());
        data.put("orderId", communitySpacePersonPo.getOrderId());
        data.put("receivableAmount", receivableAmount.toPlainString());
        data.put("receivedAmount", receivableAmount.toPlainString());
        data.put("state", CommunitySpacePersonDto.STATE_S);
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 计算支付方式：请求 payWay 优先，其次按扫码渠道 primeRate 推断（4 支付宝 → 3，3 微信 → 2），最后默认现金
     *
     * @param reqJson 请求报文
     * @return 支付方式
     */
    private String getPayWay(JSONObject reqJson) {
        String payWay = reqJson.getString("payWay");
        if (!StringUtil.isEmpty(payWay)) {
            return payWay;
        }

        String primeRate = reqJson.getString("primeRate");
        if (FeeDetailDto.PRIME_REATE_ALI_QRCODE.equals(primeRate)) {
            return PAY_WAY_ALIPAY;
        }
        if (FeeDetailDto.PRIME_REATE_WECHAT_QRCODE.equals(primeRate)) {
            return PAY_WAY_WECHAT;
        }

        return DEFAULT_PAY_WAY;
    }

    /**
     * 获取应收金额，校验阶段已计算，兜底按场地单价 × 时段数重算
     *
     * @param reqJson 请求报文
     * @return 应收金额
     */
    private BigDecimal getReceivableAmount(JSONObject reqJson) {
        String receivableAmount = reqJson.getString("receivableAmount");
        if (!StringUtil.isEmpty(receivableAmount)) {
            return new BigDecimal(receivableAmount);
        }

        CommunitySpaceDto communitySpaceDto = new CommunitySpaceDto();
        communitySpaceDto.setSpaceId(reqJson.getString("spaceId"));
        communitySpaceDto.setCommunityId(reqJson.getString("communityId"));
        List<CommunitySpaceDto> communitySpaceDtos = communitySpaceV1InnerServiceSMOImpl.queryCommunitySpaces(communitySpaceDto);

        int hours = getAppointmentTimes(reqJson.getJSONArray("openTimes")).size();
        if (communitySpaceDtos == null || communitySpaceDtos.size() < 1) {
            return new BigDecimal(DEFAULT_AMOUNT);
        }

        return new BigDecimal(communitySpaceDtos.get(0).getFeeMoney())
                .multiply(new BigDecimal(hours)).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 日期转星期（1~7 周一~周日）
     *
     * @param date 日期 YYYY-MM-DD
     * @return 星期
     */
    private String getWeekDay(String date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DateUtil.getDateFromStringB(date));
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return (dayOfWeek == Calendar.SUNDAY ? 7 : dayOfWeek - 1) + "";
    }

    /**
     * 提取需要预约的时段，忽略未开放时段
     *
     * @param openTimes 前端传入时段
     * @return 预约时段
     */
    private List<JSONObject> getAppointmentTimes(JSONArray openTimes) {
        List<JSONObject> appointmentTimes = new ArrayList<>();
        if (openTimes == null) {
            return appointmentTimes;
        }

        JSONObject openTimeObj = null;
        for (int timeIndex = 0; timeIndex < openTimes.size(); timeIndex++) {
            openTimeObj = openTimes.getJSONObject(timeIndex);
            if (openTimeObj == null || StringUtil.isEmpty(openTimeObj.getString("hours"))) {
                continue;
            }
            if ("N".equals(openTimeObj.getString("isOpen"))) {
                continue;
            }
            appointmentTimes.add(openTimeObj);
        }

        return appointmentTimes;
    }
}
