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
package com.java110.community.cmd.maintainance;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.maintainance.MaintainanceItemDto;
import com.java110.intf.community.IMaintainanceItemV1InnerServiceSMO;
import com.java110.intf.community.IMaintainanceItemValueV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainanceItemPo;
import com.java110.po.maintainance.MaintainanceItemValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存维护项命令类
 * 服务编码：maintainance.saveMaintainanceItem
 * 请求路劲：/app/maintainance.SaveMaintainanceItem
 * 功能描述：该类用于处理维护项信息的保存操作，包括维护项基本信息及其选项值的保存
 * add by 吴学文 at 2022-11-06 14:23:46 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "maintainance.saveMaintainanceItem")
public class SaveMaintainanceItemCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveMaintainanceItemCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 维护项服务接口
     */
    @Autowired
    private IMaintainanceItemV1InnerServiceSMO maintainanceItemV1InnerServiceSMOImpl;

    /**
     * 维护项选项值服务接口
     */
    @Autowired
    private IMaintainanceItemValueV1InnerServiceSMO maintainanceItemValueV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整有效
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "itemTitle", "请求报文中未包含itemTitle");
        Assert.hasKeyAndValue(reqJson, "titleType", "请求报文中未包含titleType");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含seq");

        JSONArray titleValues = null;
        // 如果标题类型不是问答类型，则需要验证选项值
        if (!MaintainanceItemDto.TITLE_TYPE_QUESTIONS.equals(reqJson.getString("titleType"))) {
            titleValues = reqJson.getJSONArray("titleValues");
            // 验证选项值数组是否为空
            if (titleValues.size() < 1) {
                throw new IllegalArgumentException("未包含选项");
            }
        }
    }

    /**
     * 执行保存维护项命令
     * 功能描述：保存维护项基本信息及其选项值信息
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为维护项PO对象
        MaintainanceItemPo maintainanceItemPo = BeanConvertUtil.covertBean(reqJson, MaintainanceItemPo.class);
        // 生成维护项ID
        maintainanceItemPo.setItemId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存维护项基本信息
        int flag = maintainanceItemV1InnerServiceSMOImpl.saveMaintainanceItem(maintainanceItemPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 如果是问答类型的维护项，直接返回成功
        if (MaintainanceItemDto.TITLE_TYPE_QUESTIONS.equals(maintainanceItemPo.getTitleType())) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 处理选项值信息
        JSONArray titleValues = reqJson.getJSONArray("titleValues");
        MaintainanceItemValuePo maintainanceItemValuePo = null;
        // 遍历选项值数组，逐个保存选项值
        for (int titleValueIndex = 0; titleValueIndex < titleValues.size(); titleValueIndex++) {
            maintainanceItemValuePo = new MaintainanceItemValuePo();
            // 设置选项值内容
            maintainanceItemValuePo.setItemValue(titleValues.getJSONObject(titleValueIndex).getString("itemValue"));
            // 设置选项排序
            maintainanceItemValuePo.setSeq(titleValues.getJSONObject(titleValueIndex).getString("seq"));
            // 设置关联的维护项ID
            maintainanceItemValuePo.setItemId(maintainanceItemPo.getItemId());
            // 生成选项值ID
            maintainanceItemValuePo.setValueId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_valueId));
            // 设置小区ID
            maintainanceItemValuePo.setCommunityId(maintainanceItemPo.getCommunityId());
            // 保存选项值信息
            maintainanceItemValueV1InnerServiceSMOImpl.saveMaintainanceItemValue(maintainanceItemValuePo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}