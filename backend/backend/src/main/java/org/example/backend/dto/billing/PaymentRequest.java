package org.example.backend.dto.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record PaymentRequest(
    @NotBlank(message = "账单号不能为空")
    String billNo,

    @NotBlank(message = "车号不能为空")
    String carId,

    @PositiveOrZero(message = "支付金额不能为负数")
    double amount
) {
}
