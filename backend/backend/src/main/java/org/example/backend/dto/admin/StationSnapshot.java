package org.example.backend.dto.admin;

import java.util.List;

import org.example.backend.dto.charging.ChargingRequestResponse;

public record StationSnapshot(
    List<PileStateView> piles,
    List<ChargingRequestResponse> waitingArea,
    List<ChargingRequestResponse> fastQueue,
    List<ChargingRequestResponse> slowQueue
) {
}
