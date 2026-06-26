package com.gsmv.bootstrap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AiReportSchemaBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiReportSchemaBootstrapRunner.class);

    private final DataSource dataSource;

    public AiReportSchemaBootstrapRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            ensureColumn(connection, "ai_research_report", "period_start",
                    "ALTER TABLE ai_research_report ADD COLUMN period_start DATETIME(3) NULL AFTER days");
            ensureColumn(connection, "ai_research_report", "period_end",
                    "ALTER TABLE ai_research_report ADD COLUMN period_end DATETIME(3) NULL AFTER period_start");
            ensureColumn(connection, "ai_research_report", "metrics_json",
                    "ALTER TABLE ai_research_report ADD COLUMN metrics_json JSON NULL AFTER evidence_json");
        } catch (SQLException exception) {
            log.warn("AI report schema bootstrap failed: {}", exception.getMessage(), exception);
        }
    }

    private void ensureColumn(Connection connection, String table, String column, String ddl) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    execute(connection, ddl);
                }
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
