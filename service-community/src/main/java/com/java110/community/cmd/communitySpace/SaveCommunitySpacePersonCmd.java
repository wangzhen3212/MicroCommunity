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
import com.java110.dto.community.CommunitySpacePersonDto;
import com.java110.dto.community.CommunitySpacePersonTimeDto;
import com.java110.intf.community.ICommunitySpacePersonTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpacePersonV1InnerServiceSMO;
import com.java110.po.communitySpace.CommunitySpacePersonPo;
import com.java110.po.communitySpace.CommunitySpacePersonTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


@Java110CmdDoc(title = "预约场地",
        description = "系统中的预约场地",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/communitySpace.saveCommunitySpacePerson",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.saveCommunitySpacePerson"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page",type = "int",length = 11, remark = "分页页数"),
        @Java110ParamDoc(name = "row",type = "int", length = 11, remark = "分页行数"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "appointmentDate", length = 30, remark = "预约日期 YYYY-MM-DD"),
        @Java110ParamDoc(name = "appointmentTime", length = 30, remark = "预约时间 HH24:MI"),
        @Java110ParamDoc(name = "payWay", length = 12, remark = "支付方式"),
        @Java110ParamDoc(name = "personName", length = 64, remark = "预约人"),
        @Java110ParamDoc(name = "personTel", length = 30, remark = "预约人电话"),
        @Java110ParamDoc(name = "receivableAmount", length = 30, remark = "应收金额"),
        @Java110ParamDoc(name = "receivedAmount", length = 30, remark = "实收金额"),
        @Java110ParamDoc(name = "spaceId", length = 30, remark = "场地ID"),
        @Java110ParamDoc(name = "openTimes",type="Array", length = 30, remark = "场地ID"),
        @Java110ParamDoc(parentNodeName = "openTimes",name = "hours", length = 30, remark = "预约时间"),

})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),

        }
)

@Java110ExampleDoc(
        reqBody="{\"spaceId\":\"102022093043260007\",\"personName\":\"wuxw\",\"personTel\":\"18909711443\",\"appointmentTime\":\"01:00\",\"receivableAmount\":\"10\",\"receivedAmount\":\"10\",\"payWay\":\"2\",\"state\":\"S\",\"remark\":\"123\",\"appointmentDate\":\"2022-09-01\",\"communityId\":\"2022081539020475\"}",
        resBody="{\"code\":0,\"msg\":\"成功\"}"
)

/**
 * 场地预约保存命令类
 * 负责处理社区场地预约的创建和保存逻辑
 * 包括预约信息验证、预约时间冲突检查、预约记录保存等功能
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-09-30
 */
@Java110Cmd(serviceCode = "communitySpace.saveCommunitySpacePerson")
public class SaveCommunitySpacePersonCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveCommunitySpacePersonCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 场地预约服务接口 */
    @Autowired
    private ICommunitySpacePersonV1InnerServiceSMO communitySpacePersonV1InnerServiceSMOImpl;

    /** 场地预约时间服务接口 */
    @Autowired
    private ICommunitySpacePersonTimeV1InnerServiceSMO communitySpacePersonTimeV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查必填字段是否存在，并验证预约时间是否冲突
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败或时间冲突时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "spaceId", "请求报文中未包含spaceId");
        Assert.hasKeyAndValue(reqJson, "personName", "请求报文中未包含personName");
        Assert.hasKeyAndValue(reqJson, "personTel", "请求报文中未包含personTel");
        Assert.hasKeyAndValue(reqJson, "appointmentTime", "请求报文中未包含appointmentTime");
        Assert.hasKeyAndValue(reqJson, "receivableAmount", "请求报文中未包含receivableAmount");
        Assert.hasKeyAndValue(reqJson, "receivedAmount", "请求报文中未包含receivedAmount");
        Assert.hasKeyAndValue(reqJson, "payWay", "请求报文中未包含payWay");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 检查是否包含预约时间段信息
        if(!reqJson.containsKey("openTimes")){
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return ;
        }

        JSONArray openTimes = reqJson.getJSONArray("openTimes");

        // 如果预约时间段为空，直接返回成功
        if(openTimes == null || openTimes.size() <1){
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return ;
        }
        
        // 检查预约时间段是否已被占用
        CommunitySpacePersonTimeDto communitySpacePersonTimeDto = null;
        int flag = 0;
        for(int timeIndex = 0 ;timeIndex < openTimes.size(); timeIndex++) {
            // 跳过未开放的时间段
            if("N".equals(openTimes.getJSONObject(timeIndex).getString("isOpen"))){
                continue;
            }
            // 构建时间段查询条件
            communitySpacePersonTimeDto = new CommunitySpacePersonTimeDto();
            communitySpacePersonTimeDto.setCommunityId(reqJson.getString("communityId"));
            communitySpacePersonTimeDto.setAppointmentTime(reqJson.getString("appointmentTime"));
            communitySpacePersonTimeDto.setHours(openTimes.getJSONObject(timeIndex).getString("hours"));
            communitySpacePersonTimeDto.setSpaceId(reqJson.getString("spaceId"));
            // 查询该时间段是否已被预约
            flag = communitySpacePersonTimeV1InnerServiceSMOImpl.queryCommunitySpacePersonTimesCount(communitySpacePersonTimeDto);
            if(flag > 0){
                throw new CmdException(reqJson.getString("appointmentTime")+","+openTimes.getJSONObject(timeIndex).getString("hours")+"已经被预约");
            }
        }

    }

    /**
     * 执行场地预约保存操作
     * 保存预约主信息和预约时间段信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 转换请求数据为PO对象
        CommunitySpacePersonPo communitySpacePersonPo = BeanConvertUtil.covertBean(reqJson, CommunitySpacePersonPo.class);
        // 生成主键ID
        communitySpacePersonPo.setCspId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 生成订单ID
        communitySpacePersonPo.setOrderId(GenerateCodeFactory.getGeneratorId("11"));
        // 设置默认状态为等待确认
        if(StringUtil.isEmpty(communitySpacePersonPo.getState())){
            communitySpacePersonPo.setState(CommunitySpacePersonDto.STATE_W);
        }
        // 保存预约主信息
        int flag = communitySpacePersonV1InnerServiceSMOImpl.saveCommunitySpacePerson(communitySpacePersonPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 检查是否包含预约时间段信息
        if(!reqJson.containsKey("openTimes")){
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return ;
        }

        JSONArray openTimes = reqJson.getJSONArray("openTimes");

        // 如果预约时间段为空，直接返回成功
        if(openTimes == null || openTimes.size() <1){
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return ;
        }
        
        // 保存预约时间段信息
        CommunitySpacePersonTimePo communitySpacePersonTimePo = null;
        for(int timeIndex = 0 ;timeIndex < openTimes.size(); timeIndex++) {
            // 跳过未开放的时间段
            if("N".equals(openTimes.getJSONObject(timeIndex).getString("isOpen"))){
                continue;
            }
            // 构建时间段PO对象
            communitySpacePersonTimePo = new CommunitySpacePersonTimePo();
            communitySpacePersonTimePo.setCommunityId(communitySpacePersonPo.getCommunityId());
            communitySpacePersonTimePo.setCspId(communitySpacePersonPo.getCspId());
            communitySpacePersonTimePo.setHours(openTimes.getJSONObject(timeIndex).getString("hours"));
            communitySpacePersonTimePo.setSpaceId(communitySpacePersonPo.getSpaceId());
            communitySpacePersonTimePo.setTimeId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置时间段状态为等待确认
            communitySpacePersonTimePo.setState(CommunitySpacePersonTimeDto.STATE_WAIT_CONFIRM);
            // 保存时间段信息
            communitySpacePersonTimeV1InnerServiceSMOImpl.saveCommunitySpacePersonTime(communitySpacePersonTimePo);
        }

        // 返回成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}