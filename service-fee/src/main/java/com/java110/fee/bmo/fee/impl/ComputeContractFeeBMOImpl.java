package com.java110.fee.bmo.fee.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.dto.contract.ContractFeeComputeDto;
import com.java110.dto.fee.FeeAmountDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.shop.BuildingShopDto;
import com.java110.fee.bill.IFeeFormula;
import com.java110.fee.bmo.fee.IComputeContractFeeBMO;
import com.java110.intf.community.IBuildingShopInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 合同费用试算：金额统一由 feeFormula_* 公式模块计算
 */
@Service("computeContractFeeBMOImpl")
public class ComputeContractFeeBMOImpl implements IComputeContractFeeBMO {

    /**
     * 按费用项公式试算
     */
    private static final String MODE_FEE_CONFIG = "feeConfig";
    /**
     * 按租金公式试算
     */
    private static final String MODE_RENT = "rent";
    /**
     * 租金公式
     */
    private static final String FORMULA_RENT = "1101";
    /**
     * 需要抄表读数的公式，合同费用不支持自动试算
     */
    private static final List<String> METER_FORMULAS = Arrays.asList("5005", "6006", "9009");
    /**
     * 建筑/室内面积类公式
     */
    private static final List<String> BUILT_UP_AREA_FORMULAS = Arrays.asList("1001", "1002");
    private static final List<String> ROOM_AREA_FORMULAS = Arrays.asList("3003");
    /**
     * 租金类公式
     */
    private static final List<String> RENT_FORMULAS = Arrays.asList("1101", "1102");

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IBuildingShopInnerServiceSMO buildingShopInnerServiceSMOImpl;

    @Override
    public ContractFeeComputeDto computeContractFeeAmount(JSONObject reqJson) {
        String communityId = reqJson.getString("communityId");
        String payerObjType = reqJson.getString("payerObjType");
        String startTime = reqJson.getString("startTime");
        String endTime = reqJson.getString("endTime");
        String mode = StringUtil.isEmpty(reqJson.getString("mode")) ? MODE_FEE_CONFIG : reqJson.getString("mode");
        int months = resolveMonths(reqJson, startTime, endTime);
        List<String> payerObjIds = resolvePayerObjIds(reqJson);
        if (ListUtil.isNull(payerObjIds)) {
            throw new CmdException("未包含标的ID");
        }

        FeeConfigDto feeConfigDto;
        if (MODE_RENT.equals(mode)) {
            feeConfigDto = buildRentFeeConfig(communityId, startTime, endTime);
        } else {
            feeConfigDto = queryFeeConfig(communityId, reqJson.getString("configId"));
        }

        String computingFormula = StringUtil.isEmpty(feeConfigDto.getComputingFormula()) ? "" : feeConfigDto.getComputingFormula();
        checkFormula(computingFormula);

        int cycle = resolveCycle(reqJson, feeConfigDto, mode, months);
        double manualAmount = parseAmount(reqJson.getString("amount"));

        ContractFeeComputeDto result = new ContractFeeComputeDto();
        result.setPayerObjType(payerObjType);
        result.setFeeName(feeConfigDto.getFeeName());
        result.setFeeTypeCd(feeConfigDto.getFeeTypeCd());
        result.setComputingFormula(computingFormula);
        result.setComputingFormulaName(resolveFormulaName(feeConfigDto));
        result.setPaymentCycle(feeConfigDto.getPaymentCycle());
        result.setMonths(months);
        result.setCycle(cycle);
        result.setUnitPrice(formatAmount(feeConfigDto.getSquarePrice()));
        result.setAdditionalAmount(formatAmount(feeConfigDto.getAdditionalAmount()));

        BigDecimal totalAmount = new BigDecimal("0");
        BigDecimal totalBase = new BigDecimal("0");
        List<ContractFeeComputeDto.ContractFeeComputeDetail> details = new ArrayList<>();
        for (String payerObjId : payerObjIds) {
            ContractFeeComputeDto.ContractFeeComputeDetail detail = buildDetail(communityId, payerObjType, payerObjId);
            checkBaseValue(feeConfigDto, detail);
            FeeAmountDto feeAmountDto = computeFeeAmount(feeConfigDto, payerObjType, payerObjId, cycle, manualAmount, detail);
            detail.setAmount(formatAmount(feeAmountDto.getAmount()));
            detail.setRemark(feeAmountDto.getRemark());
            details.add(detail);
            totalAmount = totalAmount.add(new BigDecimal(String.valueOf(feeAmountDto.getAmount())));
            totalBase = totalBase.add(resolveBaseValue(computingFormula, detail));
        }

        result.setAmount(formatAmount(totalAmount.doubleValue()));
        result.setBaseAmount(formatAmount(totalBase.doubleValue()));
        result.setRemark(details.isEmpty() ? "" : details.get(0).getRemark());
        result.setDetails(details);
        return result;
    }

    /**
     * 月数：优先取前端传入，缺失时按起止日期兜底现算
     */
    private int resolveMonths(JSONObject reqJson, String startTime, String endTime) {
        Integer months = null;
        try {
            months = reqJson.getInteger("months");
        } catch (Exception e) {
            throw new CmdException("合同月数无效，请先选择合同到期日期");
        }
        if (months != null && months > 0) {
            return months;
        }
        int computeMonths = computeMonths(startTime, endTime);
        if (computeMonths <= 0) {
            throw new CmdException("合同月数无效，请先选择合同到期日期");
        }
        return computeMonths;
    }

    /**
     * 起止日期折算月数：末月不满按整月
     */
    private int computeMonths(String startTime, String endTime) {
        Date start = parseDate(startTime);
        Date end = parseDate(endTime);
        if (start == null || end == null || end.getTime() <= start.getTime()) {
            return 0;
        }
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end);
        int months = (endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12
                + (endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH));
        if (endCal.get(Calendar.DAY_OF_MONTH) >= startCal.get(Calendar.DAY_OF_MONTH)) {
            months += 1;
        }
        return months < 1 ? 1 : months;
    }

    private Date parseDate(String value) {
        if (StringUtil.isEmpty(value)) {
            return null;
        }
        String dateValue = value.trim();
        if (dateValue.length() > 10) {
            dateValue = dateValue.substring(0, 10);
        }
        try {
            return DateUtil.getDateFromStringB(dateValue);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> resolvePayerObjIds(JSONObject reqJson) {
        String payerObjIds = reqJson.getString("payerObjIds");
        List<String> objIds = new ArrayList<>();
        if (StringUtil.isEmpty(payerObjIds)) {
            return objIds;
        }
        String[] ids = payerObjIds.split(",");
        for (String id : ids) {
            if (!StringUtil.isEmpty(id) && !StringUtil.isEmpty(id.trim())) {
                objIds.add(id.trim());
            }
        }
        return objIds;
    }

    private FeeConfigDto queryFeeConfig(String communityId, String configId) {
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(communityId);
        feeConfigDto.setConfigId(configId);
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        if (ListUtil.isNull(feeConfigDtos)) {
            throw new CmdException("费用项不存在，请重新选择费用项");
        }
        return feeConfigDtos.get(0);
    }

    /**
     * 租金模式最小费用项：仅承载公式实现需要的精度参数
     */
    private FeeConfigDto buildRentFeeConfig(String communityId, String startTime, String endTime) {
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(communityId);
        feeConfigDto.setComputingFormula(FORMULA_RENT);
        feeConfigDto.setComputingFormulaName("租金");
        feeConfigDto.setFeeName("租金");
        feeConfigDto.setScale("1");
        feeConfigDto.setDecimalPlace("2");
        feeConfigDto.setSquarePrice("0");
        feeConfigDto.setAdditionalAmount("0");
        feeConfigDto.setStartTime(startTime);
        feeConfigDto.setEndTime(endTime);
        return feeConfigDto;
    }

    private void checkFormula(String computingFormula) {
        if (StringUtil.isEmpty(computingFormula)) {
            throw new CmdException("不支持的计费公式：");
        }
        if (METER_FORMULAS.contains(computingFormula)) {
            throw new CmdException("该费用项需抄表读数，不支持在合同费用中自动计算");
        }
        IFeeFormula feeFormula = ApplicationContextFactory.getBean("feeFormula_" + computingFormula, IFeeFormula.class);
        if (feeFormula == null) {
            throw new CmdException("不支持的计费公式：" + computingFormula);
        }
    }

    /**
     * 期数：入参优先；一次性为 1；租金模式为月数；其它按缴费周期折算
     */
    private int resolveCycle(JSONObject reqJson, FeeConfigDto feeConfigDto, String mode, int months) {
        Integer cycle = null;
        try {
            cycle = reqJson.getInteger("cycle");
        } catch (Exception e) {
            cycle = null;
        }
        if (cycle != null && cycle > 0) {
            return cycle;
        }
        if (FeeDto.FEE_FLAG_ONCE.equals(feeConfigDto.getFeeFlag())) {
            return 1;
        }
        if (MODE_RENT.equals(mode)) {
            return months;
        }
        int paymentCycle = 1;
        try {
            paymentCycle = Integer.parseInt(feeConfigDto.getPaymentCycle());
        } catch (Exception e) {
            paymentCycle = 1;
        }
        if (paymentCycle < 1) {
            paymentCycle = 1;
        }
        return (int) Math.ceil((double) months / (double) paymentCycle);
    }

    private String resolveFormulaName(FeeConfigDto feeConfigDto) {
        if (!StringUtil.isEmpty(feeConfigDto.getComputingFormulaName())) {
            return feeConfigDto.getComputingFormulaName();
        }
        return StringUtil.isEmpty(feeConfigDto.getComputingFormula()) ? "" : feeConfigDto.getComputingFormula();
    }

    /**
     * 按标的类型取档案数据，用于校验与展示
     */
    private ContractFeeComputeDto.ContractFeeComputeDetail buildDetail(String communityId, String payerObjType, String payerObjId) {
        ContractFeeComputeDto.ContractFeeComputeDetail detail = new ContractFeeComputeDto.ContractFeeComputeDetail();
        detail.setPayerObjType(payerObjType);
        detail.setObjId(payerObjId);

        if (FeeDto.PAYER_OBJ_TYPE_SHOP.equals(payerObjType)) {
            BuildingShopDto shopDto = new BuildingShopDto();
            shopDto.setShopId(payerObjId);
            shopDto.setCommunityId(communityId);
            List<BuildingShopDto> shopDtos = buildingShopInnerServiceSMOImpl.queryBuildingShops(shopDto);
            if (ListUtil.isNull(shopDtos)) {
                throw new CmdException("商铺 " + payerObjId + " 不存在或已失效，请重新选择");
            }
            BuildingShopDto shop = shopDtos.get(0);
            detail.setObjName(StringUtil.isEmpty(shop.getShopNum()) ? shop.getLocationName() : shop.getShopNum());
            detail.setBuiltUpArea(shop.getBuiltUpArea());
            detail.setAreaValue(shop.getShopArea());
            detail.setRentValue(shop.getShopRent());
            return detail;
        }

        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(payerObjId);
        roomDto.setCommunityId(communityId);
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        if (ListUtil.isNull(roomDtos)) {
            throw new CmdException("房屋 " + payerObjId + " 不存在或已失效，请重新选择");
        }
        RoomDto room = roomDtos.get(0);
        detail.setObjName(StringUtil.isEmpty(room.getRoomName()) ? room.getRoomNum() : room.getRoomName());
        detail.setBuiltUpArea(room.getBuiltUpArea());
        detail.setAreaValue(room.getRoomArea());
        detail.setRentValue(room.getRoomRent());
        return detail;
    }

    /**
     * 校验档案是否具备公式所需基数，缺数时给出精确到标的的提示
     */
    private void checkBaseValue(FeeConfigDto feeConfigDto, ContractFeeComputeDto.ContractFeeComputeDetail detail) {
        String computingFormula = feeConfigDto.getComputingFormula();
        String objName = detail.getObjName();
        boolean isShop = FeeDto.PAYER_OBJ_TYPE_SHOP.equals(detail.getPayerObjType());
        String objTypeName = isShop ? "商铺" : "房屋";
        if (BUILT_UP_AREA_FORMULAS.contains(computingFormula) && parseAmount(detail.getBuiltUpArea()) <= 0) {
            throw new CmdException(objTypeName + " " + objName + " 缺少建筑面积，请先完善" + (isShop ? "商铺" : "房源") + "档案");
        }
        if (ROOM_AREA_FORMULAS.contains(computingFormula) && parseAmount(detail.getAreaValue()) <= 0) {
            throw new CmdException(objTypeName + " " + objName + " 缺少" + (isShop ? "商铺面积" : "室内面积") + "，请先完善" + (isShop ? "商铺" : "房源") + "档案");
        }
        if (RENT_FORMULAS.contains(computingFormula) && parseAmount(detail.getRentValue()) <= 0) {
            throw new CmdException(objTypeName + " " + objName + " 缺少租金，请先完善" + (isShop ? "商铺" : "房源") + "档案");
        }
        if (BUILT_UP_AREA_FORMULAS.contains(computingFormula) || ROOM_AREA_FORMULAS.contains(computingFormula)) {
            if (StringUtil.isEmpty(feeConfigDto.getSquarePrice()) || parseAmount(feeConfigDto.getSquarePrice()) <= 0) {
                throw new CmdException("费用项「" + feeConfigDto.getFeeName() + "」未配置单价，请到费用项配置中维护");
            }
        }
        if ("2002".equals(computingFormula)
                && (StringUtil.isEmpty(feeConfigDto.getAdditionalAmount()) || parseAmount(feeConfigDto.getAdditionalAmount()) < 0)) {
            throw new CmdException("费用项「" + feeConfigDto.getFeeName() + "」未配置费用金额，请到费用项配置中维护");
        }
    }

    private FeeAmountDto computeFeeAmount(FeeConfigDto feeConfigDto, String payerObjType, String payerObjId,
                                          int cycle, double manualAmount, ContractFeeComputeDto.ContractFeeComputeDetail detail) {
        String computingFormula = feeConfigDto.getComputingFormula();
        IFeeFormula feeFormula = ApplicationContextFactory.getBean("feeFormula_" + computingFormula, IFeeFormula.class);
        if (feeFormula == null) {
            throw new CmdException("不支持的计费公式：" + computingFormula);
        }
        try {
            return feeFormula.computeFeeAmount(feeConfigDto, "", payerObjId, payerObjType, cycle, manualAmount);
        } catch (CmdException e) {
            throw e;
        } catch (Exception e) {
            throw new CmdException(detail.getObjName() + " 费用计算失败：" + e.getMessage());
        }
    }

    /**
     * 结算基数：面积类累加面积，租金类累加租金
     */
    private BigDecimal resolveBaseValue(String computingFormula, ContractFeeComputeDto.ContractFeeComputeDetail detail) {
        if (RENT_FORMULAS.contains(computingFormula)) {
            return new BigDecimal(String.valueOf(parseAmount(detail.getRentValue())));
        }
        if (ROOM_AREA_FORMULAS.contains(computingFormula)) {
            return new BigDecimal(String.valueOf(parseAmount(detail.getAreaValue())));
        }
        return new BigDecimal(String.valueOf(parseAmount(detail.getBuiltUpArea())));
    }

    private double parseAmount(String value) {
        if (StringUtil.isEmpty(value)) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatAmount(String value) {
        return formatAmount(parseAmount(value));
    }

    private String formatAmount(double value) {
        return new BigDecimal(String.valueOf(value)).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }
}
