package org.example.backend.service;

import java.util.Comparator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.ChargingPile;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;
import org.example.backend.dto.admin.PileStateView;
import org.example.backend.dto.admin.PileQueueView;
import org.example.backend.dto.admin.QueueCarView;
import org.example.backend.dto.admin.StationSnapshot;
import org.example.backend.dto.charging.ChargingRequestResponse;
import org.example.backend.repository.ChargingPileRepository;
import org.example.backend.repository.ChargingRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.example.backend.common.BusinessException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulingService {

    private final ChargingPileRepository chargingPileRepository;
    private final ChargingRequestRepository chargingRequestRepository;
    private final AccountService accountService;
    private final ChargingProgressService chargingProgressService;

    public SchedulingService(
        ChargingPileRepository chargingPileRepository,
        ChargingRequestRepository chargingRequestRepository,
        AccountService accountService,
        ChargingProgressService chargingProgressService
    ) {
        this.chargingPileRepository = chargingPileRepository;
        this.chargingRequestRepository = chargingRequestRepository;
        this.accountService = accountService;
        this.chargingProgressService = chargingProgressService;
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

    public PileQueueView queueState(String pileId) {
        String normalizedPileId = pileId.trim().toUpperCase();
        ChargingPile pile = chargingPileRepository.findById(normalizedPileId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "充电桩不存在"));
        List<ChargingRequest> requests = chargingRequestRepository.findByPile(normalizedPileId);
        double accumulatedHours = 0;
        java.util.ArrayList<QueueCarView> cars = new java.util.ArrayList<>();

        for (ChargingRequest request : requests) {
            double chargedAmount = chargingProgressService.chargedAmount(request);
            double remainingAmount = Math.max(0, request.requestAmount() - chargedAmount);
            double waitHours = request.state() == ChargingRequestState.CHARGING
                ? 0
                : accumulatedHours;
            cars.add(new QueueCarView(
                request.id(),
                request.carId(),
                accountService.requireAccount(request.carId()).carCapacity(),
                request.requestAmount(),
                chargedAmount,
                request.state(),
                request.queueNum(),
                request.requestTime(),
                request.state() == ChargingRequestState.CHARGING
                    ? "正在充电"
                    : "等待第 " + (cars.size() + 1) + " 位",
                decimal(waitHours)
            ));
            accumulatedHours += remainingAmount / pile.powerKw();
        }
        return new PileQueueView(pile, cars);
    }

    private PileStateView pileState(ChargingPile pile) {
        List<ChargingRequest> active = chargingRequestRepository.findByPile(pile.id());
        List<ChargingRequestResponse> queue = active.stream()
            .filter(request -> request.state() == ChargingRequestState.QUEUING)
            .map(chargingProgressService::response)
            .toList();
        ChargingRequestResponse chargingCar = active.stream()
            .filter(request -> request.state() == ChargingRequestState.CHARGING)
            .findFirst()
            .map(chargingProgressService::response)
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
            chargingRequestRepository.assignToPile(
                request.id(),
                target.id(),
                target.id() + "-" + (activeCount + 1),
                ChargingRequestState.QUEUING
            );
        }
    }

    private List<ChargingRequestResponse> toResponses(List<ChargingRequest> requests) {
        return requests.stream().map(chargingProgressService::response).toList();
    }

    private double decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
