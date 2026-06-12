package org.example.backend.domain;

public record ChargingRequest(
    long id,
    String carId,
    double requestAmount,
    double chargedAmount,
    ChargingMode requestMode,
    ChargingRequestState state,
    String queueNum,
    String pileId,
    String requestTime,
    String startTime,
    String endTime,
    String updatedAt
) {
}
