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
import com.java110.dto.question.QuestionTitleValueDto;
import com.java110.intf.user.IQuestionTitleV1InnerServiceSMO;
import com.java110.intf.user.IQuestionTitleValueV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.question.QuestionTitleDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询问题标题列表的命令类
 * 服务编码：questionTitle.listQuestionTitle
 * 请求路劲：/app/questionTitle.ListQuestionTitle
 * 功能描述：该类负责处理问题标题列表查询请求，包括参数验证、数据查询和结果返回
 * add by 吴学文 at 2023-07-10 15:10:47 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "question.listQuestionTitle")
public class ListQuestionTitleCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListQuestionTitleCmd.class);

    @Autowired
    private IQuestionTitleV1InnerServiceSMO questionTitleV1InnerServiceSMOImpl;

    @Autowired
    private IQuestionTitleValueV1InnerServiceSMO questionTitleValueV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     *
     * @param event             命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson           请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证必填参数communityId是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行查询问题标题列表的主要业务逻辑
     *
     * @param event             命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson           请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为QuestionTitleDto对象
        QuestionTitleDto questionTitleDto = BeanConvertUtil.covertBean(reqJson, QuestionTitleDto.class);

        // 查询问题标题总数
        int count = questionTitleV1InnerServiceSMOImpl.queryQuestionTitlesCount(questionTitleDto);

        List<QuestionTitleDto> questionTitleDtos = null;

        if (count > 0) {
            // 如果存在数据，则查询问题标题列表
            questionTitleDtos = questionTitleV1InnerServiceSMOImpl.queryQuestionTitles(questionTitleDto);
            // 刷新问题标题对应的选项值
            refreshTitileValues(questionTitleDtos);
        } else {
            // 如果没有数据，返回空列表
            questionTitleDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, questionTitleDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新问题标题对应的选项值列表
     * 为每个问题标题设置其对应的选项值集合
     *
     * @param questionTitleDtos 问题标题数据列表
     */
    private void refreshTitileValues(List<QuestionTitleDto> questionTitleDtos) {
        // 如果问题标题列表为空或没有元素，直接返回
        if (questionTitleDtos == null || questionTitleDtos.size() < 1) {
            return;
        }

        // 收集所有问题标题的ID
        List<String> itemIds = new ArrayList<>();
        for (QuestionTitleDto questionTitle : questionTitleDtos) {
            itemIds.add(questionTitle.getTitleId());
        }

        // 构建查询条件对象
        QuestionTitleValueDto questionTitleValueDto = new QuestionTitleValueDto();
        // 设置要查询的问题标题ID数组
        questionTitleValueDto.setTitleIds(itemIds.toArray(new String[itemIds.size()]));
        // 设置小区ID（从第一个问题标题中获取）
        questionTitleValueDto.setCommunityId(questionTitleDtos.get(0).getCommunityId());
        // 批量查询所有问题标题的选项值
        List<QuestionTitleValueDto> questionTitleValues
                = questionTitleValueV1InnerServiceSMOImpl.queryQuestionTitleValues(questionTitleValueDto);

        List<QuestionTitleValueDto> tmpQuestionTitleValueDtos = null;
        // 遍历每个问题标题，为其设置对应的选项值
        for (QuestionTitleDto questionTitle : questionTitleDtos) {
            tmpQuestionTitleValueDtos = new ArrayList<>();
            // 从批量查询结果中筛选出当前问题标题的选项值
            for (QuestionTitleValueDto tmpQuestionTitleValueDto : questionTitleValues) {
                if (tmpQuestionTitleValueDto.getTitleId().equals(questionTitle.getTitleId())) {
                    tmpQuestionTitleValueDtos.add(tmpQuestionTitleValueDto);
                }
            }
            // 将筛选出的选项值设置到问题标题对象中
            questionTitle.setTitleValues(tmpQuestionTitleValueDtos);
        }
    }
}