package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;
import org.example.backend.domain.UserAccount;
import org.example.backend.dto.charging.ChargingRequestResponse;
import org.example.backend.dto.charging.CreateChargingRequest;
import org.example.backend.repository.ChargingRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargingRequestService {

    private final ChargingRequestRepository chargingRequestRepository;
    private final AccountService accountService;
    private final SchedulingService schedulingService;

    public ChargingRequestService(
        ChargingRequestRepository chargingRequestRepository,
        AccountService accountService,
        SchedulingService schedulingService
    ) {
        this.chargingRequestRepository = chargingRequestRepository;
        this.accountService = accountService;
        this.schedulingService = schedulingService;
    }

    @Transactional
    public ChargingRequestResponse create(CreateChargingRequest request) {
        String carId = AccountService.normalizeCarId(request.carId());
        UserAccount account = accountService.requireAccount(carId);
        validateAmount(request.requestAmount(), account);

        if (chargingRequestRepository.existsActiveByCarId(carId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该车辆已有未完成的充电申请");
        }

        chargingRequestRepository.insert(carId, request.requestAmount(), request.requestMode());
        ChargingRequest created = requireActive(carId);
        chargingRequestRepository.setInitialQueueNum(
            created.id(),
            queueNum(request.requestMode(), created.id())
        );
        schedulingService.schedule();
        return ChargingRequestResponse.from(requireActive(carId));
    }

    @Transactional
    public ChargingRequestResponse updateAmount(String carId, double amount) {
        String normalizedCarId = AccountService.normalizeCarId(carId);
        UserAccount account = accountService.requireAccount(normalizedCarId);
        ChargingRequest current = requireEditable(normalizedCarId);
        validateAmount(amount, account);

        chargingRequestRepository.updateAmount(current.id(), amount);
        schedulingService.schedule();
        return ChargingRequestResponse.from(requireActive(normalizedCarId));
    }

    @Transactional
    public ChargingRequestResponse updateMode(String carId, ChargingMode mode) {
        String normalizedCarId = AccountService.normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        ChargingRequest current = requireEditable(normalizedCarId);

        chargingRequestRepository.updateModeAndQueue(
            current.id(),
            mode,
            queueNum(mode, current.id())
        );
        schedulingService.schedule();
        return ChargingRequestResponse.from(requireActive(normalizedCarId));
    }

    @Transactional
    public void cancel(String carId) {
        String normalizedCarId = AccountService.normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        ChargingRequest current = requireEditable(normalizedCarId);
        chargingRequestRepository.cancel(current.id());
        schedulingService.schedule();
    }

    public ChargingRequestResponse getState(String carId) {
        String normalizedCarId = AccountService.normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        return ChargingRequestResponse.from(requireActive(normalizedCarId));
    }

    private ChargingRequest requireEditable(String carId) {
        ChargingRequest request = requireActive(carId);
        if (request.state() != ChargingRequestState.WAITING_AREA
            && request.state() != ChargingRequestState.QUEUING) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前充电状态不允许修改或取消申请");
        }
        return request;
    }

    private ChargingRequest requireActive(String carId) {
        return chargingRequestRepository.findActiveByCarId(carId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到进行中的充电申请"));
    }

    private void validateAmount(double amount, UserAccount account) {
        if (amount > account.carCapacity()) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "请求电量不能超过车辆电池容量 " + account.carCapacity() + " kWh"
            );
        }
    }

    private String queueNum(ChargingMode mode, long requestId) {
        return (mode == ChargingMode.FAST ? "F" : "S") + requestId;
    }
}
