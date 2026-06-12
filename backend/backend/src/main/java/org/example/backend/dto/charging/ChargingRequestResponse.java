package org.example.backend.dto.charging;

import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;

public record ChargingRequestResponse(
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
    String endTime
) {
    public static ChargingRequestResponse from(ChargingRequest request) {
        return new ChargingRequestResponse(
            request.id(),
            request.carId(),
            request.requestAmount(),
            request.chargedAmount(),
            request.requestMode(),
            request.state(),
            request.queueNum(),
            request.pileId(),
            request.requestTime(),
            request.startTime(),
            request.endTime()
        );
    }
}
