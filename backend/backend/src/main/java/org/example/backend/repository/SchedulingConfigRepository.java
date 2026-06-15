package org.example.backend.repository;

import org.example.backend.domain.SchedulingStrategy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SchedulingConfigRepository {

    private static final String STRATEGY_KEY = "scheduling_strategy";

    private final JdbcTemplate jdbcTemplate;

    public SchedulingConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SchedulingStrategy getStrategy() {
        ensureTable();
        String value = jdbcTemplate.query(
            "SELECT setting_value FROM app_setting WHERE setting_key = ?",
            (rs, rowNum) -> rs.getString("setting_value"),
            STRATEGY_KEY
        ).stream().findFirst().orElse(SchedulingStrategy.TIME_ORDER.name());
        return SchedulingStrategy.valueOf(value);
    }

    public void updateStrategy(SchedulingStrategy strategy) {
        ensureTable();
        jdbcTemplate.update(
            """
            INSERT INTO app_setting (setting_key, setting_value)
            VALUES (?, ?)
            ON CONFLICT(setting_key) DO UPDATE SET
                setting_value = excluded.setting_value,
                updated_at = CURRENT_TIMESTAMP
            """,
            STRATEGY_KEY,
            strategy.name()
        );
    }

    private void ensureTable() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS app_setting (
                setting_key TEXT PRIMARY KEY,
                setting_value TEXT NOT NULL,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """
        );
        jdbcTemplate.update(
            """
            INSERT OR IGNORE INTO app_setting (setting_key, setting_value)
            VALUES (?, ?)
            """,
            STRATEGY_KEY,
            SchedulingStrategy.TIME_ORDER.name()
        );
    }
}
