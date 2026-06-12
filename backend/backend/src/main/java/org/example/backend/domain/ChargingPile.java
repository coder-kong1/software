package org.example.backend.domain;

public record ChargingPile(
    String id,
    ChargingMode mode,
    PileStatus status,
    double powerKw,
    int queueLimit,
    int totalChargeCount,
    double totalChargeDuration,
    double totalChargeAmount,
    String createdAt,
    String updatedAt
) {
}
