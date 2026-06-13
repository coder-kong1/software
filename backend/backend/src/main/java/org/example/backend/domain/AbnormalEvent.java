package org.example.backend.domain;

public record AbnormalEvent(
    long id,
    String carId,
    String eventType,
    String description,
    double penaltyFee,
    String status,
    String createdAt,
    String resolvedAt
) {
}
