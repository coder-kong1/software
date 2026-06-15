package org.example.backend.dto.admin;

import org.example.backend.domain.SchedulingStrategy;

import jakarta.validation.constraints.NotNull;

public record SchedulingStrategyRequest(
    @NotNull(message = "调度策略不能为空")
    SchedulingStrategy strategy
) {
}
