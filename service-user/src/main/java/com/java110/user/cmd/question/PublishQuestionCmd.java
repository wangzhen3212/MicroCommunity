package com.java110.user.cmd.question;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.question.QuestionAnswerDto;
import com.java110.intf.user.IQuestionAnswerV1InnerServiceSMO;
import com.java110.po.question.QuestionAnswerPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 发布投票和问卷命令类
 * 
 * 该类负责处理发布投票和问卷的业务逻辑，通过更新问题答案状态为完成状态来实现发布功能
 * 继承自Cmd基类，使用@Java110Cmd注解标识服务代码
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "question.publishQuestion")
public class PublishQuestionCmd extends Cmd {

    /**
     * 问题答案服务接口，用于操作问题答案数据
     */
    @Autowired
    private IQuestionAnswerV1InnerServiceSMO questionAnswerV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段信息
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
        // 验证请求参数中必须包含问题答案ID
        Assert.hasKeyAndValue(reqJson, "qaId", "未包含qaId");
    }

    /**
     * 执行命令方法
     * 
     * 主要业务逻辑实现，更新问题答案状态为完成状态，完成发布操作
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数JSON对象，包含前端传递的参数
     * @throws CmdException 当发布操作失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建问题答案PO对象，用于更新操作
        QuestionAnswerPo questionAnswerPo = new QuestionAnswerPo();
        // 设置问题答案ID，从请求参数中获取
        questionAnswerPo.setQaId(reqJson.getString("qaId"));
        // 设置小区ID，从请求参数中获取
        questionAnswerPo.setCommunityId(reqJson.getString("communityId"));
        // 设置状态为完成状态，表示已发布
        questionAnswerPo.setState(QuestionAnswerDto.STATE_COMPLETE);
        
        // 调用服务接口更新问题答案状态
        int flag = questionAnswerV1InnerServiceSMOImpl.updateQuestionAnswer(questionAnswerPo);
        
        // 检查更新操作是否成功，如果影响行数小于1表示更新失败
        if (flag < 1) {
            throw new CmdException("发布失败");
        }

        //todo 通知信息推送模块 - 后续需要实现发布后的通知功能
    }
}