package org.example.backend.service;

import java.util.Comparator;
import java.util.List;

import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.ChargingPile;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;
import org.example.backend.dto.admin.PileStateView;
import org.example.backend.dto.admin.StationSnapshot;
import org.example.backend.dto.charging.ChargingRequestResponse;
import org.example.backend.repository.ChargingPileRepository;
import org.example.backend.repository.ChargingRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulingService {

    private final ChargingPileRepository chargingPileRepository;
    private final ChargingRequestRepository chargingRequestRepository;

    public SchedulingService(
        ChargingPileRepository chargingPileRepository,
        ChargingRequestRepository chargingRequestRepository
    ) {
        this.chargingPileRepository = chargingPileRepository;
        this.chargingRequestRepository = chargingRequestRepository;
    }

    @Transactional
    public void schedule() {
        scheduleMode(ChargingMode.FAST);
        scheduleMode(ChargingMode.SLOW);
    }

    @Transactional
    public void releaseAndReschedule(String pileId) {
        chargingRequestRepository.releasePile(pileId);
        schedule();
    }

    public StationSnapshot snapshot() {
        List<PileStateView> piles = chargingPileRepository.findAll().stream()
            .map(this::pileState)
            .toList();

        return new StationSnapshot(
            piles,
            toResponses(chargingRequestRepository.findByState(ChargingRequestState.WAITING_AREA)),
            toResponses(chargingRequestRepository.findWaitingByMode(ChargingMode.FAST)),
            toResponses(chargingRequestRepository.findWaitingByMode(ChargingMode.SLOW))
        );
    }

    private PileStateView pileState(ChargingPile pile) {
        List<ChargingRequest> active = chargingRequestRepository.findByPile(pile.id());
        List<ChargingRequestResponse> queue = active.stream()
            .filter(request -> request.state() == ChargingRequestState.QUEUING)
            .map(ChargingRequestResponse::from)
            .toList();
        ChargingRequestResponse chargingCar = active.stream()
            .filter(request -> request.state() == ChargingRequestState.CHARGING)
            .findFirst()
            .map(ChargingRequestResponse::from)
            .orElse(null);
        return new PileStateView(pile, queue, chargingCar);
    }

    private void scheduleMode(ChargingMode mode) {
        List<ChargingPile> runningPiles = chargingPileRepository.findRunningByMode(mode);
        if (runningPiles.isEmpty()) {
            return;
        }

        for (ChargingRequest request : chargingRequestRepository.findWaitingByMode(mode)) {
            ChargingPile target = runningPiles.stream()
                .filter(pile -> chargingRequestRepository.countActiveByPile(pile.id()) < pile.queueLimit() + 1)
                .min(Comparator
                    .comparingInt((ChargingPile pile) -> chargingRequestRepository.countActiveByPile(pile.id()))
                    .thenComparing(ChargingPile::id))
                .orElse(null);

            if (target == null) {
                return;
            }

            int activeCount = chargingRequestRepository.countActiveByPile(target.id());
            ChargingRequestState state = activeCount == 0
                ? ChargingRequestState.CHARGING
                : ChargingRequestState.QUEUING;
            chargingRequestRepository.assignToPile(
                request.id(),
                target.id(),
                target.id() + "-" + (activeCount + 1),
                state
            );
        }
    }

    private List<ChargingRequestResponse> toResponses(List<ChargingRequest> requests) {
        return requests.stream().map(ChargingRequestResponse::from).toList();
    }
}
