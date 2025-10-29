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
import com.java110.dto.question.QuestionAnswerTitleRelDto;
import com.java110.dto.question.QuestionTitleDto;
import com.java110.intf.user.*;
import com.java110.po.question.QuestionAnswerPo;
import com.java110.po.question.QuestionTitlePo;
import com.java110.po.question.QuestionTitleValuePo;
import com.java110.po.user.UserQuestionAnswerPo;
import com.java110.user.bmo.question.IQuestionAnswerBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 修改业主投票功能命令类
 * 负责处理业主投票信息的更新操作，包括投票基本信息、题目信息、选项信息和参与用户信息的更新
 * 
 * @author wuxw
 * @version 1.0
 * @since 2023-07-12
 */
@Java110Cmd(serviceCode = "question.updateOwnerVote")
public class UpdateOwnerVoteCmd extends Cmd {

    /** ID编码前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 题目服务接口 */
    @Autowired
    private IQuestionTitleV1InnerServiceSMO questionTitleV1InnerServiceSMOImpl;

    /** 题目选项服务接口 */
    @Autowired
    private IQuestionTitleValueV1InnerServiceSMO questionTitleValueV1InnerServiceSMOImpl;
    
    /** 投票服务接口 */
    @Autowired
    private IQuestionAnswerV1InnerServiceSMO questionAnswerV1InnerServiceSMOImpl;

    /** 投票题目关联服务接口 */
    @Autowired
    private IQuestionAnswerTitleRelV1InnerServiceSMO questionAnswerTitleRelV1InnerServiceSMOImpl;

    /** 用户投票答案服务接口 */
    @Autowired
    private IUserQuestionAnswerV1InnerServiceSMO userQuestionAnswerV1InnerServiceSMOImpl;

    /** 投票业务操作接口 */
    @Autowired
    private IQuestionAnswerBMO questionAnswerBMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查必要的参数是否存在，并对题目选项进行验证
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON对象，包含所有请求参数
     * @throws CmdException 当命令执行过程中出现错误时抛出
     * @throws ParseException 当解析数据时出现错误时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "qaName", "请求报文中未包含投票名称");
        Assert.hasKeyAndValue(reqJson, "qaId", "请求报文中未包含投票名称");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含结束时间");
        Assert.hasKeyAndValue(reqJson, "titleType", "未包含题目类型");
        Assert.hasKeyAndValue(reqJson, "content", "未包含说明");
        
        // 如果不是问答题类型，需要验证题目选项
        JSONArray titleValues = null;
        if (!QuestionTitleDto.TITLE_TYPE_QUESTIONS.equals(reqJson.getString("titleType"))) {
            titleValues = reqJson.getJSONArray("titleValues");
            if (titleValues.size() < 1) {
                throw new IllegalArgumentException("未包含选项");
            }
        }
    }

    /**
     * 执行修改业主投票命令
     * 更新投票信息、题目信息、选项信息，并重新设置参与用户
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON对象，包含所有请求参数
     * @throws CmdException 当命令执行过程中出现错误时抛出
     * @throws ParseException 当解析数据时出现错误时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 查询投票题目关联信息
        QuestionAnswerTitleRelDto questionAnswerTitleRelDto = new QuestionAnswerTitleRelDto();
        questionAnswerTitleRelDto.setQaId(reqJson.getString("qaId"));
        questionAnswerTitleRelDto.setCommunityId(reqJson.getString("communityId"));
        List<QuestionAnswerTitleRelDto> questionAnswerTitleRelDtos = questionAnswerTitleRelV1InnerServiceSMOImpl.queryQuestionAnswerTitleRels(questionAnswerTitleRelDto);
        
        // 验证投票题目关联信息唯一性
        Assert.listOnlyOne(questionAnswerTitleRelDtos, "投票没有题目");

        // 更新题目信息
        QuestionTitlePo questionTitlePo = new QuestionTitlePo();
        questionTitlePo.setTitleId(questionAnswerTitleRelDtos.get(0).getTitleId());
        questionTitlePo.setCommunityId(reqJson.getString("communityId"));
        questionTitlePo.setQaTitle(reqJson.getString("qaName"));
        questionTitlePo.setTitleType(reqJson.getString("titleType"));
        int flag = questionTitleV1InnerServiceSMOImpl.updateQuestionTitle(questionTitlePo);

        // 检查题目更新是否成功
        if (flag < 1) {
            throw new CmdException("修改数据失败");
        }

        // 删除原有的题目选项
        QuestionTitleValuePo questionTitleValuePo = new QuestionTitleValuePo();
        questionTitleValuePo.setTitleId(questionAnswerTitleRelDtos.get(0).getTitleId());
        questionTitleValuePo.setCommunityId(reqJson.getString("communityId"));
        questionTitleValueV1InnerServiceSMOImpl.deleteQuestionTitleValue(questionTitleValuePo);
        
        // 重新添加新的题目选项
        JSONArray titleValues = reqJson.getJSONArray("titleValues");
        JSONObject valueObj = null;
        for (int titleValueIndex = 0; titleValueIndex < titleValues.size(); titleValueIndex++) {
            valueObj = titleValues.getJSONObject(titleValueIndex);
            questionTitleValuePo = new QuestionTitleValuePo();
            questionTitleValuePo.setQaValue(valueObj.getString("qaValue"));
            questionTitleValuePo.setSeq((titleValueIndex + 1) + ""); // 设置选项顺序
            // 设置选项分数，默认为0
            if (valueObj.containsKey("score")) {
                questionTitleValuePo.setScore(valueObj.getString("score"));
            } else {
                questionTitleValuePo.setScore("0");
            }
            questionTitleValuePo.setTitleId(questionTitlePo.getTitleId());
            questionTitleValuePo.setValueId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_valueId));
            questionTitleValuePo.setCommunityId(questionTitlePo.getCommunityId());
            questionTitleValueV1InnerServiceSMOImpl.saveQuestionTitleValue(questionTitleValuePo);
        }

        // 更新投票基本信息
        QuestionAnswerPo questionAnswerPo = new QuestionAnswerPo();
        questionAnswerPo.setContent(reqJson.getString("content"));
        questionAnswerPo.setEndTime(reqJson.getString("endTime"));
        questionAnswerPo.setStartTime(reqJson.getString("startTime"));
        questionAnswerPo.setQaId(reqJson.getString("qaId"));
        questionAnswerPo.setQaName(reqJson.getString("qaName"));
        questionAnswerPo.setCommunityId(reqJson.getString("communityId"));
        questionAnswerPo.setQaType(QuestionAnswerDto.QA_TYPE_VOTE); // 设置投票类型
        questionAnswerV1InnerServiceSMOImpl.updateQuestionAnswer(questionAnswerPo);

        // 处理参与投票的用户信息
        JSONArray roomIds = reqJson.getJSONArray("roomIds");
        if(roomIds == null || roomIds.size() < 1){
            return ; // 如果没有指定参与用户，直接返回
        }
        
        // 删除原有的用户投票记录
        UserQuestionAnswerPo userQuestionAnswerPo = new UserQuestionAnswerPo();
        userQuestionAnswerPo.setQaId(reqJson.getString("qaId"));
        userQuestionAnswerPo.setCommunityId(reqJson.getString("communityId"));
        userQuestionAnswerV1InnerServiceSMOImpl.deleteUserQuestionAnswer(userQuestionAnswerPo);

        // 保存新的用户投票记录
        questionAnswerBMOImpl.saveUserQuestionAnswer(questionAnswerPo,roomIds);
    }
}