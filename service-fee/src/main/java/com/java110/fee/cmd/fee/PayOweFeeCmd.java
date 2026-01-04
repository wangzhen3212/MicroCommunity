/**
 * 欠费缴费命令处理类
 * 
 * 该类负责处理物业费用欠费缴费业务，包括费用验证、缴费明细生成、费用信息更新、
 * 账户扣款、收据生成等核心业务流程。支持多种支付方式（微信支付、账户扣款等），
 * 并处理相关的业务联动（如车辆延期、报修单更新等）。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.factory.Java110TransactionalFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.account.AccountDto;
import com.java110.dto.app.AppDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.FeeReceiptDetailDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.user.UserDto;
import com.java110.fee.bmo.fee.IFinishFeeNotify;
import com.java110.fee.smo.impl.FeeReceiptInnerServiceSMOImpl;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.community.*;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.account.AccountDetailPo;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.fee.FeeReceiptPo;
import com.java110.po.fee.FeeReceiptDetailPo;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

@Java110Cmd(serviceCode = "fee.payOweFee")
public class PayOweFeeCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(PayOweFeeCmd.class);
    
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IFeeReceiptInnerServiceSMO feeReceiptInnerServiceSMOImpl;

    @Autowired
    private IFeeReceiptDetailInnerServiceSMO feeReceiptDetailInnerServiceSMOImpl;


    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;


    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IFinishFeeNotify finishFeeNotifyImpl;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;


    /**
     * 验证请求参数的有效性
     * 
     * 验证费用信息、时间范围、金额等关键参数，检查是否存在重复缴费，
     * 并处理账户扣款相关的验证逻辑。
     *
     * @param event   命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 验证请求报文中必须包含费用信息
        Assert.hasKey(reqJson, "fees", "请求报文中未包含费用信息");

        JSONArray fees = reqJson.getJSONArray("fees");

        JSONObject feeObject = null;

        // 遍历所有费用项进行详细验证
        for (int feeIndex = 0; feeIndex < fees.size(); feeIndex++) {
            feeObject = fees.getJSONObject(feeIndex);
            // 验证费用基本信息完整性
            Assert.hasKeyAndValue(feeObject, "feeId", "未包含费用信息");
            Assert.hasKeyAndValue(feeObject, "startTime", "未包含开始时间");
            Assert.hasKeyAndValue(feeObject, "endTime", "未包含结束时间");
            Assert.hasKeyAndValue(feeObject, "receivedAmount", "未包含实收金额");

            // 查询费用详细信息用于后续验证
            FeeDto feeDto = new FeeDto();
            feeDto.setFeeId(feeObject.getString("feeId"));
            feeDto.setCommunityId(feeObject.getString("communityId"));
            Date pageEndTime = null;
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            if (feeDtos == null || feeDtos.size() != 1) {
                throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "查询费用信息失败，未查到数据或查到多条数据");
            }
            feeDto = feeDtos.get(0);
            feeObject.put("feeDto", feeDto);
            
            // 对于周期性费用，检查是否存在重复缴费
            if (!FeeDto.FEE_FLAG_CYCLE.equals(feeDto.getFeeFlag())) {
                continue;
            }

            pageEndTime = DateUtil.getDateFromStringB(feeObject.getString("endTime"));
            // 检查缴费结束时间是否小于等于当前费用结束时间，防止重复缴费
            if (pageEndTime.getTime() <= feeDto.getEndTime().getTime()) {
                throw new IllegalArgumentException("可能存在重复缴费，请刷新页面重新缴费");
            }
        }

        // 处理账户扣款相关的验证逻辑
        ifHasAccount(reqJson, fees);
    }


    /**
     * 执行欠费缴费命令
     * 
     * 处理欠费缴费的核心业务逻辑，包括生成收据、创建缴费明细、更新费用信息、
     * 执行账户扣款和相关业务联动操作。
     *
     * @param event          命令事件
     * @param dataFlowContext 数据流上下文
     * @param paramObj       参数对象
     * @throws CmdException  当业务处理失败时抛出异常
     * @throws ParseException 当日期解析失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext dataFlowContext, JSONObject paramObj) throws CmdException, ParseException {
        logger.info("======欠费缴费返回======：" + JSONArray.toJSONString(paramObj));
        
        // 获取当前操作用户信息
        String userId = CmdContextUtils.getUserId(dataFlowContext);
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户未登录");
        userDto = userDtos.get(0);
        String payOrderId = paramObj.getString("payOrderId");

        JSONArray fees = paramObj.getJSONArray("fees");
        JSONObject feeObj = null;
        String appId = dataFlowContext.getReqHeaders().get("app-id");


        // 生成收据编号
        String receiptCode = feeReceiptInnerServiceSMOImpl.generatorReceiptCode(paramObj.getString("communityId"));
        // 存储缴费明细ID的集合
        JSONArray details = new JSONArray();

        // 遍历处理每个费用项
        for (int feeIndex = 0; feeIndex < fees.size(); feeIndex++) {
            feeObj = fees.getJSONObject(feeIndex);
            feeObj.put("communityId", paramObj.getString("communityId"));
            if (paramObj.containsKey("oId")) {
                feeObj.put("oId", paramObj.getString("oId"));
            }
            String remark = paramObj.getString("remark");
            feeObj.put("remark", remark);
            
            // 根据支付方式设置费率参数和备注信息
            if (!feeObj.containsKey("primeRate") && AppDto.OWNER_WECHAT_PAY.equals(appId)) {  //微信公众号支付
                feeObj.put("primeRate", "5");
                feeObj.put("remark", "线上公众号支付");
            }
            if (!feeObj.containsKey("primeRate")) {
                feeObj.put("primeRate", "6");
            }
            logger.info("======支付方式======：" + appId + "+======+" + feeObj.containsKey("primeRate") + "======:" + JSONArray.toJSONString(dataFlowContext));
            
            // 根据不同的微信支付方式设置相应的备注
            if (AppDto.OWNER_WECHAT_PAY.equals(appId)
                    && FeeDetailDto.PRIME_REATE_WECHAT.equals(feeObj.getString("primeRate"))) {  //微信支付（欠费缴费无法区分小程序还是微信公众号）
                feeObj.put("remark", "线上公众号支付");
            } else if (AppDto.OWNER_WECHAT_PAY.equals(appId)
                    && FeeDetailDto.PRIME_REATE_WECHAT_APP.equals(feeObj.getString("primeRate"))) {
                feeObj.put("remark", "线上小程序支付");
            }
            feeObj.put("state", "1400");
            
            // 添加欠费缴费明细
            addOweFeeDetail(feeObj, details, userDto, receiptCode, payOrderId);
            // 修改欠费信息
            modifyOweFee(feeObj, dataFlowContext);

            // 执行账户扣款
            finishFeeNotifyImpl.withholdAccount(feeObj, feeObj.getString("feeId"), feeObj.getString("communityId"));

            // 更新车辆结束时间
            finishFeeNotifyImpl.updateCarEndTime(feeObj.getString("feeId"), feeObj.getString("communityId"));

            // 更新报修单状态
            finishFeeNotifyImpl.updateRepair(feeObj.getString("feeId"), feeObj.getString("communityId"), feeObj.getString("receivedAmount"));

            // 更新租金延期房屋结束时间
            finishFeeNotifyImpl.updateRoomEndTime(feeObj.getString("feeId"), feeObj.getString("communityId"));
        }


        JSONObject data = new JSONObject();
        data.put("details", details);

        dataFlowContext.setResponseEntity(ResultVo.createResponseEntity(data));
    }


    /**
     * 修改费用信息
     * 
     * 根据缴费信息更新费用的开始时间、结束时间、状态等信息，
     * 对于一次性费用或达到最大结束时间的费用标记为已完成状态。
     *
     * @param paramInJson     接口调用方传入的参数
     * @param dataFlowContext 数据上下文
     */
    public void modifyOweFee(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {

        JSONObject businessFee = new JSONObject();
        FeeDto feeInfo = (FeeDto) paramInJson.get("feeInfo");
        Map feeMap = BeanConvertUtil.beanCovertMap(feeInfo);
        // 设置费用时间信息
        feeMap.put("startTime", DateUtil.getFormatTimeString(feeInfo.getStartTime(), DateUtil.DATE_FORMATE_STRING_A));
        feeMap.put("endTime", DateUtil.getNextSecTime(paramInJson.getString("endTime")));
        feeMap.put("cycles", paramInJson.getString("cycles"));
        feeMap.put("configEndTime", feeInfo.getConfigEndTime());
        
        // 对于一次性费用，缴费后标记为完成状态
        if (FeeDto.FEE_FLAG_ONCE.equals(feeInfo.getFeeFlag())) { //缴费结束
            feeMap.put("state", FeeDto.STATE_FINISH);
        }
        
        // 检查费用是否已达到最大结束时间
        Date maxEndTime = feeInfo.getMaxEndTime();
        if (maxEndTime == null) {
            maxEndTime = feeInfo.getConfigEndTime();
        }
        if (maxEndTime != null) { //这里数据问题的情况下
            Date endTime = DateUtil.getDateFromStringA(paramInJson.getString("endTime"));
            // 如果缴费结束时间超过最大结束时间，标记为完成状态
            if (endTime.getTime() >= maxEndTime.getTime()) {
                feeMap.put("state", FeeDto.STATE_FINISH);
            }
        }

        businessFee.putAll(feeMap);
        PayFeePo payFeePo = BeanConvertUtil.covertBean(businessFee, PayFeePo.class);
        int flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
        if (flag < 1) {
            throw new CmdException("修改失败");
        }
    }

    /**
     * 添加欠费缴费明细
     * 
     * 创建缴费明细记录，计算缴费周期，设置支付信息，并保存到数据库。
     *
     * @param paramInJson   缴费参数信息
     * @param detailIds     用于存储生成的明细ID集合
     * @param userDto       操作用户信息
     * @param receiptCode   收据编号
     * @param payOrderId    支付订单ID
     */
    public void addOweFeeDetail(JSONObject paramInJson,
                                JSONArray detailIds,
                                UserDto userDto,
                                String receiptCode,
                                String payOrderId) {
        JSONObject businessFeeDetail = new JSONObject();
        businessFeeDetail.putAll(paramInJson);
        // 生成明细ID
        businessFeeDetail.put("detailId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
        businessFeeDetail.put("primeRate", paramInJson.getString("primeRate"));
        FeeDto feeDto = (FeeDto) paramInJson.get("feeDto");


        businessFeeDetail.put("startTime", paramInJson.getString("startTime"));
        BigDecimal cycles = null;
        // 计算费用单价
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        BigDecimal feePrice = new BigDecimal(feePriceAll.get("feePrice").toString());
        Date endTime = feeDto.getEndTime();
        Calendar endCalender = Calendar.getInstance();
        endCalender.setTime(endTime);

        // 设置金额信息
        if (!paramInJson.containsKey("receivableAmount") || StringUtil.isEmpty(paramInJson.getString("receivableAmount"))) {
            paramInJson.put("receivableAmount", paramInJson.getString("receivedAmount"));
        }
        BigDecimal receivableAmount = new BigDecimal(Double.parseDouble(paramInJson.getString("receivableAmount")));
        // 计算缴费周期（实收金额除以单价）
        cycles = receivableAmount.divide(feePrice, 4, BigDecimal.ROUND_HALF_EVEN);
        businessFeeDetail.put("cycles", receivableAmount.divide(feePrice, 2, BigDecimal.ROUND_HALF_UP).doubleValue());


        businessFeeDetail.put("receivableAmount", paramInJson.getString("receivableAmount"));
        businessFeeDetail.put("receivedAmount", paramInJson.getString("receivedAmount"));
        businessFeeDetail.put("payableAmount", paramInJson.getString("receivedAmount"));
        businessFeeDetail.put("endTime", paramInJson.getString("endTime"));
        paramInJson.put("feeInfo", feeDto);
        paramInJson.put("cycles", cycles.doubleValue());
        PayFeeDetailPo payFeeDetailPo = BeanConvertUtil.covertBean(businessFeeDetail, PayFeeDetailPo.class);
        
        // 设置支付订单ID
        if (paramInJson.containsKey("oId")) {
            payFeeDetailPo.setPayOrderId(paramInJson.getString("oId"));
        } else {
            String oId = Java110TransactionalFactory.getOId();
            if (StringUtil.isEmpty(oId)) {
                oId = payFeeDetailPo.getDetailId();
            }
            payFeeDetailPo.setPayOrderId(oId);

        }

        // 如果使用扫码枪支付，使用传入的支付订单ID
        if (!StringUtil.isEmpty(payOrderId)) {
            payFeeDetailPo.setPayOrderId(payOrderId);
        }
        // 设置账户扣款金额
        if (paramInJson.containsKey("accountAmount")) {
            payFeeDetailPo.setAcctAmount(paramInJson.getString("accountAmount"));
        }

        // 设置收银员信息
        payFeeDetailPo.setCashierId(userDto.getUserId());
        payFeeDetailPo.setCashierName(userDto.getName());
        payFeeDetailPo.setOpenInvoice("N");
        // 缓存收据编号，用于后续收据生成
        CommonCache.setValue(payFeeDetailPo.getDetailId() + CommonCache.RECEIPT_CODE, receiptCode, CommonCache.DEFAULT_EXPIRETIME_TWO_MIN);
        int flag = payFeeDetailV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);

        if (flag < 1) {
            throw new CmdException("保存明细失败");
        }

        paramInJson.put("detailId", businessFeeDetail.getString("detailId"));
        detailIds.add(businessFeeDetail.getString("detailId"));
    }


    /**
     * 处理账户扣款逻辑
     * 
     * 检查账户余额是否充足，并根据账户金额分摊到各个费用项中，
     * 更新每个费用项的实收金额和账户扣款金额。
     *
     * @param reqJson 请求JSON对象
     * @param fees    费用数组
     */
    private void ifHasAccount(JSONObject reqJson, JSONArray fees) {
        // 检查是否包含账户扣款信息
        if (!reqJson.containsKey("accountAmount")) {
            return;
        }
        double accountAmount = reqJson.getDouble("accountAmount");
        if (accountAmount <= 0) {
            return;
        }

        // 验证账户ID
        Assert.hasKeyAndValue(reqJson, "acctId", "未包含账户ID");
        String acctId = reqJson.getString("acctId");
        // 查询账户信息并验证余额
        AccountDto accountDto = new AccountDto();
        accountDto.setAcctId(acctId);
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);

        Assert.listOnlyOne(accountDtos, "账户不存在");

        // 检查账户余额是否充足
        if (Double.parseDouble(accountDtos.get(0).getAmount()) < accountAmount) {
            throw new CmdException("账户余额不足");
        }

        BigDecimal accountAmountDec = null;

        // 将账户金额分摊到各个费用项中
        JSONObject feeObject = null;
        double receivedAmount = 0.0;
        BigDecimal receivedAmountDec = null;
        for (int feeIndex = 0; feeIndex < fees.size(); feeIndex++) {
            if (accountAmount == 0) {
                continue;
            }
            accountAmountDec = new BigDecimal(accountAmount);
            feeObject = fees.getJSONObject(feeIndex);
            receivedAmount = feeObject.getDouble("receivedAmount");
            receivedAmountDec = new BigDecimal(receivedAmount);
            
            // 如果账户金额足够支付当前费用
            if (receivedAmount >= accountAmount) {
                receivedAmountDec = receivedAmountDec.subtract(accountAmountDec).setScale(2, BigDecimal.ROUND_HALF_UP);
                feeObject.put("receivedAmount", receivedAmountDec.doubleValue());
                feeObject.put("accountAmount", accountAmount);
                feeObject.put("acctId", acctId);
                accountAmount = 0.00;
                continue;
            }

            // 如果账户金额不足以支付当前费用，全部使用账户支付
            feeObject.put("receivedAmount", "0");
            feeObject.put("accountAmount", receivedAmount);
            feeObject.put("acctId", acctId);

            accountAmountDec = accountAmountDec.subtract(receivedAmountDec).setScale(2, BigDecimal.ROUND_HALF_UP);
            accountAmount = accountAmountDec.doubleValue();
        }


    }
}