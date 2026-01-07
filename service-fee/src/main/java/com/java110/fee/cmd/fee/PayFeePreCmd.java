package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.account.AccountDetailDto;
import com.java110.dto.account.AccountDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.coupon.CouponUserDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.ComputeDiscountDto;
import com.java110.intf.acct.IAccountDetailInnerServiceSMO;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.acct.ICouponUserV1InnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.po.account.AccountDetailPo;
import com.java110.po.account.AccountPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 费用预支付命令处理类
 * <p>
 * 功能描述：处理费用预支付请求，计算应收金额、折扣金额、账户抵扣金额和优惠券抵扣金额，
 * 最终确定实收金额，并返回预支付结果信息
 *
 * @author 吴学文 at 2021-09-16 22:26:04 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 */
@Java110Cmd(serviceCode = "fee.payFeePre")
public class PayFeePreCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(PayFeePreCmd.class);

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IFeeDiscountInnerServiceSMO feeDiscountInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMO;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IApplyRoomDiscountInnerServiceSMO applyRoomDiscountInnerServiceSMOImpl;

    @Autowired
    private IFeeDetailInnerServiceSMO iFeeDetailInnerServiceSMO;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    @Autowired
    private ICouponUserV1InnerServiceSMO couponUserV1InnerServiceSMOImpl;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    @Autowired
    private IAccountDetailInnerServiceSMO accountDetailInnerServiceSMOImpl;

    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    @Autowired
    private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

    //键(积分账户最大使用积分)
    public static final String MAXIMUM_NUMBER = "MAXIMUM_NUMBER";

    //键(积分账户抵扣比例)
    public static final String DEDUCTION_PROPORTION = "DEDUCTION_PROPORTION";

    /**
     * 验证请求参数的有效性
     *
     * @param event              命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson            请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "cycles", "请求报文中未包含cycles节点");
        Assert.jsonObjectHaveKey(reqJson, "receivedAmount", "请求报文中未包含receivedAmount节点");
        Assert.jsonObjectHaveKey(reqJson, "feeId", "请求报文中未包含feeId节点");
        Assert.jsonObjectHaveKey(reqJson, "appId", "请求报文中未包含appId节点");

        // 验证参数值是否为空
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        Assert.hasLength(reqJson.getString("cycles"), "周期不能为空");
        Assert.hasLength(reqJson.getString("receivedAmount"), "实收金额不能为空");
        Assert.hasLength(reqJson.getString("feeId"), "费用ID不能为空");
        Assert.hasLength(reqJson.getString("appId"), "appId不能为空");

        // 验证费用状态是否允许缴费
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(reqJson.getString("feeId"));
        feeDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        Assert.listOnlyOne(feeDtos, "传入费用ID错误");

        feeDto = feeDtos.get(0);

        // 检查费用是否已结束
        if (FeeDto.STATE_FINISH.equals(feeDto.getState())) {
            throw new IllegalArgumentException("收费已经结束，不能再缴费");
        }

        Date endTime = feeDto.getEndTime();

        // 验证缴费周期是否超过配置的结束时间
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(feeDto.getConfigId());
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);

        if (feeConfigDtos != null && feeConfigDtos.size() == 1) {
            try {
                // 获取配置结束时间并增加5天缓冲期
                Date configEndTime = DateUtil.getDateFromString(feeConfigDtos.get(0).getEndTime(), DateUtil.DATE_FORMATE_STRING_A);
                configEndTime = DateUtil.stepDay(configEndTime, 5);

                // 计算新的结束时间
                Date newDate = DateUtil.stepMonth(endTime, reqJson.getInteger("cycles"));

                // 检查是否超过配置结束时间
                if (newDate.getTime() > configEndTime.getTime()) {
                    throw new IllegalArgumentException("缴费周期超过缴费结束时间");
                }

            } catch (Exception e) {
                logger.error("比较费用日期失败", e);
            }
        }
    }

    /**
     * 执行费用预支付计算命令
     *
     * @param event              命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson            请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        logger.debug("ServiceDataFlowEvent : {}", event);

        // 从请求头中获取用户ID和应用ID
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");
        String appId = cmdDataFlowContext.getReqHeaders().get("app-id");
        reqJson.put("appId", appId);
        reqJson.put("userId", userId);

        // 查询费用信息
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(reqJson.getString("feeId"));
        feeDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        if (feeDtos == null || feeDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "查询费用信息失败，未查到数据或查到多条数据");
        }
        feeDto = feeDtos.get(0);
        reqJson.put("feeTypeCd", feeDto.getFeeTypeCd());
        reqJson.put("feeId", feeDto.getFeeId());

        // 计算费用单价
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        // 计算应收金额 = 单价 × 周期数
        BigDecimal receivableAmount = new BigDecimal(feePriceAll.get("feePrice").toString());
        BigDecimal cycles = new BigDecimal(Double.parseDouble(reqJson.getString("cycles")));
        double tmpReceivableAmount = cycles.multiply(receivableAmount).setScale(4, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        // 根据费用精度设置处理金额精度
        tmpReceivableAmount = MoneyUtil.computePriceScale(tmpReceivableAmount, feeDto.getScale(), Integer.parseInt(feeDto.getDecimalPlace()));

        JSONObject paramOut = new JSONObject();
        paramOut.put("receivableAmount", tmpReceivableAmount);
        paramOut.put("oId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_oId));

        // 实收金额初始化为应收金额
        BigDecimal tmpReceivedAmout = new BigDecimal(tmpReceivableAmount);

        // 1.0 判断并计算折扣金额
        double discountPrice = judgeDiscount(reqJson);
        tmpReceivedAmout = tmpReceivedAmout.subtract(new BigDecimal(discountPrice)).setScale(2, BigDecimal.ROUND_HALF_EVEN);

        // 2.0 考虑账户抵消金额
        double accountPrice = judgeAccount(reqJson);
        tmpReceivedAmout = tmpReceivedAmout.subtract(new BigDecimal(accountPrice)).setScale(2, BigDecimal.ROUND_HALF_EVEN);

        // 3.0 考虑优惠券抵扣金额
        double couponPrice = checkCouponUser(reqJson);
        tmpReceivedAmout = tmpReceivedAmout.subtract(new BigDecimal(couponPrice)).setScale(2, BigDecimal.ROUND_HALF_EVEN);

        double receivedAmount = tmpReceivedAmout.doubleValue();
        // 所有优惠折扣计算完后，如果总金额小于等于0，则返回总扣款为0
        if (receivedAmount <= 0) {
            receivedAmount = 0.0;
        }
        // 根据费用精度设置处理最终实收金额
        receivedAmount = MoneyUtil.computePriceScale(
                receivedAmount,
                feeDto.getScale(),
                Integer.parseInt(feeDto.getDecimalPlace())
        );

        paramOut.put("receivedAmount", receivedAmount);

        // 获取费用显示名称
        String feeName = getObjName(feeDto);
        paramOut.put("feeName", feeName);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(paramOut.toJSONString(), HttpStatus.OK);
        reqJson.putAll(paramOut);
        // 将预支付信息缓存24小时
        CommonCache.setValue("payFeePre" + paramOut.getString("oId"), reqJson.toJSONString(), 24 * 60 * 60);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 获取费用对象的显示名称
     *
     * @param feeDto 费用数据传输对象
     * @return 格式化的费用显示名称
     */
    private String getObjName(FeeDto feeDto) {
        // 查询小区名称
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId(feeDto.getCommunityId());
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);

        Assert.listOnlyOne(communityDtos, "小区不存在");

        List<FeeAttrDto> feeAttrDtos = feeDto.getFeeAttrDtos();
        if (feeAttrDtos == null || feeAttrDtos.size() < 1) {
            return communityDtos.get(0).getName() + "-" + feeDto.getFeeName();
        }

        // 查找支付对象名称属性
        for (FeeAttrDto feeAttrDto : feeAttrDtos) {
            if (FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME.equals(feeAttrDto.getSpecCd())) {
                return communityDtos.get(0).getName() + "-" + feeAttrDto.getValue() + "-" + feeDto.getFeeName();
            }
        }

        return communityDtos.get(0).getName() + "-" + feeDto.getFeeName();
    }

    /**
     * 计算账户抵扣金额
     * <p>
     * 处理现金账户和积分账户的抵扣逻辑：
     * 1. 现金账户直接抵扣
     * 2. 积分账户根据配置的抵扣比例和最大使用积分进行抵扣
     *
     * @param reqJson 请求参数JSON对象
     * @return 账户抵扣总金额
     */
    private double judgeAccount(JSONObject reqJson) {
        // 检查是否包含抵扣金额参数
        if (!reqJson.containsKey("deductionAmount")) {
            reqJson.put("acctAmount", 0.0);
            return 0.0;
        }

        double deductionAmount = reqJson.getDouble("deductionAmount");
        if (deductionAmount <= 0) {
            reqJson.put("acctAmount", 0.0);
            return 0.0;
        }

        // 检查是否选择了用户账户
        if (!reqJson.containsKey("selectUserAccount")) {
            reqJson.put("acctAmount", 0.0);
            return 0.0;
        }

        JSONArray selectUserAccount = reqJson.getJSONArray("selectUserAccount");
        if (selectUserAccount == null || selectUserAccount.size() < 1) {
            reqJson.put("acctAmount", 0.0);
            return 0.0;
        }

        // 提取账户ID列表
        List<String> acctIds = new ArrayList<>();
        for (int userAccountIndex = 0; userAccountIndex < selectUserAccount.size(); userAccountIndex++) {
            acctIds.add(selectUserAccount.getJSONObject(userAccountIndex).getString("acctId"));
        }

        // 查询账户信息
        AccountDto accountDto = new AccountDto();
        accountDto.setAcctIds(acctIds.toArray(new String[acctIds.size()]));
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);

        if (ListUtil.isNull(accountDtos)) {
            reqJson.put("acctAmount", 0.0);
            return 0.0;
        }

        BigDecimal money = new BigDecimal(0);
        BigDecimal cashMoney = new BigDecimal("0.00"); // 抵扣的现金账户金额
        BigDecimal pointsMoney = new BigDecimal(0.00); // 抵扣的积分数
        BigDecimal pointsMoneyNow = new BigDecimal(0.00); // 积分转换为现金的实际金额
        BigDecimal totalAccountAmount = new BigDecimal(0.00);

        // 获取应收金额
        BigDecimal dedAmount = new BigDecimal("0.00");
        if (reqJson.containsKey("receivedMoney") && !StringUtil.isEmpty(reqJson.getString("receivedMoney"))) {
            dedAmount = new BigDecimal(reqJson.getString("receivedMoney"));
        } else {
            dedAmount = new BigDecimal(reqJson.getString("deductionAmount"));
        }

        // 遍历所有账户进行抵扣计算
        for (AccountDto tmpAccountDto : accountDtos) {
            int compare = dedAmount.compareTo(BigDecimal.ZERO);
            // 现金账户抵扣逻辑
            if (AccountDto.ACCT_TYPE_CASH.equals(tmpAccountDto.getAcctType()) && compare > 0) {
                // 账户金额
                BigDecimal amount = new BigDecimal(tmpAccountDto.getAmount());
                int flag = amount.compareTo(dedAmount);
                if (flag == 1 || flag == 0) {
                    // 现金账户大于等于应收金额，用应收金额全额抵扣
                    cashMoney = dedAmount;
                    dedAmount = new BigDecimal("0.0");
                }
                if (flag == -1) {
                    // 现金账户小于应收金额，用现金账户全额抵扣
                    cashMoney = amount;
                    dedAmount = dedAmount.subtract(amount);
                }
                money = money.add(cashMoney);
            }
        }

        // 设置抵扣结果到请求参数中
        reqJson.put("acctAmount", money.doubleValue());
        reqJson.put("deductionAmount", "0"); // 这里减免金额 改成0 因为减免金额要落到账户上
        reqJson.put("cashMoney", cashMoney); // 现金抵扣金额
        reqJson.put("pointsMoney", pointsMoney); // 积分抵扣扣除数
        reqJson.put("pointsMoneyNow", pointsMoneyNow); // 积分抵扣实际现金数
        reqJson.put("selectUserAccount", BeanConvertUtil.beanCovertJSONArray(accountDtos));
        return money.doubleValue();
    }

    /**
     * 检查并计算优惠券抵扣金额
     *
     * @param paramObj 请求参数JSON对象
     * @return 优惠券抵扣总金额
     */
    private double checkCouponUser(JSONObject paramObj) {
        JSONArray couponList = paramObj.getJSONArray("couponList");
        BigDecimal couponPrice = new BigDecimal(0.0);
        List<String> couponIds = new ArrayList<String>();

        // 检查是否有优惠券
        if (couponList == null || couponList.size() < 1) {
            paramObj.put("couponPrice", couponPrice.doubleValue());
            paramObj.put("couponUserDtos", new JSONArray()); // 返回空数组
            return couponPrice.doubleValue();
        }

        // 提取优惠券ID列表
        for (int couponIndex = 0; couponIndex < couponList.size(); couponIndex++) {
            couponIds.add(couponList.getJSONObject(couponIndex).getString("couponId"));
        }

        // 查询优惠券信息
        CouponUserDto couponUserDto = new CouponUserDto();
        couponUserDto.setCouponIds(couponIds.toArray(new String[couponIds.size()]));
        List<CouponUserDto> couponUserDtos = couponUserV1InnerServiceSMOImpl.queryCouponUsers(couponUserDto);

        if (couponUserDtos == null || couponUserDtos.size() < 1) {
            paramObj.put("couponPrice", couponPrice.doubleValue());
            return couponPrice.doubleValue();
        }

        // 计算有效优惠券的总抵扣金额
        for (CouponUserDto couponUser : couponUserDtos) {
            // 只计算未过期的优惠券
            if (couponUser.getEndTime().compareTo(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B)) >= 0) {
                couponPrice = couponPrice.add(new BigDecimal(Double.parseDouble(couponUser.getActualPrice())));
            }
        }

        paramObj.put("couponPrice", couponPrice.doubleValue());
        paramObj.put("couponUserDtos", BeanConvertUtil.beanCovertJSONArray(couponUserDtos));
        return couponPrice.doubleValue();
    }

    /**
     * 计算折扣金额
     * <p>
     * 根据费用详情和周期计算适用的折扣金额
     *
     * @param paramObj 请求参数JSON对象
     * @return 折扣总金额
     * @throws CmdException 当日期解析失败时抛出
     */
    private double judgeDiscount(JSONObject paramObj) {
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setCommunityId(paramObj.getString("communityId"));
        feeDetailDto.setFeeId(paramObj.getString("feeId"));
        feeDetailDto.setCycles(paramObj.getString("cycles"));
        feeDetailDto.setPayerObjId(paramObj.getString("payerObjId"));
        feeDetailDto.setPayerObjType(paramObj.getString("payerObjType"));

        // 设置缴费开始时间为当前缴费周期的结束时间
        String endTime = paramObj.getString("endTime");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            feeDetailDto.setStartTime(simpleDateFormat.parse(endTime));
        } catch (ParseException e) {
            throw new CmdException(e.getLocalizedMessage());
        }

        feeDetailDto.setRow(20);
        feeDetailDto.setPage(1);

        // 计算折扣
        List<ComputeDiscountDto> computeDiscountDtos = feeDiscountInnerServiceSMOImpl.computeDiscount(feeDetailDto);

        if (ListUtil.isNull(computeDiscountDtos)) {
            paramObj.put("discountPrice", 0.0);
            return 0.0;
        }

        // 累加所有折扣金额
        BigDecimal discountPrice = new BigDecimal(0);
        for (ComputeDiscountDto computeDiscountDto : computeDiscountDtos) {
            discountPrice = discountPrice.add(new BigDecimal(computeDiscountDto.getDiscountPrice()));
        }

        paramObj.put("discountPrice", discountPrice.doubleValue());
        paramObj.put("discountPrice", discountPrice.doubleValue());
        paramObj.put("computeDiscountDtos", BeanConvertUtil.beanCovertJSONArray(computeDiscountDtos));
        return discountPrice.doubleValue();
    }
}