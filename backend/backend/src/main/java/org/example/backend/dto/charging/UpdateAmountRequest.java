package org.example.backend.dto.charging;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Positive;

public record UpdateAmountRequest(
    @JsonAlias("requestAmount")
    @Positive(message = "请求电量必须大于 0")
    double amount
) {
}
