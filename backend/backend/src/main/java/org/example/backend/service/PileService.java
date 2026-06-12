package org.example.backend.service;

import org.example.backend.common.BusinessException;
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
        requirePile(pileId);
        chargingPileRepository.updateStatus(normalizePileId(pileId), PileStatus.RUNNING);
        schedulingService.schedule();
    }

    @Transactional
    public void powerOff(String pileId) {
        requirePile(pileId);
        chargingPileRepository.updateStatus(normalizePileId(pileId), PileStatus.STOPPED);
        schedulingService.releaseAndReschedule(normalizePileId(pileId));
    }

    @Transactional
    public void reportFault(String pileId) {
        requirePile(pileId);
        chargingPileRepository.updateStatus(normalizePileId(pileId), PileStatus.FAULT);
        schedulingService.releaseAndReschedule(normalizePileId(pileId));
    }

    @Transactional
    public void recover(String pileId) {
        requirePile(pileId);
        chargingPileRepository.updateStatus(normalizePileId(pileId), PileStatus.RUNNING);
        schedulingService.schedule();
    }

    private void requirePile(String pileId) {
        chargingPileRepository.findById(normalizePileId(pileId))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "充电桩不存在"));
    }

    private String normalizePileId(String pileId) {
        return pileId.trim().toUpperCase();
    }
}
