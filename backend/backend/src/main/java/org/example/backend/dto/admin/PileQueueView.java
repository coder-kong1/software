package org.example.backend.dto.admin;

import java.util.List;

import org.example.backend.domain.ChargingPile;

public record PileQueueView(
    ChargingPile pile,
    List<QueueCarView> cars
) {
}
