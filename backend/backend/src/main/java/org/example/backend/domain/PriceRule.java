package org.example.backend.domain;

public record PriceRule(
    double peakPrice,
    double normalPrice,
    double valleyPrice,
    double fastServicePrice,
    double slowServicePrice,
    String updatedAt
) {
}
