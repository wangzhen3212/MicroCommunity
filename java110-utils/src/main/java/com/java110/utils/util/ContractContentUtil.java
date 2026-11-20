package com.java110.utils.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合同正文变量渲染工具，支持 {{key}} 与 ${key} 两种占位符
 * 只替换 variables 中存在的变量，正文中其它占位符原样保留
 */
public class ContractContentUtil {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}|\\$\\{\\s*([a-zA-Z0-9_]+)\\s*}");

    /**
     * 渲染合同正文
     *
     * @param content   含变量占位符的正文
     * @param variables 变量值，key 为占位符名称
     * @return 渲染后正文，无变量时返回原文
     */
    public static String render(String content, Map<String, Object> variables) {
        if (StringUtil.isEmpty(content) || variables == null || variables.isEmpty()) {
            return content;
        }
        Matcher matcher = VAR_PATTERN.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (!variables.containsKey(key)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            Object value = variables.get(key);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : value.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
