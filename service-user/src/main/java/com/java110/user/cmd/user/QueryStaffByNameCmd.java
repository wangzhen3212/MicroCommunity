package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.constant.ServiceCodeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;

/**
 * 按姓名查询员工信息命令类
 * 
 * 该类实现了根据员工姓名查询员工详细信息的业务逻辑，包括参数验证、用户信息查询、
 * 商户员工信息关联查询等功能，最终返回符合条件的员工信息列表
 * 
 * @serviceCode query.staff.byName
 */
@Java110Cmd(serviceCode = "query.staff.byName")
public class QueryStaffByNameCmd extends Cmd {

    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 检查请求报文中是否包含必要的参数节点，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含storeId节点
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId节点");
        // 验证请求报文中必须包含name节点
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name节点");
    }

    /**
     * 执行按姓名查询员工信息的主要业务逻辑
     * 
     * 通过两步查询完成员工信息获取：
     * 1. 根据姓名查询用户基本信息
     * 2. 根据用户ID查询商户员工关联信息
     * 3. 合并返回完整的员工信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 数据解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 初始化返回结果JSON对象，设置默认总数和空数据数组
        JSONObject resultJson = JSONObject.parseObject("{\"total:\":10,\"datas\":[]}");
        
        // 第一步：根据姓名查询用户基本信息
        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode(ServiceCodeConstant.SERVICE_CODE_QUERY_USER_BY_NAME);
        dataQuery.setRequestParams(reqJson);
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> responseEntity = dataQuery.getResponseEntity();

        // 检查第一步查询结果状态，如果失败则直接返回
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(responseEntity);
            return;
        }

        // 从查询结果中提取用户ID字符串
        String useIds = getUserIds(responseEntity);
        if (StringUtil.isEmpty(useIds)) {
            // 如果没有查询到用户ID，直接返回空结果
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 获取用户详细信息数组
        JSONArray userInfos = getUserInfos(responseEntity);
        
        // 准备第二步查询参数：用户ID列表和商户ID
        JSONObject paramIn = new JSONObject();
        paramIn.put("userIds", useIds);
        paramIn.put("storeId", reqJson.getString("storeId"));

        // 第二步：根据用户ID列表查询商户员工关联信息
        dataQuery = new DataQuery();
        dataQuery.setServiceCode(ServiceCodeConstant.SERVICE_CODE_QUERY_STOREUSER_BYUSERIDS);
        dataQuery.setRequestParams(paramIn);
        queryServiceSMOImpl.commonQueryService(dataQuery);
        responseEntity = dataQuery.getResponseEntity();

        // 检查第二步查询结果状态，如果失败则直接返回
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(responseEntity);
            return;
        }

        // 合并用户基本信息和商户员工信息，构建完整的员工信息
        resultJson.put("datas", getStaffUsers(userInfos, responseEntity));
        // 设置成功响应
        responseEntity = new ResponseEntity<String>(resultJson.toJSONString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 合并用户基本信息和商户员工信息，构建完整的员工信息列表
     * 
     * 通过用户ID关联用户基本信息表和商户员工关联表，返回具有商户员工身份的用户信息
     * 
     * @param userInfos 用户基本信息数组
     * @param responseEntity 商户员工关联信息响应实体
     * @return JSONArray 合并后的员工信息数组
     */
    private JSONArray getStaffUsers(JSONArray userInfos, ResponseEntity<String> responseEntity) {
        JSONObject storeUserInfo = null;
        // 初始化返回的员工信息数组
        JSONArray newStaffUsers = new JSONArray();
        
        // 从响应中解析商户员工关联信息
        JSONArray storeUsers = JSONObject.parseObject(responseEntity.getBody().toString()).getJSONArray("storeUsers");
        
        // 如果没有商户员工信息，返回空数组
        if (storeUsers == null || storeUsers.size() < 1) {
            return newStaffUsers;
        }

        // 双重循环关联用户信息和商户员工信息
        for (int storeUserIndex = 0; storeUserIndex < storeUsers.size(); storeUserIndex++) {
            storeUserInfo = storeUsers.getJSONObject(storeUserIndex);

            for (int userIndex = 0; userIndex < userInfos.size(); userIndex++) {
                // 通过用户ID进行关联匹配
                if (userInfos.getJSONObject(userIndex).getString("userId").equals(storeUserInfo.getString("userId"))) {
                    // 找到匹配的用户，添加到结果数组中
                    newStaffUsers.add(userInfos.getJSONObject(userIndex));
                }
            }
        }

        return newStaffUsers;
    }

    /**
     * 从用户查询结果中提取用户ID字符串
     * 
     * 将用户ID数组转换为逗号分隔的字符串格式，用于后续的批量查询
     * 例如：将[{"userId":"123"},{"userId":"456"}]转换为"123,456"
     * 
     * @param responseEntity 用户查询响应实体
     * @return String 逗号分隔的用户ID字符串
     */
    private String getUserIds(ResponseEntity<String> responseEntity) {
        JSONObject userInfo = null;
        String userId = "";
        
        // 从响应中解析用户信息数组
        JSONArray resultInfo = JSONObject.parseObject(responseEntity.getBody().toString()).getJSONArray("users");
        
        // 如果没有用户信息，返回空字符串
        if (resultInfo == null || resultInfo.size() < 1) {
            return userId;
        }

        // 遍历用户信息，拼接用户ID字符串
        for (int userIndex = 0; userIndex < resultInfo.size(); userIndex++) {
            userInfo = resultInfo.getJSONObject(userIndex);
            userId += (userInfo.getString("userId") + ",");
        }

        // 去除末尾多余的逗号
        userId = userId.length() > 0 ? userId.substring(0, userId.lastIndexOf(",")) : userId;

        return userId;
    }

    /**
     * 从用户查询结果中获取用户信息数组
     * 
     * 直接从响应实体中提取用户信息JSON数组
     * 
     * @param responseEntity 用户查询响应实体
     * @return JSONArray 用户信息数组，如果没有数据则返回null
     */
    private JSONArray getUserInfos(ResponseEntity<String> responseEntity) {
        // 从响应体中解析用户信息数组
        JSONArray resultInfo = JSONObject.parseObject(responseEntity.getBody().toString()).getJSONArray("users");
        
        // 检查用户信息数组是否为空
        if (resultInfo == null || resultInfo.size() < 1) {
            return null;
        }

        return resultInfo;
    }
}