package com.java110.fee.bmo.fee.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.machine.CarInoutDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.user.UserDto;
import com.java110.fee.bmo.ApiBaseBMO;
import com.java110.fee.bmo.fee.IFeeBMO;
import com.java110.intf.common.ICarInoutInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.car.CarInoutPo;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeeConfigPo;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.fee.FeeReceiptPo;
import com.java110.po.fee.FeeReceiptDetailPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.*;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @ClassName FeeBMOImpl
 * @Description TODO
 * @Author wuxw
 * @Date 2020/3/9 22:24
 * @Version 1.0
 * add by wuxw 2020/3/9
 **/
@Service("feeBMOImpl")
public class FeeBMOImpl extends ApiBaseBMO implements IFeeBMO {

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private ICarInoutInnerServiceSMO carInoutInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;


    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailNewV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IFeeReceiptInnerServiceSMO feeReceiptInnerServiceSMOImpl;

    private static Calendar getTargetEndTime(Calendar endCalender, Double cycles) {
        if (StringUtil.isInteger(cycles.toString())) {
            endCalender.add(Calendar.MONTH, new Double(cycles).intValue());
            return endCalender;
        }
        if (cycles >= 1) {
            endCalender.add(Calendar.MONTH, new Double(Math.floor(cycles)).intValue());
            cycles = cycles - Math.floor(cycles);
        }
        int futureDay = endCalender.getActualMaximum(Calendar.DAY_OF_MONTH);
        int hours = new Double(cycles * futureDay * 24).intValue();
        endCalender.add(Calendar.HOUR, hours);
        return endCalender;
    }

    /**
     * 添加费用明细信息
     *
     * @param paramObj 接口调用放传入入参
     * @param
     * @return 订单服务能够接受的报文
     */
    public PayFeeDetailPo addFeePreDetail(FeeDto feeDto, JSONObject paramObj, PayFeeDetailPo payFeeDetailPo) {

        payFeeDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));

        //todo 生成收据编号
        String receiptCode = feeReceiptInnerServiceSMOImpl.generatorReceiptCode(paramObj.getString("communityId"));
        //todo 缓存收据编号
        CommonCache.setValue(payFeeDetailPo.getDetailId() + CommonCache.RECEIPT_CODE, receiptCode, CommonCache.DEFAULT_EXPIRETIME_TWO_MIN);
        payFeeDetailPo.setPayOrderId(paramObj.getString("oId"));
        payFeeDetailPo.setbId("-1");
        payFeeDetailPo.setPrimeRate("6");
        payFeeDetailPo.setRemark("线上小程序支付");
        payFeeDetailPo.setState(FeeDetailDto.STATE_NORMAL);
        payFeeDetailPo.setOpenInvoice("N");
        payFeeDetailPo.setCommunityId(feeDto.getCommunityId());
        payFeeDetailPo.setFeeId(feeDto.getFeeId());
        payFeeDetailPo.setCycles(paramObj.getString("cycles"));
        payFeeDetailPo.setReceivableAmount(paramObj.getString("receivableAmount"));
        payFeeDetailPo.setReceivedAmount(paramObj.getString("receivedAmount"));
        payFeeDetailPo.setPayableAmount(paramObj.getString("receivableAmount"));
        payFeeDetailPo.setStartTime(DateUtil.getFormatTimeString(feeDto.getEndTime(), DateUtil.DATE_FORMATE_STRING_A));

        Date endTime = feeDto.getEndTime();
        Calendar endCalender = Calendar.getInstance();
        endCalender.setTime(endTime);

        endCalender.add(Calendar.MONTH, Integer.parseInt(paramObj.getString("cycles")));
        if (FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())) {
            if (feeDto.getDeadlineTime() != null) {
                endCalender.setTime(feeDto.getDeadlineTime());
            } else if (!StringUtil.isEmpty(feeDto.getCurDegrees())) {
                endCalender.setTime(feeDto.getCurReadingTime());
            } else if (feeDto.getImportFeeEndTime() == null) {
                endCalender.setTime(feeDto.getConfigEndTime());
            } else {
                endCalender.setTime(feeDto.getImportFeeEndTime());
            }
        } else {
            endCalender.add(Calendar.SECOND, -1);
        }

        String endTimeStr = DateUtil.getFormatTimeString(endCalender.getTime(), DateUtil.DATE_FORMATE_STRING_A);
        payFeeDetailPo.setEndTime(endTimeStr);

        payFeeDetailPo.setCashierId("-1");
        payFeeDetailPo.setCashierName("系统收银");

        String userId = paramObj.getString("userId");
        if (StringUtil.isEmpty(userId)) {
            return payFeeDetailPo;
        }

        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        if (!ListUtil.isNull(userDtos)) {
            payFeeDetailPo.setCashierId(userDtos.get(0).getUserId());
            payFeeDetailPo.setCashierName(userDtos.get(0).getName());
        }
        return payFeeDetailPo;
    }

    /**
     * 修改费用信息
     *
     * @param paramInJson 接口调用放传入入参
     * @param
     * @return 订单服务能够接受的报文
     */
    public JSONObject modifyPreFee(FeeDto feeInfo, JSONObject paramInJson, PayFeeDetailPo payFeeDetailPo) {

        String endTime = payFeeDetailPo.getEndTime();
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setEndTime(endTime);
        payFeePo.setFeeId(feeInfo.getFeeId());
        payFeePo.setCommunityId(feeInfo.getCommunityId());
        if (!FeeDto.FEE_FLAG_ONCE.equals(feeInfo.getFeeFlag())) { // 周期性减了一秒这里加回来
            endTime = DateUtil.getNextSecTime(endTime);
            payFeePo.setEndTime(endTime);
        }

        Date maxEndTime = feeInfo.getDeadlineTime();
        if (FeeDto.FEE_FLAG_CYCLE.equals(feeInfo.getFeeFlag())) {
            maxEndTime = feeInfo.getConfigEndTime();
        }

        if (FeeDto.FEE_FLAG_CYCLE_ONCE.equals(feeInfo.getFeeFlag())) {
            maxEndTime = feeInfo.getMaxEndTime();
        }

        //如果间歇性费用没有设置结束时间 则取费用项的
        if (maxEndTime == null) {
            maxEndTime = feeInfo.getConfigEndTime();
        }

        //判断 结束时间 是否大于 费用项 结束时间，这里 容错一下，如果 费用结束时间大于 费用项结束时间 30天 走报错 属于多缴费
        if (maxEndTime != null) {
            if (feeInfo.getEndTime().getTime() - maxEndTime.getTime() > 30 * 24 * 60 * 60 * 1000L) {
                throw new IllegalArgumentException("缴费超过了 费用项结束时间");
            }
        }

        // 周期性收费、缴费后，到期日期在费用项终止日期后，则设置缴费状态结束，设置结束日期为费用项终止日期
        if (!FeeFlagTypeConstant.ONETIME.equals(feeInfo.getFeeFlag())) {
            //这里 容错五天时间
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(maxEndTime);
            calendar.add(Calendar.DAY_OF_MONTH, -5);
            maxEndTime = calendar.getTime();
            if (feeInfo.getEndTime().after(maxEndTime)) {
                payFeePo.setState(FeeDto.STATE_FINISH);
                payFeePo.setEndTime(DateUtil.getFormatTimeStringA(maxEndTime));
            }
        }

        int flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
        if (flag < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "保存费用明细失败");
        }
        //为停车费单独处理
        paramInJson.put("carFeeEndTime", feeInfo.getEndTime());
        paramInJson.put("carPayerObjType", feeInfo.getPayerObjType());
        paramInJson.put("carPayerObjId", feeInfo.getPayerObjId());


        return paramInJson;
    }


    public JSONObject modifyTempCarInout(JSONObject reqJson, DataFlowContext context) {
        FeeDto feeDto = (FeeDto) reqJson.get("feeInfo");
        CarInoutDto tempCarInoutDto = new CarInoutDto();
        tempCarInoutDto.setCommunityId(reqJson.getString("communityId"));
        tempCarInoutDto.setInoutId(feeDto.getPayerObjId());
        List<CarInoutDto> carInoutDtos = carInoutInnerServiceSMOImpl.queryCarInouts(tempCarInoutDto);
        Assert.listOnlyOne(carInoutDtos, "根据费用信息反差车辆进场记录未查到 或查到多条");
        CarInoutDto carInoutDto = carInoutDtos.get(0);
        JSONObject business = JSONObject.parseObject("{\"datas\":{}}");
        business.put(CommonConstant.HTTP_BUSINESS_TYPE_CD, BusinessTypeConstant.BUSINESS_TYPE_UPDATE_CAR_INOUT);
        business.put(CommonConstant.HTTP_SEQ, DEFAULT_SEQ + 2);
        business.put(CommonConstant.HTTP_INVOKE_MODEL, CommonConstant.HTTP_INVOKE_MODEL_S);
        JSONObject businessCarInout = new JSONObject();
        businessCarInout.putAll(BeanConvertUtil.beanCovertMap(carInoutDto));
        businessCarInout.put("state", "100400");
        //计算 应收金额
        business.getJSONObject(CommonConstant.HTTP_BUSINESS_DATAS).put(CarInoutPo.class.getSimpleName(), businessCarInout);
        return business;
    }

    /**
     * 添加物业费用
     *
     * @param paramInJson 接口调用放传入入参
     * @param context     数据上下文
     * @return 订单服务能够接受的报文
     */
    public JSONObject addFee(OwnerCarDto ownerCarDto, JSONObject paramInJson, ICmdDataFlowContext context) {
        String time = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A);
        if (paramInJson.containsKey("startTime")) {
            time = paramInJson.getString("startTime");
        }

        //查询费用项
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(paramInJson.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        Assert.listOnlyOne(feeConfigDtos, "查询费用项错误！");

        JSONObject businessUnit = new JSONObject();
        businessUnit.put("feeId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
        businessUnit.put("configId", paramInJson.getString("configId"));
        businessUnit.put("feeTypeCd", paramInJson.getString("feeTypeCd"));
        businessUnit.put("incomeObjId", paramInJson.getString("storeId"));
        businessUnit.put("amount", "-1.00");
        if (paramInJson.containsKey("amount") && !StringUtil.isEmpty(paramInJson.getString("amount"))) {
            businessUnit.put("amount", paramInJson.getString("amount"));
        }
        businessUnit.put("startTime", time);
        businessUnit.put("endTime", time);
        businessUnit.put("communityId", paramInJson.getString("communityId"));
        businessUnit.put("payerObjId", ownerCarDto.getCarId());
        businessUnit.put("payerObjType", FeeDto.PAYER_OBJ_TYPE_CAR);
        businessUnit.put("feeFlag", paramInJson.getString("feeFlag"));
        businessUnit.put("state", "2008001");
        businessUnit.put("batchId", paramInJson.getString("batchId"));
        businessUnit.put("userId", context.getReqHeaders().get(CommonConstant.HTTP_USER_ID));
        paramInJson.put("feeId", businessUnit.getString("feeId"));

        if (FeeDto.FEE_FLAG_CYCLE.equals(feeConfigDtos.get(0).getFeeFlag())) {
            businessUnit.put("maxTime", feeConfigDtos.get(0).getEndTime());
        } else {
            businessUnit.put("maxTime", paramInJson.getString("endTime"));
        }
        return businessUnit;
    }

    @Override
    public FeeAttrPo addFeeAttr(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext, String specCd, String value) {
        FeeAttrPo feeAttrPo = new FeeAttrPo();
        feeAttrPo.setCommunityId(paramInJson.getString("communityId"));
        feeAttrPo.setSpecCd(specCd);
        feeAttrPo.setValue(value);
        feeAttrPo.setFeeId(paramInJson.getString("feeId"));
        feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId, true));
        return feeAttrPo;

    }


    /**
     * 添加物业费用
     *
     * @param paramInJson     接口调用放传入入参
     * @param dataFlowContext 数据上下文
     * @return 订单服务能够接受的报文
     */
    public JSONObject addRoomFee(RoomDto roomDto, JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {
        String time = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A);
        if (paramInJson.containsKey("feeEndDate")) {
            time = paramInJson.getString("feeEndDate");
        } else if (paramInJson.containsKey("startTime")) {
            time = paramInJson.getString("startTime");
        }
        //获取费用标识
        String feeFlag = paramInJson.getString("feeFlag");
        //查询费用项
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(paramInJson.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        Assert.listOnlyOne(feeConfigDtos, "查询费用项错误！");
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("feeId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
        businessUnit.put("configId", paramInJson.getString("configId"));
        businessUnit.put("feeTypeCd", paramInJson.getString("feeTypeCd"));
        businessUnit.put("incomeObjId", paramInJson.getString("storeId"));
        businessUnit.put("amount", "-1.00");
        if (paramInJson.containsKey("amount") && !StringUtil.isEmpty(paramInJson.getString("amount"))) {
            businessUnit.put("amount", paramInJson.getString("amount"));
        }
        businessUnit.put("startTime", time);
        businessUnit.put("endTime", time);
        businessUnit.put("communityId", paramInJson.getString("communityId"));
        businessUnit.put("payerObjId", roomDto.getRoomId());
        businessUnit.put("payerObjType", "3333");
        businessUnit.put("feeFlag", paramInJson.getString("feeFlag"));
        businessUnit.put("state", "2008001");
        businessUnit.put("batchId", paramInJson.getString("batchId"));
        businessUnit.put("userId", dataFlowContext.getReqHeaders().get(CommonConstant.HTTP_USER_ID));

        if (FeeDto.FEE_FLAG_CYCLE.equals(feeConfigDtos.get(0).getFeeFlag())) {
            businessUnit.put("maxTime", feeConfigDtos.get(0).getEndTime());
        } else {
            businessUnit.put("maxTime", paramInJson.getString("endTime"));
        }

        paramInJson.put("feeId", businessUnit.getString("feeId"));
        return businessUnit;
    }

    /**
     * 添加物业费用
     *
     * @param paramInJson     接口调用放传入入参
     * @param dataFlowContext 数据上下文
     * @return 订单服务能够接受的报文
     */
    public JSONObject addContractFee(ContractDto contractDto, JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {
        String time = DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A);
        if (paramInJson.containsKey("feeEndDate")) {
            time = paramInJson.getString("feeEndDate");
        } else if (paramInJson.containsKey("startTime")) {
            time = paramInJson.getString("startTime");
        }
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("feeId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
        businessUnit.put("configId", paramInJson.getString("configId"));
        businessUnit.put("feeTypeCd", paramInJson.getString("feeTypeCd"));
        businessUnit.put("incomeObjId", paramInJson.getString("storeId"));
        businessUnit.put("amount", "-1.00");
        if (paramInJson.containsKey("amount") && !StringUtil.isEmpty(paramInJson.getString("amount"))) {
            businessUnit.put("amount", paramInJson.getString("amount"));
        }
        businessUnit.put("startTime", time);
        businessUnit.put("endTime", time);
        businessUnit.put("communityId", paramInJson.getString("communityId"));
        businessUnit.put("payerObjId", contractDto.getContractId());
        businessUnit.put("payerObjType", FeeDto.PAYER_OBJ_TYPE_CONTRACT);
        businessUnit.put("feeFlag", paramInJson.getString("feeFlag"));
        businessUnit.put("state", "2008001");
        businessUnit.put("batchId", paramInJson.getString("batchId"));
        businessUnit.put("userId", paramInJson.getString("userId"));
        paramInJson.put("feeId", businessUnit.getString("feeId"));
        return businessUnit;
    }

}
