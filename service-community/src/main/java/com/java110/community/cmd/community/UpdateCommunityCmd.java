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
package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.community.bmo.community.ICommunityBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.po.privilege.RoleCommunityPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Java110CmdDoc(title = "修改小区",
        description = "主要提供给外系统修改小区",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/community.updateCommunity",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "community.updateCommunity",
        seq = 2
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区编码"),
        @Java110ParamDoc(name = "address", length = 30, remark = "小区地址"),
        @Java110ParamDoc(name = "cityCode", length = 12, remark = "地区编码"),
        @Java110ParamDoc(name = "feePrice", type = "int", length = 11, remark = "小区收费价格"),
        @Java110ParamDoc(name = "mapX", length = 12, remark = "经度"),
        @Java110ParamDoc(name = "mapY", length = 12, remark = "纬度"),
        @Java110ParamDoc(name = "name", length = 64, remark = "名称"),
        @Java110ParamDoc(name = "nearbyLandmarks", length = 64, remark = "地标，如xx 公园旁"),
        @Java110ParamDoc(name = "payFeeMonth", type = "int", length = 11, remark = "小区收费周期"),
        @Java110ParamDoc(name = "tel", length = 11, remark = "小区客服电话"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"communityId\":\"2022092293190329\",\"name\":\"api接口小区1\",\"address\":\"天津省天津市和平区api接口小区\",\"nearbyLandmarks\":\"23\",\"cityCode\":\"120101\",\"mapX\":\"101.33\",\"mapY\":\"101.33\",\"payFeeMonth\":12,\"feePrice\":0,\"tel\":\"18909711443\",\"attrs\":[{\"domain\":\"COMMON\",\"listShow\":\"Y\",\"page\":-1,\"records\":0,\"required\":\"Y\",\"row\":0,\"specCd\":\"9329000004\",\"specHoldplace\":\"必填，请填写社区编码\",\"specId\":\"9329000004\",\"specName\":\"社区编码\",\"specShow\":\"Y\",\"specType\":\"2233\",\"specValueType\":\"1001\",\"statusCd\":\"0\",\"tableName\":\"building_community_attr\",\"total\":0,\"value\":\"123123\",\"values\":[],\"attrId\":\"112022092280950341\"}]}",
        resBody = "{'code':0,'msg':'成功'}"
)
/**
 * 小区信息更新命令类
 * 该类负责处理小区信息的更新操作，包括小区基本信息、扩展属性和角色小区关联信息的更新
 * 主要功能：
 * 1. 验证更新请求参数的完整性
 * 2. 更新小区基本信息
 * 3. 处理小区扩展属性的新增和更新
 * 4. 同步更新角色小区关联信息中的小区名称
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-18
 */
@Java110Cmd(serviceCode = "community.updateCommunity")
public class UpdateCommunityCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateCommunityCmd.class);

    /**
     * 小区服务接口
     */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 角色小区关联服务接口
     */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    /**
     * 小区业务操作接口
     */
    @Autowired
    private ICommunityBMO communityBMOImpl;

    /**
     * 验证请求参数
     * 检查请求参数中必须包含的字段，确保更新操作的完整性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 检查当前运行环境，仅在开发环境下执行某些特殊逻辑
        Environment.isDevEnv();
        // 验证必须参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写小区名称");
        Assert.hasKeyAndValue(reqJson, "address", "必填，请填写小区地址");
        Assert.hasKeyAndValue(reqJson, "nearbyLandmarks", "必填，请填写小区附近地标");

        // 验证属性值
        Assert.judgeAttrValue(reqJson);
    }

    /**
     * 执行小区更新命令
     * 处理小区信息的完整更新流程，包括基本信息、扩展属性和角色小区关联信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 更新小区基本信息
        communityBMOImpl.updateCommunityOne(reqJson, cmdDataFlowContext);

        // 检查是否存在扩展属性需要处理
        if (!reqJson.containsKey("attrs")) {
            return;
        }

        JSONArray attrs = reqJson.getJSONArray("attrs");
        // 如果扩展属性为空，直接返回
        if (attrs == null || attrs.size() < 1) {
            return;
        }

        // 处理每个扩展属性
        JSONObject attr = null;
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            // 设置小区ID到属性中
            attr.put("communityId", reqJson.getString("communityId"));
            
            // 判断属性操作类型：新增或更新
            if (!attr.containsKey("attrId") || attr.getString("attrId").startsWith("-") || StringUtil.isEmpty(attr.getString("attrId"))) {
                // 新增属性
                communityBMOImpl.addAttr(attr, cmdDataFlowContext);
                continue;
            }
            // 更新属性
            communityBMOImpl.updateAttr(attr, cmdDataFlowContext);
        }

        // 如果小区名称没有修改，直接返回
        if (StringUtil.isEmpty(reqJson.getString("name"))) {
            return;
        }

        // 查询与小区关联的角色信息
        RoleCommunityDto roleCommunityDto = new RoleCommunityDto();
        roleCommunityDto.setCommunityId(reqJson.getString("communityId"));
        List<RoleCommunityDto> roleCommunityDtos = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitys(roleCommunityDto);

        // 如果没有关联的角色信息，直接返回
        if (roleCommunityDtos == null || roleCommunityDtos.size() < 1) {
            return;
        }
        
        // 更新所有关联角色的小区名称
        RoleCommunityPo roleCommunityPo = null;
        for (RoleCommunityDto tmpRoleCommunityDto : roleCommunityDtos) {
            roleCommunityPo = new RoleCommunityPo();
            roleCommunityPo.setRcId(tmpRoleCommunityDto.getRcId());
            roleCommunityPo.setCommunityName(reqJson.getString("name"));
            roleCommunityV1InnerServiceSMOImpl.updateRoleCommunity(roleCommunityPo);
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}