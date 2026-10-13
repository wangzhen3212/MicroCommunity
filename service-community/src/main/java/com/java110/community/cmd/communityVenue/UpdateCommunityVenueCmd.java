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
package com.java110.community.cmd.communityVenue;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunitySpaceOpenTimeDto;
import com.java110.intf.community.ICommunitySpaceOpenTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunityVenueV1InnerServiceSMO;
import com.java110.po.communitySpace.CommunitySpaceOpenTimePo;
import com.java110.po.communitySpace.CommunityVenuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：communityVenue.updateCommunityVenue
 * 请求路劲：/app/communityVenue.UpdateCommunityVenue
 * add by 吴学文 at 2022-10-03 03:05:16 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "communityVenue.updateCommunityVenue")
public class UpdateCommunityVenueCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateCommunityVenueCmd.class);

    public static final String WEEK_DAY_ALL = "0";

    @Autowired
    private ICommunityVenueV1InnerServiceSMO communityVenueV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpaceOpenTimeV1InnerServiceSMO communitySpaceOpenTimeV1InnerServiceSMOImpl;


    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "venueId", "venueId不能为空");
Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

       CommunityVenuePo communityVenuePo = BeanConvertUtil.covertBean(reqJson, CommunityVenuePo.class);
        int flag = communityVenueV1InnerServiceSMOImpl.updateCommunityVenue(communityVenuePo);

        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        refreshOpenTimes(communityVenuePo);

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    private void refreshOpenTimes(CommunityVenuePo communityVenuePo) {

        if (StringUtil.isEmpty(communityVenuePo.getStartTime()) || StringUtil.isEmpty(communityVenuePo.getEndTime())) {
            return;
        }

        int startHour = getHour(communityVenuePo.getStartTime(), 0);
        int endHour = getHour(communityVenuePo.getEndTime(), 24);

        CommunitySpaceOpenTimeDto communitySpaceOpenTimeDto = new CommunitySpaceOpenTimeDto();
        communitySpaceOpenTimeDto.setCommunityId(communityVenuePo.getCommunityId());
        communitySpaceOpenTimeDto.setVenueId(communityVenuePo.getVenueId());
        communitySpaceOpenTimeDto.setWeekDay(WEEK_DAY_ALL);
        communitySpaceOpenTimeDto.setStatusCd("0");
        List<CommunitySpaceOpenTimeDto> communitySpaceOpenTimeDtos
                = communitySpaceOpenTimeV1InnerServiceSMOImpl.queryCommunitySpaceOpenTimes(communitySpaceOpenTimeDto);

        if (communitySpaceOpenTimeDtos == null || communitySpaceOpenTimeDtos.size() < 1) {
            return;
        }

        String isOpen = null;
        CommunitySpaceOpenTimePo communitySpaceOpenTimePo = null;
        int hours = 0;
        for (CommunitySpaceOpenTimeDto tmpCommunitySpaceOpenTimeDto : communitySpaceOpenTimeDtos) {
            if (StringUtil.isEmpty(tmpCommunitySpaceOpenTimeDto.getHours())) {
                continue;
            }
            hours = Integer.parseInt(tmpCommunitySpaceOpenTimeDto.getHours());
            isOpen = (hours >= startHour && hours < endHour) ? "Y" : "N";
            if (isOpen.equals(tmpCommunitySpaceOpenTimeDto.getIsOpen())) {
                continue;
            }
            communitySpaceOpenTimePo = new CommunitySpaceOpenTimePo();
            communitySpaceOpenTimePo.setStatusCd("0");
            communitySpaceOpenTimePo.setTimeId(tmpCommunitySpaceOpenTimeDto.getTimeId());
            communitySpaceOpenTimePo.setCommunityId(communityVenuePo.getCommunityId());
            communitySpaceOpenTimePo.setVenueId(communityVenuePo.getVenueId());
            communitySpaceOpenTimePo.setWeekDay(WEEK_DAY_ALL);
            communitySpaceOpenTimePo.setHours(tmpCommunitySpaceOpenTimeDto.getHours());
            communitySpaceOpenTimePo.setIsOpen(isOpen);
            communitySpaceOpenTimeV1InnerServiceSMOImpl.updateCommunitySpaceOpenTime(communitySpaceOpenTimePo);
        }
    }

    private int getHour(String time, int defaultValue) {
        if (StringUtil.isEmpty(time) || time.indexOf(":") < 1) {
            return defaultValue;
        }
        try {
            int hour = Integer.parseInt(time.substring(0, time.indexOf(":")).trim());
            if (hour < 0 || hour > 24) {
                return defaultValue;
            }
            return hour;
        } catch (NumberFormatException e) {
            logger.error("解析时间失败", e);
            return defaultValue;
        }
    }
}
