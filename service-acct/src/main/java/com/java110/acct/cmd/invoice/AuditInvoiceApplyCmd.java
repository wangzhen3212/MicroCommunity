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
import com.java110.dto.invoice.InvoiceApplyItemDto;
import com.java110.dto.invoice.InvoiceEventDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.IInvoiceApplyItemV1InnerServiceSMO;
import com.java110.intf.acct.IInvoiceApplyV1InnerServiceSMO;
import com.java110.intf.acct.IInvoiceEventV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.invoice.InvoiceApplyPo;
import com.java110.po.invoice.InvoiceEventPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 发票申请审核命令类
 * 
 * 该类负责处理发票申请的审核操作，包括审核通过和审核拒绝两种情况。
 * 审核通过时，将发票申请状态更新为"待上传"，并记录审核事件，同时更新相关缴费明细的开票状态。
 * 审核拒绝时，将发票申请状态更新为"审核失败"，并记录审核事件。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-10-08
 */
@Java110Cmd(serviceCode = "invoice.auditInvoiceApply")
public class AuditInvoiceApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(AuditInvoiceApplyCmd.class);

    /**
     * 发票申请服务接口
     */
    @Autowired
    private IInvoiceApplyV1InnerServiceSMO invoiceApplyV1InnerServiceSMOImpl;

    /**
     * 发票申请项服务接口
     */
    @Autowired
    private IInvoiceApplyItemV1InnerServiceSMO invoiceApplyItemV1InnerServiceSMOImpl;

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
     * 缴费明细服务接口
     */
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在且不为空
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证申请ID是否存在
        Assert.hasKeyAndValue(reqJson, "applyId", "applyId不能为空");
        // 验证审核状态是否存在
        Assert.hasKeyAndValue(reqJson, "state", "状态不能为空");
        // 验证审核说明是否存在
        Assert.hasKeyAndValue(reqJson, "remark", "说明不能为空");
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行发票申请审核命令
     * 
     * 该方法处理发票申请的审核逻辑，包括：
     * 1. 验证用户登录状态
     * 2. 更新发票申请状态
     * 3. 记录审核事件
     * 4. 更新相关缴费明细的开票状态
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

        // 查询用户信息，验证用户是否登录
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 确保只有一个用户记录，防止数据异常
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 创建发票申请PO对象，用于更新申请状态
        InvoiceApplyPo invoiceApplyPo = new InvoiceApplyPo();
        invoiceApplyPo.setApplyId(reqJson.getString("applyId"));
        
        // 根据审核状态设置相应的发票申请状态
        // 1100表示审核通过，将状态设置为待上传
        if ("1100".equals(reqJson.getString("state"))) {
            invoiceApplyPo.setState(InvoiceApplyDto.STATE_UPLOAD);
        } else {
            // 其他状态表示审核失败
            invoiceApplyPo.setState(InvoiceApplyDto.STATE_FAIL);
        }
        
        // 更新发票申请状态
        int flag = invoiceApplyV1InnerServiceSMOImpl.updateInvoiceApply(invoiceApplyPo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 保存审核事件记录
        InvoiceEventPo invoiceEventPo = new InvoiceEventPo();
        invoiceEventPo.setApplyId(reqJson.getString("applyId"));
        invoiceEventPo.setCommunityId(reqJson.getString("communityId"));
        // 生成唯一的事件ID
        invoiceEventPo.setEventId(GenerateCodeFactory.getGeneratorId("11"));
        
        // 根据审核状态设置事件类型
        if ("1100".equals(reqJson.getString("state"))) {
            invoiceEventPo.setEventType(InvoiceEventDto.STATE_COMPLETE);
        } else {
            invoiceEventPo.setEventType(InvoiceEventDto.STATE_FAIL);
        }
        
        // 设置事件创建人信息
        invoiceEventPo.setCreateUserId(userId);
        invoiceEventPo.setCreateUserName(userDtos.get(0).getName());
        invoiceEventPo.setRemark(reqJson.getString("remark"));
        
        // 保存事件记录
        invoiceEventV1InnerServiceSMOImpl.saveInvoiceEvent(invoiceEventPo);

        // 查询发票申请相关的缴费明细项
        InvoiceApplyItemDto invoiceApplyItemDto = new InvoiceApplyItemDto();
        invoiceApplyItemDto.setApplyId(invoiceApplyPo.getApplyId());
        // 设置项类型为费用类型
        invoiceApplyItemDto.setItemType(InvoiceApplyItemDto.ITEM_TYPE_FEE);
        List<InvoiceApplyItemDto> invoiceApplyItemDtos = invoiceApplyItemV1InnerServiceSMOImpl.queryInvoiceApplyItems(invoiceApplyItemDto);
        
        // 如果没有相关的缴费明细项，直接返回
        if (ListUtil.isNull(invoiceApplyItemDtos)) {
            return;
        }

        // 遍历所有相关的缴费明细项，更新开票状态
        for (InvoiceApplyItemDto tmpInvoiceApplyItemDto : invoiceApplyItemDtos) {
            // 创建缴费明细PO对象，用于更新开票状态
            PayFeeDetailPo payFeeDetailPo = new PayFeeDetailPo();
            payFeeDetailPo.setDetailId(tmpInvoiceApplyItemDto.getItemObjId());
            payFeeDetailPo.setCommunityId(tmpInvoiceApplyItemDto.getCommunityId());
            
            // 根据审核状态设置开票状态
            if ("1100".equals(reqJson.getString("state"))) {
                // 审核通过，设置开票状态为已完成
                payFeeDetailPo.setOpenInvoice("Y");
            } else {
                // 审核失败，设置开票状态为待开票
                payFeeDetailPo.setOpenInvoice("N");
            }

            // 更新缴费明细的开票状态
            payFeeDetailV1InnerServiceSMOImpl.updatePayFeeDetailNew(payFeeDetailPo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}