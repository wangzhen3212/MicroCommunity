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
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.payFee.PayFeeDetailMonthDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.user.UserDto;
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
import com.java110.utils.constant.FeeConfigConstant;
import com.java110.utils.constant.FeeFlagTypeConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.lock.DistributedLock;
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

/**
 * 按月交费命令类
 * 
 * 该类负责处理按月缴费的业务逻辑，包括参数验证、费用计算、缴费明细生成、
 * 费用信息更新、车辆延期处理和报修单状态更新等功能。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.payMonthFee")
public class PayMonthFeeCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(PayMonthFeeCmd.class);
    
    @Autowired
    private IPayFeeDetailMonthInnerServiceSMO payFeeDetailMonthInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailNewV1InnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;
    
    @Autowired
    private IOwnerCarNewV1InnerServiceSMO ownerCarNewV1InnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolNewV1InnerServiceSMOImpl;
    
    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserNewV1InnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMO;

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMO;

    @Autowired
    private IFeeReceiptInnerServiceSMO feeReceiptInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 验证缴费请求的必填参数，包括小区ID、支付方式和缴费月份，
     * 并检查缴费月份是否连续，防止跳月缴费。
     *
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
        Assert.hasKeyAndValue(reqJson, "primeRate", "未包含支付方式");
        
        // 验证缴费月份参数
        if (!reqJson.containsKey("selectMonthIds")) {
            throw new CmdException("未包含缴费月份");
        }

        JSONArray selectMonthIds = reqJson.getJSONArray("selectMonthIds");
        if (selectMonthIds == null || selectMonthIds.size() < 1) {
            throw new CmdException("未包含缴费月份");
        }
        
        // 提取月份ID列表
        List<String> monthIds = new ArrayList<>();
        for (int monthIndex = 0; monthIndex < selectMonthIds.size(); monthIndex++) {
            monthIds.add(selectMonthIds.getString(monthIndex));
        }

        // 查询需要缴费的明细月份
        PayFeeDetailMonthDto payFeeDetailMonthDto = new PayFeeDetailMonthDto();
        payFeeDetailMonthDto.setMonthIds(monthIds.toArray(new String[monthIds.size()]));
        payFeeDetailMonthDto.setCommunityId(reqJson.getString("communityId"));
        payFeeDetailMonthDto.setDetailId("-1");
        List<PayFeeDetailMonthDto> payFeeDetailMonthDtos = payFeeDetailMonthInnerServiceSMOImpl.queryPagePayFeeDetailMonths(payFeeDetailMonthDto);

        if (payFeeDetailMonthDtos == null || payFeeDetailMonthDtos.size() < 1) {
            throw new CmdException("未包含缴费月份");
        }

        Map<String, List<PayFeeDetailMonthDto>> feeMonths = new HashMap<>();

        // 检查是否跳月缴费
        PayFeeDetailMonthDto feeDetailMonthDto = null;
        for (PayFeeDetailMonthDto tmpPayFeeDetailMonthDto : payFeeDetailMonthDtos) {
            if (!feeMonths.containsKey(tmpPayFeeDetailMonthDto.getFeeId())) {
                // 查询费用的所有月份明细
                feeDetailMonthDto = new PayFeeDetailMonthDto();
                feeDetailMonthDto.setFeeId(tmpPayFeeDetailMonthDto.getFeeId());
                feeDetailMonthDto.setDetailId("-1");
                feeDetailMonthDto.setCommunityId(reqJson.getString("communityId"));
                List<PayFeeDetailMonthDto> feeDetailMonthDtos = payFeeDetailMonthInnerServiceSMOImpl.queryPayFeeDetailMonths(feeDetailMonthDto);
                feeMonths.put(tmpPayFeeDetailMonthDto.getFeeId(), feeDetailMonthDtos);
            }
            // 单条费用校验是否跨月缴费
            validateOneMonthSkipPayFee(tmpPayFeeDetailMonthDto, feeMonths);
        }

        // 将缴费月份数据存入请求对象，供后续处理使用
        reqJson.put("payFeeDetailMonthDtos", payFeeDetailMonthDtos);
    }

    /**
     * 执行按月缴费命令
     * 
     * 处理按月缴费的核心业务逻辑，包括生成收据编号、处理每个月的费用、
     * 更新费用信息和返回处理结果。
     *
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取当前用户信息
        String userId = context.getReqHeaders().get("user-id");
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 生成收据编号
        String receiptCode = feeReceiptInnerServiceSMOImpl.generatorReceiptCode(reqJson.getString("communityId"));

        List<PayFeeDetailMonthDto> payFeeDetailMonthDtos = (List<PayFeeDetailMonthDto>) reqJson.get("payFeeDetailMonthDtos");

        Calendar createTimeCal = Calendar.getInstance();

        Map<String, FeeDto> feeDtoMap = new HashMap<>(); // 缓存费用信息，避免重复查询
        JSONArray details = new JSONArray(); // 存储处理成功的明细ID

        // 处理每个月的费用
        for (PayFeeDetailMonthDto payFeeDetailMonthDto : payFeeDetailMonthDtos) {
            // 费用只查一次提高效率
            if (!feeDtoMap.containsKey(payFeeDetailMonthDto.getFeeId())) {
                // 查询费用是否存在
                FeeDto feeDto = new FeeDto();
                feeDto.setFeeId(payFeeDetailMonthDto.getFeeId());
                feeDto.setCommunityId(payFeeDetailMonthDto.getCommunityId());
                List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
                if (feeDtos == null || feeDtos.size() != 1) {
                    throw new CmdException("费用不存在");
                }
                feeDtoMap.put(payFeeDetailMonthDto.getFeeId(), feeDtos.get(0));
            }
            
            // 递增创建时间，确保按时间顺序排序
            createTimeCal.add(Calendar.SECOND, 1);
            try {
                doMonthFee(payFeeDetailMonthDto, context, userDtos.get(0), reqJson, createTimeCal.getTime(), feeDtoMap, receiptCode);
            } catch (Exception e) {
                logger.error("处理异常", e);
                throw new CmdException(e.getMessage());
            }
            details.add(payFeeDetailMonthDto.getDetailId());
        }

        // 返回处理结果
        JSONObject data = new JSONObject();
        data.put("details", details);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 处理单个月份的费用
     * 
     * 处理单个费用月份的缴费逻辑，包括计算结束时间、生成缴费明细、
     * 更新费用信息、处理车辆延期和报修单状态。
     *
     * @param payFeeDetailMonthDto 缴费月份明细DTO
     * @param context 数据流上下文
     * @param userDto 用户DTO
     * @param reqJson 请求JSON对象
     * @param createTime 创建时间
     * @param feeDtoMap 费用DTO缓存Map
     * @param receiptCode 收据编号
     */
    private void doMonthFee(PayFeeDetailMonthDto payFeeDetailMonthDto, ICmdDataFlowContext context, UserDto userDto, JSONObject reqJson, Date createTime, Map<String, FeeDto> feeDtoMap, String receiptCode) {
        // 计算缴费结束时间（当前月份+1个月）
        Date startTime = DateUtil.getDateFromStringB(payFeeDetailMonthDto.getCurMonthTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.add(Calendar.MONTH, 1);
        String endTime = DateUtil.getFormatTimeStringB(calendar.getTime());

        // 获取订单ID
        String oId = Java110TransactionalFactory.getOId();

        // 开始分布式锁，防止并发操作
        PayFeePo payFeePo = null;
        String requestId = DistributedLock.getLockUUID();
        String key = this.getClass().getSimpleName() + payFeeDetailMonthDto.getFeeId();
        try {
            DistributedLock.waitGetDistributedLock(key, requestId);
            
            // 生成缴费明细
            PayFeeDetailPo payFeeDetailPo = addFeeDetail(payFeeDetailMonthDto, userDto, reqJson);
            if (StringUtil.isEmpty(oId)) {
                oId = payFeeDetailPo.getDetailId();
            }
            payFeeDetailPo.setPayOrderId(oId);
            payFeeDetailPo.setEndTime(endTime);
            // 按月交费时主要按时间顺序排序时能够整齐
            payFeeDetailPo.setCreateTime(DateUtil.getFormatTimeStringA(createTime));
            payFeeDetailPo.setOpenInvoice("N");

            // 缓存收据编号
            CommonCache.setValue(payFeeDetailPo.getDetailId()+CommonCache.RECEIPT_CODE, receiptCode, CommonCache.DEFAULT_EXPIRETIME_TWO_MIN);
            
            // 保存缴费明细
            int flag = payFeeDetailNewV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);
            if (flag < 1) {
                throw new CmdException("缴费失败");
            }
            
            // 更新费用信息
            payFeePo = modifyFee(payFeeDetailMonthDto, reqJson, feeDtoMap.get(payFeeDetailMonthDto.getFeeId()), endTime);
            flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
            if (flag < 1) {
                throw new CmdException("缴费失败");
            }

            // 将detailId返回出去
            payFeeDetailMonthDto.setDetailId(payFeeDetailPo.getDetailId());

        } finally {
            DistributedLock.releaseDistributedLock(key, requestId);
        }
        
        // 车辆延期处理
        updateOwnerCarEndTime(payFeePo, reqJson, feeDtoMap.get(payFeeDetailMonthDto.getFeeId()));

        // 处理报修单状态
        doDealRepairOrder(payFeeDetailMonthDto, feeDtoMap.get(payFeeDetailMonthDto.getFeeId()));
    }

    /**
     * 修改费用信息
     * 
     * 更新费用的结束时间，并处理周期性费用的状态变更逻辑。
     *
     * @param payFeeDetailMonthDto 缴费月份明细DTO
     * @param reqJson 请求JSON对象
     * @param feeInfo 费用信息DTO
     * @param endTime 结束时间
     * @return 更新后的费用PO对象
     */
    public PayFeePo modifyFee(PayFeeDetailMonthDto payFeeDetailMonthDto, JSONObject reqJson, FeeDto feeInfo, String endTime) {
        // 设置新的结束时间
        feeInfo.setEndTime(DateUtil.getDateFromStringB(endTime));
        Date maxEndTime = feeInfo.getDeadlineTime();
        
        // 对于周期性费用，使用配置结束时间作为最大结束时间
        if (FeeDto.FEE_FLAG_CYCLE.equals(feeInfo.getFeeFlag())) {
            maxEndTime = feeInfo.getConfigEndTime();
        }
        
        // 判断结束时间是否超过费用项结束时间（容错30天）
        if (maxEndTime != null) {
            if (feeInfo.getEndTime().getTime() - maxEndTime.getTime() > 30 * 24 * 60 * 60 * 1000L) {
                throw new IllegalArgumentException("缴费超过了费用项结束时间");
            }
        }

        PayFeePo payFeePo = BeanConvertUtil.covertBean(feeInfo, PayFeePo.class);
        
        // 为停车费单独处理，设置相关参数
        reqJson.put("carFeeEndTime", feeInfo.getEndTime());
        reqJson.put("carPayerObjType", feeInfo.getPayerObjType());
        reqJson.put("carPayerObjId", feeInfo.getPayerObjId());

        // 周期性收费、缴费后，到期日期在费用项终止日期后，则设置缴费状态结束
        if (!FeeFlagTypeConstant.ONETIME.equals(feeInfo.getFeeFlag())) {
            // 容错五天时间
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(maxEndTime);
            calendar.add(Calendar.DAY_OF_MONTH, -5);
            maxEndTime = calendar.getTime();
            
            if (feeInfo.getEndTime().after(maxEndTime)) {
                payFeePo.setState(FeeConfigConstant.END);
                payFeePo.setEndTime(DateUtil.getFormatTimeStringB(maxEndTime));
            }
        }

        return payFeePo;
    }

    /**
     * 添加缴费明细
     * 
     * 创建缴费明细记录，包含缴费金额、收费员信息、缴费周期等。
     *
     * @param payFeeDetailMonthDto 缴费月份明细DTO
     * @param userDto 用户DTO
     * @param reqJson 请求JSON对象
     * @return 缴费明细PO对象
     */
    public PayFeeDetailPo addFeeDetail(PayFeeDetailMonthDto payFeeDetailMonthDto, UserDto userDto, JSONObject reqJson) {
        PayFeeDetailPo feeDetailPo = new PayFeeDetailPo();
        feeDetailPo.setCashierName(userDto.getName());
        feeDetailPo.setReceivedAmount(payFeeDetailMonthDto.getReceivedAmount());
        feeDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
        feeDetailPo.setCashierId(userDto.getUserId());
        feeDetailPo.setFeeId(payFeeDetailMonthDto.getFeeId());
        feeDetailPo.setCommunityId(payFeeDetailMonthDto.getCommunityId());
        feeDetailPo.setCycles("1"); // 缴费周期为1个月
        feeDetailPo.setStartTime(payFeeDetailMonthDto.getCurMonthTime());
        feeDetailPo.setPayableAmount(payFeeDetailMonthDto.getReceivableAmount());
        feeDetailPo.setPrimeRate(reqJson.getString("primeRate")); // 支付方式
        feeDetailPo.setRemark(reqJson.getString("remark")); // 备注信息
        feeDetailPo.setState(FeeDetailDto.STATE_NORMAL); // 正常状态
        feeDetailPo.setReceivableAmount(payFeeDetailMonthDto.getReceivableAmount());
        return feeDetailPo;
    }

    /**
     * 更新车辆结束时间
     * 
     * 针对停车费用，更新车辆的结束时间，处理车位状态和车辆延期逻辑。
     *
     * @param payFeePo 费用PO对象
     * @param paramObj 参数对象
     * @param feeInfo 费用信息DTO
     */
    private void updateOwnerCarEndTime(PayFeePo payFeePo, JSONObject paramObj, FeeDto feeInfo) {
        int flag;
        // 只处理车辆类型的费用
        if (!FeeDto.PAYER_OBJ_TYPE_CAR.equals(feeInfo.getPayerObjType())) {
            return;
        }
        
        Date feeEndTime = DateUtil.getDateFromStringA(payFeePo.getEndTime());
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCommunityId(feeInfo.getCommunityId());
        ownerCarDto.setCarId(feeInfo.getPayerObjId());
        ownerCarDto.setCarTypeCd("1001"); // 业主车辆
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        Assert.listOnlyOne(ownerCarDtos, "查询业主错误！");
        
        // 获取车位ID和车辆状态
        String psId = ownerCarDtos.get(0).getPsId();
        String carState = ownerCarDtos.get(0).getState(); // 1001 正常状态，2002 欠费状态，3003 车位释放
        
        // 检查车位状态
        if (!StringUtil.isEmpty(psId) && "3003".equals(carState)) {
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setPsId(psId);
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            Assert.listOnlyOne(parkingSpaceDtos, "查询车位信息错误！");
            
            // 获取车位状态：出售 S，出租 H，空闲 F
            String state = parkingSpaceDtos.get(0).getState();
            if (!"F".equals(state)) {
                throw new IllegalArgumentException("车位已被使用，不能再缴费！");
            }
        }

        Calendar endTimeCalendar = null;
        // 处理每个车辆记录的延期
        for (OwnerCarDto tmpOwnerCarDto : ownerCarDtos) {
            // 后付费或者信用期车辆加一个月
            if (FeeConfigDto.PAYMENT_CD_AFTER.equals(feeInfo.getPaymentCd())
                    || OwnerCarDto.CAR_TYPE_CREDIT.equals(tmpOwnerCarDto.getCarType())) {
                endTimeCalendar = Calendar.getInstance();
                endTimeCalendar.setTime(feeEndTime);
                endTimeCalendar.add(Calendar.MONTH, 1);
                feeEndTime = endTimeCalendar.getTime();
            }
            
            // 如果当前结束时间已经大于新的结束时间，则跳过
            if (tmpOwnerCarDto.getEndTime().getTime() >= feeEndTime.getTime()) {
                continue;
            }
            
            // 更新车辆结束时间
            OwnerCarPo ownerCarPo = new OwnerCarPo();
            ownerCarPo.setMemberId(tmpOwnerCarDto.getMemberId());
            ownerCarPo.setEndTime(DateUtil.getFormatTimeString(feeEndTime, DateUtil.DATE_FORMATE_STRING_A));
            flag = ownerCarNewV1InnerServiceSMOImpl.updateOwnerCarNew(ownerCarPo);
            if (flag < 1) {
                throw new CmdException("缴费失败");
            }
        }
    }

    /**
     * 验证单个月份是否跳月缴费
     * 
     * 检查缴费月份是否连续，防止用户跳过未缴费的月份。
     *
     * @param tmpPayFeeDetailMonthDto 当前缴费月份明细DTO
     * @param feeMonths 费用月份Map
     */
    private void validateOneMonthSkipPayFee(PayFeeDetailMonthDto tmpPayFeeDetailMonthDto, Map<String, List<PayFeeDetailMonthDto>> feeMonths) {
        List<PayFeeDetailMonthDto> feeDetailMonthDtos = feeMonths.get(tmpPayFeeDetailMonthDto.getFeeId());
        if (feeDetailMonthDtos == null || feeDetailMonthDtos.size() < 1) {
            throw new CmdException("未连续交费，请连续交费");
        }
        
        List<PayFeeDetailMonthDto> newFeeDetailMonthDtos = new ArrayList<>();
        
        // 检查是否存在跳月的情况
        for (PayFeeDetailMonthDto tmpFeeDetailMonthDto : feeDetailMonthDtos) {
            // 如果存在更早的未缴费月份，则提示需要连续缴费
            if (Integer.parseInt(tmpFeeDetailMonthDto.getDetailYear()) <= Integer.parseInt(tmpPayFeeDetailMonthDto.getDetailYear())
                    && Integer.parseInt(tmpFeeDetailMonthDto.getDetailMonth()) < Integer.parseInt(tmpPayFeeDetailMonthDto.getDetailMonth())) {
                throw new CmdException("未连续缴费，请选择" + tmpFeeDetailMonthDto.getDetailYear() + "-" + tmpFeeDetailMonthDto.getDetailMonth()
                        + " ," + tmpPayFeeDetailMonthDto.getFeeName());
            }
            
            // 跳过当前缴费月份
            if (Integer.parseInt(tmpFeeDetailMonthDto.getDetailYear()) == Integer.parseInt(tmpPayFeeDetailMonthDto.getDetailYear())
                    && Integer.parseInt(tmpFeeDetailMonthDto.getDetailMonth()) == Integer.parseInt(tmpPayFeeDetailMonthDto.getDetailMonth())) {
                continue;
            }
            
            newFeeDetailMonthDtos.add(tmpFeeDetailMonthDto);
        }

        // 更新费用月份信息
        feeMonths.put(tmpPayFeeDetailMonthDto.getFeeId(), newFeeDetailMonthDtos);
    }

    /**
     * 处理报修单
     * 
     * 缴费完成后，更新相关报修单的状态，包括派单状态和支付状态。
     *
     * @param payFeeDetailMonthDto 缴费月份明细DTO
     * @param feeDto 费用DTO
     */
    private void doDealRepairOrder(PayFeeDetailMonthDto payFeeDetailMonthDto, FeeDto feeDto) {
        int flag;
        // 判断是否有派单属性ID
        FeeAttrDto feeAttrDto = new FeeAttrDto();
        feeAttrDto.setCommunityId(feeDto.getCommunityId());
        feeAttrDto.setFeeId(feeDto.getFeeId());
        feeAttrDto.setSpecCd(FeeAttrDto.SPEC_CD_REPAIR); // 报修相关属性
        List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);
        
        // 修改派单状态
        if (feeAttrDtos != null && feeAttrDtos.size() > 0) {
            RepairDto repairDto = new RepairDto();
            repairDto.setRepairId(feeAttrDtos.get(0).getValue());
            // 查询报修记录
            List<RepairDto> repairDtos = repairInnerServiceSMO.queryRepairs(repairDto);
            Assert.listOnlyOne(repairDtos, "报修信息错误！");
            
            // 获取报修渠道
            String repairChannel = repairDtos.get(0).getRepairChannel();
            RepairPoolPo repairPoolPo = new RepairPoolPo();
            repairPoolPo.setRepairId(feeAttrDtos.get(0).getValue());
            repairPoolPo.setCommunityId(feeDto.getCommunityId());
            
            // 根据报修渠道设置不同的状态
            if (repairChannel.equals("Z")) { 
                // 业主自主报修，状态变成待评价
                repairPoolPo.setState(RepairDto.STATE_APPRAISE);
            } else { 
                // 员工代客报修或电话报修，状态变成待回访
                repairPoolPo.setState(RepairDto.STATE_RETURN_VISIT);
            }
            
            flag = repairPoolNewV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
            if (flag < 1) {
                throw new CmdException("更新微信派单池信息失败");
            }

            // 重新查询报修记录
            repairDto = new RepairDto();
            repairDto.setRepairId(feeAttrDtos.get(0).getValue());
            repairDtos = repairInnerServiceSMO.queryRepairs(repairDto);
            Assert.listOnlyOne(repairDtos, "报修信息错误！");
            
            repairChannel = repairDtos.get(0).getRepairChannel();
            RepairUserDto repairUserDto = new RepairUserDto();
            repairUserDto.setRepairId(feeAttrDtos.get(0).getValue());
            repairUserDto.setState(RepairUserDto.STATE_PAY_FEE); // 待支付状态
            
            // 查询待支付状态的记录
            List<RepairUserDto> repairUserDtoList = repairUserInnerServiceSMO.queryRepairUsers(repairUserDto);
            Assert.listOnlyOne(repairUserDtoList, "信息错误！");
            
            RepairUserPo repairUserPo = new RepairUserPo();
            repairUserPo.setRuId(repairUserDtoList.get(0).getRuId());
            
            if ("Z".equals(repairChannel)) {  
                // 业主自主报修，状态变成已支付，并新增一条待评价状态
                repairUserPo.setState(RepairUserDto.STATE_FINISH_PAY_FEE);
                repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                repairUserPo.setContext("已支付" + payFeeDetailMonthDto.getReceivedAmount() + "元");
                
                // 新增待评价状态记录
                RepairUserPo repairUser = new RepairUserPo();
                repairUser.setRuId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ruId));
                repairUser.setStartTime(repairUserPo.getEndTime());
                repairUser.setState(RepairUserDto.STATE_EVALUATE); // 待评价状态
                repairUser.setContext("待评价");
                repairUser.setCommunityId(feeDto.getCommunityId());
                repairUser.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                repairUser.setRepairId(repairUserDtoList.get(0).getRepairId());
                repairUser.setStaffId(repairUserDtoList.get(0).getStaffId());
                repairUser.setStaffName(repairUserDtoList.get(0).getStaffName());
                repairUser.setPreStaffId(repairUserDtoList.get(0).getStaffId());
                repairUser.setPreStaffName(repairUserDtoList.get(0).getStaffName());
                repairUser.setPreRuId(repairUserDtoList.get(0).getRuId());
                repairUser.setRepairEvent("auditUser");
                
                flag = repairUserNewV1InnerServiceSMOImpl.saveRepairUserNew(repairUser);
                if (flag < 1) {
                    throw new CmdException("更新微信派单池信息失败");
                }
            } else {  
                // 员工代客报修或电话报修，状态变成已支付
                repairUserPo.setState(RepairUserDto.STATE_FINISH_PAY_FEE);
                repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                repairUserPo.setContext("已支付" + payFeeDetailMonthDto.getReceivedAmount() + "元");
            }
            
            flag = repairUserNewV1InnerServiceSMOImpl.updateRepairUserNew(repairUserPo);
            if (flag < 1) {
                throw new CmdException("更新微信派单池信息失败");
            }
        }
    }
}