package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.ChargingPile;
import org.example.backend.domain.PileStatus;
import org.example.backend.repository.ChargingPileRepository;
import org.example.backend.repository.ChargingRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PileService {

    private final ChargingPileRepository chargingPileRepository;
    private final ChargingRequestRepository chargingRequestRepository;
    private final SchedulingService schedulingService;

    public PileService(
        ChargingPileRepository chargingPileRepository,
        ChargingRequestRepository chargingRequestRepository,
        SchedulingService schedulingService
    ) {
        this.chargingPileRepository = chargingPileRepository;
        this.chargingRequestRepository = chargingRequestRepository;
        this.schedulingService = schedulingService;
    }

    @Transactional
    public void powerOn(String pileId) {
        ChargingPile pile = requirePile(pileId);
        requireStatus(pile, PileStatus.STOPPED, "只有已关闭的充电桩可以开机");
        chargingPileRepository.updateStatus(pile.id(), PileStatus.POWER_ON);
    }

    @Transactional
    public void start(String pileId) {
        ChargingPile pile = requirePile(pileId);
        requireStatus(pile, PileStatus.POWER_ON, "只有已开机的充电桩可以运行");
        chargingPileRepository.updateStatus(pile.id(), PileStatus.RUNNING);
        schedulingService.schedule();
    }

    @Transactional
    public void powerOff(String pileId) {
        ChargingPile pile = requirePile(pileId);
        if (pile.status() != PileStatus.POWER_ON && pile.status() != PileStatus.RUNNING) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有已开机或运行中的充电桩可以关闭");
        }
        if (pile.status() == PileStatus.RUNNING
            && chargingRequestRepository.findChargingByPile(pile.id()).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前有车辆正在充电，不能关闭充电桩");
        }
        chargingPileRepository.updateStatus(pile.id(), PileStatus.STOPPED);
        if (pile.status() == PileStatus.RUNNING) {
            schedulingService.releaseAndReschedule(pile.id());
        }
    }

    @Transactional
    public void reportFault(String pileId) {
        ChargingPile pile = requirePile(pileId);
        requireStatus(pile, PileStatus.RUNNING, "只有运行中的充电桩可以标记故障");
        chargingPileRepository.updateStatus(pile.id(), PileStatus.FAULT);
        schedulingService.releaseAndReschedule(pile.id());
    }

    @Transactional
    public void recover(String pileId) {
        ChargingPile pile = requirePile(pileId);
        requireStatus(pile, PileStatus.FAULT, "只有故障充电桩可以恢复");
        chargingPileRepository.updateStatus(pile.id(), PileStatus.RUNNING);
        schedulingService.recoverFaultPileAndReschedule(pile.id());
    }

    private ChargingPile requirePile(String pileId) {
        return chargingPileRepository.findById(normalizePileId(pileId))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "充电桩不存在"));
    }

    private void requireStatus(ChargingPile pile, PileStatus expected, String message) {
        if (pile.status() != expected) {
            throw new BusinessException(HttpStatus.CONFLICT, message);
        }
    }

    private String normalizePileId(String pileId) {
        return pileId.trim().toUpperCase();
    }
}
