/**
 * 业主问卷答案保存命令类
 * 
 * 该类负责处理业主问卷答案的保存操作，包括参数验证和业务逻辑处理。
 * 主要功能包括验证用户提交的问卷答案数据，保存答案值到数据库，并更新问卷状态。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
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
import com.java110.dto.user.UserDto;
import com.java110.dto.user.UserQuestionAnswerDto;
import com.java110.intf.user.IUserQuestionAnswerV1InnerServiceSMO;
import com.java110.intf.user.IUserQuestionAnswerValueV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.user.UserQuestionAnswerPo;
import com.java110.po.user.UserQuestionAnswerValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "question.saveOwnerQuestionAnswer")
public class SaveOwnerQuestionAnswerCmd extends Cmd {

    /**
     * 用户问卷答案服务接口
     */
    @Autowired
    private IUserQuestionAnswerV1InnerServiceSMO userQuestionAnswerV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 用户问卷答案值服务接口
     */
    @Autowired
    private IUserQuestionAnswerValueV1InnerServiceSMO userQuestionAnswerValueV1InnerServiceSMOImpl;

    /**
     * 验证方法 - 验证请求参数的合法性
     * 
     * 该方法主要验证以下内容：
     * 1. 必填参数是否存在
     * 2. 答案数据是否完整
     * 3. 用户登录状态
     * 4. 用户是否存在
     * 5. 问卷数据是否存在
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数：小区ID和问卷ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "userQaId", "未包含题目");

        // 验证答案数据是否存在
        Assert.hasKey(reqJson, "questionAnswerTitles", "未包含答案");

        JSONArray questionAnswerTitles = reqJson.getJSONArray("questionAnswerTitles");

        // 验证答案数组不为空
        if (questionAnswerTitles == null || questionAnswerTitles.size() < 1) {
            throw new IllegalArgumentException("未包含答案");
        }

        JSONObject titleObj = null;
        // 遍历所有答案项，验证每个答案的完整性
        for (int questionAnswerTitleIndex = 0; questionAnswerTitleIndex < questionAnswerTitles.size(); questionAnswerTitleIndex++) {
            titleObj = questionAnswerTitles.getJSONObject(questionAnswerTitleIndex);
            // 如果有题目描述，则显示具体题目的错误信息
            if (titleObj.containsKey("qaTitle") && !StringUtil.isEmpty(titleObj.getString("qaTitle"))) {
                Assert.hasKeyAndValue(titleObj, "valueContent", titleObj.getString("qaTitle") + ",未填写答案");
            } else {
                Assert.hasKeyAndValue(titleObj, "valueContent", "未填写答案");
            }
        }

        // 验证用户登录状态
        String userId = context.getReqHeaders().get("user-id");
        Assert.hasLength(userId, "用户未登录");

        // 验证用户是否存在
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");

        // 验证问卷数据是否存在
        UserQuestionAnswerDto userQuestionAnswerDto = new UserQuestionAnswerDto();
        userQuestionAnswerDto.setUserQaId(reqJson.getString("userQaId"));
        userQuestionAnswerDto.setCommunityId(reqJson.getString("communityId"));
        userQuestionAnswerDto.setLink(userDtos.get(0).getTel()); // 使用用户电话作为关联标识
        int count = userQuestionAnswerV1InnerServiceSMOImpl.queryUserQuestionAnswersCount(userQuestionAnswerDto);
        if (count < 1) {
            throw new CmdException("数据不存在");
        }
    }

    /**
     * 执行命令方法 - 保存业主问卷答案
     * 
     * 该方法主要执行以下操作：
     * 1. 查询问卷信息
     * 2. 构建答案值对象列表
     * 3. 保存答案值到数据库
     * 4. 更新问卷状态为已完成
     * 5. 返回操作结果
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 查询问卷信息
        UserQuestionAnswerDto userQuestionAnswerDto = new UserQuestionAnswerDto();
        userQuestionAnswerDto.setUserQaId(reqJson.getString("userQaId"));
        userQuestionAnswerDto.setCommunityId(reqJson.getString("communityId"));
        List<UserQuestionAnswerDto> userQuestionAnswers = userQuestionAnswerV1InnerServiceSMOImpl.queryUserQuestionAnswers(userQuestionAnswerDto);

        // 验证问卷数据存在且唯一
        Assert.listOnlyOne(userQuestionAnswers, "数据不存在");

        // 获取答案数据数组
        JSONArray questionAnswerTitles = reqJson.getJSONArray("questionAnswerTitles");
        JSONObject titleObj = null;
        UserQuestionAnswerValuePo tmpUserUserQuestionAnswerValue = null;
        List<UserQuestionAnswerValuePo> tmpUserUserQuestionAnswerValues = new ArrayList<>();

        // 遍历所有答案项，构建答案值对象
        for (int questionAnswerTitleIndex = 0; questionAnswerTitleIndex < questionAnswerTitles.size(); questionAnswerTitleIndex++) {
            titleObj = questionAnswerTitles.getJSONObject(questionAnswerTitleIndex);
            tmpUserUserQuestionAnswerValue = new UserQuestionAnswerValuePo();
            
            // 设置答案值对象的基本属性
            tmpUserUserQuestionAnswerValue.setCommunityId(userQuestionAnswers.get(0).getCommunityId());
            tmpUserUserQuestionAnswerValue.setQaId(userQuestionAnswers.get(0).getQaId());
            tmpUserUserQuestionAnswerValue.setTitleId(titleObj.getString("titleId"));
            tmpUserUserQuestionAnswerValue.setUserQaId(userQuestionAnswers.get(0).getUserQaId());
            tmpUserUserQuestionAnswerValue.setUserTitleId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_userTitleId));
            
            // 根据题目类型设置答案值
            if ("3003".equals(titleObj.getString("titleType"))) {
                // 对于特定类型的题目（如文本输入题），设置特殊的值ID和内容
                tmpUserUserQuestionAnswerValue.setValueId("999");
                tmpUserUserQuestionAnswerValue.setValueContent(titleObj.getString("valueContent"));
            } else {
                // 对于其他类型的题目，值ID和内容相同
                tmpUserUserQuestionAnswerValue.setValueId(titleObj.getString("valueContent"));
                tmpUserUserQuestionAnswerValue.setValueContent(titleObj.getString("valueContent"));
            }
            tmpUserUserQuestionAnswerValues.add(tmpUserUserQuestionAnswerValue);
        }

        // 批量保存答案值到数据库
        int flag = userQuestionAnswerValueV1InnerServiceSMOImpl.saveUserQuestionAnswerValues(tmpUserUserQuestionAnswerValues);
        if (flag < 0) {
            throw new IllegalArgumentException("保存失败");
        }

        // 更新问卷状态为已完成（1202）
        UserQuestionAnswerPo userQuestionAnswerPo = new UserQuestionAnswerPo();
        userQuestionAnswerPo.setUserQaId(userQuestionAnswers.get(0).getUserQaId());
        userQuestionAnswerPo.setState("1202"); // 设置状态为已完成
        flag = userQuestionAnswerV1InnerServiceSMOImpl.updateUserQuestionAnswer(userQuestionAnswerPo);
        if (flag < 0) {
            throw new IllegalArgumentException("保存失败");
        }
        
        // 返回成功响应
        context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功"));
    }
}