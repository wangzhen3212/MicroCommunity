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
import com.java110.intf.user.IUserQuestionAnswerV1InnerServiceSMO;
import com.java110.po.question.QuestionAnswerPo;
import com.java110.po.question.QuestionAnswerTitleRelPo;
import com.java110.po.user.UserQuestionAnswerPo;
import com.java110.user.bmo.question.IQuestionAnswerBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：questionAnswer.updateQuestionAnswer
 * 请求路劲：/app/questionAnswer.UpdateQuestionAnswer
 * add by 吴学文 at 2023-07-13 00:03:56 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 更新问卷调查答案命令类
 * 
 * 该类负责处理更新问卷调查答案的业务逻辑，包括验证请求参数、更新问卷信息、
 * 处理问卷题目关联关系以及用户问卷答案关联关系等操作。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-07-13
 */
@Java110Cmd(serviceCode = "question.updateQuestionAnswer")
public class UpdateQuestionAnswerCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateQuestionAnswerCmd.class);

    /**
     * 问卷调查答案服务接口
     */
    @Autowired
    private IQuestionAnswerV1InnerServiceSMO questionAnswerV1InnerServiceSMOImpl;

    /**
     * 问卷题目关联关系服务接口
     */
    @Autowired
    private IQuestionAnswerTitleRelV1InnerServiceSMO questionAnswerTitleRelV1InnerServiceSMOImpl;

    /**
     * 问卷调查业务操作接口
     */
    @Autowired
    private IQuestionAnswerBMO questionAnswerBMOImpl;

    /**
     * 用户问卷答案关联关系服务接口
     */
    @Autowired
    private IUserQuestionAnswerV1InnerServiceSMO userQuestionAnswerV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证更新问卷调查答案请求中的必要参数是否完整有效，
     * 包括社区ID、问卷名称、问卷ID、开始时间、结束时间、问卷说明和题目列表等。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws IllegalArgumentException 当必要参数缺失或题目列表为空时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "qaName", "请求报文中未包含投票名称");
        Assert.hasKeyAndValue(reqJson, "qaId", "请求报文中未包含题目");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含结束时间");
        Assert.hasKeyAndValue(reqJson, "content", "未包含调研说明");
        Assert.hasKey(reqJson, "questionTitles", "请求报文中未包含题目");

        // 验证题目列表是否为空
        JSONArray questionTitles = reqJson.getJSONArray("questionTitles");
        if (questionTitles == null || questionTitles.size() < 1) {
            throw new IllegalArgumentException("未包含题目");
        }

    }

    /**
     * 执行更新问卷调查答案命令
     * 
     * 该方法负责执行更新问卷调查答案的核心业务逻辑，包括：
     * 1. 更新问卷基本信息
     * 2. 删除并重新创建问卷题目关联关系
     * 3. 处理用户问卷答案关联关系
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 创建问卷答案PO对象并设置属性
        QuestionAnswerPo questionAnswerPo = new QuestionAnswerPo();
        questionAnswerPo.setContent(reqJson.getString("content"));
        questionAnswerPo.setEndTime(reqJson.getString("endTime"));
        questionAnswerPo.setStartTime(reqJson.getString("startTime"));
        questionAnswerPo.setQaId(reqJson.getString("qaId"));
        questionAnswerPo.setQaName(reqJson.getString("qaName"));
        questionAnswerPo.setCommunityId(reqJson.getString("communityId"));
        questionAnswerPo.setQaType(QuestionAnswerDto.QA_TYPE_QUESTION); // 设置问卷类型为问题类型
        questionAnswerPo.setState(QuestionAnswerDto.STATE_WAIT); // 设置问卷状态为等待状态
        
        // 更新问卷基本信息
        int flag = questionAnswerV1InnerServiceSMOImpl.updateQuestionAnswer(questionAnswerPo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 处理问卷题目关联关系
        JSONArray questionTitles = reqJson.getJSONArray("questionTitles");
        QuestionAnswerTitleRelPo questionAnswerTitleRelPo = new QuestionAnswerTitleRelPo();
        questionAnswerTitleRelPo.setQaId(reqJson.getString("qaId"));
        questionAnswerTitleRelPo.setCommunityId(reqJson.getString("communityId"));
        
        // 先删除原有的题目关联关系
        questionAnswerTitleRelV1InnerServiceSMOImpl.deleteQuestionAnswerTitleRel(questionAnswerTitleRelPo);
        
        // 重新创建题目关联关系
        JSONObject title = null;
        for (int titleIndex = 0; titleIndex < questionTitles.size(); titleIndex++) {
            title = questionTitles.getJSONObject(titleIndex);
            questionAnswerTitleRelPo = new QuestionAnswerTitleRelPo();
            questionAnswerTitleRelPo.setCommunityId(reqJson.getString("communityId"));
            questionAnswerTitleRelPo.setTitleId(title.getString("titleId"));
            questionAnswerTitleRelPo.setSeq((titleIndex + 1) + ""); // 设置题目顺序
            questionAnswerTitleRelPo.setScore("0"); // 默认分数为0
            
            // 如果题目包含分数信息，则设置分数
            if(title.containsKey("score")){
                questionAnswerTitleRelPo.setScore(title.getString("score"));
            }
            questionAnswerTitleRelPo.setQaId(questionAnswerPo.getQaId());
            questionAnswerTitleRelPo.setQatrId(GenerateCodeFactory.getGeneratorId("11")); // 生成关联关系ID
            questionAnswerTitleRelV1InnerServiceSMOImpl.saveQuestionAnswerTitleRel(questionAnswerTitleRelPo);
        }

        // 处理用户问卷答案关联关系
        JSONArray roomIds = reqJson.getJSONArray("roomIds");
        if(roomIds == null || roomIds.size() < 1){
            return ; // 如果没有房间ID，直接返回
        }
        
        // 删除原有的用户问卷答案关联关系
        UserQuestionAnswerPo userQuestionAnswerPo = new UserQuestionAnswerPo();
        userQuestionAnswerPo.setQaId(reqJson.getString("qaId"));
        userQuestionAnswerPo.setCommunityId(reqJson.getString("communityId"));
        userQuestionAnswerV1InnerServiceSMOImpl.deleteUserQuestionAnswer(userQuestionAnswerPo);

        // 保存新的用户问卷答案关联关系
        questionAnswerBMOImpl.saveUserQuestionAnswer(questionAnswerPo, reqJson.getJSONArray("roomIds"));

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}