package org.example.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.ChargingPile;
import org.example.backend.domain.PileStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChargingPileRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChargingPileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ChargingPile> findAll() {
        return jdbcTemplate.query(
            """
            SELECT id, mode, status, power_kw, queue_limit, total_charge_count,
                   total_charge_duration, total_charge_amount, created_at, updated_at
            FROM charging_pile
            ORDER BY mode, id
            """,
            this::mapRow
        );
    }

    public List<ChargingPile> findRunningByMode(ChargingMode mode) {
        return jdbcTemplate.query(
            """
            SELECT id, mode, status, power_kw, queue_limit, total_charge_count,
                   total_charge_duration, total_charge_amount, created_at, updated_at
            FROM charging_pile
            WHERE mode = ? AND status = 'RUNNING'
            ORDER BY id
            """,
            this::mapRow,
            mode.name()
        );
    }

    public Optional<ChargingPile> findById(String pileId) {
        return jdbcTemplate.query(
            """
            SELECT id, mode, status, power_kw, queue_limit, total_charge_count,
                   total_charge_duration, total_charge_amount, created_at, updated_at
            FROM charging_pile
            WHERE id = ?
            """,
            this::mapRow,
            pileId
        ).stream().findFirst();
    }

    public void updateStatus(String pileId, PileStatus status) {
        jdbcTemplate.update(
            """
            UPDATE charging_pile
            SET status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            status.name(),
            pileId
        );
    }

    private ChargingPile mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ChargingPile(
            resultSet.getString("id"),
            ChargingMode.valueOf(resultSet.getString("mode")),
            PileStatus.valueOf(resultSet.getString("status")),
            resultSet.getDouble("power_kw"),
            resultSet.getInt("queue_limit"),
            resultSet.getInt("total_charge_count"),
            resultSet.getDouble("total_charge_duration"),
            resultSet.getDouble("total_charge_amount"),
            resultSet.getString("created_at"),
            resultSet.getString("updated_at")
        );
    }
}
