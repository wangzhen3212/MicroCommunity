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
import com.java110.dto.community.CommunitySpacePersonDto;
import com.java110.intf.community.ICommunitySpacePersonV1InnerServiceSMO;
import com.java110.po.communitySpace.CommunitySpacePersonPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Java110CmdDoc(title = "修改场地预约",
        description = "修改场地预约订单存量数据；收款已由 communitySpace.saveCommunitySpacePerson 在建单时一次完成，本命令不再承担收款职责",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/communitySpace.updateCommunitySpacePerson",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.updateCommunitySpacePerson"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "cspId", length = 30, remark = "预约ID"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "receivedAmount", length = 30, remark = "实收金额（存量兼容字段，收款已不由此命令完成）"),
        @Java110ParamDoc(name = "payWay", length = 12, remark = "支付方式（存量兼容字段，收款已不由此命令完成）"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"cspId\":\"102022093043260007\",\"communityId\":\"2022081539020475\",\"receivedAmount\":\"20.00\",\"payWay\":\"1\"}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)

/**
 * 类表述：更新
 * 服务编码：communitySpacePerson.updateCommunitySpacePerson
 * 请求路劲：/app/communitySpacePerson.UpdateCommunitySpacePerson
 * add by 吴学文 at 2022-09-30 11:36:52 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * // modify by wuxw at 2026-10-13 收款已由 communitySpace.saveCommunitySpacePerson 在建单时一次写入，
 * // 本命令移出收款流程：不再做 待支付 → 已预约 的状态流转，不再写 payTime/payWay/receivedAmount，仅保留存量数据更新能力
 */
@Java110Cmd(serviceCode = "communitySpace.updateCommunitySpacePerson")
public class UpdateCommunitySpacePersonCmd extends Cmd {

    @Autowired
    private ICommunitySpacePersonV1InnerServiceSMO communitySpacePersonV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "cspId", "cspId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        CommunitySpacePersonDto communitySpacePersonDto = new CommunitySpacePersonDto();
        communitySpacePersonDto.setCspId(reqJson.getString("cspId"));
        communitySpacePersonDto.setCommunityId(reqJson.getString("communityId"));
        List<CommunitySpacePersonDto> communitySpacePersonDtos = communitySpacePersonV1InnerServiceSMOImpl.queryCommunitySpacePersons(communitySpacePersonDto);

        Assert.listOnlyOne(communitySpacePersonDtos, "预约订单不存在");

        String state = communitySpacePersonDtos.get(0).getState();

        if (CommunitySpacePersonDto.STATE_CL.equals(state)) {
            throw new CmdException("已取消的预约不能修改");
        }

        if (CommunitySpacePersonDto.STATE_C.equals(state)) {
            throw new CmdException("已完成的预约不能修改");
        }

        CommunitySpacePersonPo communitySpacePersonPo = BeanConvertUtil.covertBean(reqJson, CommunitySpacePersonPo.class);

        if (StringUtil.isEmpty(communitySpacePersonPo.getState())) {
            communitySpacePersonPo.setState(CommunitySpacePersonDto.STATE_S);
        }

        int flag = communitySpacePersonV1InnerServiceSMOImpl.updateCommunitySpacePerson(communitySpacePersonPo);

        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}
