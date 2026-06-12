package org.example.backend.domain;

public record Bill(
    long id,
    String billNo,
    long requestId,
    String carId,
    String pileId,
    double chargeAmount,
    double chargeDuration,
    double chargeFee,
    double serviceFee,
    double totalFee,
    String status,
    String startTime,
    String endTime,
    String createdAt,
    String paidAt
) {
}
