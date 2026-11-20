package com.java110.store.util;

import com.java110.po.contract.ContractCollectionPlanPo;

import java.math.BigDecimal;

/**
 * 合同收款计划落库前的空串归一
 * <p>
 * Mapper 一律不做判空（不写 nullif / ifnull），空串归一全部收口在 SMO 实现层调用本工具，
 * 保证 Mapper 拿到的金额、期数永远是合法值或 null。
 */
public class ContractPlanNormalizeUtil {

    private static final String DEFAULT_PLAN_SEQ = "1";
    private static final String DEFAULT_AMOUNT = "0.00";

    private ContractPlanNormalizeUtil() {
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
     * 整型字段（期次）：空串 / 空白 → 默认值；null 按 fillNull 决定
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
            throw new IllegalArgumentException("收款计划[" + label + "]的格式不正确，请填写数字");
        }
    }

    private static void assertInteger(String value, String label) {
        try {
            new BigDecimal(value).toBigIntegerExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("收款计划[" + label + "]的格式不正确，请填写整数");
        }
    }
}
