package com.gsmv.bootstrap;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4.x 移除了 Flyway 自动配置，需要手动初始化。
 * 每次应用启动时自动执行新的数据库迁移。
 *
 * 当前数据库已通过 flyway_schema_history 表标记 V1~V27 为已执行，
 * 后续启动只会运行 V28 及之后的迁移文件。
 * 全新安装时（空数据库），Flyway 会从 V1 开始顺序执行所有迁移。
 */
@Configuration
public class FlywayMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationConfig.class);

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        log.info("Flyway migration initialized successfully");
        return flyway;
    }
}
