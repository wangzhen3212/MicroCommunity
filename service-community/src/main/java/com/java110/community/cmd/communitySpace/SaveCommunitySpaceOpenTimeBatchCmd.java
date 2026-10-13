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
import com.java110.dto.community.CommunityVenueDto;
import com.java110.intf.community.ICommunitySpaceOpenTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunityVenueV1InnerServiceSMO;
import com.java110.po.communitySpace.CommunitySpaceOpenTimePo;
import com.java110.po.communitySpace.CommunityVenuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 类表述：批量保存场馆开放时间
 * 服务编码：communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch
 * 请求路劲：/app/communitySpaceOpenTime.SaveCommunitySpaceOpenTimeBatch
 * add by 吴学文 at 2026-10-13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 */
@Java110CmdDoc(title = "批量保存场馆开放时间",
        description = "按场馆 + 星期重建开放时段配置，支持一次提交多星期配置，可选同步场馆开场/关场时间",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "venueId", length = 30, remark = "场馆ID"),
        @Java110ParamDoc(name = "weekDay", length = 2, remark = "星期 0 全周通用 1~7 周一~周日（不传 openTimeConfigs 时使用）"),
        @Java110ParamDoc(name = "startTime", length = 10, remark = "开场时间 HH:mm"),
        @Java110ParamDoc(name = "endTime", length = 10, remark = "关场时间 HH:mm"),
        @Java110ParamDoc(name = "openTimes", type = "Array", remark = "开放时段（不传 openTimeConfigs 时使用）"),
        @Java110ParamDoc(parentNodeName = "openTimes", name = "hours", length = 11, remark = "小时"),
        @Java110ParamDoc(parentNodeName = "openTimes", name = "isOpen", length = 2, remark = "是否开放 Y 是 N 否"),
        @Java110ParamDoc(name = "openTimeConfigs", type = "Array", remark = "多星期开放配置，传该参数时按每项 weekDay 重建，结构：weekDay + startTime + endTime + openTimes"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"communityId\":\"2022081539020475\",\"venueId\":\"102022093043260007\",\"startTime\":\"08:00\",\"endTime\":\"22:00\",\"openTimeConfigs\":[{\"weekDay\":\"1\",\"startTime\":\"08:00\",\"endTime\":\"22:00\",\"openTimes\":[{\"hours\":\"8\",\"isOpen\":\"Y\"}]},{\"weekDay\":\"2\",\"startTime\":\"08:00\",\"endTime\":\"22:00\",\"openTimes\":[]}]}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch")
public class SaveCommunitySpaceOpenTimeBatchCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveCommunitySpaceOpenTimeBatchCmd.class);

    public static final String CODE_PREFIX_ID = "10";

    public static final String WEEK_DAY_ALL = "0";

    private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):([0-5]\\d)$";

    @Autowired
    private ICommunitySpaceOpenTimeV1InnerServiceSMO communitySpaceOpenTimeV1InnerServiceSMOImpl;

    @Autowired
    private ICommunityVenueV1InnerServiceSMO communityVenueV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "venueId", "请求报文中未包含venueId");

        String startTime = reqJson.getString("startTime");
        String endTime = reqJson.getString("endTime");
        if (!StringUtil.isEmpty(startTime) && !startTime.matches(TIME_PATTERN)) {
            throw new CmdException("开场时间格式错误，应为 HH:mm");
        }
        if (!StringUtil.isEmpty(endTime) && !endTime.matches(TIME_PATTERN)) {
            throw new CmdException("关场时间格式错误，应为 HH:mm");
        }
        if (!StringUtil.isEmpty(startTime) && !StringUtil.isEmpty(endTime)
                && endTime.compareTo(startTime) <= 0) {
            throw new CmdException("关场时间必须晚于开场时间");
        }

        JSONArray openTimeConfigs = reqJson.getJSONArray("openTimeConfigs");
        if (openTimeConfigs == null || openTimeConfigs.size() < 1) {
            validateSingleWeekDay(reqJson);
            return;
        }

        List<String> weekDayList = new ArrayList<>();
        for (int index = 0; index < openTimeConfigs.size(); index++) {
            JSONObject openTimeConfig = openTimeConfigs.getJSONObject(index);
            if (openTimeConfig == null) {
                throw new CmdException("开放时间配置格式错误");
            }
            String weekDay = openTimeConfig.getString("weekDay");
            if (StringUtil.isEmpty(weekDay) || !isWeekDayValid(weekDay)) {
                throw new CmdException("星期取值错误，应为 0~7");
            }
            if (weekDayList.contains(weekDay)) {
                throw new CmdException("星期 " + weekDay + " 重复配置");
            }
            weekDayList.add(weekDay);

            String dayStartTime = openTimeConfig.getString("startTime");
            String dayEndTime = openTimeConfig.getString("endTime");
            if (!StringUtil.isEmpty(dayStartTime) && !dayStartTime.matches(TIME_PATTERN)) {
                throw new CmdException("开场时间格式错误，应为 HH:mm");
            }
            if (!StringUtil.isEmpty(dayEndTime) && !dayEndTime.matches(TIME_PATTERN)) {
                throw new CmdException("关场时间格式错误，应为 HH:mm");
            }
            if (hasOpenHour(openTimeConfig.getJSONArray("openTimes"))
                    && !StringUtil.isEmpty(dayStartTime) && !StringUtil.isEmpty(dayEndTime)
                    && dayEndTime.compareTo(dayStartTime) <= 0) {
                throw new CmdException("关场时间必须晚于开场时间");
            }
        }
    }

    private void validateSingleWeekDay(JSONObject reqJson) {
        JSONArray openTimes = reqJson.getJSONArray("openTimes");
        if (openTimes == null || openTimes.size() < 1) {
            throw new CmdException("请配置开放时段");
        }

        String weekDay = reqJson.getString("weekDay");
        if (StringUtil.isEmpty(weekDay)) {
            reqJson.put("weekDay", WEEK_DAY_ALL);
            return;
        }
        if (!isWeekDayValid(weekDay)) {
            throw new CmdException("星期取值错误，应为 0~7");
        }
    }

    private boolean isWeekDayValid(String weekDay) {
        return WEEK_DAY_ALL.equals(weekDay) || weekDay.matches("^[1-7]$");
    }

    private boolean hasOpenHour(JSONArray openTimes) {
        if (openTimes == null || openTimes.size() < 1) {
            return false;
        }
        for (int index = 0; index < openTimes.size(); index++) {
            JSONObject openTimeObj = openTimes.getJSONObject(index);
            if (openTimeObj != null && "Y".equals(openTimeObj.getString("isOpen"))) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        String communityId = reqJson.getString("communityId");
        String venueId = reqJson.getString("venueId");

        CommunityVenueDto communityVenueDto = new CommunityVenueDto();
        communityVenueDto.setVenueId(venueId);
        communityVenueDto.setCommunityId(communityId);
        List<CommunityVenueDto> communityVenueDtos = communityVenueV1InnerServiceSMOImpl.queryCommunityVenues(communityVenueDto);
        Assert.listOnlyOne(communityVenueDtos, "场馆不存在");

        //同步场馆开场/关场时间
        if (!StringUtil.isEmpty(reqJson.getString("startTime")) && !StringUtil.isEmpty(reqJson.getString("endTime"))) {
            CommunityVenuePo communityVenuePo = new CommunityVenuePo();
            communityVenuePo.setVenueId(venueId);
            communityVenuePo.setCommunityId(communityId);
            communityVenuePo.setStartTime(reqJson.getString("startTime"));
            communityVenuePo.setEndTime(reqJson.getString("endTime"));
            communityVenueV1InnerServiceSMOImpl.updateCommunityVenue(communityVenuePo);
        }

        JSONArray openTimeConfigs = reqJson.getJSONArray("openTimeConfigs");
        if (openTimeConfigs == null || openTimeConfigs.size() < 1) {
            rebuildWeekConfig(communityId, venueId, reqJson.getString("weekDay"), reqJson.getJSONArray("openTimes"));
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        for (int index = 0; index < openTimeConfigs.size(); index++) {
            JSONObject openTimeConfig = openTimeConfigs.getJSONObject(index);
            rebuildWeekConfig(communityId, venueId, openTimeConfig.getString("weekDay"),
                    openTimeConfig.getJSONArray("openTimes"));
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    private void rebuildWeekConfig(String communityId, String venueId, String weekDay, JSONArray openTimes) {
        if (StringUtil.isEmpty(weekDay)) {
            weekDay = WEEK_DAY_ALL;
        }

        CommunitySpaceOpenTimePo deletePo = new CommunitySpaceOpenTimePo();
        deletePo.setStatusCd("1");
        deletePo.setCommunityId(communityId);
        deletePo.setVenueId(venueId);
        deletePo.setWeekDay(weekDay);
        communitySpaceOpenTimeV1InnerServiceSMOImpl.updateCommunitySpaceOpenTime(deletePo);

        if (openTimes == null || openTimes.size() < 1) {
            return;
        }

        List<CommunitySpaceOpenTimePo> communitySpaceOpenTimePos = new ArrayList<>();
        CommunitySpaceOpenTimePo communitySpaceOpenTimePo = null;
        for (int index = 0; index < openTimes.size(); index++) {
            JSONObject openTimeObj = openTimes.getJSONObject(index);
            if (openTimeObj == null || StringUtil.isEmpty(openTimeObj.getString("hours"))) {
                continue;
            }
            communitySpaceOpenTimePo = new CommunitySpaceOpenTimePo();
            communitySpaceOpenTimePo.setCommunityId(communityId);
            communitySpaceOpenTimePo.setVenueId(venueId);
            communitySpaceOpenTimePo.setWeekDay(weekDay);
            communitySpaceOpenTimePo.setSpaceId("");
            communitySpaceOpenTimePo.setHours(openTimeObj.getString("hours"));
            communitySpaceOpenTimePo.setIsOpen("Y".equals(openTimeObj.getString("isOpen")) ? "Y" : "N");
            communitySpaceOpenTimePo.setTimeId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            communitySpaceOpenTimePos.add(communitySpaceOpenTimePo);
        }

        if (communitySpaceOpenTimePos.size() < 1) {
            return;
        }

        int flag = communitySpaceOpenTimeV1InnerServiceSMOImpl.saveCommunitySpaceOpenTimes(communitySpaceOpenTimePos);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
    }
}
