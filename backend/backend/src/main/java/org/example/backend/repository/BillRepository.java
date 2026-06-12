package org.example.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.example.backend.domain.Bill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BillRepository {

    private static final String BILL_SELECT = """
        SELECT b.id, b.bill_no, b.request_id, b.car_id, b.pile_id,
               b.charge_amount, b.charge_duration, b.charge_fee, b.service_fee,
               b.total_fee, b.status, r.start_time, r.end_time, b.created_at, b.paid_at
        FROM bill b
        JOIN charging_request r ON r.id = b.request_id
        """;

    private final JdbcTemplate jdbcTemplate;

    public BillRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
        String billNo,
        long requestId,
        String carId,
        String pileId,
        double chargeAmount,
        double chargeDuration,
        double chargeFee,
        double serviceFee,
        double totalFee
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO bill (
                bill_no, request_id, car_id, pile_id, charge_amount, charge_duration,
                charge_fee, service_fee, total_fee
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            billNo,
            requestId,
            carId,
            pileId,
            chargeAmount,
            chargeDuration,
            chargeFee,
            serviceFee,
            totalFee
        );
    }

    public Optional<Bill> findByBillNo(String billNo) {
        return jdbcTemplate.query(
            BILL_SELECT + " WHERE b.bill_no = ?",
            this::mapRow,
            billNo
        ).stream().findFirst();
    }

    public Optional<Bill> findLatestByCarId(String carId) {
        return jdbcTemplate.query(
            BILL_SELECT + " WHERE b.car_id = ? ORDER BY b.id DESC LIMIT 1",
            this::mapRow,
            carId
        ).stream().findFirst();
    }

    public List<Bill> findByCarId(String carId, String date) {
        if (date == null || date.isBlank()) {
            return jdbcTemplate.query(
                BILL_SELECT + " WHERE b.car_id = ? ORDER BY b.id DESC",
                this::mapRow,
                carId
            );
        }
        return jdbcTemplate.query(
            BILL_SELECT + " WHERE b.car_id = ? AND date(b.created_at) = date(?) ORDER BY b.id DESC",
            this::mapRow,
            carId,
            date
        );
    }

    public List<Bill> findAll() {
        return jdbcTemplate.query(
            BILL_SELECT + " ORDER BY b.id DESC",
            this::mapRow
        );
    }

    public void markPaid(String billNo) {
        jdbcTemplate.update(
            """
            UPDATE bill
            SET status = 'PAID', paid_at = CURRENT_TIMESTAMP
            WHERE bill_no = ? AND status = 'UNPAID'
            """,
            billNo
        );
    }

    private Bill mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Bill(
            resultSet.getLong("id"),
            resultSet.getString("bill_no"),
            resultSet.getLong("request_id"),
            resultSet.getString("car_id"),
            resultSet.getString("pile_id"),
            resultSet.getDouble("charge_amount"),
            resultSet.getDouble("charge_duration"),
            resultSet.getDouble("charge_fee"),
            resultSet.getDouble("service_fee"),
            resultSet.getDouble("total_fee"),
            resultSet.getString("status"),
            resultSet.getString("start_time"),
            resultSet.getString("end_time"),
            resultSet.getString("created_at"),
            resultSet.getString("paid_at")
        );
    }
}
