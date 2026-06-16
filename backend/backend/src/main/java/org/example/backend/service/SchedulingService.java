package org.example.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.ChargingPile;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;
import org.example.backend.domain.SchedulingLog;
import org.example.backend.domain.SchedulingStrategy;
import org.example.backend.dto.admin.PileQueueView;
import org.example.backend.dto.admin.PileStateView;
import org.example.backend.dto.admin.QueueCarView;
import org.example.backend.dto.admin.SchedulingStrategyRequest;
import org.example.backend.dto.admin.StationSnapshot;
import org.example.backend.dto.charging.ChargingRequestResponse;
import org.example.backend.repository.ChargingPileRepository;
import org.example.backend.repository.ChargingRequestRepository;
import org.example.backend.repository.SchedulingConfigRepository;
import org.example.backend.repository.SchedulingLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulingService {

    private final ChargingPileRepository chargingPileRepository;
    private final ChargingRequestRepository chargingRequestRepository;
    private final SchedulingConfigRepository schedulingConfigRepository;
    private final SchedulingLogRepository schedulingLogRepository;
    private final AccountService accountService;
    private final ChargingProgressService chargingProgressService;

    public SchedulingService(
        ChargingPileRepository chargingPileRepository,
        ChargingRequestRepository chargingRequestRepository,
        SchedulingConfigRepository schedulingConfigRepository,
        SchedulingLogRepository schedulingLogRepository,
        AccountService accountService,
        ChargingProgressService chargingProgressService
    ) {
        this.chargingPileRepository = chargingPileRepository;
        this.chargingRequestRepository = chargingRequestRepository;
        this.schedulingConfigRepository = schedulingConfigRepository;
        this.schedulingLogRepository = schedulingLogRepository;
        this.accountService = accountService;
        this.chargingProgressService = chargingProgressService;
    }

    @Transactional
    public void schedule() {
        schedule("常规调度：按当前策略为等待区车辆分配充电桩");
    }

    private void schedule(String triggerReason) {
        promoteChargingCars(triggerReason + "；检查空闲充电桩");
        scheduleMode(ChargingMode.FAST, triggerReason);
        scheduleMode(ChargingMode.SLOW, triggerReason);
        promoteChargingCars(triggerReason + "；分配完成后启动首车");
    }

    public SchedulingStrategy getStrategy() {
        return schedulingConfigRepository.getStrategy();
    }

    @Transactional
    public SchedulingStrategy updateStrategy(SchedulingStrategyRequest request) {
        schedulingConfigRepository.updateStrategy(request.strategy());
        schedule("调度策略切换为" + strategyText(request.strategy()) + "后重新调度");
        return request.strategy();
    }

    @Transactional
    public void releaseAndReschedule(String pileId) {
        List<ChargingRequest> releasedRequests = chargingRequestRepository.findByPile(pileId);
        for (ChargingRequest request : releasedRequests) {
            if (request.state() == ChargingRequestState.CHARGING) {
                chargingRequestRepository.updateChargedAmount(
                    request.id(),
                    chargingProgressService.chargedAmount(request)
                );
            }
        }
        chargingRequestRepository.releasePile(pileId);
        SchedulingStrategy strategy = schedulingConfigRepository.getStrategy();
        for (ChargingRequest request : releasedRequests) {
            schedulingLogRepository.insert(
                request.id(),
                request.carId(),
                request.requestMode(),
                request.state().name(),
                ChargingRequestState.WAITING_AREA.name(),
                pileId,
                null,
                "P-" + pileId + "-" + request.id(),
                strategy,
                "充电桩 " + pileId + " 不可用，车辆释放回等待区等待重新调度"
            );
        }
        schedule("充电桩 " + pileId + " 不可用后重新调度");
    }

    @Transactional
    public void recoverFaultPileAndReschedule(String pileId) {
        ChargingPile recoveredPile = chargingPileRepository.findById(pileId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "充电桩不存在"));
        scheduleMode(recoveredPile.mode(), "充电桩 " + pileId + " 恢复运行后重新调度");
    }

    public StationSnapshot snapshot() {
        promoteChargingCars("查看队列状态时同步充电桩首车状态");
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
        promoteChargingCars("查询充电桩队列时同步充电状态");
        String normalizedPileId = pileId.trim().toUpperCase();
        ChargingPile pile = chargingPileRepository.findById(normalizedPileId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "充电桩不存在"));
        List<ChargingRequest> requests = chargingRequestRepository.findByPile(normalizedPileId);
        double accumulatedHours = 0;
        java.util.ArrayList<QueueCarView> cars = new java.util.ArrayList<>();

        int waitingPosition = 1;
        for (ChargingRequest request : requests) {
            double chargedAmount = chargingProgressService.chargedAmount(request);
            double remainingAmount = Math.max(0, request.requestAmount() - chargedAmount);
            boolean charging = request.state() == ChargingRequestState.CHARGING;
            double waitHours = charging ? 0 : accumulatedHours;
            String position = charging ? "正在充电" : "等待第 " + waitingPosition++ + " 位";
            cars.add(new QueueCarView(
                request.id(),
                request.carId(),
                accountService.requireAccount(request.carId()).carCapacity(),
                request.requestAmount(),
                chargedAmount,
                request.state(),
                request.queueNum(),
                request.requestTime(),
                position,
                decimal(waitHours)
            ));
            accumulatedHours += remainingAmount / pile.powerKw();
        }
        return new PileQueueView(pile, cars);
    }

    public List<SchedulingLog> schedulingLogs() {
        return schedulingLogRepository.findRecent(120);
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

    private void scheduleMode(ChargingMode mode, String triggerReason) {
        List<ChargingPile> runningPiles = chargingPileRepository.findRunningByMode(mode);
        if (runningPiles.isEmpty()) {
            return;
        }

        SchedulingStrategy strategy = schedulingConfigRepository.getStrategy();
        List<ChargingRequest> waitingRequests = strategy == SchedulingStrategy.PRIORITY
            ? priorityWaitingRequests(mode)
            : chargingRequestRepository.findWaitingByMode(mode);

        for (ChargingRequest request : waitingRequests) {
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
            boolean hasChargingCar = chargingRequestRepository.findChargingByPile(target.id()).isPresent();
            ChargingRequestState targetState = hasChargingCar ? ChargingRequestState.QUEUING : ChargingRequestState.CHARGING;
            String queueNum = target.id() + "-" + (activeCount + 1);
            ChargingRequestState fromState = request.state();
            String fromPileId = request.pileId();
            chargingRequestRepository.assignToPile(
                request.id(),
                target.id(),
                queueNum,
                targetState
            );
            schedulingLogRepository.insert(
                request.id(),
                request.carId(),
                request.requestMode(),
                fromState.name(),
                targetState.name(),
                fromPileId,
                target.id(),
                queueNum,
                strategy,
                triggerReason + "；" + strategyText(strategy) + "选择车辆 " + request.carId()
                    + "，目标桩 " + target.id() + " 当前占用 " + activeCount
                    + "/" + (target.queueLimit() + 1)
                    + "，分配结果：" + (targetState == ChargingRequestState.CHARGING ? "直接开始充电" : "进入桩内等待")
            );
        }
    }


    private void promoteChargingCars(String reason) {
        SchedulingStrategy strategy = schedulingConfigRepository.getStrategy();
        for (ChargingPile pile : chargingPileRepository.findAll()) {
            if (pile.status().name().equals("RUNNING")
                && chargingRequestRepository.findChargingByPile(pile.id()).isEmpty()) {
                chargingRequestRepository.findFirstQueuedByPile(pile.id()).ifPresent(request -> {
                    chargingRequestRepository.startCharging(request.id());
                    schedulingLogRepository.insert(
                        request.id(),
                        request.carId(),
                        request.requestMode(),
                        ChargingRequestState.QUEUING.name(),
                        ChargingRequestState.CHARGING.name(),
                        pile.id(),
                        pile.id(),
                        request.queueNum(),
                        strategy,
                        reason + "；充电桩 " + pile.id() + " 无正在充电车辆，队首车辆 " + request.carId() + " 自动开始充电"
                    );
                });
            }
        }
    }
    private List<ChargingRequest> priorityWaitingRequests(ChargingMode mode) {
        java.util.ArrayList<ChargingRequest> requests = new java.util.ArrayList<>();
        requests.addAll(chargingRequestRepository.findPriorityWaitingByMode(mode));
        requests.addAll(chargingRequestRepository.findNormalWaitingByMode(mode));
        return requests;
    }

    private List<ChargingRequestResponse> toResponses(List<ChargingRequest> requests) {
        return requests.stream().map(chargingProgressService::response).toList();
    }

    private double decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String strategyText(SchedulingStrategy strategy) {
        if (strategy == SchedulingStrategy.PRIORITY) {
            return "优先级调度";
        }
        return "时间顺序调度";
    }
}
