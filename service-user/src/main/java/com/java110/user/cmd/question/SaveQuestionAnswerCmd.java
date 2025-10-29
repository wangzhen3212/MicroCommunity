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
import com.java110.dto.question.QuestionAnswerDto;
import com.java110.intf.user.IQuestionAnswerTitleRelV1InnerServiceSMO;
import com.java110.intf.user.IQuestionAnswerV1InnerServiceSMO;
import com.java110.po.question.QuestionAnswerPo;
import com.java110.po.question.QuestionAnswerTitleRelPo;
import com.java110.user.bmo.question.IQuestionAnswerBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存问卷答案命令类
 * 服务编码：questionAnswer.saveQuestionAnswer
 * 请求路劲：/app/questionAnswer.SaveQuestionAnswer
 * 功能描述：该类负责处理问卷答案的保存操作，包括问卷基本信息、题目关联信息和用户答题信息的保存
 * add by 吴学文 at 2023-07-13 00:03:56 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "question.saveQuestionAnswer")
public class SaveQuestionAnswerCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveQuestionAnswerCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 问卷答案服务接口 */
    @Autowired
    private IQuestionAnswerV1InnerServiceSMO questionAnswerV1InnerServiceSMOImpl;

    /** 问卷题目关联服务接口 */
    @Autowired
    private IQuestionAnswerTitleRelV1InnerServiceSMO questionAnswerTitleRelV1InnerServiceSMOImpl;

    /** 问卷答案业务操作接口 */
    @Autowired
    private IQuestionAnswerBMO questionAnswerBMOImpl;

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
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "qaName", "请求报文中未包含投票名称");
        Assert.hasKey(reqJson, "roomIds", "请求报文中未包含投票房屋");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含结束时间");
        Assert.hasKeyAndValue(reqJson, "content", "未包含调研说明");
        Assert.hasKey(reqJson, "questionTitles", "请求报文中未包含题目");
        
        // 验证题目数组是否为空
        JSONArray questionTitles = reqJson.getJSONArray("questionTitles");
        if (questionTitles == null || questionTitles.size() < 1) {
            throw new IllegalArgumentException("未包含题目");
        }
    }

    /**
     * 执行命令方法
     * 功能描述：保存问卷答案信息，包括问卷基本信息、题目关联信息和用户答题信息
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建问卷答案PO对象并设置属性
        QuestionAnswerPo questionAnswerPo = new QuestionAnswerPo();
        questionAnswerPo.setContent(reqJson.getString("content"));
        questionAnswerPo.setEndTime(reqJson.getString("endTime"));
        questionAnswerPo.setStartTime(reqJson.getString("startTime"));
        questionAnswerPo.setQaId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        questionAnswerPo.setQaName(reqJson.getString("qaName"));
        questionAnswerPo.setCommunityId(reqJson.getString("communityId"));
        questionAnswerPo.setQaType(QuestionAnswerDto.QA_TYPE_QUESTION); // 设置问卷类型为调研类型
        questionAnswerPo.setState(QuestionAnswerDto.STATE_WAIT); // 设置问卷状态为等待状态
        
        // 根据不同的业务场景设置对象类型和对象ID
        if (!StringUtil.isEmpty(reqJson.getString("communityId"))) {
            questionAnswerPo.setObjType("3306"); // 3306 是小区，3307 是商户
            questionAnswerPo.setObjId(reqJson.getString("communityId"));
        } else if (!StringUtil.isEmpty(reqJson.getString("storeId"))) {
            questionAnswerPo.setObjType("3307"); // 3306 是小区，3307 是商户
            questionAnswerPo.setObjId(reqJson.getString("storeId"));
        }
        
        // 保存问卷基本信息
        int flag = questionAnswerV1InnerServiceSMOImpl.saveQuestionAnswer(questionAnswerPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 处理问卷题目关联信息
        JSONArray questionTitles = reqJson.getJSONArray("questionTitles");
        JSONObject title = null;
        for (int titleIndex = 0; titleIndex < questionTitles.size(); titleIndex++) {
            title = questionTitles.getJSONObject(titleIndex);
            QuestionAnswerTitleRelPo questionAnswerTitleRelPo = new QuestionAnswerTitleRelPo();
            questionAnswerTitleRelPo.setCommunityId(reqJson.getString("communityId"));
            questionAnswerTitleRelPo.setTitleId(title.getString("titleId"));
            questionAnswerTitleRelPo.setSeq((titleIndex + 1) + ""); // 设置题目顺序
            questionAnswerTitleRelPo.setScore("0"); // 默认分数为0
            if (title.containsKey("score")) {
                questionAnswerTitleRelPo.setScore(title.getString("score")); // 如果题目有分数则设置分数
            }
            questionAnswerTitleRelPo.setQaId(questionAnswerPo.getQaId());
            questionAnswerTitleRelPo.setQatrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 保存题目关联信息
            questionAnswerTitleRelV1InnerServiceSMOImpl.saveQuestionAnswerTitleRel(questionAnswerTitleRelPo);
        }
        
        // 保存用户答题信息
        questionAnswerBMOImpl.saveUserQuestionAnswer(questionAnswerPo, reqJson.getJSONArray("roomIds"));
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}