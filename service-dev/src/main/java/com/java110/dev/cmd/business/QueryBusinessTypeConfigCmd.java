package com.java110.dev.cmd.business;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.system.CbusinesstypeDto;
import com.java110.intf.store.ICbusinesstypeInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 查询业务类型配置命令类
 * 
 * 该类用于处理查询业务类型配置的请求，通过调用业务类型服务获取业务类型数据
 * 并返回给前端
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "business.queryBusinessTypeConfig")
public class QueryBusinessTypeConfigCmd extends Cmd {

    /**
     * 业务类型内部服务接口
     * 用于调用业务类型相关的数据操作
     */
    @Autowired
    private ICbusinesstypeInnerServiceSMO iCbusinesstypeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 该方法用于验证请求参数的有效性
     * 当前实现为空，可根据业务需求添加具体的验证逻辑
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 参数验证逻辑
        // 可根据业务需求添加具体的参数验证，如：
        // - 检查必填字段是否存在
        // - 验证字段格式和取值范围
        // - 校验权限等
    }

    /**
     * 命令执行方法
     * 
     * 该方法负责主要的业务逻辑处理：
     * 1. 将请求参数转换为业务类型DTO对象
     * 2. 调用业务类型服务查询业务类型数据
     * 3. 将查询结果封装为HTTP响应返回
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        
        // 将请求JSON对象转换为业务类型DTO对象
        CbusinesstypeDto cbusinesstypeDtoParam = BeanConvertUtil.covertBean(reqJson, CbusinesstypeDto.class);
        
        // 调用业务类型服务查询业务类型数据
        List<CbusinesstypeDto> cbusinesstypeDtoList = iCbusinesstypeInnerServiceSMOImpl.queryCbusinesstypes(cbusinesstypeDtoParam);
        
        // 将查询结果转换为JSON字符串并封装为HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(cbusinesstypeDtoList), 
            HttpStatus.OK
        );

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}