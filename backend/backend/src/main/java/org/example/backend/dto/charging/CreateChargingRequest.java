package org.example.backend.dto.charging;

import org.example.backend.domain.ChargingMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateChargingRequest(
    @NotBlank(message = "车号不能为空")
    String carId,

    @Positive(message = "请求电量必须大于 0")
    double requestAmount,

    @NotNull(message = "充电模式不能为空")
    ChargingMode requestMode
) {
}
