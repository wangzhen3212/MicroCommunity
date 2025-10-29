package com.java110.user.cmd.question;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.question.QuestionAnswerDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.user.UserQuestionAnswerDto;
import com.java110.intf.user.IUserQuestionAnswerV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 查询用户投票或问卷答案的命令类
 * 
 * 该命令类负责处理用户查询自己参与的投票或问卷答案的请求，
 * 通过用户ID获取用户信息，然后查询对应的问卷或投票答案数据
 * 
 * @serviceCode question.queryOwnerQuestionAnswer
 */
@Java110Cmd(serviceCode = "question.queryOwnerQuestionAnswer")
public class QueryOwnerQuestionAnswerCmd extends Cmd {

    /**
     * 用户服务接口，用于查询用户信息
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 用户问卷答案服务接口，用于查询用户的问卷或投票答案
     */
    @Autowired
    private IUserQuestionAnswerV1InnerServiceSMO userQuestionAnswerV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的合法性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头等信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
        // 验证qaType参数的代码被注释，表示该参数不是必须的
        //Assert.hasKeyAndValue(reqJson, "qaType", "未包含qaType");
        
        // 从请求头中获取用户ID，并验证用户是否已登录
        String userId = context.getReqHeaders().get("user-id");
        Assert.hasLength(userId, "用户未登录");
    }

    /**
     * 执行查询用户问卷或投票答案的核心业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取当前用户ID
        String userId = context.getReqHeaders().get("user-id");
        
        // 创建用户查询对象，设置用户ID
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        
        // 调用用户服务查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 验证查询结果必须且只能有一条用户记录
        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 创建用户问卷答案查询对象
        UserQuestionAnswerDto userQuestionAnswerDto = new UserQuestionAnswerDto();
        // 设置查询条件：使用用户手机号作为关联标识
        userQuestionAnswerDto.setLink(userDtos.get(0).getTel());
        // 设置查询条件：小区ID
        userQuestionAnswerDto.setCommunityId(reqJson.getString("communityId"));
        // 设置查询条件：问卷或投票类型（可选参数）
        userQuestionAnswerDto.setQaType(reqJson.getString("qaType"));
        // 设置查询条件：状态（可选参数）
        userQuestionAnswerDto.setState(reqJson.getString("state"));
        // 设置查询条件：用户问卷ID（可选参数）
        userQuestionAnswerDto.setUserQaId(reqJson.getString("userQaId"));
        
        // 调用服务查询用户的问卷或投票答案列表
        List<QuestionAnswerDto> questionAnswerDtos = userQuestionAnswerV1InnerServiceSMOImpl.queryOwnerQuestionAnswers(userQuestionAnswerDto);
        
        // 将查询结果设置到响应中返回给前端
        context.setResponseEntity(ResultVo.createResponseEntity(questionAnswerDtos));
    }
}