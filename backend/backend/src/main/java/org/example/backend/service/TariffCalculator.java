package org.example.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.PriceRule;
import org.springframework.stereotype.Component;

@Component
public class TariffCalculator {

    public FeeBreakdown calculate(
        LocalDateTime startTime,
        LocalDateTime endTime,
        double chargeAmount,
        ChargingMode mode,
        PriceRule rule
    ) {
        if (chargeAmount <= 0) {
            return new FeeBreakdown(0, 0, 0);
        }

        long totalSeconds = Math.max(0, Duration.between(startTime, endTime).getSeconds());
        double chargeFee;
        if (totalSeconds == 0) {
            chargeFee = chargeAmount * priceAt(startTime.toLocalTime(), rule);
        } else {
            double energyPerSecond = chargeAmount / totalSeconds;
            double accumulated = 0;
            LocalDateTime cursor = startTime;
            while (cursor.isBefore(endTime)) {
                LocalDateTime boundary = nextBoundary(cursor);
                LocalDateTime segmentEnd = boundary.isBefore(endTime) ? boundary : endTime;
                long segmentSeconds = Duration.between(cursor, segmentEnd).getSeconds();
                accumulated += segmentSeconds * energyPerSecond
                    * priceAt(cursor.toLocalTime(), rule);
                cursor = segmentEnd;
            }
            chargeFee = accumulated;
        }

        double servicePrice = mode == ChargingMode.FAST
            ? rule.fastServicePrice()
            : rule.slowServicePrice();
        double serviceFee = chargeAmount * servicePrice;
        return new FeeBreakdown(
            money(chargeFee),
            money(serviceFee),
            money(chargeFee + serviceFee)
        );
    }

    private double priceAt(LocalTime time, PriceRule rule) {
        if ((atLeast(time, 10, 0) && before(time, 15, 0))
            || (atLeast(time, 18, 0) && before(time, 21, 0))) {
            return rule.peakPrice();
        }
        if ((atLeast(time, 7, 0) && before(time, 10, 0))
            || (atLeast(time, 15, 0) && before(time, 18, 0))
            || (atLeast(time, 21, 0) && before(time, 23, 0))) {
            return rule.normalPrice();
        }
        return rule.valleyPrice();
    }

    private LocalDateTime nextBoundary(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        int[] hours = {7, 10, 15, 18, 21, 23};
        for (int hour : hours) {
            LocalTime boundary = LocalTime.of(hour, 0);
            if (time.isBefore(boundary)) {
                return LocalDateTime.of(date, boundary);
            }
        }
        return LocalDateTime.of(date.plusDays(1), LocalTime.of(7, 0));
    }

    private boolean atLeast(LocalTime time, int hour, int minute) {
        return !time.isBefore(LocalTime.of(hour, minute));
    }

    private boolean before(LocalTime time, int hour, int minute) {
        return time.isBefore(LocalTime.of(hour, minute));
    }

    private double money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public record FeeBreakdown(double chargeFee, double serviceFee, double totalFee) {
    }
}
