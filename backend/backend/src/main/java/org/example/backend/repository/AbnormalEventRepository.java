package org.example.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.example.backend.domain.AbnormalEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AbnormalEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public AbnormalEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
        String carId,
        String eventType,
        String description,
        double penaltyFee
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO abnormal_event (car_id, event_type, description, penalty_fee)
            VALUES (?, ?, ?, ?)
            """,
            carId,
            eventType,
            description,
            penaltyFee
        );
    }

    public Optional<AbnormalEvent> findById(long id) {
        return jdbcTemplate.query(
            """
            SELECT id, car_id, event_type, description, penalty_fee,
                   status, created_at, resolved_at
            FROM abnormal_event
            WHERE id = ?
            """,
            this::mapRow,
            id
        ).stream().findFirst();
    }

    public Optional<AbnormalEvent> findLatestByCarId(String carId) {
        return jdbcTemplate.query(
            """
            SELECT id, car_id, event_type, description, penalty_fee,
                   status, created_at, resolved_at
            FROM abnormal_event
            WHERE car_id = ?
            ORDER BY id DESC
            LIMIT 1
            """,
            this::mapRow,
            carId
        ).stream().findFirst();
    }

    public List<AbnormalEvent> findAll() {
        return jdbcTemplate.query(
            """
            SELECT id, car_id, event_type, description, penalty_fee,
                   status, created_at, resolved_at
            FROM abnormal_event
            ORDER BY CASE status WHEN 'PENDING' THEN 0 ELSE 1 END, id DESC
            """,
            this::mapRow
        );
    }

    public List<AbnormalEvent> findByCarId(String carId) {
        return jdbcTemplate.query(
            """
            SELECT id, car_id, event_type, description, penalty_fee,
                   status, created_at, resolved_at
            FROM abnormal_event
            WHERE car_id = ?
            ORDER BY id DESC
            """,
            this::mapRow,
            carId
        );
    }

    public int resolve(long id) {
        return jdbcTemplate.update(
            """
            UPDATE abnormal_event
            SET status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'PENDING'
            """,
            id
        );
    }

    public int countByStatus(String status) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM abnormal_event WHERE status = ?",
            Integer.class,
            status
        );
        return count == null ? 0 : count;
    }

    private AbnormalEvent mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AbnormalEvent(
            resultSet.getLong("id"),
            resultSet.getString("car_id"),
            resultSet.getString("event_type"),
            resultSet.getString("description"),
            resultSet.getDouble("penalty_fee"),
            resultSet.getString("status"),
            resultSet.getString("created_at"),
            resultSet.getString("resolved_at")
        );
    }
}
