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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IQuestionAnswerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.question.QuestionAnswerDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询问题答案列表的命令类
 * 服务编码：questionAnswer.listQuestionAnswer
 * 请求路劲：/app/questionAnswer.ListQuestionAnswer
 * 功能描述：该类负责处理查询问题答案列表的请求，包括参数验证、数据查询、投票统计和结果返回
 * add by 吴学文 at 2023-07-13 00:03:56 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "question.listQuestionAnswer")
public class ListQuestionAnswerCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListQuestionAnswerCmd.class);

    /**
     * 问题答案服务接口
     */
    @Autowired
    private IQuestionAnswerV1InnerServiceSMO questionAnswerV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，包括分页参数和必填参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证社区ID参数是否为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 功能描述：执行查询问题答案列表的主要业务逻辑，包括数据查询、投票统计和结果封装
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为QuestionAnswerDto对象
        QuestionAnswerDto questionAnswerDto = BeanConvertUtil.covertBean(reqJson, QuestionAnswerDto.class);

        // 查询问题答案总数
        int count = questionAnswerV1InnerServiceSMOImpl.queryQuestionAnswersCount(questionAnswerDto);

        List<QuestionAnswerDto> questionAnswerDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询问题答案列表数据
            questionAnswerDtos = questionAnswerV1InnerServiceSMOImpl.queryQuestionAnswers(questionAnswerDto);
        } else {
            // 如果没有数据，返回空列表
            questionAnswerDtos = new ArrayList<>();
        }

        // 计算投票数量和得分
        computeVotedCountAndScore(questionAnswerDtos);

        // 构造返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, questionAnswerDtos);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 计算投票数量和得分
     * 功能描述：为问题答案列表中的每个答案计算投票数量和得分
     * @param questionAnswerDtos 问题答案数据列表
     */
    private void computeVotedCountAndScore(List<QuestionAnswerDto> questionAnswerDtos) {
        // 检查参数有效性
        if (questionAnswerDtos == null || questionAnswerDtos.size() < 1) {
            return;
        }
        
        // 收集所有问题答案ID
        List<String> qaIds = new ArrayList<>();
        for (QuestionAnswerDto questionAnswerDto : questionAnswerDtos) {
            qaIds.add(questionAnswerDto.getQaId());
        }
        
        // 查询投票人数和得分数据
        List<QuestionAnswerDto> votedQAs = questionAnswerV1InnerServiceSMOImpl.queryVotedCountAndScore(qaIds.toArray(new String[qaIds.size()]));
        
        // 检查查询结果
        if (votedQAs == null || votedQAs.size() < 1) {
            return;
        }
        
        // 将投票统计数据合并到原始数据中
        for (QuestionAnswerDto questionAnswerDto : questionAnswerDtos) {
            for (QuestionAnswerDto votedQa : votedQAs) {
                // 匹配相同的问题答案ID
                if (!questionAnswerDto.getQaId().equals(votedQa.getQaId())) {
                    continue;
                }
                // 设置投票数量
                questionAnswerDto.setVotedCount(votedQa.getVotedCount());
                // 设置得分
                questionAnswerDto.setScore(votedQa.getScore());
            }
        }
    }
}