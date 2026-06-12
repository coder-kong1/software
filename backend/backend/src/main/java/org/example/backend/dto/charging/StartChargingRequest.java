package org.example.backend.dto.charging;

import jakarta.validation.constraints.NotBlank;

public record StartChargingRequest(
    @NotBlank(message = "充电桩编号不能为空")
    String pileId
) {
}
