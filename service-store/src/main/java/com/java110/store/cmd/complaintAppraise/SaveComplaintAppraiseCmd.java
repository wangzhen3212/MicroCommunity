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
 * 类表述：投诉评价保存命令类
 * 服务编码：complaintAppraise.saveComplaintAppraise
 * 请求路劲：/app/complaintAppraise.SaveComplaintAppraise
 * add by 吴学文 at 2024-02-21 13:04:55 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理投诉评价的保存操作，包括参数验证、评价信息保存和评价事件记录
 * 主要职责：
 * 1. 验证请求参数完整性
 * 2. 保存投诉评价信息
 * 3. 记录评价相关事件
 * 4. 返回操作结果
 */
@Java110Cmd(serviceCode = "complaintAppraise.saveComplaintAppraise")
public class SaveComplaintAppraiseCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveComplaintAppraiseCmd.class);

    /** 评价ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 投诉评价服务接口 */
    @Autowired
    private IComplaintAppraiseV1InnerServiceSMO complaintAppraiseV1InnerServiceSMOImpl;

    /** 投诉事件服务接口 */
    @Autowired
    private IComplaintEventV1InnerServiceSMO complaintEventV1InnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证投诉ID是否存在
        Assert.hasKeyAndValue(reqJson, "complaintId", "请求报文中未包含complaintId");
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证评分是否存在
        Assert.hasKeyAndValue(reqJson, "score", "请求报文中未包含score");
    }

    /**
     * 执行命令方法
     * 处理投诉评价的保存逻辑，包括用户验证、评价信息保存和事件记录
     *
     * @param event CMD事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败或用户未登录时抛出异常
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

        // 验证用户是否存在且唯一
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 将请求参数转换为投诉评价PO对象
        ComplaintAppraisePo complaintAppraisePo = BeanConvertUtil.covertBean(reqJson, ComplaintAppraisePo.class);
        // 生成评价ID
        complaintAppraisePo.setAppraiseId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置评价状态为待处理
        complaintAppraisePo.setState("W");
        // 设置创建用户信息
        complaintAppraisePo.setCreateUserId(userDtos.get(0).getUserId());
        complaintAppraisePo.setCreateUserName(userDtos.get(0).getName());
        // 保存投诉评价信息
        int flag = complaintAppraiseV1InnerServiceSMOImpl.saveComplaintAppraise(complaintAppraisePo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建投诉事件记录
        ComplaintEventPo complaintEventPo = new ComplaintEventPo();
        // 生成事件ID
        complaintEventPo.setEventId(GenerateCodeFactory.getGeneratorId("11"));
        // 设置事件创建用户信息
        complaintEventPo.setCreateUserId(userDtos.get(0).getUserId());
        complaintEventPo.setCreateUserName(userDtos.get(0).getName());
        // 设置关联的投诉ID
        complaintEventPo.setComplaintId(reqJson.getString("complaintId"));
        // 设置事件备注信息
        complaintEventPo.setRemark(reqJson.getString("context"));
        // 设置事件类型为评价类型
        complaintEventPo.setEventType(ComplaintEventDto.EVENT_TYPE_APPRAISE);
        // 设置小区ID
        complaintEventPo.setCommunityId(reqJson.getString("communityId"));

        // 保存投诉事件记录
        complaintEventV1InnerServiceSMOImpl.saveComplaintEvent(complaintEventPo);

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}