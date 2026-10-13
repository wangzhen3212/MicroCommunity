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
import com.java110.dto.community.CommunitySpacePersonDto;
import com.java110.intf.community.ICommunitySpacePersonV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


@Java110CmdDoc(title = "预约订单汇总",
        description = "统计预约订单总数、待支付、进行中、已完成、已取消以及应收实收合计",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/communitySpace.listCommunitySpacePersonSummary",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.listCommunitySpacePersonSummary"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "venueId", length = 30, remark = "场馆ID"),
        @Java110ParamDoc(name = "spaceId", length = 30, remark = "场地ID"),
        @Java110ParamDoc(name = "keyword", length = 64, remark = "关键字 预约人/电话/订单号"),
        @Java110ParamDoc(name = "beginDate", length = 30, remark = "预约日期开始 YYYY-MM-DD"),
        @Java110ParamDoc(name = "endDate", length = 30, remark = "预约日期结束 YYYY-MM-DD"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "total", type = "int", remark = "订单总数"),
                @Java110ParamDoc(parentNodeName = "data", name = "pending", type = "String", remark = "待支付"),
                @Java110ParamDoc(parentNodeName = "data", name = "active", type = "String", remark = "进行中"),
                @Java110ParamDoc(parentNodeName = "data", name = "finished", type = "String", remark = "已完成"),
                @Java110ParamDoc(parentNodeName = "data", name = "canceled", type = "String", remark = "已取消"),
                @Java110ParamDoc(parentNodeName = "data", name = "receivableTotal", type = "String", remark = "应收合计"),
                @Java110ParamDoc(parentNodeName = "data", name = "receivedTotal", type = "String", remark = "实收合计"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/communitySpace.listCommunitySpacePersonSummary?communityId=2022081539020475",
        resBody = "{\"code\":0,\"msg\":\"成功\",\"data\":{\"total\":12,\"pending\":2,\"active\":6,\"finished\":3,\"canceled\":1,\"receivableTotal\":\"240.00\",\"receivedTotal\":\"200.00\"}}"
)

/**
 * 类表述：预约订单汇总
 * 服务编码：communitySpace.listCommunitySpacePersonSummary
 * add by 吴学文 at 2026-10-13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 */
@Java110Cmd(serviceCode = "communitySpace.listCommunitySpacePersonSummary")
public class ListCommunitySpacePersonSummaryCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCommunitySpacePersonSummaryCmd.class);

    @Autowired
    private ICommunitySpacePersonV1InnerServiceSMO communitySpacePersonV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        CommunitySpacePersonDto communitySpacePersonDto = BeanConvertUtil.covertBean(reqJson, CommunitySpacePersonDto.class);

        JSONObject summary = communitySpacePersonV1InnerServiceSMOImpl.queryCommunitySpacePersonSummary(communitySpacePersonDto);

        if (summary == null) {
            summary = new JSONObject();
        }

        CommunitySpacePersonDto summaryDto = new CommunitySpacePersonDto();
        summaryDto.setTotal(summary.getIntValue("total"));
        summaryDto.setPending(String.valueOf(summary.getIntValue("pending")));
        summaryDto.setActive(String.valueOf(summary.getIntValue("active")));
        summaryDto.setFinished(String.valueOf(summary.getIntValue("finished")));
        summaryDto.setCanceled(String.valueOf(summary.getIntValue("canceled")));
        summaryDto.setReceivableTotal(formatAmount(summary.getDoubleValue("receivableTotal")));
        summaryDto.setReceivedTotal(formatAmount(summary.getDoubleValue("receivedTotal")));

        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(summaryDto));
    }

    /**
     * 金额统一保留两位小数
     *
     * @param amount 金额
     * @return 金额字符串
     */
    private String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
}
