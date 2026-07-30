package com.java110.api.SYSclient.proxy;

import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 出站代理 Bean 后置处理器（已禁用自动注入）
 *
 * OutboundProxyInterceptor 的注册/注销已迁移至 SysWebSocketClient，
 * 由上位机 WebSocket 连接成功（REGISTER_ACK/OK）后统一控制注入，
 * 断开连接时自动移除，与真实的业务场景保持一致。
 *
 * 本类保留仅用于占位，不做任何操作。
 */
@Component
public class OutboundProxyBeanPostProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboundProxyBeanPostProcessor.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
