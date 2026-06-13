package org.example.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.example.backend.domain.Payment;
import org.example.backend.domain.PenaltyBill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PenaltyBillRepository {

    private final JdbcTemplate jdbcTemplate;

    public PenaltyBillRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String billNo, long eventId, String carId, double amount) {
        jdbcTemplate.update(
            """
            INSERT INTO penalty_bill (bill_no, event_id, car_id, amount)
            VALUES (?, ?, ?, ?)
            """,
            billNo,
            eventId,
            carId,
            amount
        );
    }

    public Optional<PenaltyBill> findByBillNo(String billNo) {
        return jdbcTemplate.query(
            """
            SELECT id, bill_no, event_id, car_id, amount, status, created_at, paid_at
            FROM penalty_bill
            WHERE bill_no = ?
            """,
            this::mapRow,
            billNo
        ).stream().findFirst();
    }

    public Optional<PenaltyBill> findByEventId(long eventId) {
        return jdbcTemplate.query(
            """
            SELECT id, bill_no, event_id, car_id, amount, status, created_at, paid_at
            FROM penalty_bill
            WHERE event_id = ?
            """,
            this::mapRow,
            eventId
        ).stream().findFirst();
    }

    public List<PenaltyBill> findByCarId(String carId) {
        return jdbcTemplate.query(
            """
            SELECT id, bill_no, event_id, car_id, amount, status, created_at, paid_at
            FROM penalty_bill
            WHERE car_id = ?
            ORDER BY id DESC
            """,
            this::mapRow,
            carId
        );
    }

    public List<PenaltyBill> findAll() {
        return jdbcTemplate.query(
            """
            SELECT id, bill_no, event_id, car_id, amount, status, created_at, paid_at
            FROM penalty_bill
            ORDER BY id DESC
            """,
            this::mapRow
        );
    }

    public void markPaid(String billNo) {
        jdbcTemplate.update(
            """
            UPDATE penalty_bill
            SET status = 'PAID', paid_at = CURRENT_TIMESTAMP
            WHERE bill_no = ? AND status = 'UNPAID'
            """,
            billNo
        );
    }

    public void insertPayment(String billNo, String carId, double amount) {
        jdbcTemplate.update(
            """
            INSERT INTO penalty_payment (bill_no, car_id, amount)
            VALUES (?, ?, ?)
            """,
            billNo,
            carId,
            amount
        );
    }

    public Optional<Payment> findPaymentByBillNo(String billNo) {
        return jdbcTemplate.query(
            """
            SELECT id, bill_no, car_id, amount, status, paid_at
            FROM penalty_payment
            WHERE bill_no = ?
            """,
            this::mapPayment,
            billNo
        ).stream().findFirst();
    }

    public List<Payment> findPaymentsByCarId(String carId) {
        return jdbcTemplate.query(
            """
            SELECT id, bill_no, car_id, amount, status, paid_at
            FROM penalty_payment
            WHERE car_id = ?
            ORDER BY id DESC
            """,
            this::mapPayment,
            carId
        );
    }

    private PenaltyBill mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PenaltyBill(
            resultSet.getLong("id"),
            resultSet.getString("bill_no"),
            resultSet.getLong("event_id"),
            resultSet.getString("car_id"),
            resultSet.getDouble("amount"),
            resultSet.getString("status"),
            resultSet.getString("created_at"),
            resultSet.getString("paid_at")
        );
    }

    private Payment mapPayment(ResultSet resultSet, int rowNumber) throws SQLException {
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
