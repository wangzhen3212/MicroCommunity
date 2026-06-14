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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.account.AccountReceiptDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.invoice.InvoiceApplyDto;
import com.java110.dto.invoice.InvoiceApplyItemDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.invoice.OwnerInvoiceDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.IInvoiceApplyItemV1InnerServiceSMO;
import com.java110.intf.acct.IInvoiceApplyV1InnerServiceSMO;
import com.java110.intf.acct.IOwnerInvoiceV1InnerServiceSMO;
import com.java110.intf.fee.IAccountReceiptV1InnerServiceSMO;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.invoice.InvoiceApplyPo;
import com.java110.po.invoice.InvoiceApplyItemPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 发票申请保存命令类
 * 
 * 该类负责处理发票申请的保存操作，包括参数验证、业务逻辑处理和数据库操作。
 * 主要功能包括验证请求参数、计算发票金额、保存发票申请信息和发票申请项信息。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-10-08
 */
@Java110Cmd(serviceCode = "invoice.saveInvoiceApply")
public class SaveInvoiceApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveInvoiceApplyCmd.class);

    /** 申请ID前缀 */
    public static final String CODE_PREFIX_ID = "10";

    @Autowired
    private IInvoiceApplyV1InnerServiceSMO invoiceApplyV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerInvoiceV1InnerServiceSMO ownerInvoiceV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IInvoiceApplyItemV1InnerServiceSMO invoiceApplyItemV1InnerServiceSMOImpl;

    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    @Autowired
    private IAccountReceiptV1InnerServiceSMO accountReceiptV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整有效，包括必填字段检查、发票抬头验证和业主信息验证。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文对象
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "oiId", "请求报文中未包含oiId");
        Assert.hasKeyAndValue(reqJson, "invoiceType", "请求报文中未包含invoiceType");
        Assert.hasKeyAndValue(reqJson, "ownerName", "请求报文中未包含ownerName");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 查询发票抬头信息并验证
        OwnerInvoiceDto ownerInvoiceDto = new OwnerInvoiceDto();
        ownerInvoiceDto.setOiId(reqJson.getString("oiId"));
        ownerInvoiceDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerInvoiceDto> ownerInvoiceDtos = ownerInvoiceV1InnerServiceSMOImpl.queryOwnerInvoices(ownerInvoiceDto);

        Assert.listOnlyOne(ownerInvoiceDtos, "发票抬头不存在");

        // 查询业主信息并验证
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(ownerInvoiceDtos.get(0).getOwnerId());
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        if (ownerDtos == null || ownerDtos.isEmpty()) {
            throw new CmdException("业主不存在");
        }

        // 设置申请手机号
        reqJson.put("applyTel", ownerDtos.get(0).getLink());
    }

    /**
     * 执行发票申请保存命令
     * 
     * 处理发票申请的核心业务逻辑，包括：
     * 1. 用户信息验证
     * 2. 发票申请信息保存
     * 3. 发票金额计算
     * 4. 发票申请项保存
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文对象
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 获取当前用户ID
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 验证用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户未登录");

        // 转换请求数据为发票申请PO对象
        InvoiceApplyPo invoiceApplyPo = BeanConvertUtil.covertBean(reqJson, InvoiceApplyPo.class);
        invoiceApplyPo.setApplyId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));

        List<InvoiceApplyItemPo> invoiceApplyItemPos = new ArrayList<>();
        BigDecimal invoiceAmount = new BigDecimal("0.0");
        
        // 计算已缴费用的发票金额
        invoiceAmount = computeFeeDetail(reqJson, invoiceApplyItemPos, invoiceAmount, invoiceApplyPo);

        // 计算账户费用的发票金额
        invoiceAmount = computeAcctDetail(reqJson, invoiceApplyItemPos, invoiceAmount, invoiceApplyPo);

        // 验证是否包含开票项
        if (invoiceApplyItemPos.isEmpty()) {
            throw new CmdException("未包含开票项");
        }

        // 设置发票总金额和创建信息
        invoiceApplyPo.setInvoiceAmount(invoiceAmount.doubleValue() + "");
        invoiceApplyPo.setCreateUserId(userId);
        invoiceApplyPo.setCreateUserName(userDtos.get(0).getName());
        invoiceApplyPo.setState(InvoiceApplyDto.STATE_WAIT);
        
        // 保存发票申请信息
        int flag = invoiceApplyV1InnerServiceSMOImpl.saveInvoiceApply(invoiceApplyPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 保存发票申请项信息
        invoiceApplyItemV1InnerServiceSMOImpl.saveInvoiceApplyItems(invoiceApplyItemPos);

        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 计算已缴费用的发票金额
     * 
     * 处理缴费明细的开票逻辑，包括：
     * 1. 验证缴费明细数据
     * 2. 计算缴费金额总和
     * 3. 验证开票时间限制
     * 4. 更新缴费明细的开票状态
     * 
     * @param reqJson 请求JSON对象，包含detailIds数组
     * @param invoiceApplyItemPos 发票申请项列表
     * @param invoiceAmount 当前发票金额
     * @param invoiceApplyPo 发票申请对象
     * @return 更新后的发票金额
     * @throws CmdException 当开票时间超过限制时抛出异常
     */
    private BigDecimal computeFeeDetail(JSONObject reqJson, List<InvoiceApplyItemPo> invoiceApplyItemPos, BigDecimal invoiceAmount, InvoiceApplyPo invoiceApplyPo) {

        // 检查是否包含缴费明细ID
        if (!reqJson.containsKey("detailIds")) {
            return invoiceAmount;
        }

        JSONArray detailIds = reqJson.getJSONArray("detailIds");
        if (detailIds == null || detailIds.isEmpty()) {
            return invoiceAmount;
        }

        // 查询缴费明细信息
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setDetailIds(detailIds.toArray(new String[detailIds.size()]));
        feeDetailDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(feeDetailDto);

        if (ListUtil.isNull(feeDetailDtos)) {
            return invoiceAmount;
        }
        
        InvoiceApplyItemPo invoiceApplyItemPo = null;
        for (FeeDetailDto tmpFeeDetailDto : feeDetailDtos) {
            // 验证开票时间：不能预开明年的票
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, 1); // 设置为2月
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 设置为1号
            calendar.add(Calendar.YEAR, 1); // 加一年，即明年的2月1日
            if (tmpFeeDetailDto.getStartTime().getTime() > calendar.getTime().getTime()) {
                throw new CmdException("不能预开明年的票");
            }
            
            // 累加发票金额
            invoiceAmount = invoiceAmount.add(new BigDecimal(tmpFeeDetailDto.getReceivedAmount()));
            
            // 创建发票申请项
            invoiceApplyItemPo = new InvoiceApplyItemPo();
            invoiceApplyItemPo.setApplyId(invoiceApplyPo.getApplyId());
            invoiceApplyItemPo.setCommunityId(invoiceApplyPo.getCommunityId());
            invoiceApplyItemPo.setItemAmount(tmpFeeDetailDto.getReceivedAmount());
            invoiceApplyItemPo.setItemId(GenerateCodeFactory.getGeneratorId("11"));
            invoiceApplyItemPo.setItemName(tmpFeeDetailDto.getFeeName());
            invoiceApplyItemPo.setRemark(tmpFeeDetailDto.getPayerObjName()+tmpFeeDetailDto.getFeeName()
                    + "("
                    + DateUtil.getFormatTimeStringB(tmpFeeDetailDto.getStartTime())
                    + "~"
                    + DateUtil.getFormatTimeStringB(tmpFeeDetailDto.getEndTime())
                    + ")");
            invoiceApplyItemPo.setPayTime(DateUtil.getFormatTimeStringA(tmpFeeDetailDto.getCreateTime()));
            invoiceApplyItemPo.setItemObjId(tmpFeeDetailDto.getDetailId());
            invoiceApplyItemPo.setItemType(InvoiceApplyItemDto.ITEM_TYPE_FEE);
            invoiceApplyItemPos.add(invoiceApplyItemPo);

            // 更新缴费明细的开票状态为"开票中"
            PayFeeDetailPo payFeeDetailPo = new PayFeeDetailPo();
            payFeeDetailPo.setDetailId(tmpFeeDetailDto.getDetailId());
            payFeeDetailPo.setCommunityId(tmpFeeDetailDto.getCommunityId());
            payFeeDetailPo.setOpenInvoice("D"); // 开票中状态
            payFeeDetailV1InnerServiceSMOImpl.updatePayFeeDetailNew(payFeeDetailPo);
        }

        return invoiceAmount;
    }

    /**
     * 计算账户费用的发票金额
     * 
     * 处理账户收据的开票逻辑，包括：
     * 1. 验证账户收据数据
     * 2. 计算账户收据金额总和
     * 3. 创建账户收据对应的发票申请项
     * 
     * @param reqJson 请求JSON对象，包含arIds数组
     * @param invoiceApplyItemPos 发票申请项列表
     * @param invoiceAmount 当前发票金额
     * @param invoiceApplyPo 发票申请对象
     * @return 更新后的发票金额
     */
    private BigDecimal computeAcctDetail(JSONObject reqJson, List<InvoiceApplyItemPo> invoiceApplyItemPos, BigDecimal invoiceAmount, InvoiceApplyPo invoiceApplyPo) {
        // 检查是否包含账户收据ID
        if (!reqJson.containsKey("arIds")) {
            return invoiceAmount;
        }

        JSONArray arIds = reqJson.getJSONArray("arIds");
        if (arIds == null || arIds.isEmpty()) {
            return invoiceAmount;
        }

        // 查询账户收据信息
        AccountReceiptDto accountReceiptDto = new AccountReceiptDto();
        accountReceiptDto.setArIds(arIds.toArray(new String[arIds.size()]));
        accountReceiptDto.setCommunityId(reqJson.getString("communityId"));
        List<AccountReceiptDto> accountReceiptDtos = accountReceiptV1InnerServiceSMOImpl.queryAccountReceipts(accountReceiptDto);
        if (accountReceiptDtos == null || accountReceiptDtos.isEmpty()) {
            return invoiceAmount;
        }

        InvoiceApplyItemPo invoiceApplyItemPo = null;
        for (AccountReceiptDto tmpAccountReceiptDto : accountReceiptDtos) {
            // 累加发票金额
            invoiceAmount = invoiceAmount.add(new BigDecimal(tmpAccountReceiptDto.getReceivedAmount()));
            
            // 创建发票申请项
            invoiceApplyItemPo = new InvoiceApplyItemPo();
            invoiceApplyItemPo.setApplyId(invoiceApplyPo.getApplyId());
            invoiceApplyItemPo.setCommunityId(invoiceApplyPo.getCommunityId());
            invoiceApplyItemPo.setItemAmount(tmpAccountReceiptDto.getReceivedAmount());
            invoiceApplyItemPo.setItemId(GenerateCodeFactory.getGeneratorId("11"));
            invoiceApplyItemPo.setItemName(tmpAccountReceiptDto.getOwnerName());
            invoiceApplyItemPo.setItemObjId(tmpAccountReceiptDto.getArId());
            invoiceApplyItemPo.setItemType(InvoiceApplyItemDto.ITEM_TYPE_ACCT);
            invoiceApplyItemPo.setPayTime(DateUtil.getFormatTimeStringA(tmpAccountReceiptDto.getCreateTime()));

            invoiceApplyItemPos.add(invoiceApplyItemPo);
        }

        return invoiceAmount;
    }
}