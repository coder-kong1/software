package org.example.backend.dto.billing;

import org.example.backend.domain.ChargingMode;

public record ChargingDetailResponse(
    String carId,
    String currentPosition,
    ChargingMode requestMode,
    double requestAmount,
    double chargedAmount,
    String queueNum,
    String pileId,
    String startTime,
    double chargeDuration,
    double estimatedChargeFee,
    double estimatedServiceFee,
    double estimatedTotalFee
) {
}
