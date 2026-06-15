package org.example.backend.service;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.ChargingPile;
import org.example.backend.domain.PileStatus;
import org.example.backend.repository.ChargingPileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PileService {

    private final ChargingPileRepository chargingPileRepository;
    private final SchedulingService schedulingService;

    public PileService(
        ChargingPileRepository chargingPileRepository,
        SchedulingService schedulingService
    ) {
        this.chargingPileRepository = chargingPileRepository;
        this.schedulingService = schedulingService;
    }

    @Transactional
    public void powerOn(String pileId) {
        ChargingPile pile = requirePile(pileId);
        requireStatus(pile, PileStatus.STOPPED, "只有已关闭的充电桩可以启动");
        chargingPileRepository.updateStatus(pile.id(), PileStatus.RUNNING);
        schedulingService.schedule();
    }

    @Transactional
    public void powerOff(String pileId) {
        ChargingPile pile = requirePile(pileId);
        requireStatus(pile, PileStatus.RUNNING, "只有运行中的充电桩可以关闭");
        chargingPileRepository.updateStatus(pile.id(), PileStatus.STOPPED);
        schedulingService.releaseAndReschedule(pile.id());
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
