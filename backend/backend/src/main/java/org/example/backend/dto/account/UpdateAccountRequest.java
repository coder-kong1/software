package org.example.backend.dto.account;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
    @Size(min = 1, max = 64, message = "用户名长度必须为 1 到 64 个字符")
    String userName,

    @Size(min = 6, max = 72, message = "密码长度必须为 6 到 72 个字符")
    String password,

    @Positive(message = "电池容量必须大于 0")
    Double carCapacity
) {
}
