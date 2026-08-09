package com.procureai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Resilient DataSource Bean Configuration.
 *
 * Checks primary database connectivity on startup (e.g. MySQL on Render / Aiven / local).
 * If the configured DB_URL is unresolvable, unreachable, or fails authentication,
 * automatically falls back to an embedded H2 in-memory database with zero downtime or crash.
 */
@Configuration
public class ResilientDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilientDataSourceConfig.class);

    @Value("${spring.datasource.url:}")
    private String primaryUrl;

    @Value("${spring.datasource.username:root}")
    private String primaryUsername;

    @Value("${spring.datasource.password:}")
    private String primaryPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String primaryDriver;

    @Bean
    @Primary
    public DataSource dataSource() {
        if (primaryUrl != null && !primaryUrl.isBlank() && primaryUrl.startsWith("jdbc:mysql:")) {
            log.info("Testing primary database connection to: {}", maskUrl(primaryUrl));
            try {
                Class.forName(primaryDriver);
                DriverManager.setLoginTimeout(3); // 3 seconds max timeout for test
                try (Connection conn = DriverManager.getConnection(primaryUrl, primaryUsername, primaryPassword)) {
                    log.info("Successfully connected to primary MySQL database!");
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(primaryUrl);
                    config.setUsername(primaryUsername);
                    config.setPassword(primaryPassword);
                    config.setDriverClassName(primaryDriver);
                    config.setInitializationFailTimeout(5000);
                    config.setConnectionTimeout(5000);
                    config.setMaximumPoolSize(15);
                    config.setMinimumIdle(2);
                    return new HikariDataSource(config);
                }
            } catch (Exception ex) {
                log.warn("⚠️ Primary MySQL database unreachable ({}: {}) — activating zero-setup embedded H2 fallback database.",
                        ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        log.info("=================================================");
        log.info("Active Database: Embedded H2 In-Memory Database");
        log.info("=================================================");
        HikariConfig h2Config = new HikariConfig();
        h2Config.setJdbcUrl("jdbc:h2:mem:procureai;DB_CLOSE_DELAY=-1;MODE=MySQL");
        h2Config.setDriverClassName("org.h2.Driver");
        h2Config.setUsername("sa");
        h2Config.setPassword("");
        h2Config.setConnectionTimeout(5000);
        return new HikariDataSource(h2Config);
    }

    private String maskUrl(String url) {
        if (url == null) return "";
        return url.replaceAll("password=[^&]*", "password=***");
    }
}
