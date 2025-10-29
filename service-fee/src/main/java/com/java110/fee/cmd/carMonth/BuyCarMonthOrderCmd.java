package com.java110.fee.cmd.carMonth;

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
import com.java110.dto.IotDataDto;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.community.IRoomV1InnerServiceSMO;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeConfigV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.job.IIotInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.car.OwnerCarPo;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeeConfigPo;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 购买月租车月卡订单处理命令类
 * 
 * 该类负责处理月租车月卡购买业务，包括参数验证、费用配置查询、费用记录创建、
 * 费用属性设置、费用明细记录以及车辆信息更新等操作。通过物联网服务进行月卡信息
 * 查询和购买结果通知，确保月卡购买流程的完整性和数据一致性。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "carMonth.buyCarMonthOrder")
public class BuyCarMonthOrderCmd extends Cmd {

    // 车辆相关服务接口
    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;
    
    // 费用配置服务接口
    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    // 小区成员服务接口
    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    // 费用服务接口
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    // 业主服务接口
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    // 业主房屋关系服务接口
    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    // 费用属性服务接口
    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    // 费用明细服务接口
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    // 停车位服务接口
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    // 物联网服务接口
    @Autowired
    private IIotInnerServiceSMO iotInnerServiceSMOImpl;

    // 用户服务接口
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;


    /**
     * 验证请求参数的有效性
     * 
     * 该方法验证购买月租车月卡订单所需的必要参数，包括车辆信息、月卡信息等，
     * 并查询相关数据为后续业务处理做准备。验证内容包括：
     * 1. 必要参数是否存在
     * 2. 车辆信息是否存在
     * 3. 停车位信息是否存在
     * 4. 月卡信息是否存在
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求JSON对象，包含请求参数
     * @throws CmdException 当参数验证失败或数据查询异常时抛出
     * @throws ParseException 当日期解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "cardId", "请求报文中未包含cardId");
        Assert.hasKeyAndValue(reqJson, "carNum", "请求报文中未包含carNum");
        Assert.hasKeyAndValue(reqJson, "carId", "请求报文中未包含carId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "primeRate", "请求报文中未包含primeRate");
        Assert.hasKeyAndValue(reqJson, "receivedAmount", "请求报文中未包含receivedAmount");

        // 查询车辆信息
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCarId(reqJson.getString("carId"));
        ownerCarDto.setCarNum(reqJson.getString("carNum"));
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        if (ListUtil.isNull(ownerCarDtos)) {
            throw new CmdException("月租车不存在");
        }

        // 查询停车位信息
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(reqJson.getString("communityId"));
        parkingSpaceDto.setPsId(ownerCarDtos.get(0).getPsId());
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);

        if (ListUtil.isNull(parkingSpaceDtos)) {
            throw new CmdException("车辆停车位不存在");
        }

        // 通过物联网服务查询月卡信息
        JSONObject paramIn = new JSONObject();
        paramIn.put("row", 1);
        paramIn.put("page", 1);
        paramIn.put("cardId", reqJson.getString("cardId"));
        paramIn.put("communityId", reqJson.getString("communityId"));
        paramIn.put("paNum", parkingSpaceDtos.get(0).getAreaNum()); // 设置停车区域编号

        ResultVo data = iotInnerServiceSMOImpl.postIotData(new IotDataDto("queryCarMonthCardBmoImpl", paramIn));

        if (data.getCode() != ResultVo.CODE_OK) {
            throw new CmdException(data.getMsg());
        }

        // 处理物联网返回的月卡数据
        Object obj = data.getData();
        JSONArray carMonthCardDtos = null;
        if (obj instanceof List) {
            carMonthCardDtos = new JSONArray((List) obj);
        }else{
            carMonthCardDtos = (JSONArray) obj;
        }

        if (ListUtil.isNull(carMonthCardDtos)) {
            throw new CmdException("月卡不存在");
        }

        // 将查询到的月卡信息设置到请求参数中，供后续处理使用
        reqJson.put("receivableAmount", carMonthCardDtos.getJSONObject(0).getString("cardPrice"));
        reqJson.put("cardMonth", carMonthCardDtos.getJSONObject(0).getString("cardMonth"));
        reqJson.put("carMemberId", ownerCarDtos.get(0).getMemberId());
        reqJson.put("endTime", ownerCarDtos.get(0).getEndTime());
        reqJson.put("psId", ownerCarDtos.get(0).getPsId());
    }

    /**
     * 执行购买月租车月卡订单的核心业务逻辑
     * 
     * 该方法处理月卡购买的全流程，包括用户验证、费用配置、费用记录创建、
     * 费用属性设置、费用明细记录、车辆信息更新和物联网通知等操作。整个流程
     * 在事务管理下执行，确保数据的一致性。
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求JSON对象，包含请求参数
     * @throws CmdException 当业务处理失败时抛出
     * @throws ParseException 当日期解析异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 获取当前用户信息
        String userId = CmdContextUtils.getUserId(context);

        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户未登录");

        // 计算月卡开始时间和结束时间
        Date startTime = reqJson.getDate("endTime");

        // 如果原有结束时间早于当前时间，则从当前时间开始计算
        if (startTime.getTime() < DateUtil.getCurrentDate().getTime()) {
            startTime = DateUtil.getCurrentDate();
        }

        // 计算月卡结束时间：开始时间加上月卡月数
        String endTime = DateUtil.getAddMonthStringA(startTime, reqJson.getIntValue("cardMonth"));

        // 查询或创建月租车月卡费用配置
        FeeConfigDto feeConfigDto = getFeeConfigDto(reqJson.getString("communityId"));

        // 查询物业信息
        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        communityMemberDto.setCommunityId(reqJson.getString("communityId"));
        communityMemberDto.setMemberTypeCd(CommunityMemberDto.MEMBER_TYPE_PROPERTY);
        List<CommunityMemberDto> communityMemberDtos = communityMemberV1InnerServiceSMOImpl.queryCommunityMembers(communityMemberDto);

        Assert.listOnlyOne(communityMemberDtos, "物业不存在");

        // 创建费用记录
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setCommunityId(reqJson.getString("communityId"));
        payFeePo.setConfigId(feeConfigDto.getConfigId());
        payFeePo.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_CAR); // 缴费对象类型为车辆
        payFeePo.setStartTime(DateUtil.getFormatTimeStringA(startTime));
        payFeePo.setEndTime(endTime);
        payFeePo.setAmount(reqJson.getString("receivedAmount"));
        payFeePo.setFeeFlag(feeConfigDto.getFeeFlag());
        payFeePo.setFeeTypeCd(feeConfigDto.getFeeTypeCd());
        payFeePo.setIncomeObjId(communityMemberDtos.get(0).getMemberId()); // 收款对象为物业
        payFeePo.setBatchId("-1");
        payFeePo.setState(FeeDto.STATE_FINISH); // 费用状态为已完成

        payFeePo.setFeeId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
        payFeePo.setPayerObjId(reqJson.getString("carId"));
        payFeePo.setUserId("-1");
        payFeePo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        List<PayFeePo> payFeePos = new ArrayList<>();
        payFeePos.add(payFeePo);
        feeInnerServiceSMOImpl.saveFee(payFeePos);

        // 创建费用属性记录
        List<FeeAttrPo> feeAttrsPos = new ArrayList<>();
        // 查询车辆信息并设置缴费对象名称属性
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setCommunityId(feeConfigDto.getCommunityId());
        ownerCarDto.setMemberId(reqJson.getString("carId"));
        List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
        if (!ListUtil.isNull(ownerCarDtos)) {
            // 设置缴费对象名称：区域编号-车位编号(车牌号)
            feeAttrsPos.add(addFeeAttr(payFeePo, FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME,
                    ownerCarDtos.get(0).getAreaNum() + "-" + ownerCarDtos.get(0).getNum() + "(" + ownerCarDtos.get(0).getCarNum() + ")"));
        }

        // 查询业主信息并设置相关属性
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(ownerCarDtos.get(0).getOwnerId());
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
        if (!ListUtil.isNull(ownerDtos)) {

            feeAttrsPos.add(addFeeAttr(payFeePo, FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME,
                    payFeePo.getEndTime()));

            feeAttrsPos.add(addFeeAttr(payFeePo, FeeAttrDto.SPEC_CD_OWNER_ID, ownerDtos.get(0).getOwnerId()));
            feeAttrsPos.add(addFeeAttr(payFeePo, FeeAttrDto.SPEC_CD_OWNER_LINK, ownerDtos.get(0).getLink()));
            feeAttrsPos.add(addFeeAttr(payFeePo, FeeAttrDto.SPEC_CD_OWNER_NAME, ownerDtos.get(0).getName()));

        }
        feeAttrInnerServiceSMOImpl.saveFeeAttrs(feeAttrsPos);
        
        // 获取事务ID
        String oId = Java110TransactionalFactory.getOId();

        // 创建费用明细记录
        PayFeeDetailPo payFeeDetailPo = new PayFeeDetailPo();
        payFeeDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
        if (StringUtil.isEmpty(oId)) {
            oId = payFeeDetailPo.getDetailId();
        }
        payFeeDetailPo.setCommunityId(feeConfigDto.getCommunityId());
        payFeeDetailPo.setReceivedAmount(reqJson.getString("receivedAmount"));
        payFeeDetailPo.setReceivableAmount(reqJson.getString("receivedAmount"));
        payFeeDetailPo.setCycles("1");
        payFeeDetailPo.setPrimeRate(reqJson.getString("primeRate"));
        payFeeDetailPo.setFeeId(payFeePo.getFeeId());
        payFeeDetailPo.setStartTime(payFeePo.getStartTime());
        payFeeDetailPo.setEndTime(DateUtil.getPreSecTime(payFeePo.getEndTime())); // 结束时间减去1秒
        payFeeDetailPo.setRemark(reqJson.getString("remark"));
        payFeeDetailPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        payFeeDetailPo.setState("1400"); // 费用明细状态
        payFeeDetailPo.setPayableAmount(reqJson.getString("receivedAmount"));
        payFeeDetailPo.setPayOrderId(oId);
        payFeeDetailPo.setOpenInvoice("N"); // 未开发票
        payFeeDetailPo.setDeductionAmount("0");
        payFeeDetailPo.setGiftAmount("0");
        payFeeDetailPo.setDiscountAmount("0");
        payFeeDetailPo.setLateAmount("0");
        payFeeDetailPo.setCashierName(userDtos.get(0).getName()); // 收款员姓名
        payFeeDetailPo.setCashierId("-1");

        payFeeDetailV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);

        // 更新车辆月卡结束时间
        OwnerCarPo ownerCarPo = new OwnerCarPo();
        ownerCarPo.setCarId(ownerCarDtos.get(0).getCarId());
        ownerCarPo.setEndTime(endTime);
        ownerCarPo.setCommunityId(ownerCarDtos.get(0).getCommunityId());
        ownerCarV1InnerServiceSMOImpl.updateOwnerCar(ownerCarPo);

        // 通知物联网系统月卡购买信息
        JSONObject paramIn = new JSONObject();
        paramIn.put("cardId", reqJson.getString("cardId"));
        paramIn.put("carNum", reqJson.getString("carNum"));
        paramIn.put("communityId", reqJson.getString("communityId"));
        paramIn.put("primeRate", reqJson.getString("primeRate"));
        paramIn.put("receivedAmount", reqJson.getString("receivedAmount"));
        paramIn.put("endTime", endTime);
        paramIn.put("cashierId", userId);
        paramIn.put("cashierName", userDtos.get(0).getName());

        ResultVo data = iotInnerServiceSMOImpl.postIotData(new IotDataDto("buyCarMonthOrderBmoImpl", paramIn));

        if (data.getCode() != ResultVo.CODE_OK) {
            throw new CmdException(data.getMsg());
        }
    }

    /**
     * 获取月租车月卡费用配置
     * 
     * 查询指定小区下的月租车月卡费用配置，如果不存在则创建默认配置。
     * 费用配置包括费用类型、计算公式、支付方式等关键信息。
     * 
     * @param communityId 小区ID，用于查询特定小区的费用配置
     * @return 费用配置数据传输对象，包含费用配置的详细信息
     */
    private FeeConfigDto getFeeConfigDto(String communityId) {

        String feeName = "月租车月卡";

        // 查询现有费用配置
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setFeeName(feeName);
        feeConfigDto.setCommunityId(communityId);
        feeConfigDto.setFeeTypeCd(FeeConfigDto.FEE_TYPE_CD_PARKING); // 费用类型为停车费
        feeConfigDto.setComputingFormula(FeeConfigDto.COMPUTING_FORMULA_DYNAMIC); // 计算公式为动态
        List<FeeConfigDto> feeConfigDtos = payFeeConfigV1InnerServiceSMOImpl.queryPayFeeConfigs(feeConfigDto);

        // 如果配置存在则直接返回
        if (!ListUtil.isNull(feeConfigDtos)) {
            return feeConfigDtos.get(0);
        }

        // 创建新的费用配置
        PayFeeConfigPo payFeeConfigPo = new PayFeeConfigPo();
        payFeeConfigPo.setConfigId(GenerateCodeFactory.getGeneratorId("11"));
        payFeeConfigPo.setCommunityId(communityId);
        payFeeConfigPo.setFeeTypeCd(FeeConfigDto.FEE_TYPE_CD_PARKING);
        payFeeConfigPo.setSquarePrice("0");
        payFeeConfigPo.setAdditionalAmount("0");
        payFeeConfigPo.setIsDefault(FeeConfigDto.DEFAULT_FEE_CONFIG);
        payFeeConfigPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        payFeeConfigPo.setEndTime(DateUtil.getLastTime());
        payFeeConfigPo.setFeeFlag(FeeDto.FEE_FLAG_ONCE); // 费用标志为一次性费用
        payFeeConfigPo.setFeeName(feeName);
        payFeeConfigPo.setComputingFormula(FeeConfigDto.COMPUTING_FORMULA_DYNAMIC);
        payFeeConfigPo.setBillType(FeeConfigDto.BILL_TYPE_YEAR); // 账单类型为年
        payFeeConfigPo.setPaymentCd(FeeConfigDto.PAYMENT_CD_PRE); // 支付方式为预付
        payFeeConfigPo.setPaymentCycle("1");
        payFeeConfigPo.setComputingFormulaText("");
        payFeeConfigPo.setDeductFrom(FeeConfigDto.DEDUCT_FROM_N);
        payFeeConfigPo.setPayOnline("Y"); // 支持在线支付
        payFeeConfigPo.setScale("1");
        payFeeConfigPo.setDecimalPlace("2");
        payFeeConfigPo.setUnits("元");
        payFeeConfigPo.setPrepaymentPeriod("1");
        payFeeConfigPo.setState("Y"); // 配置状态为启用
        payFeeConfigV1InnerServiceSMOImpl.savePayFeeConfig(payFeeConfigPo);

        // 重新查询并返回新创建的费用配置
        feeConfigDto = new FeeConfigDto();
        feeConfigDto.setFeeName(feeName);
        feeConfigDto.setCommunityId(communityId);
        feeConfigDto.setFeeTypeCd(FeeConfigDto.FEE_TYPE_CD_PARKING);
        feeConfigDto.setComputingFormula(FeeConfigDto.COMPUTING_FORMULA_DYNAMIC);
        feeConfigDtos = payFeeConfigV1InnerServiceSMOImpl.queryPayFeeConfigs(feeConfigDto);

        return feeConfigDtos.get(0);
    }

    /**
     * 创建费用属性对象
     * 
     * 根据费用记录和属性信息创建费用属性持久化对象。费用属性用于存储
     * 费用的附加信息，如缴费对象名称、业主信息等。
     * 
     * @param payFeePo 费用持久化对象，包含费用基本信息
     * @param specCd 属性规格代码，定义属性的类型
     * @param value 属性值，属性的具体内容
     * @return 费用属性持久化对象，包含完整的属性信息
     */
    public FeeAttrPo addFeeAttr(PayFeePo payFeePo, String specCd, String value) {
        FeeAttrPo feeAttrPo = new FeeAttrPo();
        feeAttrPo.setCommunityId(payFeePo.getCommunityId());
        feeAttrPo.setSpecCd(specCd);
        feeAttrPo.setValue(value);
        feeAttrPo.setFeeId(payFeePo.getFeeId());
        feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
        return feeAttrPo;
    }
}