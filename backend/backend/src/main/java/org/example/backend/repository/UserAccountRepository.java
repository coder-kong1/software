package org.example.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.example.backend.domain.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByCarId(String carId) {
        return jdbcTemplate.query(
            """
            SELECT id, car_id, user_name, password_hash, car_capacity, created_at, updated_at
            FROM user_account
            WHERE car_id = ?
            """,
            this::mapRow,
            carId
        ).stream().findFirst();
    }

    public boolean existsByCarId(String carId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_account WHERE car_id = ?",
            Integer.class,
            carId
        );
        return count != null && count > 0;
    }

    public void insert(String carId, String userName, String passwordHash, double carCapacity) {
        jdbcTemplate.update(
            """
            INSERT INTO user_account (car_id, user_name, password_hash, car_capacity)
            VALUES (?, ?, ?, ?)
            """,
            carId,
            userName,
            passwordHash,
            carCapacity
        );
    }

    public void update(
        String carId,
        String userName,
        String passwordHash,
        double carCapacity
    ) {
        jdbcTemplate.update(
            """
            UPDATE user_account
            SET user_name = ?, password_hash = ?, car_capacity = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE car_id = ?
            """,
            userName,
            passwordHash,
            carCapacity,
            carId
        );
    }

    public void deleteByCarId(String carId) {
        jdbcTemplate.update("DELETE FROM user_account WHERE car_id = ?", carId);
    }

    private UserAccount mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserAccount(
            resultSet.getLong("id"),
            resultSet.getString("car_id"),
            resultSet.getString("user_name"),
            resultSet.getString("password_hash"),
            resultSet.getDouble("car_capacity"),
            resultSet.getString("created_at"),
            resultSet.getString("updated_at")
        );
    }
}
