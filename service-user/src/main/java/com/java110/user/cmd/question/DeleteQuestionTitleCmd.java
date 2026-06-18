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
 * 类表述：删除问题标题命令类
 * 服务编码：questionTitle.deleteQuestionTitle
 * 请求路劲：/app/questionTitle.DeleteQuestionTitle
 * 功能描述：该命令类用于处理删除问题标题及其相关选项值的业务逻辑，支持事务处理
 * add by 吴学文 at 2023-07-10 15:10:47 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "question.deleteQuestionTitle")
public class DeleteQuestionTitleCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteQuestionTitleCmd.class);

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
     * 功能描述：验证请求参数是否包含必需的字段
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证titleId参数不能为空
        Assert.hasKeyAndValue(reqJson, "titleId", "titleId不能为空");
        // 验证communityId参数不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 功能描述：执行删除问题标题及其相关选项值的业务逻辑，支持事务处理
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为问题标题PO对象
        QuestionTitlePo questionTitlePo = BeanConvertUtil.covertBean(reqJson, QuestionTitlePo.class);
        // 调用服务删除问题标题记录
        int flag = questionTitleV1InnerServiceSMOImpl.deleteQuestionTitle(questionTitlePo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 如果问题标题类型是普通问题类型，直接返回成功响应
        if (QuestionTitleDto.TITLE_TYPE_QUESTIONS.equals(questionTitlePo.getTitleType())) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 对于选择题类型的问题标题，需要同时删除相关的选项值
        QuestionTitleValueDto questionTitleValueDto = new QuestionTitleValueDto();
        questionTitleValueDto.setTitleId(reqJson.getString("titleId"));
        // 查询该问题标题下的所有选项值
        List<QuestionTitleValueDto> questionTitleValues = questionTitleValueV1InnerServiceSMOImpl.queryQuestionTitleValues(questionTitleValueDto);
        
        // 如果存在选项值，则删除所有相关选项值
        if (questionTitleValues != null && questionTitleValues.size() > 0) {
            QuestionTitleValuePo deleteQuestionTitleValuePo = new QuestionTitleValuePo();
            deleteQuestionTitleValuePo.setTitleId(questionTitlePo.getTitleId());
            // 删除问题标题的所有选项值
            flag = questionTitleValueV1InnerServiceSMOImpl.deleteQuestionTitleValue(deleteQuestionTitleValuePo);
            // 检查选项值删除操作是否成功
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}