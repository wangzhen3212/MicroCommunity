package com.java110.api.SYSclient.service;

import com.java110.core.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * 数据库全量备份服务
 *
 * 作为社区ID全表替换(replaceCommunityId)前的保底措施：在执行破坏性全表 UPDATE 前，
 * 调用 mysqldump 将整库备份到日志目录(由启动时 LOG_PATH 系统属性指定)，确保出现问题时可按备份恢复。
 *
 * 备份采用 --single-transaction，对 InnoDB 不锁表；输出经 gzip 压缩。
 * 密码通过 MYSQL_PWD 环境变量传递，避免出现在命令行(ps 可见)。
 *
 * 策略为"尽力而为"：备份失败仅记 ERROR 日志，不阻断后续替换(避免因备份工具异常导致社区ID冲突无法解决)；
 * 如需改为"备份失败即终止替换"，可在调用方判断返回值并中止流程。
 *
 * @author SYS-client
 */
@Component
public class DbBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DbBackupService.class);

    /** mysqldump 执行超时(分钟) */
    private static final long TIMEOUT_MINUTES = 30L;

    /** 匹配 jdbc:mysql://host:port/schema?... */
    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("jdbc:mysql://([^:/]+)(?::(\\d+))?/([^?]+)");

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    /**
     * 全量备份数据库到日志目录
     *
     * @param tag 备份文件标签(如旧社区ID)，用于区分备份用途
     * @return 备份文件绝对路径；失败返回 null
     */
    public String backup(String tag) {
        // 备份目录使用日志实际保存目录(LOG_PATH)，由 BootApplicationStart.createLogDir() 在启动时设置
        String logPath = System.getProperty("LOG_PATH");
        if (logPath == null || logPath.isEmpty()) {
            logPath = "./logs";
        }
        File backupDir = new File(logPath);
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            logger.error("[DB备份] 无法创建备份目录: {}", backupDir.getAbsolutePath());
            return null;
        }

        Matcher m = JDBC_URL_PATTERN.matcher(datasourceUrl == null ? "" : datasourceUrl);
        if (!m.find()) {
            logger.error("[DB备份] 无法解析数据源URL: {}", datasourceUrl);
            return null;
        }
        String host = m.group(1);
        String port = m.group(2);
        String schema = m.group(3);

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String safeTag = (tag == null || tag.isEmpty()) ? "db" : tag.replaceAll("[^a-zA-Z0-9_-]", "");
        File backupFile = new File(backupDir, "db_backup_" + safeTag + "_" + timestamp + ".sql.gz");

        List<String> cmd = new ArrayList<>();
        cmd.add("mysqldump");
        cmd.add("--single-transaction");
        cmd.add("--quick");
        cmd.add("--routines");
        cmd.add("--triggers");
        cmd.add("--events");
        if (host != null && !host.isEmpty()) {
            cmd.add("-h" + host);
        }
        if (port != null && !port.isEmpty()) {
            cmd.add("-P" + port);
        }
        if (datasourceUsername != null && !datasourceUsername.isEmpty()) {
            cmd.add("-u" + datasourceUsername);
        }
        cmd.add(schema);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (datasourcePassword != null) {
            pb.environment().put("MYSQL_PWD", datasourcePassword);
        }
        pb.redirectErrorStream(false);

        logger.info("[DB备份] 开始全量备份 schema={} -> {}", schema, backupFile.getAbsolutePath());

        Process process = null;
        try {
            process = pb.start();
            final Process p = process;

            // 后台读取 stderr，防止其缓冲区写满导致 mysqldump 阻塞死锁
            final StringBuilder errBuf = new StringBuilder();
            Thread errThread = new Thread(() -> {
                try (InputStream eis = p.getErrorStream()) {
                    byte[] b = new byte[4096];
                    int n;
                    while ((n = eis.read(b)) != -1) {
                        errBuf.append(new String(b, 0, n));
                    }
                } catch (Exception ignore) {
                    // 忽略 stderr 读取异常
                }
            });
            errThread.setDaemon(true);
            errThread.start();

            // 读取 mysqldump stdout 并 gzip 压缩写入备份文件
            try (InputStream in = p.getInputStream();
                 OutputStream out = new GZIPOutputStream(new FileOutputStream(backupFile))) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }

            errThread.join(5000L);
            boolean finished = p.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                p.destroyForcibly();
                logger.error("[DB备份] 备份超时({}分钟)，已终止: {}", TIMEOUT_MINUTES, backupFile.getAbsolutePath());
                return null;
            }
            int code = p.exitValue();
            if (code != 0) {
                logger.error("[DB备份] mysqldump 退出码={} 错误信息: {}", code, errBuf.toString());
                return null;
            }
            logger.info("[DB备份] 备份成功: {} ({} bytes)", backupFile.getAbsolutePath(), backupFile.length());
            return backupFile.getAbsolutePath();
        } catch (Exception e) {
            logger.error("[DB备份] 备份失败: {}", e.getMessage(), e);
            return null;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
