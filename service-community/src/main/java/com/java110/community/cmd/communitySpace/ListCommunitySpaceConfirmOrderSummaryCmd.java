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
import com.java110.dto.community.CommunitySpacePersonTimeDto;
import com.java110.intf.community.ICommunitySpaceConfirmOrderV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpacePersonTimeV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 类表述：查询核销统计（今日核销笔数/今日核销金额/待核销订单/累计已核销）
 * 服务编码：communitySpace.listCommunitySpaceConfirmOrderSummary
 * 请求路劲：/app/communitySpace.ListCommunitySpaceConfirmOrderSummary
 * add by 吴学文 at 2026-10-13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 */
@Java110CmdDoc(title = "查询预约核销统计",
        description = "PC 端核销页 4 个统计卡：今日核销笔数、今日核销金额、待核销订单、累计已核销",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/communitySpace.listCommunitySpaceConfirmOrderSummary",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.listCommunitySpaceConfirmOrderSummary"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "统计数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "todayCount", type = "int", remark = "今日核销笔数"),
                @Java110ParamDoc(parentNodeName = "data", name = "todayAmount", type = "String", remark = "今日核销金额"),
                @Java110ParamDoc(parentNodeName = "data", name = "waitCount", type = "int", remark = "待核销订单"),
                @Java110ParamDoc(parentNodeName = "data", name = "totalCount", type = "int", remark = "累计已核销"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/communitySpace.listCommunitySpaceConfirmOrderSummary?communityId=2022081539020475",
        resBody = "{\"code\":0,\"data\":{\"todayAmount\":\"0\",\"todayCount\":0,\"totalCount\":0,\"waitCount\":0},\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "communitySpace.listCommunitySpaceConfirmOrderSummary")
public class ListCommunitySpaceConfirmOrderSummaryCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCommunitySpaceConfirmOrderSummaryCmd.class);

    @Autowired
    private ICommunitySpaceConfirmOrderV1InnerServiceSMO communitySpaceConfirmOrderV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpacePersonTimeV1InnerServiceSMO communitySpacePersonTimeV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        Map<String, Object> result = new HashMap<>();

        //今日核销笔数 / 今日核销金额 / 累计已核销
        CommunitySpaceConfirmOrderDto communitySpaceConfirmOrderDto = new CommunitySpaceConfirmOrderDto();
        communitySpaceConfirmOrderDto.setCommunityId(reqJson.getString("communityId"));
        communitySpaceConfirmOrderDto.setStatusCd("0");
        Map summaryMap = communitySpaceConfirmOrderV1InnerServiceSMOImpl.queryCommunitySpaceConfirmOrderSummary(communitySpaceConfirmOrderDto);

        result.put("todayCount", getSummaryValue(summaryMap, "todayCount", "0"));
        result.put("todayAmount", getSummaryValue(summaryMap, "todayAmount", "0"));
        result.put("totalCount", getSummaryValue(summaryMap, "totalCount", "0"));

        //待核销订单（预约时段待核销数）
        CommunitySpacePersonTimeDto communitySpacePersonTimeDto = new CommunitySpacePersonTimeDto();
        communitySpacePersonTimeDto.setCommunityId(reqJson.getString("communityId"));
        communitySpacePersonTimeDto.setState(CommunitySpacePersonTimeDto.STATE_WAIT_CONFIRM);
        communitySpacePersonTimeDto.setStatusCd("0");
        result.put("waitCount", communitySpacePersonTimeV1InnerServiceSMOImpl.queryCommunitySpacePersonTimesCount(communitySpacePersonTimeDto));

        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(result));
    }

    private String getSummaryValue(Map summaryMap, String key, String defaultValue) {
        if (summaryMap == null || summaryMap.get(key) == null) {
            return defaultValue;
        }
        return summaryMap.get(key).toString();
    }
}
