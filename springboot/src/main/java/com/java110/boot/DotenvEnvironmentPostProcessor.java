package com.java110.boot;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.File;
import java.util.Properties;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = findEnvFile();
        if (envFile != null && envFile.exists()) {
            loadDotenv(environment, envFile);
        }
    }

    private File findEnvFile() {
        File envFile = new File(".env");
        if (envFile.exists()) {
            return envFile;
        }

        envFile = new File("./.env");
        if (envFile.exists()) {
            return envFile;
        }

        envFile = new File(System.getProperty("user.dir"), ".env");
        if (envFile.exists()) {
            return envFile;
        }

        return null;
    }

    private void loadDotenv(ConfigurableEnvironment environment, File envFile) {
        try {
            logger.info("[环境变量] 加载.env文件: {}", envFile.getAbsolutePath());

            String parentDir = envFile.getParent();
            if (parentDir == null) {
                parentDir = ".";
            }

            Dotenv dotenv = Dotenv.configure()
                    .directory(parentDir)
                    .filename(envFile.getName())
                    .load();

            Properties properties = new Properties();
            int count = 0;
            for (DotenvEntry entry : dotenv.entries()) {
                String key = entry.getKey();
                String value = entry.getValue();
                properties.setProperty(key, value);
                count++;
                logger.info("[环境变量] 设置: {}={}", key, maskSensitive(key, value));
            }

            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(new PropertiesPropertySource("dotenv", properties));

            logger.info("[环境变量] 成功加载 {} 个环境变量", count);
        } catch (Exception e) {
            logger.error("[环境变量] 加载.env文件失败: {}", e.getMessage(), e);
        }
    }

    private String maskSensitive(String key, String value) {
        if (key == null || value == null) {
            return value;
        }
        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("password") || lowerKey.contains("secret") || lowerKey.contains("key")) {
            if (value.length() <= 8) {
                return "***";
            }
            return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
        }
        return value;
    }
}