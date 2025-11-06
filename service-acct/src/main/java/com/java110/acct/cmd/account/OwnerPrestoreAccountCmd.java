package com.java110.acct.cmd.account;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.bmo.account.IOwnerPrestoreAccountBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.account.AccountDetailDto;
import com.java110.dto.account.AccountDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.acct.IAccountDetailInnerServiceSMO;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IAccountReceiptV1InnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.po.account.AccountDetailPo;
import com.java110.po.account.AccountPo;
import com.java110.po.account.AccountReceiptPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

/**
 * 业主预存账户命令类
 * 处理业主账户预存操作，包括账户创建、预存金额、生成交易明细和收款单等功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "account.ownerPrestoreAccount")
public class OwnerPrestoreAccountCmd extends Cmd {

    @Autowired
    private IOwnerPrestoreAccountBMO ownerPrestoreAccountBMOImpl;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    @Autowired
    private IAccountDetailInnerServiceSMO accountDetailInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IAccountReceiptV1InnerServiceSMO accountReceiptV1InnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查必要的参数是否存在且有效
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
        Assert.hasKeyAndValue(reqJson, "ownerId", "业主不能为空");
        Assert.hasKeyAndValue(reqJson, "amount", "金额不能为空");
        Assert.hasKeyAndValue(reqJson, "acctType", "账户类型不能为空");
        Assert.hasKeyAndValue(reqJson, "primeRate", "未包含支付方式");
        
        // 根据账户类型验证是否需要房屋ID
        String acctType = reqJson.getString("acctType");
        if (AccountDto.ACCT_TYPE_PROPERTY_FEE.equals(acctType) || AccountDto.ACCT_TYPE_METER.equals(acctType)) {
            Assert.hasKeyAndValue(reqJson, "roomId", "未包含扣款房屋");
        }
    }

    /**
     * 执行业主预存账户命令
     * 处理业主账户预存逻辑，包括账户检查、预存操作、生成交易明细和收款单
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建账户明细对象并设置基本信息
        AccountDetailPo accountDetailPo = new AccountDetailPo();
        accountDetailPo.setRemark(reqJson.getString("remark"));
        accountDetailPo.setObjId(reqJson.getString("ownerId"));
        accountDetailPo.setAmount(reqJson.getString("amount"));
        String acctType = reqJson.getString("acctType");

        // 查询业主是否已有账户
        AccountDto accountDto = new AccountDto();
        accountDto.setObjId(accountDetailPo.getObjId());
        accountDto.setObjType(AccountDto.OBJ_TYPE_PERSON); // 设置对象类型为个人
        accountDto.setPartId(reqJson.getString("communityId"));
        accountDto.setAcctType(reqJson.getString("acctType"));
        if (AccountDto.ACCT_TYPE_PROPERTY_FEE.equals(acctType) || AccountDto.ACCT_TYPE_METER.equals(acctType)) {
            accountDto.setRoomId(reqJson.getString("roomId"));
        }
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);
        
        if (ListUtil.isNull(accountDtos)) {
            // 如果账户不存在，创建新账户
            accountDto = addAccountDto(reqJson);
            
            // 保存交易明细
            AccountDetailPo accountDetail = BeanConvertUtil.covertBean(accountDetailPo, AccountDetailPo.class);
            accountDetail.setOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_orderId));
            accountDetail.setAcctId(accountDto.getAcctId());
            accountDetail.setObjType(AccountDetailDto.ORDER_TYPE_USER); // 设置订单类型为用户
            accountDetail.setDetailType(AccountDetailDto.DETAIL_TYPE_IN); // 设置明细类型为收入
            
            // 生成明细ID和关联账户ID
            if (StringUtil.isEmpty(accountDetail.getDetailId())) {
                accountDetail.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
            }
            if (StringUtil.isEmpty(accountDetail.getRelAcctId())) {
                accountDetail.setRelAcctId("-1"); // 默认关联账户ID
            }
            accountDetailInnerServiceSMOImpl.saveAccountDetails(accountDetail);
        } else {
            // 如果账户已存在，执行预存操作
            accountDto = accountDtos.get(0);
            accountDetailPo.setOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_orderId));
            accountDetailPo.setAcctId(accountDto.getAcctId());
            accountDetailPo.setObjType(AccountDetailDto.ORDER_TYPE_USER);
            
            // 执行预存操作并检查结果
            int flag = accountInnerServiceSMOImpl.prestoreAccount(accountDetailPo);
            if (flag < 1) {
                throw new CmdException("预存失败");
            }
        }
        
        // 查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(reqJson.getString("ownerId"));
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
        Assert.listOnlyOne(ownerDtos, "业主不存在");

        // 创建并保存账户收款单
        AccountReceiptPo accountReceiptPo = new AccountReceiptPo();
        accountReceiptPo.setOwnerId(reqJson.getString("ownerId"));
        accountReceiptPo.setOwnerName(ownerDtos.get(0).getName());
        accountReceiptPo.setLink(ownerDtos.get(0).getLink());
        accountReceiptPo.setArId(GenerateCodeFactory.getGeneratorId("11")); // 生成收款单ID
        accountReceiptPo.setAcctId(accountDto.getAcctId());
        accountReceiptPo.setPrimeRate(reqJson.getString("primeRate")); // 设置支付方式
        accountReceiptPo.setReceivableAmount(reqJson.getString("amount")); // 应收金额
        accountReceiptPo.setReceivedAmount(reqJson.getString("amount")); // 实收金额
        accountReceiptPo.setRemark(reqJson.getString("remark"));
        accountReceiptPo.setCommunityId(reqJson.getString("communityId"));
        accountReceiptV1InnerServiceSMOImpl.saveAccountReceipt(accountReceiptPo);

        // 设置成功响应
        context.setResponseEntity(ResultVo.success());
    }

    /**
     * 添加账户信息
     * 创建新的账户并返回账户DTO对象
     *
     * @param reqJson 请求的JSON数据
     * @return AccountDto 账户数据传输对象
     */
    private AccountDto addAccountDto(JSONObject reqJson) {
        // 验证房屋信息
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(reqJson.getString("roomId"));
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        Assert.listOnlyOne(roomDtos,"房屋不存在");

        // 验证业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(reqJson.getString("ownerId"));
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);
        Assert.listOnlyOne(ownerDtos, "业主不存在");

        // 创建账户对象并设置属性
        AccountPo accountPo = new AccountPo();
        accountPo.setAmount(reqJson.getString("amount")); // 设置账户金额
        accountPo.setAcctId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_acctId)); // 生成账户ID
        accountPo.setObjId(reqJson.getString("ownerId")); // 设置对象ID（业主ID）
        accountPo.setObjType(AccountDto.OBJ_TYPE_PERSON); // 设置对象类型为个人
        accountPo.setAcctType(reqJson.getString("acctType")); // 设置账户类型
        accountPo.setAcctName(ownerDtos.get(0).getName()); // 设置账户名称为业主姓名
        accountPo.setPartId(reqJson.getString("communityId")); // 设置分区ID（小区ID）
        accountPo.setLink(ownerDtos.get(0).getLink()); // 设置联系方式
        accountPo.setRoomId(roomDtos.get(0).getRoomId()); // 设置房屋ID
        accountPo.setRoomName(roomDtos.get(0).getRoomName()); // 设置房屋名称
        
        // 保存账户信息
        accountInnerServiceSMOImpl.saveAccount(accountPo);
        
        // 转换为DTO对象并返回
        return BeanConvertUtil.covertBean(accountPo, AccountDto.class);
    }

}