package org.example.backend.dto.admin;

import org.example.backend.domain.ChargingRequestState;

public record QueueCarView(
    long id,
    String carId,
    double carCapacity,
    double requestAmount,
    double chargedAmount,
    ChargingRequestState state,
    String queueNum,
    String requestTime,
    String position,
    double estimatedWaitHours
) {
}
