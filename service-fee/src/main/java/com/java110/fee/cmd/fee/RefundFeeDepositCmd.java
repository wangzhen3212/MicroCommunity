package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.account.AccountDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.user.UserDto;
import com.java110.fee.dao.IPayFeeDetailNewV1ServiceDao;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.account.AccountDetailPo;
import com.java110.po.account.AccountPo;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * 押金退费命令类
 * 负责处理押金退费业务逻辑，包括验证退费参数、更新缴费明细状态、
 * 创建退费记录、更新账户余额等操作
 *
 * @author Java110
 * @version 1.0
 * @serviceCode fee.refundFeeDeposit
 */
@Java110Cmd(serviceCode = "fee.refundFeeDeposit")
public class RefundFeeDepositCmd extends Cmd {

    /**
     * 费用明细内部服务接口
     */
    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    /**
     * 缴费明细V1内部服务接口
     */
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    /**
     * 用户V1内部服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 费用计算服务接口
     */
    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    /**
     * 账户内部服务接口
     */
    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    /**
     * 验证退费请求参数
     * 检查必要的参数是否存在且有效
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException   当参数验证失败时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求中是否包含必要的参数
        Assert.hasKeyAndValue(reqJson, "detailId", "未包含缴费ID");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 执行押金退费命令
     * 处理押金退费的核心业务逻辑，包括更新缴费状态、创建退费记录、更新账户余额等
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException   当业务处理失败时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取当前操作用户ID
        String userId = CmdContextUtils.getUserId(context);

        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 查询缴费明细信息
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setCommunityId(reqJson.getString("communityId"));
        feeDetailDto.setDetailId(reqJson.getString("detailId"));
        feeDetailDto.setFeeId(reqJson.getString("feeId"));
        feeDetailDto.setState(FeeDetailDto.STATE_NORMAL);
        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(feeDetailDto);
        // 验证缴费记录存在且唯一
        Assert.listOnlyOne(feeDetailDtos, "不存在该缴费记录");

        // 根据费用ID查询业主信息
        OwnerDto ownerDto = computeFeeSMOImpl.getFeeOwnerDto(feeDetailDtos.get(0).getFeeId());
        if (ownerDto == null) {
            throw new CmdException("费用未查到业主");
        }

        // 查询现金账户信息
        AccountDto accountDto = new AccountDto();
        accountDto.setObjId(ownerDto.getMemberId());
        accountDto.setAcctType(AccountDto.ACCT_TYPE_CASH); // 2003 现金账户
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);

        // 验证账户存在
        if (ListUtil.isNull(accountDtos)) {
            throw new CmdException("账户不存在,请到账户页面预存0元，生成账户");
        }

        // 更新原缴费明细状态为已退款
        PayFeeDetailPo payFeeDetailPo = new PayFeeDetailPo();
        payFeeDetailPo.setDetailId(reqJson.getString("detailId"));
        payFeeDetailPo.setState(FeeDetailDto.STATE_RETURNED);
        int flag = payFeeDetailV1InnerServiceSMOImpl.updatePayFeeDetailNew(payFeeDetailPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建退费记录（押金转存）
        PayFeeDetailPo refundPayFeeDetailPo = BeanConvertUtil.covertBean(feeDetailDtos.get(0), PayFeeDetailPo.class);
        refundPayFeeDetailPo.setRemark("押金转存");
        refundPayFeeDetailPo.setbId("-1");
        refundPayFeeDetailPo.setState(FeeDetailDto.STATE_RETURN_ORDER);
        refundPayFeeDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId("11"));
        refundPayFeeDetailPo.setCashierId(userDtos.get(0).getUserId());
        refundPayFeeDetailPo.setCashierName(userDtos.get(0).getName());
        // 设置金额为负数表示退款
        refundPayFeeDetailPo.setReceivableAmount(unum(refundPayFeeDetailPo.getReceivableAmount()) + "");
        refundPayFeeDetailPo.setReceivedAmount(unum(refundPayFeeDetailPo.getReceivedAmount()) + "");
        // 设置其他金额字段为0
        refundPayFeeDetailPo.setLateAmount("0");
        refundPayFeeDetailPo.setDiscountAmount("0");
        refundPayFeeDetailPo.setGiftAmount("0");
        refundPayFeeDetailPo.setDeductionAmount("0");
        refundPayFeeDetailPo.setAcctAmount("0");
        // 设置周期数为负数
        refundPayFeeDetailPo.setCycles(unum(refundPayFeeDetailPo.getCycles()) + "");
        // 设置费率为退款账户费率
        refundPayFeeDetailPo.setPrimeRate(FeeDetailDto.PRIME_REATE_REFUND_ACCT);
        refundPayFeeDetailPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        // 保存退费记录
        payFeeDetailV1InnerServiceSMOImpl.savePayFeeDetailNew(refundPayFeeDetailPo);

        // 更新账户余额（预存押金退款金额）
        AccountDetailPo accountDetailPo = new AccountDetailPo();
        accountDetailPo.setAcctId(accountDtos.get(0).getAcctId());
        accountDetailPo.setAmount(feeDetailDtos.get(0).getReceivedAmount());
        accountDetailPo.setObjId(accountDtos.get(0).getObjId());
        accountDetailPo.setObjType(accountDtos.get(0).getObjType());
        accountDetailPo.setRemark("押金退款预存账户");
        accountInnerServiceSMOImpl.prestoreAccount(accountDetailPo);
    }

    /**
     * 将字符串数值转换为负数
     * 用于处理退款金额的负数转换
     *
     * @param value 原始数值字符串
     * @return 转换后的负数值
     */
    private double unum(String value) {
        double dValue = Double.parseDouble(value);
        return dValue * -1;
    }
}