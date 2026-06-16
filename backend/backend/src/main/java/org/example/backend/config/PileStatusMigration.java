package org.example.backend.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class PileStatusMigration {

    private final JdbcTemplate jdbcTemplate;

    public PileStatusMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        String tableSql = jdbcTemplate.queryForObject(
            """
            SELECT sql
            FROM sqlite_master
            WHERE type = 'table' AND name = 'charging_pile'
            """,
            String.class
        );
        if (tableSql == null || tableSql.contains("'POWER_ON'")) {
            return;
        }

        try {
            jdbcTemplate.execute("PRAGMA foreign_keys = OFF");
            jdbcTemplate.execute("DROP TABLE IF EXISTS charging_pile_new");
            jdbcTemplate.execute(
                """
                CREATE TABLE charging_pile_new (
                    id TEXT PRIMARY KEY,
                    mode TEXT NOT NULL CHECK (mode IN ('FAST', 'SLOW')),
                    status TEXT NOT NULL DEFAULT 'RUNNING'
                        CHECK (status IN ('POWER_ON', 'RUNNING', 'STOPPED', 'FAULT')),
                    power_kw REAL NOT NULL CHECK (power_kw > 0),
                    queue_limit INTEGER NOT NULL DEFAULT 2 CHECK (queue_limit >= 0),
                    total_charge_count INTEGER NOT NULL DEFAULT 0,
                    total_charge_duration REAL NOT NULL DEFAULT 0,
                    total_charge_amount REAL NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
            );
            jdbcTemplate.update(
                """
                INSERT INTO charging_pile_new (
                    id, mode, status, power_kw, queue_limit, total_charge_count,
                    total_charge_duration, total_charge_amount, created_at, updated_at
                )
                SELECT id, mode, status, power_kw, queue_limit, total_charge_count,
                       total_charge_duration, total_charge_amount, created_at, updated_at
                FROM charging_pile
                """
            );
            jdbcTemplate.execute("DROP TABLE charging_pile");
            jdbcTemplate.execute("ALTER TABLE charging_pile_new RENAME TO charging_pile");
        } finally {
            jdbcTemplate.execute("PRAGMA foreign_keys = ON");
        }
    }
}
