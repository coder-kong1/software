package org.example.backend.dto.account;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @JsonAlias("account")
    @NotBlank(message = "车号不能为空")
    String carId,

    @NotBlank(message = "密码不能为空")
    String password
) {
}
