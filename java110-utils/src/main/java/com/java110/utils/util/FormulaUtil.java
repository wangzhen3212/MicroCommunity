package com.java110.utils.util;

import com.java110.utils.constant.FeeConfigConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;

/**
 * 公式计算工具类
 * <p>
 * Java 15 及以上（含 Java 17）已移除内置 nashorn 引擎，javax.script 取不到 JavaScript 引擎，
 * 这里统一通过 nashorn-core 依赖获取引擎，并对引擎做线程内缓存复用。
 */
public final class FormulaUtil {

    private static final Logger logger = LoggerFactory.getLogger(FormulaUtil.class);

    /**
     * 引擎名称，JDK8 内置为 JavaScript，nashorn-core 同时注册 nashorn 与 JavaScript
     */
    private static final String[] ENGINE_NAMES = {"nashorn", "JavaScript", "javascript"};

    private static final ThreadLocal<ScriptEngine> ENGINE_CACHE = new ThreadLocal<ScriptEngine>() {
        @Override
        protected ScriptEngine initialValue() {
            return createEngine();
        }
    };

    private static volatile String engineInfo;

    private FormulaUtil() {
    }

    private static ScriptEngine createEngine() {
        for (String engineName : ENGINE_NAMES) {
            try {
                ScriptEngine engine = new ScriptEngineManager(FormulaUtil.class.getClassLoader()).getEngineByName(engineName);
                if (engine != null) {
                    logger.info("公式计算引擎初始化成功，引擎名称为：{}", engineName);
                    return engine;
                }
            } catch (Throwable e) {
                logger.error("公式计算引擎初始化异常，引擎名称为：" + engineName, e);
            }
        }
        logger.error("公式计算引擎初始化失败，未获取到 nashorn 引擎，请确认 java110-utils 依赖 org.openjdk.nashorn:nashorn-core 已随包发布");
        return null;
    }

    /**
     * 获取当前线程缓存的公式引擎
     *
     * @return 引擎，获取不到时返回 null
     */
    public static ScriptEngine getEngine() {
        return ENGINE_CACHE.get();
    }

    /**
     * 公式归一化：去掉换行符并去空格
     *
     * @param expression 公式表达式
     * @return 归一化后的表达式
     */
    public static String normalize(String expression) {
        if (expression == null) {
            return "";
        }
        return expression.replace("\n", "")
                .replace("\r", "")
                .trim();
    }

    /**
     * 公式求值
     *
     * @param expression 已完成变量替换的表达式
     * @return 求值结果，异常时返回 0
     */
    public static BigDecimal evalAmount(String expression) {
        return evalAmount(expression, expression);
    }

    /**
     * 公式求值
     *
     * @param expression  已完成变量替换的表达式
     * @param formulaText 公式原文，仅用于异常日志
     * @return 求值结果，异常时返回 0
     */
    public static BigDecimal evalAmount(String expression, String formulaText) {
        String expr = normalize(expression);
        if (StringUtil.isEmpty(expr)) {
            return new BigDecimal(0);
        }
        try {
            ScriptEngine engine = getEngine();
            if (engine == null) {
                logger.error("公式计算异常，未获取到 nashorn 引擎，公式为【{}】，计算【{}】", formulaText, expr);
                return new BigDecimal(0);
            }
            Object value = engine.eval(expr);
            BigDecimal valueObj = new BigDecimal(Double.parseDouble(value.toString()));
            return valueObj.setScale(FeeConfigConstant.FEE_SCALE, BigDecimal.ROUND_HALF_UP);
        } catch (Exception e) {
            logger.error("公式计算异常，公式为【" + formulaText + "】,计算 【" + expr + "】异常", e);
            return new BigDecimal(0);
        }
    }

    /**
     * 引擎信息，供启动自检及运维排查使用
     *
     * @return 引擎信息
     */
    public static String engineInfo() {
        if (engineInfo != null) {
            return engineInfo;
        }
        ScriptEngine engine = getEngine();
        if (engine == null) {
            engineInfo = "未获取到公式引擎(nashorn 缺失)";
            return engineInfo;
        }
        engineInfo = engine.getFactory().getEngineName() + " " + engine.getFactory().getEngineVersion();
        return engineInfo;
    }
}
