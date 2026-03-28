package com.ruoyi.web.plugin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 插件数据库脚本执行服务
 */
@Service
public class PluginDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(PluginDatabaseService.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 执行插件数据库脚本
     */
    @Transactional
    public void executeSqlScripts(List<String> sqlScripts, String pluginId) {
        try (Connection conn = dataSource.getConnection()) {
            for (String sql : sqlScripts) {
                executeSqlFile(conn, sql, pluginId);
            }
            log.info("插件 [{}] 数据库脚本执行成功", pluginId);
        } catch (SQLException e) {
            throw new RuntimeException("执行插件 SQL 失败: " + pluginId, e);
        }
    }

    private void executeSqlFile(Connection conn, String sqlContent, String pluginId) throws SQLException {
        String[] statements = sqlContent.split(";");

        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                    continue;
                }
                try {
                    stmt.execute(trimmed);
                    log.debug("执行 SQL: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                } catch (SQLException e) {
                    log.error("SQL 执行失败: {}", trimmed);
                    throw e;
                }
            }
        }
    }
}
