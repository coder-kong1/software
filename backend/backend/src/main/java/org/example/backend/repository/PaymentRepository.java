package org.example.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.example.backend.domain.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String billNo, String carId, double amount) {
        jdbcTemplate.update(
            """
            INSERT INTO payment (bill_no, car_id, amount)
            VALUES (?, ?, ?)
            """,
            billNo,
            carId,
            amount
        );
    }

    public Optional<Payment> findByBillNo(String billNo) {
        return jdbcTemplate.query(
            """
            SELECT id, bill_no, car_id, amount, status, paid_at
            FROM payment
            WHERE bill_no = ?
            """,
            this::mapRow,
            billNo
        ).stream().findFirst();
    }

    public List<Payment> findByCarId(String carId) {
        return jdbcTemplate.query(
            """
            SELECT id, bill_no, car_id, amount, status, paid_at
            FROM payment
            WHERE car_id = ?
            ORDER BY id DESC
            """,
            this::mapRow,
            carId
        );
    }

    private Payment mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Payment(
            resultSet.getLong("id"),
            resultSet.getString("bill_no"),
            resultSet.getString("car_id"),
            resultSet.getDouble("amount"),
            resultSet.getString("status"),
            resultSet.getString("paid_at")
        );
    }
}
