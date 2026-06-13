package org.example.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAbnormalEventRequest(
    @NotBlank(message = "车号不能为空")
    String carId,

    @NotBlank(message = "异常类型不能为空")
    String eventType,

    String description,

    @PositiveOrZero(message = "罚款金额不能为负数")
    double penaltyFee
) {
}
