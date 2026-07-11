package com.java110.boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LogPathConfig {

    private static final Logger logger = LoggerFactory.getLogger(LogPathConfig.class);

    static {
        String logPath = System.getenv("LOG_PATH");
        if (logPath == null || logPath.isEmpty()) {
            logPath = "./logs";
        }
        File logDir = new File(logPath);
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (created) {
                System.out.println("日志目录创建成功: " + logDir.getAbsolutePath());
            } else {
                System.out.println("日志目录创建失败: " + logDir.getAbsolutePath());
            }
        }
    }
}
