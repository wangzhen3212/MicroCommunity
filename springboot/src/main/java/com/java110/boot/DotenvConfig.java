package com.java110.boot;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

@Configuration
public class DotenvConfig {

    private static Logger logger = LoggerFactory.getLogger(DotenvConfig.class);

    @PostConstruct
    public void init() {
        loadDotenv();
    }

    private void loadDotenv() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("./.env");
        }
        if (!envFile.exists()) {
            envFile = new File(System.getProperty("user.dir"), ".env");
        }

        if (envFile.exists()) {
            logger.info("加载.env文件: {}", envFile.getAbsolutePath());
            Dotenv dotenv = Dotenv.configure()
                    .directory(envFile.getParent())
                    .filename(envFile.getName())
                    .load();

            for (DotenvEntry entry : dotenv.entries()) {
                String key = entry.getKey();
                String value = entry.getValue();
                setEnv(key, value);
                logger.debug("环境变量设置: {}={}", key, value);
            }
        } else {
            logger.info("未找到.env文件，跳过加载");
        }
    }

    private void setEnv(String key, String value) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.put(key, value);

            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> caseInsensitiveEnv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            caseInsensitiveEnv.put(key, value);
        } catch (Exception e) {
            logger.warn("设置环境变量失败: {}", key, e);
        }
    }
}