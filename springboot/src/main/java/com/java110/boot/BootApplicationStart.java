/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.boot;

import com.java110.core.annotation.Java110CmdDiscovery;
import com.java110.core.client.OutRestTemplate;
import com.java110.core.client.RestTemplate;
import com.java110.core.context.Environment;
import com.java110.core.event.CacheFlushCompleteEvent;
import com.java110.core.event.cmd.ServiceCmdEventPublishing;
import com.java110.core.log.LoggerFactory;
import com.java110.core.trace.Java110FeignClientInterceptor;
import com.java110.core.trace.Java110RestTemplateInterceptor;
import com.java110.doc.annotation.Java110ApiDocDiscovery;
import com.java110.doc.annotation.Java110CmdDocDiscovery;
import com.java110.doc.registrar.ApiDocCmdPublishing;
import com.java110.doc.registrar.ApiDocPublishing;
import com.java110.intf.dev.ICacheV1InnerServiceSMO;
import com.java110.service.init.ServiceStartInit;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.StringUtil;
import okhttp3.ConnectionPool;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;


/**
 * 这个服务是将 系统部署为spring boot版
 * 如果是spring cloud 微服务部署 不用启动这个类
 * <p>
 *
 * @version v0.1
 * @auther com.java110.wuxw
 * @mail 928255095@qq.com
 * @date 2016年8月6日
 * @tag
 */
@SpringBootApplication(scanBasePackages = {
        "com.java110.service",
        "com.java110.db",
        "com.java110.core",
        "com.java110.config.properties.code",
        "com.java110.acct",
        "com.java110.common",
        "com.java110.community",
        "com.java110.dev",
        "com.java110.fee",
        "com.java110.job",
        "com.java110.oa",
        "com.java110.order",
        "com.java110.report",
        "com.java110.store",
        "com.java110.user",
        "com.java110.doc",
        "com.java110.scm",
        "com.java110.api",
        "com.java110.boot"
},
        exclude = {LiquibaseAutoConfiguration.class,
                org.activiti.spring.boot.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                com.github.pagehelper.autoconfigure.MapperAutoConfiguration.class
        }

)
@Java110CmdDiscovery(cmdPublishClass = ServiceCmdEventPublishing.class,
        basePackages = {
                "com.java110.acct.cmd",
                "com.java110.common.cmd",
                "com.java110.community.cmd",
                "com.java110.dev.cmd",
                "com.java110.fee.cmd",
                "com.java110.job.cmd",
                "com.java110.oa.cmd",
                "com.java110.order.cmd",
                "com.java110.report.cmd",
                "com.java110.store.cmd",
                "com.java110.scm.cmd",
                "com.java110.user.cmd"
        })
@EnableScheduling
@EnableAsync
//文档
@Java110ApiDocDiscovery(basePackages = {"com.java110.boot.rest"}, apiDocClass = ApiDocPublishing.class)
@Java110CmdDocDiscovery(basePackages = {
        "com.java110.acct.cmd",
        "com.java110.acct.payment.business",
        "com.java110.common.cmd",
        "com.java110.community.cmd",
        "com.java110.dev.cmd",
        "com.java110.fee.cmd",
        "com.java110.job.cmd",
        "com.java110.oa.cmd",
        "com.java110.order.cmd",
        "com.java110.report.cmd",
        "com.java110.store.cmd",
        "com.java110.scm.cmd",
        "com.java110.user.cmd"
},
        cmdDocClass = ApiDocCmdPublishing.class)
public class BootApplicationStart {

    static {
        createLogDir();
    }

    private static Logger logger = LoggerFactory.getLogger(BootApplicationStart.class);

    @Resource
    private Java110RestTemplateInterceptor java110RestTemplateInterceptor;

    /**
     * 实例化RestTemplate
     *
     * @return restTemplate
     */
    @Bean
    public OutRestTemplate outRestTemplate() {
        StringHttpMessageConverter m = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        m.setWriteAcceptCharset(false);
        OutRestTemplate restTemplate = new RestTemplateBuilder().additionalMessageConverters(m).build(OutRestTemplate.class);
        restTemplate.getInterceptors().add(java110RestTemplateInterceptor);

        //设置超时时间
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(10000);
        httpRequestFactory.setConnectTimeout(10000);
        httpRequestFactory.setReadTimeout(10000);
        restTemplate.setRequestFactory(httpRequestFactory);
        return restTemplate;
    }

    @Bean
    //@LoadBalanced
    public RestTemplate formRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return restTemplate;
    }

    /**
     * 实例化RestTemplate，通过@LoadBalanced注解开启均衡负载能力.
     *
     * @return restTemplate
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        StringHttpMessageConverter m = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        m.setWriteAcceptCharset(false);
        RestTemplate restTemplate = new RestTemplateBuilder().additionalMessageConverters(m).build(RestTemplate.class);
        restTemplate.getInterceptors().add(java110RestTemplateInterceptor);
        //设置超时时间
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(10000);
        httpRequestFactory.setConnectTimeout(10000);
        httpRequestFactory.setReadTimeout(10000);
        restTemplate.setRequestFactory(httpRequestFactory);
        return restTemplate;
    }

    @Bean
    @ConditionalOnBean(Java110FeignClientInterceptor.class)
    public okhttp3.OkHttpClient okHttpClient(@Autowired
                                             Java110FeignClientInterceptor okHttpLoggingInterceptor) {
        okhttp3.OkHttpClient.Builder ClientBuilder = new okhttp3.OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS) //读取超时
                .connectTimeout(10, TimeUnit.SECONDS) //连接超时
                .writeTimeout(60, TimeUnit.SECONDS) //写入超时
                .connectionPool(new ConnectionPool(10 /*maxIdleConnections*/, 3, TimeUnit.MINUTES))
                .addInterceptor(okHttpLoggingInterceptor);
        return ClientBuilder.build();
    }

    private static void createLogDir() {
        String envLogPath = System.getenv("LOG_PATH");
        String appName = "MicroCommunityBoot";
        String logPath;
        if (envLogPath == null || envLogPath.isEmpty()) {
            logPath = "./" + appName + "-logs";
        } else {
            logPath = envLogPath + "/" + appName + "-logs";
        }

        File currentDir = new File(".");
        checkDirectoryPermission(currentDir, "当前运行目录");

        File logDir = new File(logPath);
        File parentDir = logDir.getParentFile();
        if (parentDir != null) {
            checkDirectoryPermission(parentDir, "日志父目录");
        }

        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (created) {
                System.out.println("日志目录创建成功: " + logDir.getAbsolutePath());
            } else {
                System.err.println("ERROR: 日志目录创建失败，请检查目录权限: " + logDir.getAbsolutePath());
                System.err.println("建议: 请确保当前用户对父目录有写入权限，或使用 chown/chmod 修改权限");
                System.exit(1);
            }
        } else {
            checkDirectoryPermission(logDir, "日志目录");
        }
        System.setProperty("LOG_PATH", logPath);
    }

    private static void checkDirectoryPermission(File dir, String description) {
        String absPath = dir.getAbsolutePath();
        if (!dir.exists()) {
            System.err.println("ERROR: " + description + "不存在: " + absPath);
            System.exit(1);
        }
        if (!dir.isDirectory()) {
            System.err.println("ERROR: " + description + "不是目录: " + absPath);
            System.exit(1);
        }
        if (!dir.canRead()) {
            System.err.println("ERROR: " + description + "没有读权限: " + absPath);
            System.exit(1);
        }
        if (!dir.canWrite()) {
            System.err.println("ERROR: " + description + "没有写权限: " + absPath);
            System.err.println("当前用户: " + getCurrentUser());
            System.err.println("建议: 执行 'chown -R www:www " + absPath + "' 或 'chmod 755 " + absPath + "'");
            System.exit(1);
        }
        if (!dir.canExecute()) {
            System.err.println("ERROR: " + description + "没有执行权限: " + absPath);
            System.exit(1);
        }
    }

    private static String getCurrentUser() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getenv("USERNAME");
        } else {
            return System.getenv("USER");
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            ServiceStartInit.preInitSystemConfig();
            ApplicationContext context = SpringApplication.run(BootApplicationStart.class, args);
            //服务启动加载
            ServiceStartInit.initSystemConfig(context);

            Environment.setSystemStartWay(Environment.SPRING_BOOT);

            //刷新缓存（内部会发布 CacheFlushCompleteEvent，触发客户机注册）
            flushMainCache(args);

            //服务启动完成
            ServiceStartInit.printStartSuccessInfo();


        } catch (Throwable e) {
            logger.error("系统启动失败", e);
        }
    }



    /**
     * 刷新主要的缓存
     *
     * @param args
     */
    private static void flushMainCache(String[] args) {

        logger.debug("判断是否需要刷新日志，参数 args 为 {}", args);

        //因为好多朋友启动时 不加 参数-Dcache 所以启动时检测 redis 中是否存在 java110_hc_version
        //String mapping = MappingCache.getValue(MappingConstant.ENV_DOMAIN,"java110_hc_version");
        String mapping = "";
        if (StringUtil.isEmpty(mapping)) {
            ICacheV1InnerServiceSMO devServiceCacheSMOImpl = (ICacheV1InnerServiceSMO) ApplicationContextFactory.getBean(ICacheV1InnerServiceSMO.class);
            devServiceCacheSMOImpl.startFlush();
            publishCacheFlushCompleteEvent();
            return;
        }

        if (args == null || args.length == 0) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-Dcache")) {
                logger.debug("开始刷新日志，入参为：{}", args[i]);
                ICacheV1InnerServiceSMO devServiceCacheSMOImpl = (ICacheV1InnerServiceSMO) ApplicationContextFactory.getBean(ICacheV1InnerServiceSMO.class);
                devServiceCacheSMOImpl.startFlush();
                publishCacheFlushCompleteEvent();
            }
        }
    }

    /**
     * 发布缓存刷新完成事件
     * 通知其他模块缓存已加载完成，可以执行需要业务数据的操作
     */
    private static void publishCacheFlushCompleteEvent() {
        try {
            org.springframework.context.ApplicationContext context = ApplicationContextFactory.getApplicationContext();
            if (context != null) {
                logger.info("[启动时序] 缓存刷新完成，发布 CacheFlushCompleteEvent 事件");
                context.publishEvent(new CacheFlushCompleteEvent(context));
            }
        } catch (Exception e) {
            logger.warn("[启动时序] 发布缓存刷新完成事件失败: {}", e.getMessage());
        }
    }
}