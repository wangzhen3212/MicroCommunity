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
        description = "按场馆 + 星期重建开放时段配置，可选同步场馆开场/关场时间",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "venueId", length = 30, remark = "场馆ID"),
        @Java110ParamDoc(name = "weekDay", length = 2, remark = "星期 0 全周通用 1~7 周一~周日"),
        @Java110ParamDoc(name = "startTime", length = 10, remark = "开场时间 HH:mm"),
        @Java110ParamDoc(name = "endTime", length = 10, remark = "关场时间 HH:mm"),
        @Java110ParamDoc(name = "openTimes", type = "Array", remark = "开放时段"),
        @Java110ParamDoc(parentNodeName = "openTimes", name = "hours", length = 11, remark = "小时"),
        @Java110ParamDoc(parentNodeName = "openTimes", name = "isOpen", length = 2, remark = "是否开放 Y 是 N 否"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"communityId\":\"2022081539020475\",\"venueId\":\"102022093043260007\",\"weekDay\":\"0\",\"startTime\":\"08:00\",\"endTime\":\"22:00\",\"openTimes\":[{\"hours\":\"8\",\"isOpen\":\"Y\"}]}",
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

        JSONArray openTimes = reqJson.getJSONArray("openTimes");
        if (openTimes == null || openTimes.size() < 1) {
            throw new CmdException("请配置开放时段");
        }

        String weekDay = reqJson.getString("weekDay");
        if (StringUtil.isEmpty(weekDay)) {
            reqJson.put("weekDay", WEEK_DAY_ALL);
        } else if (!WEEK_DAY_ALL.equals(weekDay) && !weekDay.matches("^[1-7]$")) {
            throw new CmdException("星期取值错误，应为 0~7");
        }

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
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        String communityId = reqJson.getString("communityId");
        String venueId = reqJson.getString("venueId");
        String weekDay = reqJson.getString("weekDay");

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

        //按场馆 + 星期逻辑删除旧配置
        CommunitySpaceOpenTimePo deletePo = new CommunitySpaceOpenTimePo();
        deletePo.setStatusCd("1");
        deletePo.setCommunityId(communityId);
        deletePo.setVenueId(venueId);
        deletePo.setWeekDay(weekDay);
        communitySpaceOpenTimeV1InnerServiceSMOImpl.updateCommunitySpaceOpenTime(deletePo);

        JSONArray openTimes = reqJson.getJSONArray("openTimes");
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
            throw new CmdException("请配置开放时段");
        }

        int flag = communitySpaceOpenTimeV1InnerServiceSMOImpl.saveCommunitySpaceOpenTimes(communitySpaceOpenTimePos);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}
