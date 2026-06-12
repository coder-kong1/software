package org.example.backend.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
    @NotBlank(message = "车号不能为空")
    @Size(max = 32, message = "车号不能超过 32 个字符")
    String carId,

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名不能超过 64 个字符")
    String userName,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度必须为 6 到 72 个字符")
    String password,

    @Positive(message = "电池容量必须大于 0")
    double carCapacity
) {
}
