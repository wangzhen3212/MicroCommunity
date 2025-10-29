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
package com.java110.user.cmd.question;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.question.QuestionTitleDto;
import com.java110.intf.user.IQuestionTitleV1InnerServiceSMO;
import com.java110.intf.user.IQuestionTitleValueV1InnerServiceSMO;
import com.java110.po.question.QuestionTitlePo;
import com.java110.po.question.QuestionTitleValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存问题标题命令类
 * 服务编码：questionTitle.saveQuestionTitle
 * 请求路劲：/app/questionTitle.SaveQuestionTitle
 * 功能描述：该类用于处理问题标题的保存请求，包括问题标题的基本信息和对应的选项值
 * 主要功能：
 * 1. 验证请求参数的完整性
 * 2. 保存问题标题基本信息
 * 3. 保存问题标题对应的选项值（非简答题类型）
 * 4. 返回操作结果
 * add by 吴学文 at 2023-07-10 15:10:47 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "question.saveQuestionTitle")
public class SaveQuestionTitleCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveQuestionTitleCmd.class);

    /**
     * 问题标题ID的前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 问题标题服务接口
     */
    @Autowired
    private IQuestionTitleV1InnerServiceSMO questionTitleV1InnerServiceSMOImpl;

    /**
     * 问题标题选项值服务接口
     */
    @Autowired
    private IQuestionTitleValueV1InnerServiceSMO questionTitleValueV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 功能描述：验证请求报文中的必填字段是否完整，对于非简答题类型需要验证选项值是否存在
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求报文JSON对象
     * @throws IllegalArgumentException 当必填字段缺失或选项值不符合要求时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "qaTitle", "请求报文中未包含qaTitle");
        Assert.hasKeyAndValue(reqJson, "titleType", "请求报文中未包含titleType");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 如果不是简答题类型，需要验证选项值
        JSONArray titleValues = null;
        if (!QuestionTitleDto.TITLE_TYPE_QUESTIONS.equals(reqJson.getString("titleType"))) {
            // 获取选项值数组
            titleValues = reqJson.getJSONArray("titleValues");
            // 验证选项值是否至少有一个
            if (titleValues.size() < 1) {
                throw new IllegalArgumentException("未包含选项");
            }
        }

    }

    /**
     * 执行保存问题标题命令
     * 功能描述：保存问题标题的基本信息，对于非简答题类型还需要保存对应的选项值
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求报文JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为问题标题PO对象
        QuestionTitlePo questionTitlePo = BeanConvertUtil.covertBean(reqJson, QuestionTitlePo.class);
        // 生成问题标题ID
        questionTitlePo.setTitleId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存问题标题基本信息
        int flag = questionTitleV1InnerServiceSMOImpl.saveQuestionTitle(questionTitlePo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 如果是简答题类型，直接返回成功，不需要保存选项值
        if (QuestionTitleDto.TITLE_TYPE_QUESTIONS.equals(questionTitlePo.getTitleType())) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 处理非简答题类型的选项值
        JSONArray titleValues = reqJson.getJSONArray("titleValues");
        QuestionTitleValuePo questionTitleValuePo = null;
        JSONObject valueObj = null;
        // 遍历所有选项值
        for (int titleValueIndex = 0; titleValueIndex < titleValues.size(); titleValueIndex++) {
            valueObj = titleValues.getJSONObject(titleValueIndex);
            questionTitleValuePo = new QuestionTitleValuePo();
            // 设置选项值
            questionTitleValuePo.setQaValue(valueObj.getString("itemValue"));
            // 设置序号（从1开始）
            questionTitleValuePo.setSeq((titleValueIndex+1)+"");
            // 设置分数，如果请求中包含分数则使用，否则默认为0
            if(valueObj.containsKey("score")) {
                questionTitleValuePo.setScore(valueObj.getString("score"));
            }else{
                questionTitleValuePo.setScore("0");
            }
            // 设置关联的问题标题ID
            questionTitleValuePo.setTitleId(questionTitlePo.getTitleId());
            // 生成选项值ID
            questionTitleValuePo.setValueId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_valueId));
            // 设置小区ID
            questionTitleValuePo.setCommunityId(questionTitlePo.getCommunityId());
            // 保存选项值
            questionTitleValueV1InnerServiceSMOImpl.saveQuestionTitleValue(questionTitleValuePo);
        }

        // 返回成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}