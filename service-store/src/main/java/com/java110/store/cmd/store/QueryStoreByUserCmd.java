package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 查询用户是否存在商户信息命令类
 * 根据用户ID查询对应的商户信息，支持从请求头或请求参数中获取用户ID
 * 服务码：query.store.byuser
 */
@Java110Cmd(serviceCode = "query.store.byuser")
public class QueryStoreByUserCmd extends Cmd {
    
    /**
     * 查询服务接口
     */
    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /**
     * 参数验证方法
     * 当前实现中未进行具体参数验证，留待后续扩展
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 参数验证逻辑暂未实现，可根据业务需求添加具体验证规则
    }

    /**
     * 执行命令方法
     * 根据用户ID查询商户信息，区分员工和业主身份返回不同结果
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID
        String userId = event.getCmdDataFlowContext().getReqHeaders().get("user-id");

        // 如果请求头中未获取到用户ID，则从请求参数中获取
        if(StringUtil.isEmpty(userId)){
            userId = reqJson.getString("userId");
        }

        // 构建数据查询对象
        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode("query.store.byuser");
        JSONObject param = new JSONObject();
        param.put("userId", userId);
        dataQuery.setRequestParams(param);
        
        // 执行查询服务
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> responseEntity = dataQuery.getResponseEntity();
        
        // 检查查询是否成功，如果HTTP状态码不是200，则返回初始化商户的提示
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(new ResponseEntity<>("初始化商户", HttpStatus.FORBIDDEN));
            return;
        }

        // 获取查询结果
        String storeInfo = responseEntity.getBody();
        
        // 检查返回结果是否为有效的JSON对象且包含storeId字段
        // 如果包含storeId，说明是员工身份，直接返回查询结果
        if (Assert.isJsonObject(storeInfo) && JSONObject.parseObject(storeInfo).containsKey("storeId")) {
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 如果不包含storeId，说明是业主身份，返回空结果
        context.setResponseEntity(ResultVo.success());
    }
}