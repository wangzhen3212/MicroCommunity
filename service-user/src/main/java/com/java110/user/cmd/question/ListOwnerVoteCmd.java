package com.java110.user.cmd.question;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.question.QuestionAnswerDto;
import com.java110.dto.question.QuestionTitleValueDto;
import com.java110.intf.user.IQuestionAnswerV1InnerServiceSMO;
import com.java110.intf.user.IQuestionTitleV1InnerServiceSMO;
import com.java110.intf.user.IQuestionTitleValueV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询业主投票命令类
 * 
 * 该类用于处理业主投票查询请求，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 查询投票数据并统计相关信息
 * 3. 计算投票选项的投票情况
 * 4. 返回分页格式的投票结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "question.listOwnerVote")
public class ListOwnerVoteCmd extends Cmd {

    /**
     * 问题答案服务接口，用于查询投票答案相关信息
     */
    @Autowired
    private IQuestionAnswerV1InnerServiceSMO questionAnswerV1InnerServiceSMOImpl;

    /**
     * 问题标题服务接口，用于查询投票标题信息
     */
    @Autowired
    private IQuestionTitleV1InnerServiceSMO questionTitleV1InnerServiceSMOImpl;

    /**
     * 问题标题选项服务接口，用于查询投票选项和投票人数统计
     */
    @Autowired
    private IQuestionTitleValueV1InnerServiceSMO questionTitleValueV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证传入的请求参数是否包含必要的小区信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行查询业主投票命令
     * 
     * 该方法主要功能：
     * 1. 查询投票数据总数和分页数据
     * 2. 计算投票选项的投票情况
     * 3. 构建分页响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为QuestionAnswerDto对象
        QuestionAnswerDto questionAnswerDto = BeanConvertUtil.covertBean(reqJson, QuestionAnswerDto.class);
        // 设置查询类型为投票类型
        questionAnswerDto.setQaType(QuestionAnswerDto.QA_TYPE_VOTE);
        
        // 查询符合条件的投票总数
        int count = questionAnswerV1InnerServiceSMOImpl.queryQuestionAnswersCount(questionAnswerDto);

        List<QuestionAnswerDto> questionAnswerDtos = null;

        if (count > 0) {
            // 如果有数据，查询分页的投票数据
            questionAnswerDtos = questionAnswerV1InnerServiceSMOImpl.queryQuestionAnswers(questionAnswerDto);
        } else {
            // 如果没有数据，返回空列表
            questionAnswerDtos = new ArrayList<>();
        }

        // 计算投票选项的投票情况，包括投票人数统计
        computeQuestionAnswerValue(questionAnswerDtos);

        // 构建分页响应结果，计算总页数
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, questionAnswerDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应数据到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算投票选项的投票情况
     * 
     * 该方法用于：
     * 1. 收集所有投票ID
     * 2. 查询每个投票的选项和投票人数
     * 3. 统计每个投票的总投票人数
     * 4. 设置投票选项和统计信息到对应的投票对象
     * 
     * @param questionAnswerDtos 投票答案数据列表，包含需要计算投票情况的投票数据
     */
    private void computeQuestionAnswerValue(List<QuestionAnswerDto> questionAnswerDtos) {
        // 如果投票数据为空或没有数据，直接返回
        if (questionAnswerDtos == null || questionAnswerDtos.size() < 1) {
            return;
        }

        // 收集所有投票ID，用于批量查询投票选项
        List<String> qaId = new ArrayList<>();
        for (QuestionAnswerDto tmpQuestionAnswerDto : questionAnswerDtos) {
            qaId.add(tmpQuestionAnswerDto.getQaId());
        }

        // 构建查询条件对象
        QuestionTitleValueDto questionTitleValueDto = new QuestionTitleValueDto();
        // 设置要查询的投票ID数组
        questionTitleValueDto.setQaIds(qaId.toArray(new String[qaId.size()]));
        // 设置小区ID
        questionTitleValueDto.setCommunityId(questionAnswerDtos.get(0).getCommunityId());
        
        // 查询投票选项和投票人数统计信息
        List<QuestionTitleValueDto> questionTitleValueDtos = questionTitleValueV1InnerServiceSMOImpl.queryQuestionOwnerTitleValues(questionTitleValueDto);
        
        // 如果没有投票选项数据，直接返回
        if (questionTitleValueDtos == null || questionTitleValueDtos.size() < 1) {
            return;
        }
        
        List<QuestionTitleValueDto> values = null;
        long votedCount = 0;
        
        // 遍历每个投票答案，设置对应的投票选项和统计信息
        for (QuestionAnswerDto tmpQuestionAnswerDto : questionAnswerDtos) {
            values = new ArrayList<>();
            votedCount = 0;
            
            // 遍历所有投票选项，找到属于当前投票的选项
            for (QuestionTitleValueDto tmpQuestionTitleValueDto : questionTitleValueDtos) {
                // 如果投票ID不匹配，跳过当前选项
                if (!tmpQuestionTitleValueDto.getQaId().equals(tmpQuestionAnswerDto.getQaId())) {
                    continue;
                }
                // 将匹配的选项添加到当前投票的选项列表中
                values.add(tmpQuestionTitleValueDto);
                // 设置标题类型
                tmpQuestionAnswerDto.setTitleType(tmpQuestionTitleValueDto.getTitleType());
                // 累加投票人数
                votedCount += tmpQuestionTitleValueDto.getPersonCount();
            }
            
            // 设置当前投票的选项列表
            tmpQuestionAnswerDto.setTitleValues(values);
            // 设置当前投票的总投票人数
            tmpQuestionAnswerDto.setVotedCount(votedCount);
        }
    }
}