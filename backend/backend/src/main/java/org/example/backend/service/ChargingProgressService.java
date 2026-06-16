package org.example.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.ChargingPile;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;
import org.example.backend.dto.charging.ChargingRequestResponse;
import org.example.backend.repository.ChargingPileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChargingProgressService {

    private static final DateTimeFormatter SQLITE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChargingPileRepository chargingPileRepository;

    public ChargingProgressService(ChargingPileRepository chargingPileRepository) {
        this.chargingPileRepository = chargingPileRepository;
    }

    public ChargingProgress progress(ChargingRequest request) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (request.state() != ChargingRequestState.CHARGING || request.startTime() == null) {
            return new ChargingProgress(now, now, request.chargedAmount(), 0);
        }

        ChargingPile pile = chargingPileRepository.findById(request.pileId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "充电桩不存在"));
        LocalDateTime segmentStartTime = request.updatedAt() == null
            ? parseTime(request.startTime())
            : parseTime(request.updatedAt());
        double currentSegmentHours = Math.max(
            0,
            Duration.between(segmentStartTime, now).toSeconds() / 3600.0
        );
        double totalChargedAmount = Math.min(
            request.requestAmount(),
            request.chargedAmount() + currentSegmentHours * pile.powerKw()
        );
        double totalDurationHours = totalChargedAmount / pile.powerKw();
        LocalDateTime effectiveStartTime = now.minusSeconds(
            Math.max(0L, Math.round(totalDurationHours * 3600))
        );
        return new ChargingProgress(
            effectiveStartTime,
            now,
            decimal(totalChargedAmount, 4),
            decimal(totalDurationHours, 4)
        );
    }

    public double chargedAmount(ChargingRequest request) {
        return progress(request).chargedAmount();
    }

    public ChargingRequestResponse response(ChargingRequest request) {
        return ChargingRequestResponse.from(request, chargedAmount(request));
    }

    private LocalDateTime parseTime(String value) {
        try {
            return LocalDateTime.parse(value, SQLITE_TIME);
        } catch (DateTimeParseException exception) {
            return LocalDateTime.parse(value);
        }
    }

    private double decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public record ChargingProgress(
        LocalDateTime startTime,
        LocalDateTime endTime,
        double chargedAmount,
        double durationHours
    ) {
    }
}
