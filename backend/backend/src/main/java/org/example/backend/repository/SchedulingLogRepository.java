package org.example.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.SchedulingLog;
import org.example.backend.domain.SchedulingStrategy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SchedulingLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public SchedulingLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
        Long requestId,
        String carId,
        ChargingMode requestMode,
        String fromState,
        String toState,
        String fromPileId,
        String toPileId,
        String queueNum,
        SchedulingStrategy strategy,
        String reason
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO scheduling_log (
                request_id, car_id, request_mode, from_state, to_state,
                from_pile_id, to_pile_id, queue_num, strategy, reason
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            requestId,
            carId,
            requestMode == null ? null : requestMode.name(),
            fromState,
            toState,
            fromPileId,
            toPileId,
            queueNum,
            strategy == null ? null : strategy.name(),
            reason
        );
    }

    public List<SchedulingLog> findRecent(int limit) {
        return jdbcTemplate.query(
            """
            SELECT id, request_id, car_id, request_mode, from_state, to_state,
                   from_pile_id, to_pile_id, queue_num, strategy, reason, created_at
            FROM scheduling_log
            ORDER BY id DESC
            LIMIT ?
            """,
            this::mapRow,
            limit
        );
    }

    private SchedulingLog mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        String requestMode = resultSet.getString("request_mode");
        String strategy = resultSet.getString("strategy");
        long requestId = resultSet.getLong("request_id");
        Long requestIdValue = resultSet.wasNull() ? null : requestId;
        return new SchedulingLog(
            resultSet.getLong("id"),
            requestIdValue,
            resultSet.getString("car_id"),
            requestMode == null ? null : ChargingMode.valueOf(requestMode),
            resultSet.getString("from_state"),
            resultSet.getString("to_state"),
            resultSet.getString("from_pile_id"),
            resultSet.getString("to_pile_id"),
            resultSet.getString("queue_num"),
            strategy == null ? null : SchedulingStrategy.valueOf(strategy),
            resultSet.getString("reason"),
            resultSet.getString("created_at")
        );
    }
}

