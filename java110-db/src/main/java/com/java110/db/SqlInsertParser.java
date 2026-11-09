package com.java110.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * insert 语句列段/VALUES 段解析器
 * 按 VALUES 子句结构逐列解析，占位符（?）按参数顺序取值，字面量列（如 '0'、-1、now()）跳过，
 * 从而避免「列序号 == 参数序号」的硬配对假设。
 */
public class SqlInsertParser {

    private static final String VALUES_KEY_WORD = "values";

    private static final String PLACEHOLDER = "?";

    private SqlInsertParser() {
    }

    /**
     * 解析 insert 语句的值部分
     *
     * @param sql    已转为小写且空白压缩后的 insert 语句
     * @param values 按 ParameterMapping 顺序排列的参数值（已带单引号）
     * @return 每行一个「列名 -> 值」有序 Map；解析失败（结构无法安全对齐）返回 null；无有效值返回空列表
     */
    public static List<Map<String, String>> parseInsertValues(String sql, List<String> values) {
        if (sql == null || "".equals(sql)) {
            return null;
        }
        List<String> valueList = values == null ? new ArrayList<String>() : values;
        int intoIndex = sql.indexOf("into");
        if (intoIndex < 0) {
            return null;
        }
        int columnStart = sql.indexOf('(', intoIndex);
        if (columnStart < 0) {
            return null;
        }
        int columnEnd = findClosingBracket(sql, columnStart);
        if (columnEnd < 0) {
            return null;
        }
        List<String> columns = splitTopLevel(sql.substring(columnStart + 1, columnEnd));
        int valuesIndex = sql.indexOf(VALUES_KEY_WORD, columnEnd);
        if (valuesIndex < 0) {
            return null;
        }
        List<String> rows = parseRows(sql, valuesIndex + VALUES_KEY_WORD.length());
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        List<Map<String, String>> rowValues = new ArrayList<>();
        int valueIndex = 0;
        for (String row : rows) {
            List<String> tokens = splitTopLevel(row);
            if (tokens.size() != columns.size()) {
                return null;
            }
            Map<String, String> rowValue = new LinkedHashMap<>();
            for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
                String token = tokens.get(tokenIndex).trim();
                int placeholderNum = countPlaceholder(token);
                if (placeholderNum == 0) {
                    // 字面量列不参与回退日志
                    continue;
                }
                if (placeholderNum > 1 || !PLACEHOLDER.equals(token)) {
                    // 函数/表达式内含占位符时无法安全对齐，解析失败，宁缺勿错
                    return null;
                }
                if (valueIndex >= valueList.size()) {
                    return null;
                }
                String value = valueList.get(valueIndex);
                valueIndex++;
                if (value == null || "''".equals(value)) {
                    continue;
                }
                rowValue.put(columns.get(tokenIndex), value);
            }
            if (!rowValue.isEmpty()) {
                rowValues.add(rowValue);
            }
        }
        if (valueIndex != valueList.size()) {
            return null;
        }
        return rowValues;
    }

    /**
     * 切出 values 后的每一行（...）
     *
     * @param sql    insert 语句
     * @param start  values 关键字之后的下标
     * @return 每行括号内部内容
     */
    private static List<String> parseRows(String sql, int start) {
        List<String> rows = new ArrayList<>();
        int index = start;
        while (index < sql.length()) {
            char tmpChar = sql.charAt(index);
            if (tmpChar == ',' || Character.isWhitespace(tmpChar)) {
                index++;
                continue;
            }
            if (tmpChar != '(') {
                break;
            }
            int rowEnd = findClosingBracket(sql, index);
            if (rowEnd < 0) {
                return null;
            }
            rows.add(sql.substring(index + 1, rowEnd));
            index = rowEnd + 1;
        }
        return rows;
    }

    /**
     * 查找与起始括号配对的右括号，忽略单引号内的括号
     */
    private static int findClosingBracket(String sql, int startIndex) {
        boolean inSingleQuote = false;
        int depth = 0;
        for (int index = startIndex; index < sql.length(); index++) {
            char tmpChar = sql.charAt(index);
            if (tmpChar == '\'') {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (inSingleQuote) {
                continue;
            }
            if (tmpChar == '(') {
                depth++;
            } else if (tmpChar == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    /**
     * 按顶层逗号切分，忽略单引号内及括号内的逗号
     */
    private static List<String> splitTopLevel(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        StringBuilder tmpToken = new StringBuilder();
        boolean inSingleQuote = false;
        int parenthesisNum = 0;
        for (int index = 0; index < text.length(); index++) {
            char tmpChar = text.charAt(index);
            if (tmpChar == '\'') {
                inSingleQuote = !inSingleQuote;
                tmpToken.append(tmpChar);
            } else if (!inSingleQuote && tmpChar == '(') {
                parenthesisNum++;
                tmpToken.append(tmpChar);
            } else if (!inSingleQuote && tmpChar == ')') {
                parenthesisNum--;
                tmpToken.append(tmpChar);
            } else if (!inSingleQuote && parenthesisNum == 0 && tmpChar == ',') {
                tokens.add(tmpToken.toString());
                tmpToken.setLength(0);
            } else {
                tmpToken.append(tmpChar);
            }
        }
        if (tmpToken.toString().trim().length() > 0) {
            tokens.add(tmpToken.toString());
        }
        return tokens;
    }

    /**
     * 统计占位符个数，忽略单引号内的 ?
     */
    private static int countPlaceholder(String token) {
        if (token == null) {
            return 0;
        }
        int num = 0;
        boolean inSingleQuote = false;
        for (int index = 0; index < token.length(); index++) {
            char tmpChar = token.charAt(index);
            if (tmpChar == '\'') {
                inSingleQuote = !inSingleQuote;
            } else if (!inSingleQuote && tmpChar == '?') {
                num++;
            }
        }
        return num;
    }
}
