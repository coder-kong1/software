package org.example.backend.config;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class PriceRuleMigration {

    private final JdbcTemplate jdbcTemplate;

    public PriceRuleMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(price_rule)",
            (resultSet, rowNumber) -> resultSet.getString("name")
        );
        boolean addedFastServicePrice = false;
        boolean addedSlowServicePrice = false;

        if (!columns.contains("fast_service_price")) {
            jdbcTemplate.update(
                """
                ALTER TABLE price_rule
                ADD COLUMN fast_service_price REAL NOT NULL DEFAULT 1.0
                """
            );
            addedFastServicePrice = true;
        }

        if (!columns.contains("slow_service_price")) {
            jdbcTemplate.update(
                """
                ALTER TABLE price_rule
                ADD COLUMN slow_service_price REAL NOT NULL DEFAULT 0.8
                """
            );
            addedSlowServicePrice = true;
        }

        if (addedFastServicePrice || addedSlowServicePrice) {
            jdbcTemplate.update(
                """
                UPDATE price_rule
                SET fast_service_price = CASE
                        WHEN ? THEN COALESCE(service_price + 0.2, 1.0)
                        ELSE fast_service_price
                    END,
                    slow_service_price = CASE
                        WHEN ? THEN COALESCE(service_price, 0.8)
                        ELSE slow_service_price
                    END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = 1
                """,
                addedFastServicePrice,
                addedSlowServicePrice
            );
        }
    }
}
