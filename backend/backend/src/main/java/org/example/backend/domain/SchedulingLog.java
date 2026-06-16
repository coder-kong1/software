package org.example.backend.domain;

public record SchedulingLog(
    long id,
    Long requestId,
    String carId,
    ChargingMode requestMode,
    String fromState,
    String toState,
    String fromPileId,
    String toPileId,
    String queueNum,
    SchedulingStrategy strategy,
    String reason,
    String createdAt
) {
}
