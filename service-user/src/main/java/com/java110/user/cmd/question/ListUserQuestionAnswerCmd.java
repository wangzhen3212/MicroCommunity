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
import com.java110.dto.user.UserQuestionAnswerValueDto;
import com.java110.intf.user.IUserQuestionAnswerV1InnerServiceSMO;
import com.java110.intf.user.IUserQuestionAnswerValueV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.user.UserQuestionAnswerDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：userQuestionAnswer.listUserQuestionAnswer
 * 请求路劲：/app/userQuestionAnswer.ListUserQuestionAnswer
 * add by 吴学文 at 2023-07-13 00:38:00 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 用户问题答案查询命令类
 * 负责处理用户问题答案列表查询请求，包括查询用户问题答案基本信息及其对应的答案值信息
 * 通过服务编码"question.listUserQuestionAnswer"标识，用于前端调用
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-07-13
 */
@Java110Cmd(serviceCode = "question.listUserQuestionAnswer")
public class ListUserQuestionAnswerCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListUserQuestionAnswerCmd.class);
    
    /**
     * 用户问题答案服务接口，用于查询用户问题答案基本信息
     */
    @Autowired
    private IUserQuestionAnswerV1InnerServiceSMO userQuestionAnswerV1InnerServiceSMOImpl;

    /**
     * 用户问题答案值服务接口，用于查询用户问题答案的具体值信息
     */
    @Autowired
    private IUserQuestionAnswerValueV1InnerServiceSMO userQuestionAnswerValueV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的小区ID和问题ID信息
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
        // 验证请求参数中必须包含问题ID
        Assert.hasKeyAndValue(reqJson, "qaId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 查询用户问题答案列表，包括分页信息和答案值详情
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为用户问题答案数据传输对象
        UserQuestionAnswerDto userQuestionAnswerDto = BeanConvertUtil.covertBean(reqJson, UserQuestionAnswerDto.class);

        // 查询用户问题答案总记录数
        int count = userQuestionAnswerV1InnerServiceSMOImpl.queryUserQuestionAnswersCount(userQuestionAnswerDto);

        List<UserQuestionAnswerDto> userQuestionAnswerDtos = null;

        if (count > 0) {
            // 如果存在记录，则查询具体的用户问题答案列表
            userQuestionAnswerDtos = userQuestionAnswerV1InnerServiceSMOImpl.queryUserQuestionAnswers(userQuestionAnswerDto);
        } else {
            // 如果没有记录，返回空列表
            userQuestionAnswerDtos = new ArrayList<>();
        }

        // 为每个用户问题答案刷入对应的答案值信息
        freshUserQuestionAnswerValues(userQuestionAnswerDtos);

        // 构建分页结果对象，计算总页数
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, userQuestionAnswerDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新用户问题答案值信息
     * 为每个用户问题答案对象设置对应的答案值列表
     *
     * @param userQuestionAnswerDtos 用户问题答案数据传输对象列表
     */
    private void freshUserQuestionAnswerValues(List<UserQuestionAnswerDto> userQuestionAnswerDtos) {
        // 检查参数有效性，如果列表为空则直接返回
        if (userQuestionAnswerDtos == null || userQuestionAnswerDtos.size() < 1) {
            return;
        }
        
        // 创建答案值查询条件对象
        UserQuestionAnswerValueDto userQuestionAnswerValueDto = new UserQuestionAnswerValueDto();
        // 设置问题ID作为查询条件
        userQuestionAnswerValueDto.setQaId(userQuestionAnswerDtos.get(0).getQaId());
        // 设置小区ID作为查询条件
        userQuestionAnswerValueDto.setCommunityId(userQuestionAnswerDtos.get(0).getCommunityId());
        
        // 查询符合条件的答案值列表
        List<UserQuestionAnswerValueDto> values
                = userQuestionAnswerValueV1InnerServiceSMOImpl.queryUserQuestionAnswerValues(userQuestionAnswerValueDto);

        // 如果没有查询到答案值，直接返回
        if (values == null || values.size() < 1) {
            return;
        }
        
        // 临时存储每个用户问题答案对应的答案值列表
        List<UserQuestionAnswerValueDto> tValues = null;
        
        // 遍历所有用户问题答案，为每个答案设置对应的值列表
        for (UserQuestionAnswerDto userQuestionAnswerDto : userQuestionAnswerDtos) {
            tValues = new ArrayList<>();
            // 遍历所有答案值，找到属于当前用户问题答案的值
            for (UserQuestionAnswerValueDto tmpUserQuestionAnswerValue : values) {
                // 通过用户问题答案ID进行匹配
                if (userQuestionAnswerDto.getUserQaId().equals(tmpUserQuestionAnswerValue.getUserQaId())) {
                    tValues.add(tmpUserQuestionAnswerValue);
                }
            }
            // 将匹配的答案值列表设置到用户问题答案对象中
            userQuestionAnswerDto.setValues(tValues);
        }
    }
}