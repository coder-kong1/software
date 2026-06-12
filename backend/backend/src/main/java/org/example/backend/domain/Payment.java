package org.example.backend.domain;

public record Payment(
    long id,
    String billNo,
    String carId,
    double amount,
    String status,
    String paidAt
) {
}
