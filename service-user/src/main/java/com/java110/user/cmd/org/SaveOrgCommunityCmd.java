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
package com.java110.user.cmd.org;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.user.IOrgCommunityV1InnerServiceSMO;
import com.java110.po.community.OrgCommunityPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存组织小区关系命令类
 * 服务编码：orgCommunity.saveOrgCommunity
 * 请求路劲：/app/orgCommunity.SaveOrgCommunity
 * 功能描述：该命令类用于处理组织与小区关系的保存操作，包括参数验证和业务处理
 * add by 吴学文 at 2022-08-08 18:05:07 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "org.saveOrgCommunity")
public class SaveOrgCommunityCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveOrgCommunityCmd.class);

    /**
     * ID生成前缀常量
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 组织小区关系服务接口
     */
    @Autowired
    private IOrgCommunityV1InnerServiceSMO orgCommunityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整和有效
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段：组织ID
        Assert.hasKeyAndValue(reqJson, "orgId", "必填，请填写组织ID");
        // 验证必填字段：组织名称
        Assert.hasKeyAndValue(reqJson, "orgName", "必填，请填写组织名称");
        // 验证小区信息数组是否存在且不为空
        if (!reqJson.containsKey("communitys") || reqJson.getJSONArray("communitys").size() < 1) {
            throw new IllegalArgumentException("未包含小区信息");
        }

    }

    /**
     * 命令执行方法
     * 功能描述：处理组织小区关系的保存业务逻辑，支持批量保存
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 获取小区信息数组
        JSONArray communitys = reqJson.getJSONArray("communitys");
        // 遍历小区数组，逐个添加组织小区关系
        for (int communityIndex = 0; communityIndex < communitys.size(); communityIndex++) {
            addOrgCommunity(reqJson, communitys.getJSONObject(communityIndex), communityIndex);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 添加组织小区关系方法
     * 功能描述：将单个小区与组织建立关联关系并保存到数据库
     * @param paramInJson 接口调用方传入的原始参数
     * @param communityObj 单个小区信息对象
     * @param seq 小区在数组中的序号（用于日志记录和调试）
     * @throws CmdException 当保存数据失败时抛出异常
     */
    public void addOrgCommunity(JSONObject paramInJson, JSONObject communityObj, int seq) {

        // 创建业务对象，合并组织信息和小区的ID和名称
        JSONObject businessOrg = new JSONObject();
        businessOrg.putAll(paramInJson);
        businessOrg.put("communityId", communityObj.getString("communityId"));
        businessOrg.put("communityName", communityObj.getString("communityName"));

        // 将JSON对象转换为POJO对象
        OrgCommunityPo orgCommunityPo = BeanConvertUtil.covertBean(businessOrg, OrgCommunityPo.class);
        // 生成组织小区关系的主键ID
        orgCommunityPo.setOrgCommunityId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));

        // 调用服务层保存组织小区关系
        int flag = orgCommunityV1InnerServiceSMOImpl.saveOrgCommunity(orgCommunityPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
    }
}