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
import com.java110.dto.question.QuestionTitleDto;
import com.java110.intf.user.IQuestionAnswerTitleRelV1InnerServiceSMO;
import com.java110.intf.user.IQuestionAnswerV1InnerServiceSMO;
import com.java110.intf.user.IQuestionTitleV1InnerServiceSMO;
import com.java110.intf.user.IQuestionTitleValueV1InnerServiceSMO;
import com.java110.po.question.QuestionAnswerPo;
import com.java110.po.question.QuestionAnswerTitleRelPo;
import com.java110.po.question.QuestionTitlePo;
import com.java110.po.question.QuestionTitleValuePo;
import com.java110.user.bmo.question.IQuestionAnswerBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 保存业主投票功能命令类
 * 负责处理业主投票的创建和保存，包括题目信息、选项信息、投票信息以及关联关系的存储
 * 
 * @author wuxw
 * @version 1.0
 * @since 2023-07-12
 */
@Java110Cmd(serviceCode = "question.saveOwnerVote")
public class SaveOwnerVoteCmd extends Cmd {

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 题目服务接口
     */
    @Autowired
    private IQuestionTitleV1InnerServiceSMO questionTitleV1InnerServiceSMOImpl;

    /**
     * 题目选项服务接口
     */
    @Autowired
    private IQuestionTitleValueV1InnerServiceSMO questionTitleValueV1InnerServiceSMOImpl;
    
    /**
     * 投票答案服务接口
     */
    @Autowired
    private IQuestionAnswerV1InnerServiceSMO questionAnswerV1InnerServiceSMOImpl;

    /**
     * 投票答案与题目关联服务接口
     */
    @Autowired
    private IQuestionAnswerTitleRelV1InnerServiceSMO questionAnswerTitleRelV1InnerServiceSMOImpl;

    /**
     * 投票答案业务处理接口
     */
    @Autowired
    private IQuestionAnswerBMO questionAnswerBMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求JSON对象，包含前端传递的参数
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "qaName", "请求报文中未包含投票名称");
        Assert.hasKey(reqJson, "roomIds", "请求报文中未包含投票房屋");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含结束时间");
        Assert.hasKeyAndValue(reqJson, "titleType", "未包含题目类型");
        Assert.hasKeyAndValue(reqJson, "content", "未包含说明");
        
        // 如果不是问答类型的题目，需要验证选项信息
        JSONArray titleValues = null;
        if (!QuestionTitleDto.TITLE_TYPE_QUESTIONS.equals(reqJson.getString("titleType"))) {
            titleValues = reqJson.getJSONArray("titleValues");
            if (titleValues.size() < 1) {
                throw new IllegalArgumentException("未包含选项");
            }
        }
    }

    /**
     * 执行保存业主投票命令
     * 处理投票数据的完整保存流程，包括题目、选项、投票信息和关联关系
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当命令执行失败时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 创建并保存题目信息
        QuestionTitlePo questionTitlePo = new QuestionTitlePo();
        questionTitlePo.setTitleId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        questionTitlePo.setCommunityId(reqJson.getString("communityId"));
        questionTitlePo.setQaTitle(reqJson.getString("qaName"));
        questionTitlePo.setTitleType(reqJson.getString("titleType"));
        int flag = questionTitleV1InnerServiceSMOImpl.saveQuestionTitle(questionTitlePo);

        // 检查题目保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 保存题目选项信息
        JSONArray titleValues = reqJson.getJSONArray("titleValues");
        QuestionTitleValuePo questionTitleValuePo = null;
        JSONObject valueObj = null;
        for (int titleValueIndex = 0; titleValueIndex < titleValues.size(); titleValueIndex++) {
            valueObj = titleValues.getJSONObject(titleValueIndex);
            questionTitleValuePo = new QuestionTitleValuePo();
            questionTitleValuePo.setQaValue(valueObj.getString("itemValue")); // 设置选项值
            questionTitleValuePo.setSeq((titleValueIndex + 1) + ""); // 设置选项顺序
            if (valueObj.containsKey("score")) {
                questionTitleValuePo.setScore(valueObj.getString("score")); // 设置选项分数
            } else {
                questionTitleValuePo.setScore("0"); // 默认分数为0
            }
            questionTitleValuePo.setTitleId(questionTitlePo.getTitleId()); // 关联题目ID
            questionTitleValuePo.setValueId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_valueId));
            questionTitleValuePo.setCommunityId(questionTitlePo.getCommunityId());
            questionTitleValueV1InnerServiceSMOImpl.saveQuestionTitleValue(questionTitleValuePo);
        }

        // 创建并保存投票信息
        QuestionAnswerPo questionAnswerPo = new QuestionAnswerPo();
        questionAnswerPo.setContent(reqJson.getString("content")); // 投票说明内容
        questionAnswerPo.setEndTime(reqJson.getString("endTime")); // 投票结束时间
        questionAnswerPo.setStartTime(reqJson.getString("startTime")); // 投票开始时间
        questionAnswerPo.setQaId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID)); // 生成投票ID
        questionAnswerPo.setQaName(reqJson.getString("qaName")); // 投票名称
        questionAnswerPo.setCommunityId(reqJson.getString("communityId")); // 小区ID
        questionAnswerPo.setQaType(QuestionAnswerDto.QA_TYPE_VOTE); // 设置投票类型
        questionAnswerPo.setState(QuestionAnswerDto.STATE_WAIT); // 设置投票状态为等待
        
        // 设置投票对象类型和ID
        if (!StringUtil.isEmpty(reqJson.getString("communityId"))) {
            questionAnswerPo.setObjType("3306"); // 3306 是小区，3307 是商户
            questionAnswerPo.setObjId(reqJson.getString("communityId"));
        } else if (!StringUtil.isEmpty(reqJson.getString("storeId"))) {
            questionAnswerPo.setObjType("3307"); // 3306 是小区，3307 是商户
            questionAnswerPo.setObjId(reqJson.getString("storeId"));
        }
        questionAnswerV1InnerServiceSMOImpl.saveQuestionAnswer(questionAnswerPo);

        // 创建并保存投票与题目的关联关系
        QuestionAnswerTitleRelPo questionAnswerTitleRelPo = new QuestionAnswerTitleRelPo();
        questionAnswerTitleRelPo.setCommunityId(reqJson.getString("communityId"));
        questionAnswerTitleRelPo.setTitleId(questionTitleValuePo.getTitleId()); // 关联题目ID
        questionAnswerTitleRelPo.setSeq("1"); // 设置顺序
        questionAnswerTitleRelPo.setScore("0"); // 设置分数
        questionAnswerTitleRelPo.setQaId(questionAnswerPo.getQaId()); // 关联投票ID
        questionAnswerTitleRelPo.setQatrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID)); // 生成关联ID
        questionAnswerTitleRelV1InnerServiceSMOImpl.saveQuestionAnswerTitleRel(questionAnswerTitleRelPo);

        // 保存用户投票答案信息
        questionAnswerBMOImpl.saveUserQuestionAnswer(questionAnswerPo, reqJson.getJSONArray("roomIds"));
    }
}