/**
 * 物业员工权限检查命令类
 * 
 * 该类用于检查物业员工是否具有相关物业的访问权限
 * 通过调用下游权限查询服务验证员工权限信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.property;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;

/**
 * 物业员工权限检查命令实现类
 * 服务码：check.property.staffHasProperty
 */
@Java110Cmd(serviceCode = "check.property.staffHasProperty")
public class CheckPropertyStaffHasPropertyCmd extends Cmd {
    
    /**
     * 查询服务接口，用于调用下游权限查询服务
     */
    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 该方法用于验证请求参数的合法性
     * 当前实现中该方法为空，可根据业务需求添加参数验证逻辑
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 参数验证逻辑可在此处添加
        // 当前实现为空，表示不进行特殊参数验证
    }

    /**
     * 命令执行方法
     * 
     * 主要业务逻辑：
     * 1. 从请求头获取用户ID
     * 2. 构建权限查询请求
     * 3. 调用下游权限查询服务
     * 4. 验证查询结果格式和内容
     * 5. 根据权限数量判断员工是否有物业权限
     * 6. 设置响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");
        
        // 创建数据查询对象，用于调用下游权限查询服务
        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode("query.user.privilege"); // 设置查询服务码
        
        // 构建查询参数
        JSONObject param = new JSONObject();
        param.put("userId", userId); // 将用户ID作为查询参数
        dataQuery.setRequestParams(param);
        
        // 调用查询服务获取用户权限信息
        queryServiceSMOImpl.commonQueryService(dataQuery);
        
        // 获取查询响应结果
        ResponseEntity<String> responseEntity = dataQuery.getResponseEntity();
        
        // 检查响应状态码，如果不是200则直接返回
        if(responseEntity.getStatusCode() != HttpStatus.OK){
            return ; // 服务调用失败，直接返回
        }
        
        // 获取响应体内容
        String resObj = responseEntity.getBody().toString();
        
        // 验证响应内容是否为有效的JSON格式
        Assert.isJsonObject(resObj,"下游服务返回格式错误，不是有效json格式"+resObj);
        
        // 解析JSON响应
        JSONObject resJson = JSONObject.parseObject(resObj);
        
        // 验证响应JSON中是否包含count字段
        Assert.jsonObjectHaveKey(resJson,"count","下游服务返回格式错误，返回报文中未包含count"+resObj);
        
        // 获取权限数量
        long count = resJson.getLongValue("count");
        
        // 检查权限数量，如果小于1表示没有相关物业权限
        if(count < 1){
            // 抛出异常，表示当前员工没有相关物业信息
            throw  new ListenerExecuteException(ResponseConstant.RESULT_CODE_INNER_ERROR,"当前员工没有相关物业信息，数据异常请检查");
        }
        
        // 创建成功响应
        responseEntity = new ResponseEntity<String>("成功",HttpStatus.OK);
        
        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}