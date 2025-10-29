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
import com.java110.dto.question.QuestionTitleValueDto;
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

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：questionTitle.updateQuestionTitle
 * 请求路劲：/app/questionTitle.UpdateQuestionTitle
 * add by 吴学文 at 2023-07-10 15:10:47 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 问题标题更新命令类
 * 
 * 该类负责处理问题标题的更新操作，包括问题标题基本信息更新和关联选项值的更新
 * 支持不同类型的问题标题处理，如选择题和问答题
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-07-10
 */
@Java110Cmd(serviceCode = "question.updateQuestionTitle")
public class UpdateQuestionTitleCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateQuestionTitleCmd.class);

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
     * 参数验证方法
     * 
     * 验证请求参数是否完整有效，包括必填字段检查和非问答题类型的选项验证
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "titleId", "titleId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

        JSONArray titleValues = null;
        // 如果不是问答题类型，需要验证选项
        if (!QuestionTitleDto.TITLE_TYPE_QUESTIONS.equals(reqJson.getString("titleType"))) {
            titleValues = reqJson.getJSONArray("titleValues");
            // 检查选项数组是否为空
            if (titleValues.size() < 1) {
                throw new IllegalArgumentException("未包含选项");
            }
        }
    }

    /**
     * 执行更新问题标题命令
     * 
     * 该方法负责更新问题标题的基本信息，并根据标题类型处理关联的选项值：
     * - 对于问答题类型，只更新标题信息
     * - 对于选择题类型，先删除原有选项，再添加新的选项
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当更新操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为问题标题PO对象
        QuestionTitlePo questionTitlePo = BeanConvertUtil.covertBean(reqJson, QuestionTitlePo.class);
        // 更新问题标题基本信息
        int flag = questionTitleV1InnerServiceSMOImpl.updateQuestionTitle(questionTitlePo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 如果是问答题类型，直接返回成功
        if (QuestionTitleDto.TITLE_TYPE_QUESTIONS.equals(questionTitlePo.getTitleType())) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 处理选择题类型的选项值更新
        // 先查询原有的选项值
        QuestionTitleValueDto questionTitleValueDto = new QuestionTitleValueDto();
        questionTitleValueDto.setTitleId(reqJson.getString("titleId"));
        List<QuestionTitleValueDto> questionTitleValues = questionTitleValueV1InnerServiceSMOImpl.queryQuestionTitleValues(questionTitleValueDto);
        
        // 如果存在原有选项，先删除所有选项
        if (questionTitleValues != null && questionTitleValues.size() > 0) {
            QuestionTitleValuePo deleteQuestionTitleValuePo = new QuestionTitleValuePo();
            deleteQuestionTitleValuePo.setTitleId(questionTitlePo.getTitleId());
            flag = questionTitleValueV1InnerServiceSMOImpl.deleteQuestionTitleValue(deleteQuestionTitleValuePo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }
        
        // 添加新的选项值
        JSONArray titleValues = reqJson.getJSONArray("titleValues");
        QuestionTitleValuePo questionTitleValuePo = null;
        // 遍历选项数组，逐个创建并保存选项值
        for (int titleValueIndex = 0; titleValueIndex < titleValues.size(); titleValueIndex++) {
            questionTitleValuePo = new QuestionTitleValuePo();
            // 设置选项值内容
            questionTitleValuePo.setQaValue(titleValues.getJSONObject(titleValueIndex).getString("qaValue"));
            // 设置选项排序序号
            questionTitleValuePo.setSeq(titleValues.getJSONObject(titleValueIndex).getString("seq"));
            // 设置选项分数，如果请求中未包含分数则默认为0
            if(titleValues.getJSONObject(titleValueIndex).containsKey("score")) {
                questionTitleValuePo.setScore(titleValues.getJSONObject(titleValueIndex).getString("score"));
            }else{
                questionTitleValuePo.setScore("0");
            }
            // 设置关联的问题标题ID
            questionTitleValuePo.setTitleId(questionTitlePo.getTitleId());
            // 生成选项值ID
            questionTitleValuePo.setValueId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_valueId));
            // 设置社区ID
            questionTitleValuePo.setCommunityId(questionTitlePo.getCommunityId());
            // 保存选项值
            questionTitleValueV1InnerServiceSMOImpl.saveQuestionTitleValue(questionTitleValuePo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}