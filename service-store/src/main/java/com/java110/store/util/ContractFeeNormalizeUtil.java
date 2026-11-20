package com.java110.store.util;

import com.java110.dto.contract.ContractFeeDto;
import com.java110.po.contract.ContractCollectionPlanPo;
import com.java110.po.contract.ContractFeePo;

import java.math.BigDecimal;

/**
 * 合同费用构成 / 收款计划落库前的空串归一
 * <p>
 * Mapper 一律不做判空（不写 nullif / ifnull），空串归一全部收口在 SMO 实现层调用本工具，
 * 保证 Mapper 拿到的金额、期数、系数永远是合法值或 null。
 */
public class ContractFeeNormalizeUtil {

    private static final String DEFAULT_FEE_SEQ = "1";
    private static final String DEFAULT_PLAN_SEQ = "1";
    private static final String DEFAULT_AMOUNT = "0.00";
    private static final String DEFAULT_COEFFICIENT = "1.0000";
    private static final String DEFAULT_MONTHS = "0";

    private ContractFeeNormalizeUtil() {
    }

    /**
     * 费用行新增前归一：空串 / null 一律补列默认值（NOT NULL 列不能写 null）
     */
    public static void normalizeForSave(ContractFeePo feePo) {
        if (feePo == null) {
            return;
        }
        feePo.setFeeSeq(normalizeInt(feePo.getFeeSeq(), DEFAULT_FEE_SEQ, "排序号", true));
        feePo.setUnitPrice(normalizeDecimal(feePo.getUnitPrice(), DEFAULT_AMOUNT, "单价", true));
        feePo.setAdditionalAmount(normalizeDecimal(feePo.getAdditionalAmount(), DEFAULT_AMOUNT, "附加费", true));
        feePo.setBaseAmount(normalizeDecimal(feePo.getBaseAmount(), DEFAULT_AMOUNT, "结算基数", true));
        feePo.setAmount(normalizeDecimal(feePo.getAmount(), DEFAULT_AMOUNT, "费用金额", true));
        feePo.setCoefficient(normalizeDecimal(feePo.getCoefficient(), DEFAULT_COEFFICIENT, "系数", true));
        feePo.setMonths(normalizeInt(feePo.getMonths(), DEFAULT_MONTHS, "期数", true));
        feePo.setManualAmount(normalizeManualAmount(feePo.getManualAmount()));
        feePo.setCycle(normalizeCycle(feePo.getCycle()));
        feePo.setIncludeInAmount(normalizeIncludeInAmount(feePo.getIncludeInAmount()));
    }

    /**
     * 费用行修改前归一：空串补列默认值，null 一律保持 null（不参与更新，避免局部更新被补值）
     */
    public static void normalizeForUpdate(ContractFeePo feePo) {
        if (feePo == null) {
            return;
        }
        feePo.setFeeSeq(normalizeInt(feePo.getFeeSeq(), DEFAULT_FEE_SEQ, "排序号", false));
        feePo.setUnitPrice(normalizeDecimal(feePo.getUnitPrice(), DEFAULT_AMOUNT, "单价", false));
        feePo.setAdditionalAmount(normalizeDecimal(feePo.getAdditionalAmount(), DEFAULT_AMOUNT, "附加费", false));
        feePo.setBaseAmount(normalizeDecimal(feePo.getBaseAmount(), DEFAULT_AMOUNT, "结算基数", false));
        feePo.setAmount(normalizeDecimal(feePo.getAmount(), DEFAULT_AMOUNT, "费用金额", false));
        feePo.setCoefficient(normalizeDecimal(feePo.getCoefficient(), DEFAULT_COEFFICIENT, "系数", false));
        feePo.setMonths(normalizeInt(feePo.getMonths(), DEFAULT_MONTHS, "期数", false));
        feePo.setManualAmount(normalizeManualAmount(feePo.getManualAmount()));
        feePo.setCycle(normalizeCycle(feePo.getCycle()));
        if (feePo.getIncludeInAmount() != null) {
            feePo.setIncludeInAmount(normalizeIncludeInAmount(feePo.getIncludeInAmount()));
        }
    }

    /**
     * 收款计划新增前归一
     */
    public static void normalizePlanForSave(ContractCollectionPlanPo planPo) {
        if (planPo == null) {
            return;
        }
        planPo.setPlanSeq(normalizeInt(planPo.getPlanSeq(), DEFAULT_PLAN_SEQ, "期次", true));
        planPo.setPlanAmount(normalizeDecimal(planPo.getPlanAmount(), DEFAULT_AMOUNT, "计划金额", true));
        planPo.setRatio(normalizeDecimal(planPo.getRatio(), DEFAULT_AMOUNT, "比例", true));
        planPo.setPlanTime(normalizeTime(planPo.getPlanTime()));
        planPo.setPayTime(normalizeTime(planPo.getPayTime()));
    }

    /**
     * 收款计划修改前归一：null 一律保持 null
     */
    public static void normalizePlanForUpdate(ContractCollectionPlanPo planPo) {
        if (planPo == null) {
            return;
        }
        planPo.setPlanAmount(normalizeDecimal(planPo.getPlanAmount(), DEFAULT_AMOUNT, "计划金额", false));
        planPo.setRatio(normalizeDecimal(planPo.getRatio(), DEFAULT_AMOUNT, "比例", false));
        planPo.setPlanSeq(normalizeInt(planPo.getPlanSeq(), DEFAULT_PLAN_SEQ, "期次", false));
        planPo.setPlanTime(normalizeTime(planPo.getPlanTime()));
        planPo.setPayTime(normalizeTime(planPo.getPayTime()));
    }

    /**
     * 是否计入合同金额归一化为 1/0（前端可能传布尔）
     */
    public static String normalizeIncludeInAmount(Object includeInAmount) {
        if (includeInAmount == null) {
            return ContractFeeDto.INCLUDE_IN_AMOUNT_YES;
        }
        if (includeInAmount instanceof Boolean) {
            return ((Boolean) includeInAmount) ? ContractFeeDto.INCLUDE_IN_AMOUNT_YES : ContractFeeDto.INCLUDE_IN_AMOUNT_NO;
        }
        String value = String.valueOf(includeInAmount).trim();
        if (isEmpty(value) || "1".equals(value) || "true".equalsIgnoreCase(value)) {
            return ContractFeeDto.INCLUDE_IN_AMOUNT_YES;
        }
        return ContractFeeDto.INCLUDE_IN_AMOUNT_NO;
    }

    /**
     * 金额类字段：空串 / 空白 → 默认值；null 按 fillNull 决定是否补默认值；非法数字直接报错
     */
    private static String normalizeDecimal(String value, String defaultValue, String label, boolean fillNull) {
        if (value == null) {
            return fillNull ? defaultValue : null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return defaultValue;
        }
        assertDecimal(trimmed, label);
        return trimmed;
    }

    /**
     * 整型字段（排序号 / 期数）：空串 / 空白 → 默认值；null 按 fillNull 决定
     */
    private static String normalizeInt(String value, String defaultValue, String label, boolean fillNull) {
        if (value == null) {
            return fillNull ? defaultValue : null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return defaultValue;
        }
        assertInteger(trimmed, label);
        return trimmed;
    }

    /**
     * 手工金额：空串统一转 null，保留「未手工录入」语义（与手工录入 0 元区分）
     */
    private static String normalizeManualAmount(String manualAmount) {
        String trimmed = trimToNull(manualAmount);
        if (trimmed == null) {
            return null;
        }
        assertDecimal(trimmed, "手工金额");
        return trimmed;
    }

    /**
     * 付费周期 char(2)：空串转 null，不写无意义值
     */
    private static String normalizeCycle(String cycle) {
        return trimToNull(cycle);
    }

    /**
     * 日期时间：空串转 null
     */
    private static String normalizeTime(String time) {
        return trimToNull(time);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return isEmpty(trimmed) ? null : trimmed;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    private static void assertDecimal(String value, String label) {
        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("费用项[" + label + "]的格式不正确，请填写数字");
        }
    }

    private static void assertInteger(String value, String label) {
        try {
            new BigDecimal(value).toBigIntegerExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("费用项[" + label + "]的格式不正确，请填写整数");
        }
    }
}
