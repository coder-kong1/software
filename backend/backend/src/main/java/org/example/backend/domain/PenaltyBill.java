package org.example.backend.domain;

public record PenaltyBill(
    long id,
    String billNo,
    long eventId,
    String carId,
    double amount,
    String status,
    String createdAt,
    String paidAt
) {
}
