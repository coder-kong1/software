package org.example.backend.dto.account;

import org.example.backend.domain.UserAccount;

public record AccountResponse(
    long id,
    String carId,
    String userName,
    double carCapacity,
    String createdAt,
    String updatedAt
) {
    public static AccountResponse from(UserAccount account) {
        return new AccountResponse(
            account.id(),
            account.carId(),
            account.userName(),
            account.carCapacity(),
            account.createdAt(),
            account.updatedAt()
        );
    }
}
