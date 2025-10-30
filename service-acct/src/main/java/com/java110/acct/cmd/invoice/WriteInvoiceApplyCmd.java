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
package com.java110.acct.cmd.invoice;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.invoice.InvoiceApplyDto;
import com.java110.dto.invoice.InvoiceEventDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.IInvoiceApplyV1InnerServiceSMO;
import com.java110.intf.acct.IInvoiceEventV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.invoice.InvoiceApplyPo;
import com.java110.po.invoice.InvoiceEventPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * 类表述：审核发票
 * 服务编码：invoiceApply.updateInvoiceApply
 * 请求路劲：/app/invoiceApply.UpdateInvoiceApply
 * add by 吴学文 at 2023-10-08 16:26:34 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 发票申请处理命令类
 * 负责处理发票申请的相关操作，包括发票申请的审核和事件记录
 * 主要功能包括参数验证、发票申请状态更新、发票事件记录等
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-10-08
 */
@Java110Cmd(serviceCode = "invoice.writeInvoiceApply")
public class WriteInvoiceApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(WriteInvoiceApplyCmd.class);

    /**
     * 发票申请服务接口
     */
    @Autowired
    private IInvoiceApplyV1InnerServiceSMO invoiceApplyV1InnerServiceSMOImpl;

    /**
     * 发票事件服务接口
     */
    @Autowired
    private IInvoiceEventV1InnerServiceSMO invoiceEventV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "applyId", "applyId不能为空");
        Assert.hasKeyAndValue(reqJson, "eventType", "类型不能为空");
        Assert.hasKeyAndValue(reqJson, "remark", "说明不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行发票申请处理命令
     * 根据事件类型处理发票申请，包括发票领用操作和事件记录
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

        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户信息，确保用户已登录且唯一
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 处理发票领用事件
        if (InvoiceEventDto.STATE_GET.equals(reqJson.getString("eventType"))) {
            // 创建发票申请更新对象
            InvoiceApplyPo invoiceApplyPo = new InvoiceApplyPo();
            invoiceApplyPo.setApplyId(reqJson.getString("applyId"));
            // 设置申请状态为完成
            invoiceApplyPo.setState(InvoiceApplyDto.STATE_COMPLETE);
            // 更新发票申请状态
            int flag = invoiceApplyV1InnerServiceSMOImpl.updateInvoiceApply(invoiceApplyPo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }

        // 保存发票事件记录
        InvoiceEventPo invoiceEventPo = new InvoiceEventPo();
        // 设置申请ID
        invoiceEventPo.setApplyId(reqJson.getString("applyId"));
        // 设置小区ID
        invoiceEventPo.setCommunityId(reqJson.getString("communityId"));
        // 生成事件ID
        invoiceEventPo.setEventId(GenerateCodeFactory.getGeneratorId("11"));
        // 设置事件类型
        invoiceEventPo.setEventType(reqJson.getString("eventType"));
        // 设置创建用户信息
        invoiceEventPo.setCreateUserId(userId);
        invoiceEventPo.setCreateUserName(userDtos.get(0).getName());
        // 设置备注信息
        invoiceEventPo.setRemark(reqJson.getString("remark"));
        // 保存事件记录
        invoiceEventV1InnerServiceSMOImpl.saveInvoiceEvent(invoiceEventPo);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}