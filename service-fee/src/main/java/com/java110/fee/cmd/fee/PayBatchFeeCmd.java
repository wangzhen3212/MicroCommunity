/**
 * 批量缴费命令处理类
 * 
 * 该类负责处理批量缴费业务，包括参数验证、费用计算、账户扣款、缴费记录生成等操作
 * 支持多种缴费方式：自定义金额缴费、自定义结束时间缴费、自定义周期缴费
 * 同时支持账户余额抵扣功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.factory.Java110TransactionalFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.account.AccountDto;
import com.java110.dto.fee.*;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.user.UserDto;
import com.java110.fee.bmo.fee.IFinishFeeNotify;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.community.*;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.FeeFlagTypeConstant;
import com.java110.utils.constant.FeeConfigConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

@Java110Cmd(serviceCode = "fee.payBatchFee")
public class PayBatchFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(PayBatchFeeCmd.class);

    // 费用相关服务
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailNewV1InnerServiceSMOImpl;

    // 费用配置服务
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    // 费用计算服务
    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    // 用户服务
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    // 收据服务
    @Autowired
    private IFeeReceiptInnerServiceSMO feeReceiptInnerServiceSMOImpl;

    // 缴费完成通知服务
    @Autowired
    private IFinishFeeNotify finishFeeNotifyImpl;

    // 账户服务
    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括：
     * 1. 必填字段验证
     * 2. 费用状态验证
     * 3. 缴费周期验证
     * 4. 用户登录验证
     * 5. 账户余额验证
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证小区ID必填
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");

        // 验证费用列表必填
        if (!reqJson.containsKey("fees")) {
            throw new CmdException("未包含费用");
        }

        JSONArray fees = reqJson.getJSONArray("fees");

        if (ListUtil.isNull(fees)) {
            throw new CmdException("未包含费用");
        }

        // 查询并验证用户登录信息
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户未登录");

        JSONObject paramInObj = null;
        List<PayFeeDataDto> feeDataDtos = new ArrayList<>();
        PayFeeDataDto payFeeDataDto = null;
        
        // 遍历所有费用项进行验证
        for (int feeIndex = 0; feeIndex < fees.size(); feeIndex++) {
            paramInObj = fees.getJSONObject(feeIndex);
            // 验证费用项必填字段
            Assert.hasKeyAndValue(paramInObj, "feeId", "未包含费用ID");
            Assert.hasKeyAndValue(paramInObj, "tempCycle", "未包含缴费周期类型");
            Assert.hasKeyAndValue(paramInObj, "primeRate", "未包含缴费缴费方式");
            Assert.hasKeyAndValue(paramInObj, "receivedAmount", "未包含实收金额");
            
            // 验证费用状态，已结束的费用不能再次缴费
            FeeDto feeDto = new FeeDto();
            feeDto.setFeeId(paramInObj.getString("feeId"));
            feeDto.setCommunityId(reqJson.getString("communityId"));
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            Assert.listOnlyOne(feeDtos, "传入费用ID错误");
            if (FeeDto.STATE_FINISH.equals(feeDtos.get(0).getState())) {
                throw new IllegalArgumentException("收费已经结束，不能再缴费");
            }
            
            Date endTime = feeDtos.get(0).getEndTime();
            // 查询费用配置信息
            FeeConfigDto feeConfigDto = new FeeConfigDto();
            feeConfigDto.setConfigId(feeDtos.get(0).getConfigId());
            feeConfigDto.setCommunityId(paramInObj.getString("communityId"));
            List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
            if (ListUtil.isNull(feeConfigDtos)) {
                throw new IllegalArgumentException("费用项不存在");
            }
            
            Date maxEndTime = feeDtos.get(0).getDeadlineTime();
            // 周期性费用处理
            if (maxEndTime == null) {
                maxEndTime = DateUtil.getDateFromStringA(feeConfigDtos.get(0).getEndTime());
            }

            // 验证缴费周期是否超过最大结束时间
            if (maxEndTime != null && endTime != null && !FeeDto.FEE_FLAG_ONCE.equals(feeDtos.get(0).getFeeFlag())) {
                Date newDate = DateUtil.stepMonth(endTime, paramInObj.getDouble("cycles").intValue());
                if (newDate.getTime() > maxEndTime.getTime()) {
                    throw new CmdException(feeConfigDtos.get(0).getFeeName() + "缴费周期超过 缴费结束时间,请用按结束时间方式缴费");
                }
            }
            
            // 封装费用数据
            payFeeDataDto = new PayFeeDataDto();
            payFeeDataDto.setFeeDto(feeDtos.get(0));
            payFeeDataDto.setFeeConfigDto(feeConfigDtos.get(0));
            payFeeDataDto.setCommunityId(reqJson.getString("communityId"));
            payFeeDataDto.setTempCycle(paramInObj.getString("tempCycle"));
            payFeeDataDto.setCustEndTime(paramInObj.getString("custEndTime"));
            payFeeDataDto.setPrimeRate(paramInObj.getString("primeRate"));
            payFeeDataDto.setRemark(paramInObj.getString("remark"));
            payFeeDataDto.setReceivedAmount(paramInObj.getString("receivedAmount"));
            payFeeDataDto.setCashierId(userDtos.get(0).getUserId());
            payFeeDataDto.setCashierName(userDtos.get(0).getName());
            payFeeDataDto.setFeeId(paramInObj.getString("feeId"));
            payFeeDataDto.setCycles(paramInObj.getString("cycles"));
            feeDataDtos.add(payFeeDataDto);
        }

        // 处理账户抵扣逻辑
        ifHasAccount(reqJson, feeDataDtos);

        // 将封装好的费用数据放入请求参数中供后续使用
        reqJson.put("feeDataDtos", feeDataDtos);
    }

    /**
     * 执行批量缴费命令
     * 
     * 主要功能：
     * 1. 生成缴费订单和收据编号
     * 2. 对每个费用项执行缴费操作
     * 3. 使用分布式锁保证并发安全
     * 4. 更新费用状态和相关业务数据
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当缴费操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 获取封装好的费用数据
        List<PayFeeDataDto> feeDataDtos = (List<PayFeeDataDto>) reqJson.get("feeDataDtos");
        String communityId = reqJson.getString("communityId");
        String payOrderId = reqJson.getString("payOrderId");

        // 生成收据编号
        String receiptCode = feeReceiptInnerServiceSMOImpl.generatorReceiptCode(communityId);
        JSONArray details = new JSONArray();
        // 获取订单ID
        String oId = Java110TransactionalFactory.getOId();

        // 遍历所有费用项进行缴费处理
        for (PayFeeDataDto payFeeDataDto : feeDataDtos) {
            payFeeDataDto.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
            payFeeDataDto.setPayOrderId(oId);
            // 如果使用扫码枪支付，使用传入的支付订单ID
            if (!StringUtil.isEmpty(payOrderId)) {
                payFeeDataDto.setPayOrderId(payOrderId);
            }

            // 使用分布式锁保证同一费用的并发安全
            String requestId = DistributedLock.getLockUUID();
            String key = this.getClass().getSimpleName() + payFeeDataDto.getFeeId();
            DistributedLock.waitGetDistributedLock(key, requestId);
            try {
                // 执行单个费用项的缴费操作
                doPayFee(payFeeDataDto, receiptCode);
            } finally {
                DistributedLock.releaseDistributedLock(key, requestId);
            }
            details.add(payFeeDataDto.getDetailId());
        }

        // 构建响应数据
        JSONObject data = new JSONObject();
        data.put("details", details);

        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 执行单个费用项的缴费操作
     * 
     * 主要步骤：
     * 1. 设置缴费明细基本信息
     * 2. 计算结束时间、周期和金额
     * 3. 保存缴费明细记录
     * 4. 更新费用状态
     * 5. 执行相关业务通知
     * 
     * @param payFeeDataDto 缴费数据对象
     * @param receiptCode 收据编号
     */
    private void doPayFee(PayFeeDataDto payFeeDataDto, String receiptCode) {

        // 创建缴费明细对象并设置基本信息
        PayFeeDetailPo payFeeDetailPo = payFeeDataDto;
        payFeeDetailPo.setOpenInvoice("N"); // 默认不开具发票
        payFeeDetailPo.setState(FeeDetailDto.STATE_NORMAL); // 设置状态为正常
        payFeeDetailPo.setStartTime(DateUtil.getFormatTimeStringA(payFeeDataDto.getFeeDto().getEndTime())); // 开始时间为原费用结束时间

        // 计算结束时间、缴费周期和应收金额
        computeEndTimeCycleAmount(payFeeDataDto, payFeeDetailPo);
        payFeeDetailPo.setAcctAmount(payFeeDataDto.getAccountAmount()+"");

        // 缓存收据编号，有效期2分钟
        CommonCache.setValue(payFeeDetailPo.getDetailId() + CommonCache.RECEIPT_CODE, receiptCode, CommonCache.DEFAULT_EXPIRETIME_TWO_MIN);
        
        // 保存缴费明细记录
        int flag = payFeeDetailNewV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);
        if (flag < 1) {
            throw new CmdException("缴费失败");
        }

        // 更新费用主表信息
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setFeeId(payFeeDataDto.getFeeId());
        payFeePo.setEndTime(DateUtil.getNextSecTime(payFeeDetailPo.getEndTime())); // 结束时间加1秒避免重复
        payFeePo.setState(FeeDto.STATE_FINISH); // 设置为完成状态

        Date maxTime = payFeeDataDto.getFeeDto().getDeadlineTime();

        if (maxTime == null) {
            maxTime = DateUtil.getDateFromStringA(payFeeDataDto.getFeeConfigDto().getEndTime());
        }
        
        // 对于非一次性费用，如果缴费结束时间接近最大结束时间，保持进行中状态
        if (!FeeDto.FEE_FLAG_ONCE.equals(payFeeDataDto.getFeeDto().getFeeFlag()) && maxTime != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(maxTime);
            calendar.add(Calendar.DAY_OF_MONTH, -5); // 提前5天
            maxTime = calendar.getTime();
            if (maxTime.after(DateUtil.getDateFromStringA(payFeeDetailPo.getEndTime()))) {
                payFeePo.setState(FeeDto.STATE_DOING); // 设置为进行中状态
            }
        }

        flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
        if (flag < 1) {
            throw new CmdException("缴费失败");
        }

        // 执行账户扣款操作
        finishFeeNotifyImpl.withholdAccount(payFeeDataDto, payFeeDataDto.getFeeId(), payFeeDataDto.getCommunityId());

        // 更新车辆结束时间
        finishFeeNotifyImpl.updateCarEndTime(payFeeDataDto.getFeeId(), payFeeDataDto.getCommunityId());

        // 更新报修单状态
        finishFeeNotifyImpl.updateRepair(payFeeDataDto.getFeeId(), payFeeDataDto.getCommunityId(), payFeeDetailPo.getReceivedAmount());

        // 更新房屋租赁结束时间
        finishFeeNotifyImpl.updateRoomEndTime(payFeeDataDto.getFeeId(), payFeeDataDto.getCommunityId());
    }

    /**
     * 计算缴费结束时间、周期和应收金额
     * 
     * 根据不同的缴费方式计算：
     * 1. 自定义金额缴费：根据金额计算周期和结束时间
     * 2. 自定义结束时间缴费：根据结束时间计算周期和金额
     * 3. 自定义周期缴费：根据周期计算结束时间和金额
     * 
     * @param payFeeDataDto 缴费数据对象
     * @param payFeeDetailPo 缴费明细对象，计算结果将设置到此对象中
     */
    private void computeEndTimeCycleAmount(PayFeeDataDto payFeeDataDto, PayFeeDetailPo payFeeDetailPo) {
        Date targetEndTime = null;
        BigDecimal cycles = null;
        String receivableAmount = "";
        
        // 获取费用单价
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(payFeeDataDto.getFeeDto());
        BigDecimal feePrice = new BigDecimal(feePriceAll.get("feePrice").toString());
        
        // 自定义金额缴费方式处理
        if (PayFeeDataDto.TEMP_CYCLE_CUSTOM_AMOUNT.equals(payFeeDataDto.getTempCycle())) {
            Date endTime = payFeeDataDto.getFeeDto().getEndTime();
            Calendar endCalender = Calendar.getInstance();
            endCalender.setTime(endTime);
            BigDecimal receivedAmount = new BigDecimal(Double.parseDouble(payFeeDataDto.getReceivedAmount()));
            
            // 考虑账户抵扣金额
            if (payFeeDataDto.getAccountAmount() > 0) {
                receivedAmount = receivedAmount.add(new BigDecimal(payFeeDataDto.getAccountAmount()));
            }
            
            // 根据金额计算周期数
            cycles = receivedAmount.divide(feePrice, 4, BigDecimal.ROUND_HALF_EVEN);
            targetEndTime = computeFeeSMOImpl.getTargetEndTime(cycles.doubleValue(), endCalender.getTime(),true);
            receivableAmount = payFeeDataDto.getReceivedAmount();
            
        // 自定义结束时间缴费方式处理
        } else if (PayFeeDataDto.TEMP_CYCLE_CUSTOM_END_TIME.equals(payFeeDataDto.getTempCycle())) {
            String custEndTime = payFeeDataDto.getCustEndTime();
            // 处理时间格式，如果没有时分秒则补充
            if (!custEndTime.contains(":")) {
                custEndTime += " 23:59:59";
            }
            targetEndTime = DateUtil.getDateFromStringA(custEndTime);
            BigDecimal receivedAmount1 = new BigDecimal(Double.parseDouble(payFeeDataDto.getReceivedAmount()));
            cycles = receivedAmount1.divide(feePrice, 4, BigDecimal.ROUND_HALF_EVEN);
            receivableAmount = payFeeDataDto.getReceivedAmount();

        // 自定义周期缴费方式处理
        } else {
            // 根据缴费周期计算结束时间
            targetEndTime = computeFeeSMOImpl.getFeeEndTimeByCycles(payFeeDataDto.getFeeDto(), payFeeDataDto.getCycles());
            cycles = new BigDecimal(Double.parseDouble(payFeeDataDto.getCycles()));
            double tmpReceivableAmount = cycles.multiply(feePrice).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
            receivableAmount = tmpReceivableAmount + "";
            
            // 出租递增费用特殊处理
            if (FeeConfigDto.COMPUTING_FORMULA_RANT_RATE.equals(payFeeDataDto.getFeeDto().getComputingFormula())) {
                computeFeeSMOImpl.dealRentRateCycle(payFeeDataDto.getFeeDto(), cycles.doubleValue());
                if (payFeeDataDto.getFeeDto().getOweFee() > 0) {
                    receivableAmount = payFeeDataDto.getFeeDto().getAmountOwed();
                }
            }
        }

        // 设置计算结果到缴费明细对象
        payFeeDetailPo.setCycles(cycles.doubleValue() + "");
        payFeeDetailPo.setEndTime(DateUtil.getFormatTimeStringA(targetEndTime));
        payFeeDetailPo.setReceivableAmount(receivableAmount);
        payFeeDetailPo.setPayableAmount(receivableAmount);
    }

    /**
     * 处理账户余额抵扣逻辑
     * 
     * 如果请求中包含账户抵扣信息，则：
     * 1. 验证账户是否存在和余额是否充足
     * 2. 按费用项顺序进行抵扣
     * 3. 更新每个费用项的实收金额和账户抵扣金额
     * 
     * @param reqJson 请求JSON对象
     * @param feeDataDtos 费用数据列表
     */
    private void ifHasAccount(JSONObject reqJson, List<PayFeeDataDto> feeDataDtos) {
        // 检查是否包含账户抵扣信息
        if (!reqJson.containsKey("accountAmount")) {
            return;
        }
        double accountAmount = reqJson.getDouble("accountAmount");
        if (accountAmount <= 0) {
            return;
        }

        // 验证账户ID必填
        Assert.hasKeyAndValue(reqJson, "acctId", "未包含账户ID");
        String acctId = reqJson.getString("acctId");
        
        // 查询账户信息并验证余额
        AccountDto accountDto = new AccountDto();
        accountDto.setAcctId(acctId);
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);

        Assert.listOnlyOne(accountDtos, "账户不存在");

        if (Double.parseDouble(accountDtos.get(0).getAmount()) < accountAmount) {
            throw new CmdException("账户余额不足");
        }

        BigDecimal accountAmountDec = null;
        double receivedAmount = 0.0;
        BigDecimal receivedAmountDec = null;
        
        // 遍历费用项进行账户抵扣
        for (PayFeeDataDto payFeeDataDto : feeDataDtos) {
            if (accountAmount == 0) {
                continue;
            }
            accountAmountDec = new BigDecimal(accountAmount);
            receivedAmount = Double.parseDouble(payFeeDataDto.getReceivedAmount());
            receivedAmountDec = new BigDecimal(receivedAmount);
            
            // 如果账户余额足够抵扣当前费用
            if (receivedAmount >= accountAmount) {
                receivedAmountDec = receivedAmountDec.subtract(accountAmountDec).setScale(2, BigDecimal.ROUND_HALF_UP);
                payFeeDataDto.setReceivedAmount(receivedAmountDec.doubleValue() + "");
                payFeeDataDto.setAccountAmount(accountAmount);
                payFeeDataDto.setAcctId(acctId);
                accountAmount = 0.00;
                continue;
            }

            // 如果账户余额不足以抵扣当前费用，全部抵扣
            payFeeDataDto.setReceivedAmount("0");
            payFeeDataDto.setAccountAmount(receivedAmount);
            payFeeDataDto.setAcctId(acctId);

            accountAmountDec = accountAmountDec.subtract(receivedAmountDec).setScale(2, BigDecimal.ROUND_HALF_UP);
            accountAmount = accountAmountDec.doubleValue();
        }
    }
}