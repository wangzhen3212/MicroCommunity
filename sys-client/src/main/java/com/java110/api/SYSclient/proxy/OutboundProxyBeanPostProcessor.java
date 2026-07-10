package com.java110.api.SYSclient.proxy;

import com.java110.api.SYSclient.SysClientProperties;
import com.java110.core.client.OutRestTemplate;
import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Component
public class OutboundProxyBeanPostProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboundProxyBeanPostProcessor.class);

    @Resource
    private OutboundProxyInterceptor outboundProxyInterceptor;

    @Resource
    private SysClientProperties sysClientProperties;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!sysClientProperties.isEnabled() || !sysClientProperties.isProxyEnabled()) {
            return bean;
        }

        String proxyUrl = sysClientProperties.getProxyUrl();
        if (proxyUrl == null || proxyUrl.trim().isEmpty()) {
            return bean;
        }

        if (bean instanceof OutRestTemplate || (bean instanceof RestTemplate && "outRestTemplate".equals(beanName))) {
            RestTemplate restTemplate = (RestTemplate) bean;
            boolean alreadyAdded = false;
            for (org.springframework.http.client.ClientHttpRequestInterceptor interceptor : restTemplate.getInterceptors()) {
                if (interceptor instanceof OutboundProxyInterceptor) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                restTemplate.getInterceptors().add(outboundProxyInterceptor);
                logger.info("[出站代理] 已向 outRestTemplate 注入拦截器，所有外部HTTP请求将转发到: {}", proxyUrl);
            }
        }

        return bean;
    }
}