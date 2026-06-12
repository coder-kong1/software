package org.example.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChargingRequestRepository {

    private static final String ACTIVE_STATES = "'WAITING_AREA', 'QUEUING', 'CHARGING'";

    private final JdbcTemplate jdbcTemplate;

    public ChargingRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ChargingRequest> findActiveByCarId(String carId) {
        return jdbcTemplate.query(
            """
            SELECT id, car_id, request_amount, charged_amount, request_mode, state,
                   queue_num, pile_id, request_time, start_time, end_time, updated_at
            FROM charging_request
            WHERE car_id = ? AND state IN (%s)
            ORDER BY id DESC
            LIMIT 1
            """.formatted(ACTIVE_STATES),
            this::mapRow,
            carId
        ).stream().findFirst();
    }

    public boolean existsActiveByCarId(String carId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM charging_request WHERE car_id = ? AND state IN ("
                + ACTIVE_STATES + ")",
            Integer.class,
            carId
        );
        return count != null && count > 0;
    }

    public void insert(String carId, double requestAmount, ChargingMode requestMode) {
        jdbcTemplate.update(
            """
            INSERT INTO charging_request (car_id, request_amount, request_mode)
            VALUES (?, ?, ?)
            """,
            carId,
            requestAmount,
            requestMode.name()
        );
    }

    public void setInitialQueueNum(long requestId, String queueNum) {
        jdbcTemplate.update(
            """
            UPDATE charging_request
            SET queue_num = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            queueNum,
            requestId
        );
    }

    public void updateAmount(long requestId, double amount) {
        jdbcTemplate.update(
            """
            UPDATE charging_request
            SET request_amount = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            amount,
            requestId
        );
    }

    public void updateModeAndQueue(long requestId, ChargingMode mode, String queueNum) {
        jdbcTemplate.update(
            """
            UPDATE charging_request
            SET request_mode = ?, state = 'WAITING_AREA', queue_num = ?, pile_id = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            mode.name(),
            queueNum,
            requestId
        );
    }

    public void cancel(long requestId) {
        jdbcTemplate.update(
            """
            UPDATE charging_request
            SET state = 'CANCELED', queue_num = NULL, pile_id = NULL,
                end_time = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            requestId
        );
    }

    private ChargingRequest mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ChargingRequest(
            resultSet.getLong("id"),
            resultSet.getString("car_id"),
            resultSet.getDouble("request_amount"),
            resultSet.getDouble("charged_amount"),
            ChargingMode.valueOf(resultSet.getString("request_mode")),
            ChargingRequestState.valueOf(resultSet.getString("state")),
            resultSet.getString("queue_num"),
            resultSet.getString("pile_id"),
            resultSet.getString("request_time"),
            resultSet.getString("start_time"),
            resultSet.getString("end_time"),
            resultSet.getString("updated_at")
        );
    }
}
