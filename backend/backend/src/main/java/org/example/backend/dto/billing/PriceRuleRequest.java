package org.example.backend.dto.billing;

import jakarta.validation.constraints.PositiveOrZero;

public record PriceRuleRequest(
    @PositiveOrZero(message = "峰时电价不能为负数")
    double peakPrice,

    @PositiveOrZero(message = "平时电价不能为负数")
    double normalPrice,

    @PositiveOrZero(message = "谷时电价不能为负数")
    double valleyPrice,

    @PositiveOrZero(message = "服务费不能为负数")
    double servicePrice
) {
}
