package com.java110.db;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.client.RestTemplate;
import com.java110.core.context.Environment;
import com.java110.core.factory.Java110TransactionalFactory;
import com.java110.db.dao.IQueryServiceDAO;
import com.java110.dto.order.OrderItemDto;
import com.java110.utils.constant.ServiceConstant;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.http.*;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.*;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class,
                Object.class})
})
public class Java110MybatisInterceptor implements Interceptor {
    private static Logger logger = LoggerFactory.getLogger(Java110MybatisInterceptor.class);

    IQueryServiceDAO queryServiceDAOImpl;
    RestTemplate restTemplate;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (StringUtil.isEmpty(Java110TransactionalFactory.getOId())) { //未开启事务
            return invocation.proceed();
        }
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        Object parameter = null;
        if (invocation.getArgs().length > 1) {
            parameter = invocation.getArgs()[1];
        }

        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        Configuration configuration = mappedStatement.getConfiguration();
        Map<String, Object> sqlValue = new HashMap<>();
        List<Map<String, String>> insertValues = new ArrayList<>();
        //获取sql语句
        String sql = showSql(configuration, boundSql, sqlValue, insertValues, sqlCommandType);
        if(Environment.isStartBootWay()){
            restTemplate = ApplicationContextFactory.getBean("outRestTemplate", RestTemplate.class);
        }else {
            restTemplate = ApplicationContextFactory.getBean("restTemplate", RestTemplate.class);
        }
        switch (sqlCommandType) {
            case INSERT:
                dealInsertSql(mappedStatement, parameter, sql, insertValues);
                break;
            case UPDATE:
                dealUpdateSql(mappedStatement, parameter, sql, sqlValue);
                break;
            case DELETE:
                dealDeleteSql(mappedStatement, parameter, sql, sqlValue);
                break;
        }
        return invocation.proceed();
    }

    /**
     * 处理删除sql
     *
     * @param mappedStatement
     * @param parameter
     */
    private void dealDeleteSql(MappedStatement mappedStatement, Object parameter, String sql, Map<String, Object> sqlValue) {

        String tmpTable = sql.substring(sql.indexOf("from") + 4, sql.indexOf("where")).trim();
        String tmpTableHasT = tmpTable;
        if(tmpTable.indexOf(" ") > 0){
            tmpTable = tmpTable.substring(0,tmpTable.indexOf(" "));
        }
        String tmpWhere = sql.substring(sql.indexOf("where"));
        //插入操作时之前的 没有数据 所以 preValue 为空对象
        JSONArray preValues = new JSONArray();

        String execSql = "select * from " + tmpTableHasT + " " + tmpWhere;

        queryServiceDAOImpl = ApplicationContextFactory.getBean("queryServiceDAOImpl", IQueryServiceDAO.class);
        List<Map<String, Object>> deleteDatas = queryServiceDAOImpl.executeSql(execSql, null);

        if (deleteDatas != null && deleteDatas.size() > 0) {
            for (Map<String, Object> map : deleteDatas) {
                dealReturnMap(map);
                preValues.add(map);
            }
        }

        JSONArray afterValues = new JSONArray();

        JSONObject logText = new JSONObject();
        logText.put("preValue", preValues);
        logText.put("afterValue", afterValues);

        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setbId("-1");
        orderItemDto.setAction("DEL");
        orderItemDto.setActionObj(tmpTable.trim());
        orderItemDto.setLogText(logText.toJSONString());
        orderItemDto.setServiceName(ApplicationContextFactory.getApplicationName());
        orderItemDto.setoId(Java110TransactionalFactory.getOId());

        String url = ServiceConstant.SERVICE_ORDER_URL + "/order/oIdApi/createOrderItem";
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity httpEntity = new HttpEntity(orderItemDto.toString(), httpHeaders);
        if(Environment.isStartBootWay()){
            url = ServiceConstant.BOOT_SERVICE_ORDER_URL + "/order/oIdApi/createOrderItem";
        }
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException("注册事务回滚日志失败" + responseEntity);
        }
    }

    /**
     * 处理修改 sql
     *
     * @param mappedStatement
     * @param parameter
     */
    private void dealUpdateSql(MappedStatement mappedStatement, Object parameter, String sql, Map<String, Object> sqlValue) {
        //RestTemplate restTemplate = ApplicationContextFactory.getBean("restTemplate", RestTemplate.class);

        String tmpTable = sql.substring(sql.indexOf("update") + 6, sql.indexOf("set ")).trim();

        String tmpTableHasT = tmpTable;

        if(tmpTable.indexOf(" ") > 0){
            tmpTable = tmpTable.substring(0,tmpTable.indexOf(" "));
        }
        String tmpWhere = sql.substring(sql.indexOf("where"));

        //插入操作时之前的 没有数据 所以 preValue 为空对象
        JSONArray preValues = new JSONArray();
        JSONArray afterValues = new JSONArray();
        JSONObject afterVaule = null;
        String execSql = "select * from " + tmpTableHasT + " " + tmpWhere;
        queryServiceDAOImpl = ApplicationContextFactory.getBean("queryServiceDAOImpl", IQueryServiceDAO.class);
        List<Map<String, Object>> deleteDatas = queryServiceDAOImpl.executeSql(execSql, null);

        if (deleteDatas != null && deleteDatas.size() > 0) {
            for (Map<String, Object> map : deleteDatas) {
                dealReturnMap(map);
                preValues.add(map);
                afterVaule = new JSONObject();
                afterVaule.putAll(map);
                afterVaule.putAll(sqlValue);
                afterValues.add(afterVaule);
            }
        }

        JSONObject logText = new JSONObject();
        logText.put("preValue", preValues);
        logText.put("afterValue", afterValues);

        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setbId("-1");
        orderItemDto.setAction("MOD");
        orderItemDto.setActionObj(tmpTable.trim());
        orderItemDto.setLogText(logText.toJSONString());
        orderItemDto.setServiceName(ApplicationContextFactory.getApplicationName());
        orderItemDto.setoId(Java110TransactionalFactory.getOId());

        String url = ServiceConstant.SERVICE_ORDER_URL + "/order/oIdApi/createOrderItem";
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity httpEntity = new HttpEntity(orderItemDto.toString(), httpHeaders);
        ResponseEntity<String> responseEntity = null;
        if(Environment.isStartBootWay()){
            url = ServiceConstant.BOOT_SERVICE_ORDER_URL + "/order/oIdApi/createOrderItem";
        }
        responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException("注册事务回滚日志失败" + responseEntity);
        }
    }

    private void dealReturnMap(Map<String, Object> map) {
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof String) {
                map.put(key, "'" + ((String) value).replace("'", "''") + "'");
            } else if (value instanceof Date) {
                String tmpValue = DateUtil.getFormatTimeString((Date) value, DateUtil.DATE_FORMATE_STRING_A);
                map.put(key, "'" + tmpValue + "'");
            } else if (value instanceof Timestamp) {
                Date date = new Date(((Timestamp) value).getTime());
                String tmpValue = DateUtil.getFormatTimeString(date, DateUtil.DATE_FORMATE_STRING_A);
                map.put(key, "'" + tmpValue + "'");
            } else if (value instanceof Double) {
                map.put(key, "'" + map.get(key) + "'");
            } else {
                if (value != null) {
                    map.put(key, "'" + value.toString() + "'");
                } else {
                    map.put(key, "''");
                }
            }
        }
    }

    /**
     * 处理insert 语句
     *
     * @param mappedStatement
     * @param parameter
     */
    private void dealInsertSql(MappedStatement mappedStatement, Object parameter, String sql, List<Map<String, String>> insertValues) {

        // RestTemplate restTemplate = ApplicationContextFactory.getBean("restTemplate", RestTemplate.class);

        String tmpTable = sql.substring(sql.toLowerCase().indexOf("into") + 4, sql.indexOf("(")).trim();

        if(tmpTable.indexOf(" ") > 0){
            tmpTable = tmpTable.substring(0,tmpTable.indexOf(" "));
        }

        if (insertValues == null || insertValues.isEmpty()) {
            logger.warn("未解析到 insert 列值，跳过事务回退日志，actionObj=" + tmpTable + " sql=" + sql);
            return;
        }

        //插入操作时之前的 没有数据 所以 preValue 为空对象
        JSONArray preValues = new JSONArray();

        JSONArray afterValues = new JSONArray();

        // 多行插入时每行一条记录，列顺序与 SQL 列顺序保持一致
        for (Map<String, String> insertValue : insertValues) {
            JSONObject afterValue = new JSONObject(true);
            afterValue.putAll(insertValue);
            afterValues.add(afterValue);
        }


        JSONObject logText = new JSONObject();
        logText.put("preValue", preValues);
        logText.put("afterValue", afterValues);

        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setbId("-1");
        orderItemDto.setAction("ADD");
        orderItemDto.setActionObj(tmpTable.trim());
        orderItemDto.setLogText(logText.toJSONString());
        orderItemDto.setServiceName(ApplicationContextFactory.getApplicationName());
        orderItemDto.setoId(Java110TransactionalFactory.getOId());

        String url = ServiceConstant.SERVICE_ORDER_URL + "/order/oIdApi/createOrderItem";
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity httpEntity = new HttpEntity(orderItemDto.toString(), httpHeaders);
        if(Environment.isStartBootWay()){
            url = ServiceConstant.BOOT_SERVICE_ORDER_URL + "/order/oIdApi/createOrderItem";
        }
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new IllegalArgumentException("注册事务回滚日志失败" + responseEntity);
        }

    }


    @Override
    public Object plugin(Object target) {

        return Plugin.wrap(target, this);

    }

    @Override
    public void setProperties(Properties properties) {

    }

    public String showSql(Configuration configuration, BoundSql boundSql, Map<String, Object> sqlValue,
                          List<Map<String, String>> insertValues, SqlCommandType sqlCommandType) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ").toLowerCase();

        List<String> values = new ArrayList<>();
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                values.add(getParameterValue(parameterObject));
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        values.add(getParameterValue(metaObject.getValue(propertyName)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        values.add(getParameterValue(boundSql.getAdditionalParameter(propertyName)));
                    }
                }
            }
        }

        // 按占位符顺序重建带值 sql，避免 replaceFirst 的正则替换风险（值含 $、\、? 时会异常或替换错位）
        String sqlWithValues = buildSqlWithValues(sql, values);

        if (sqlCommandType == SqlCommandType.INSERT) {
            List<Map<String, String>> rows = SqlInsertParser.parseInsertValues(sql, values);
            if (rows == null) {
                logger.warn("insert 语句列与占位符无法安全配对，跳过事务回退日志，sql=" + sql);
            } else if (insertValues != null) {
                insertValues.addAll(rows);
            }
        } else if (sqlCommandType == SqlCommandType.UPDATE) {
            String tmpKey = sqlWithValues.substring(sqlWithValues.indexOf("set") + 3, sqlWithValues.indexOf("where"));
            List<String> tmpKeys = splitSetKeys(tmpKey);
            for (String tmpSetKey : tmpKeys) {
                int equalIndex = tmpSetKey.indexOf("=");
                if (equalIndex < 0) {
                    throw new IllegalArgumentException("update 语句可能有问题，没有 set 中出错 " + sqlWithValues);
                }
                String key = tmpSetKey.substring(0, equalIndex);
                if (key.contains(".")) {
                    key = key.substring(key.indexOf(".") + 1).trim();
                } else {
                    key = key.trim();
                }
                String value = tmpSetKey.substring(equalIndex + 1).trim();
                if ("''".equals(value)) {
                    continue;
                }
                sqlValue.put(key, value);
            }

        } else if (sqlCommandType == SqlCommandType.DELETE) {

        }


        return sqlWithValues;
    }

    /**
     * 按占位符顺序用参数值重建 sql
     *
     * @param sql    已转小写并压缩空白的 sql
     * @param values 参数值列表
     * @return 带值的 sql
     */
    private String buildSqlWithValues(String sql, List<String> values) {
        if (values == null || values.isEmpty()) {
            return sql;
        }
        StringBuilder sqlBuilder = new StringBuilder();
        int valueIndex = 0;
        boolean inSingleQuote = false;
        for (int index = 0; index < sql.length(); index++) {
            char tmpChar = sql.charAt(index);
            if (tmpChar == '\'') {
                inSingleQuote = !inSingleQuote;
                sqlBuilder.append(tmpChar);
                continue;
            }
            if (tmpChar == '?' && !inSingleQuote && valueIndex < values.size()) {
                String value = values.get(valueIndex);
                valueIndex++;
                sqlBuilder.append(value == null ? "''" : value);
            } else {
                sqlBuilder.append(tmpChar);
            }
        }
        return sqlBuilder.toString();
    }


    /**
     * 按逗号切分 update set 片段
     * 跳过单引号内和括号内（函数调用内）的逗号，
     * 保证 nullif('xxx', '') 等函数表达式作为一个完整片段
     *
     * @param tmpKey set 部分 sql
     * @return set 片段列表
     */
    private List<String> splitSetKeys(String tmpKey) {
        List<String> setKeys = new ArrayList<>();
        StringBuilder tmpSetKey = new StringBuilder();
        boolean inSingleQuote = false;
        int parenthesesNum = 0;
        for (int index = 0; index < tmpKey.length(); index++) {
            char tmpChar = tmpKey.charAt(index);
            if (tmpChar == '\'') {
                inSingleQuote = !inSingleQuote;
                tmpSetKey.append(tmpChar);
            } else if (tmpChar == '(' && !inSingleQuote) {
                parenthesesNum++;
                tmpSetKey.append(tmpChar);
            } else if (tmpChar == ')' && !inSingleQuote) {
                parenthesesNum--;
                tmpSetKey.append(tmpChar);
            } else if (tmpChar == ',' && !inSingleQuote && parenthesesNum == 0) {
                setKeys.add(tmpSetKey.toString());
                tmpSetKey.setLength(0);
            } else {
                tmpSetKey.append(tmpChar);
            }
        }
        if (tmpSetKey.length() > 0) {
            setKeys.add(tmpSetKey.toString());
        }
        return setKeys;
    }


    private String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString().replace("'", "''") + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(obj) + "'";
//	            System.out.println(value);
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "''";
            }

        }
        return value;
    }



}
