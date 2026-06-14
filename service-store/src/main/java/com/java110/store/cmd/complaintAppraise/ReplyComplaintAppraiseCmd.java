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
package com.java110.store.cmd.complaintAppraise;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.complaint.ComplaintAppraiseDto;
import com.java110.dto.complaint.ComplaintEventDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IComplaintAppraiseV1InnerServiceSMO;
import com.java110.intf.store.IComplaintEventV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.complaint.ComplaintAppraisePo;
import com.java110.po.complaint.ComplaintEventPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：complaintAppraise.updateComplaintAppraise
 * 请求路劲：/app/complaintAppraise.UpdateComplaintAppraise
 * add by 吴学文 at 2024-02-21 13:04:55 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 投诉评价回复命令类
 * 
 * 该类用于处理投诉评价的回复操作，包括验证回复请求参数、更新评价回复信息
 * 并记录回复事件到投诉事件表中
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024-02-21
 */
@Java110Cmd(serviceCode = "complaintAppraise.replyComplaintAppraise")
public class ReplyComplaintAppraiseCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ReplyComplaintAppraiseCmd.class);

    /**
     * 投诉评价服务接口
     */
    @Autowired
    private IComplaintAppraiseV1InnerServiceSMO complaintAppraiseV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 投诉事件服务接口
     */
    @Autowired
    private IComplaintEventV1InnerServiceSMO complaintEventV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证回复投诉评价请求的必要参数是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证评价ID是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "appraiseId", "appraiseId不能为空");
        // 验证小区ID是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        // 验证回复内容是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "replyContext", "回复内容不能为空");
    }

    /**
     * 执行回复投诉评价命令
     * 
     * 该方法处理投诉评价的回复操作，包括：
     * 1. 获取当前登录用户信息
     * 2. 查询评价记录
     * 3. 更新评价回复信息
     * 4. 记录回复事件
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 构建用户查询条件
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户信息，确保用户已登录且唯一
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 构建评价查询条件
        ComplaintAppraiseDto complaintAppraiseDto = new ComplaintAppraiseDto();
        complaintAppraiseDto.setAppraiseId(reqJson.getString("appraiseId"));
        // 查询评价记录
        List<ComplaintAppraiseDto> complaintAppraiseDtos = complaintAppraiseV1InnerServiceSMOImpl.queryComplaintAppraises(complaintAppraiseDto);

        // 验证评价记录存在且唯一
        Assert.listOnlyOne(complaintAppraiseDtos, "未包含评价记录");

        // 将请求JSON转换为评价PO对象
        ComplaintAppraisePo complaintAppraisePo = BeanConvertUtil.covertBean(reqJson, ComplaintAppraisePo.class);
        // 设置评价状态为已完成（C）
        complaintAppraisePo.setState("C");
        // 设置回复用户ID和用户名
        complaintAppraisePo.setReplyUserId(userDtos.get(0).getUserId());
        complaintAppraisePo.setReplyUserName(userDtos.get(0).getName());
        // 更新评价信息
        int flag = complaintAppraiseV1InnerServiceSMOImpl.updateComplaintAppraise(complaintAppraisePo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 创建投诉事件记录
        ComplaintEventPo complaintEventPo = new ComplaintEventPo();
        // 生成事件ID
        complaintEventPo.setEventId(GenerateCodeFactory.getGeneratorId("11"));
        // 设置创建用户信息
        complaintEventPo.setCreateUserId(userDtos.get(0).getUserId());
        complaintEventPo.setCreateUserName(userDtos.get(0).getName());
        // 设置关联的投诉ID
        complaintEventPo.setComplaintId(complaintAppraiseDtos.get(0).getComplaintId());
        // 设置回复内容
        complaintEventPo.setRemark(reqJson.getString("replyContext"));
        // 设置事件类型为回复类型
        complaintEventPo.setEventType(ComplaintEventDto.EVENT_TYPE_REPLY);
        // 设置小区ID
        complaintEventPo.setCommunityId(reqJson.getString("communityId"));

        // 保存投诉事件记录
        complaintEventV1InnerServiceSMOImpl.saveComplaintEvent(complaintEventPo);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}