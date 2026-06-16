package org.example.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.example.backend.domain.ChargingMode;
import org.example.backend.domain.PriceRule;
import org.example.backend.service.TariffCalculator;
import org.junit.jupiter.api.Test;

class TariffCalculatorTests {

    private final TariffCalculator calculator = new TariffCalculator();
    private final PriceRule rule = new PriceRule(1.0, 0.7, 0.4, 1.0, 0.8, null);

    @Test
    void splitsEnergyAcrossNormalAndPeakPeriods() {
        TariffCalculator.FeeBreakdown fee = calculator.calculate(
            LocalDateTime.of(2026, 6, 12, 9, 0),
            LocalDateTime.of(2026, 6, 12, 11, 0),
            20,
            ChargingMode.SLOW,
            rule
        );

        assertThat(fee.chargeFee()).isEqualTo(17.0);
        assertThat(fee.serviceFee()).isEqualTo(16.0);
        assertThat(fee.totalFee()).isEqualTo(33.0);
    }

    @Test
    void appliesValleyPriceAcrossMidnight() {
        TariffCalculator.FeeBreakdown fee = calculator.calculate(
            LocalDateTime.of(2026, 6, 12, 23, 30),
            LocalDateTime.of(2026, 6, 13, 0, 30),
            10,
            ChargingMode.SLOW,
            rule
        );

        assertThat(fee.chargeFee()).isEqualTo(4.0);
        assertThat(fee.serviceFee()).isEqualTo(8.0);
        assertThat(fee.totalFee()).isEqualTo(12.0);
    }
}
