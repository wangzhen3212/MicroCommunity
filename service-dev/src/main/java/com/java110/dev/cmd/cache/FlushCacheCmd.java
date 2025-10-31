package com.java110.dev.cmd.cache;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dev.smo.IDevServiceCacheSMO;
import com.java110.utils.exception.CmdException;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 刷新缓存命令处理类
 * 负责处理服务编码为"flush.center.cache"的缓存刷新请求
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
@Java110Cmd(serviceCode = "flush.center.cache")
public class FlushCacheCmd extends Cmd {

    /**
     * 缓存服务管理对象，用于执行具体的缓存刷新操作
     */
    @Autowired
    private IDevServiceCacheSMO devServiceCacheSMOImpl;

    /**
     * 参数验证方法
     * 用于验证请求参数的合法性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置数据流信息
     * @param reqJson 请求参数JSON对象，包含客户端传入的参数
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 当前方法为空实现，可根据业务需求添加参数验证逻辑
        // 例如：验证必填参数、参数格式、参数范围等
    }

    /**
     * 命令执行方法
     * 执行缓存刷新操作，并将结果设置到响应中
     * 使用@Java110Transactional注解确保事务性操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用缓存服务刷新缓存，将JSON参数转换为Map对象传递
        devServiceCacheSMOImpl.flush(reqJson.toJavaObject(Map.class));
        
        // 设置响应结果为成功状态
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}