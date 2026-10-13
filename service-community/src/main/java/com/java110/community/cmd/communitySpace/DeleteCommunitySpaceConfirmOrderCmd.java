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
import com.java110.core.annotation.Java110Transactional;
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
import com.java110.po.communitySpace.CommunitySpaceConfirmOrderPo;
import com.java110.po.communitySpace.CommunitySpacePersonPo;
import com.java110.po.communitySpace.CommunitySpacePersonTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：撤销核销
 * 服务编码：communitySpace.deleteCommunitySpaceConfirmOrder
 * 请求路劲：/app/communitySpace.DeleteCommunitySpaceConfirmOrder
 * add by 吴学文 at 2026-10-13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 */
@Java110CmdDoc(title = "撤销预约核销",
        description = "撤销核销记录：逻辑删除核销单，预约时段由已核销回退待核销，预约订单由已完成回退已预约",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/communitySpace.deleteCommunitySpaceConfirmOrder",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.deleteCommunitySpaceConfirmOrder"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "orderId", length = 30, remark = "核销单ID"),
        @Java110ParamDoc(name = "timeId", length = 30, remark = "预约时段ID（与核销单ID二选一）"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"communityId\":\"2022081539020475\",\"orderId\":\"102022101485880007\"}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "communitySpace.deleteCommunitySpaceConfirmOrder")
public class DeleteCommunitySpaceConfirmOrderCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(DeleteCommunitySpaceConfirmOrderCmd.class);

    @Autowired
    private ICommunitySpaceConfirmOrderV1InnerServiceSMO communitySpaceConfirmOrderV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpacePersonTimeV1InnerServiceSMO communitySpacePersonTimeV1InnerServiceSMOImpl;

    @Autowired
    private ICommunitySpacePersonV1InnerServiceSMO communitySpacePersonV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        if (StringUtil.isEmpty(reqJson.getString("orderId")) && StringUtil.isEmpty(reqJson.getString("timeId"))) {
            throw new CmdException("请求报文中未包含orderId或timeId");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        CommunitySpaceConfirmOrderDto communitySpaceConfirmOrderDto = new CommunitySpaceConfirmOrderDto();
        communitySpaceConfirmOrderDto.setCommunityId(reqJson.getString("communityId"));
        communitySpaceConfirmOrderDto.setOrderId(reqJson.getString("orderId"));
        communitySpaceConfirmOrderDto.setTimeId(reqJson.getString("timeId"));
        communitySpaceConfirmOrderDto.setStatusCd("0");
        List<CommunitySpaceConfirmOrderDto> communitySpaceConfirmOrderDtos
                = communitySpaceConfirmOrderV1InnerServiceSMOImpl.queryCommunitySpaceConfirmOrders(communitySpaceConfirmOrderDto);

        if (communitySpaceConfirmOrderDtos == null || communitySpaceConfirmOrderDtos.size() < 1) {
            throw new CmdException("未找到核销记录");
        }

        CommunitySpaceConfirmOrderPo communitySpaceConfirmOrderPo = null;
        for (CommunitySpaceConfirmOrderDto tmpCommunitySpaceConfirmOrderDto : communitySpaceConfirmOrderDtos) {
            //1、逻辑删除核销单
            communitySpaceConfirmOrderPo = new CommunitySpaceConfirmOrderPo();
            communitySpaceConfirmOrderPo.setStatusCd("1");
            communitySpaceConfirmOrderPo.setOrderId(tmpCommunitySpaceConfirmOrderDto.getOrderId());
            communitySpaceConfirmOrderV1InnerServiceSMOImpl.updateCommunitySpaceConfirmOrder(communitySpaceConfirmOrderPo);

            //2、预约时段由已核销回退为待核销（已取消的时段不回退）
            refreshPersonTimeState(tmpCommunitySpaceConfirmOrderDto);

            //3、预约订单由已完成回退为已预约
            refreshPersonState(tmpCommunitySpaceConfirmOrderDto);
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 预约时段状态 C → W
     */
    private void refreshPersonTimeState(CommunitySpaceConfirmOrderDto communitySpaceConfirmOrderDto) {

        if (StringUtil.isEmpty(communitySpaceConfirmOrderDto.getTimeId())) {
            return;
        }

        CommunitySpacePersonTimeDto communitySpacePersonTimeDto = new CommunitySpacePersonTimeDto();
        communitySpacePersonTimeDto.setTimeId(communitySpaceConfirmOrderDto.getTimeId());
        communitySpacePersonTimeDto.setCommunityId(communitySpaceConfirmOrderDto.getCommunityId());
        communitySpacePersonTimeDto.setStatusCd("0");
        List<CommunitySpacePersonTimeDto> communitySpacePersonTimeDtos
                = communitySpacePersonTimeV1InnerServiceSMOImpl.queryCommunitySpacePersonTimes(communitySpacePersonTimeDto);

        if (communitySpacePersonTimeDtos == null || communitySpacePersonTimeDtos.size() < 1) {
            return;
        }

        if (!CommunitySpacePersonTimeDto.STATE_FINISH.equals(communitySpacePersonTimeDtos.get(0).getState())) {
            return;
        }

        CommunitySpacePersonTimePo communitySpacePersonTimePo = new CommunitySpacePersonTimePo();
        communitySpacePersonTimePo.setStatusCd("0");
        communitySpacePersonTimePo.setTimeId(communitySpaceConfirmOrderDto.getTimeId());
        communitySpacePersonTimePo.setState(CommunitySpacePersonTimeDto.STATE_WAIT_CONFIRM);
        communitySpacePersonTimeV1InnerServiceSMOImpl.updateCommunitySpacePersonTime(communitySpacePersonTimePo);
    }

    /**
     * 预约订单状态 C → S
     */
    private void refreshPersonState(CommunitySpaceConfirmOrderDto communitySpaceConfirmOrderDto) {

        if (StringUtil.isEmpty(communitySpaceConfirmOrderDto.getCspId())) {
            return;
        }

        CommunitySpacePersonDto communitySpacePersonDto = new CommunitySpacePersonDto();
        communitySpacePersonDto.setCspId(communitySpaceConfirmOrderDto.getCspId());
        communitySpacePersonDto.setCommunityId(communitySpaceConfirmOrderDto.getCommunityId());
        communitySpacePersonDto.setStatusCd("0");
        List<CommunitySpacePersonDto> communitySpacePersonDtos
                = communitySpacePersonV1InnerServiceSMOImpl.queryCommunitySpacePersons(communitySpacePersonDto);

        if (communitySpacePersonDtos == null || communitySpacePersonDtos.size() < 1) {
            return;
        }

        if (!CommunitySpacePersonDto.STATE_C.equals(communitySpacePersonDtos.get(0).getState())) {
            return;
        }

        CommunitySpacePersonPo communitySpacePersonPo = new CommunitySpacePersonPo();
        communitySpacePersonPo.setCspId(communitySpaceConfirmOrderDto.getCspId());
        communitySpacePersonPo.setState(CommunitySpacePersonDto.STATE_S);
        communitySpacePersonV1InnerServiceSMOImpl.updateCommunitySpacePerson(communitySpacePersonPo);
    }
}
