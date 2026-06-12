package org.example.backend.dto.admin;

import java.util.List;

import org.example.backend.domain.ChargingPile;
import org.example.backend.dto.charging.ChargingRequestResponse;

public record PileStateView(
    ChargingPile pile,
    List<ChargingRequestResponse> queue,
    ChargingRequestResponse chargingCar
) {
}
