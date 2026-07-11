package com.java110.boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

@Component
public class LogPathConfig {

    private static final Logger logger = LoggerFactory.getLogger(LogPathConfig.class);

    @Value("${logging.file.path:./logs}")
    private String logPath;

    @PostConstruct
    public void init() {
        File logDir = new File(logPath);
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (created) {
                logger.info("日志目录创建成功: {}", logDir.getAbsolutePath());
            } else {
                logger.warn("日志目录创建失败: {}", logDir.getAbsolutePath());
            }
        }
    }
}
