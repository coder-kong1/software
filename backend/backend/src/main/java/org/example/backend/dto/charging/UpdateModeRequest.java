package org.example.backend.dto.charging;

import com.fasterxml.jackson.annotation.JsonAlias;

import org.example.backend.domain.ChargingMode;

import jakarta.validation.constraints.NotNull;

public record UpdateModeRequest(
    @JsonAlias("requestMode")
    @NotNull(message = "充电模式不能为空")
    ChargingMode mode
) {
}
