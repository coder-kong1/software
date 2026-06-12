package org.example.backend.domain;

public record UserAccount(
    long id,
    String carId,
    String userName,
    String passwordHash,
    double carCapacity,
    String createdAt,
    String updatedAt
) {
}
