package com.java110.api.SYSclient.proxy;

import com.java110.api.SYSclient.SysClientProperties;
import com.java110.core.client.OutRestTemplate;
import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OutboundProxyBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(OutboundProxyBeanPostProcessor.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        try {
            SysClientProperties sysClientProperties = applicationContext.getBean(SysClientProperties.class);
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
                    OutboundProxyInterceptor outboundProxyInterceptor = applicationContext.getBean(OutboundProxyInterceptor.class);
                    restTemplate.getInterceptors().add(outboundProxyInterceptor);
                    logger.info("[出站代理] 已向 outRestTemplate 注入拦截器，所有外部HTTP请求将转发到: {}", proxyUrl);
                }
            }
        } catch (Exception e) {
            logger.debug("[出站代理] BeanPostProcessor 初始化时依赖未就绪，跳过: {}", e.getMessage());
        }

        return bean;
    }
}