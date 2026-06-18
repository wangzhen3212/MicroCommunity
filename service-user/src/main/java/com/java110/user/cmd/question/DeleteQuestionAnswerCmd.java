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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.question.QuestionAnswerTitleRelDto;
import com.java110.intf.user.*;
import com.java110.po.question.QuestionAnswerPo;
import com.java110.po.question.QuestionAnswerTitleRelPo;
import com.java110.po.question.QuestionTitlePo;
import com.java110.po.question.QuestionTitleValuePo;
import com.java110.po.user.UserQuestionAnswerPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：删除问题回答命令类
 * 服务编码：questionAnswer.deleteQuestionAnswer
 * 请求路劲：/app/questionAnswer.DeleteQuestionAnswer
 * 功能描述：该类负责处理删除问题回答及其相关数据的业务逻辑，包括删除问题回答、题目关联、题目信息、题目选项和用户投票记录
 * add by 吴学文 at 2023-07-13 00:03:56 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "question.deleteQuestionAnswer")
public class DeleteQuestionAnswerCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteQuestionAnswerCmd.class);

    /**
     * 问题回答服务接口
     */
    @Autowired
    private IQuestionAnswerV1InnerServiceSMO questionAnswerV1InnerServiceSMOImpl;

    /**
     * 问题回答与题目关联服务接口
     */
    @Autowired
    private IQuestionAnswerTitleRelV1InnerServiceSMO questionAnswerTitleRelV1InnerServiceSMOImpl;

    /**
     * 题目信息服务接口
     */
    @Autowired
    private IQuestionTitleV1InnerServiceSMO questionTitleV1InnerServiceSMOImpl;

    /**
     * 题目选项服务接口
     */
    @Autowired
    private IQuestionTitleValueV1InnerServiceSMO questionTitleValueV1InnerServiceSMOImpl;

    /**
     * 用户问题回答服务接口
     */
    @Autowired
    private IUserQuestionAnswerV1InnerServiceSMO userQuestionAnswerV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否包含必要的字段
     * @param event 命令事件对象，包含命令执行的相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含客户端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含qaId字段
        Assert.hasKeyAndValue(reqJson, "qaId", "qaId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除命令方法
     * 功能描述：执行删除问题回答及其相关数据的完整业务流程，包括删除问题回答、题目关联、题目信息、题目选项和用户投票记录
     * 该方法采用事务处理，确保所有删除操作要么全部成功，要么全部回滚
     * @param event 命令事件对象，包含命令执行的相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含客户端传递的参数
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为问题回答PO对象
        QuestionAnswerPo questionAnswerPo = BeanConvertUtil.covertBean(reqJson, QuestionAnswerPo.class);
        // 调用服务删除问题回答记录
        int flag = questionAnswerV1InnerServiceSMOImpl.deleteQuestionAnswer(questionAnswerPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 构建查询条件，查找问题回答与题目的关联关系
        QuestionAnswerTitleRelDto questionAnswerTitleRelDto = new QuestionAnswerTitleRelDto();
        questionAnswerTitleRelDto.setQaId(reqJson.getString("qaId"));
        questionAnswerTitleRelDto.setCommunityId(reqJson.getString("communityId"));
        // 查询问题回答与题目的关联记录
        List<QuestionAnswerTitleRelDto> questionAnswerTitleRelDtos = questionAnswerTitleRelV1InnerServiceSMOImpl.queryQuestionAnswerTitleRels(questionAnswerTitleRelDto);
        
        // 如果没有找到关联记录，直接返回成功
        if (questionAnswerTitleRelDtos == null || questionAnswerTitleRelDtos.size() < 1) {
            return;
        }

        // 删除问题回答与题目的关联记录
        QuestionAnswerTitleRelPo questionAnswerTitleRelPo = new QuestionAnswerTitleRelPo();
        questionAnswerTitleRelPo.setQatrId(questionAnswerTitleRelDtos.get(0).getQatrId());
        questionAnswerTitleRelPo.setCommunityId(reqJson.getString("communityId"));
        questionAnswerTitleRelV1InnerServiceSMOImpl.deleteQuestionAnswerTitleRel(questionAnswerTitleRelPo);

        // 删除题目信息记录
        QuestionTitlePo questionTitlePo = new QuestionTitlePo();
        questionTitlePo.setTitleId(questionAnswerTitleRelDtos.get(0).getTitleId());
        questionTitlePo.setCommunityId(reqJson.getString("communityId"));
        questionTitleV1InnerServiceSMOImpl.deleteQuestionTitle(questionTitlePo);

        // 删除题目选项记录
        QuestionTitleValuePo questionTitleValuePo = new QuestionTitleValuePo();
        questionTitleValuePo.setTitleId(questionAnswerTitleRelDtos.get(0).getTitleId());
        questionTitleValuePo.setCommunityId(reqJson.getString("communityId"));
        questionTitleValueV1InnerServiceSMOImpl.deleteQuestionTitleValue(questionTitleValuePo);

        // 删除用户投票记录
        UserQuestionAnswerPo userQuestionAnswerPo = new UserQuestionAnswerPo();
        userQuestionAnswerPo.setQaId(reqJson.getString("qaId"));
        userQuestionAnswerPo.setCommunityId(reqJson.getString("communityId"));
        userQuestionAnswerV1InnerServiceSMOImpl.deleteUserQuestionAnswer(userQuestionAnswerPo);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}