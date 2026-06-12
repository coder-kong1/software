package org.example.backend.domain;

public record PriceRule(
    double peakPrice,
    double normalPrice,
    double valleyPrice,
    double servicePrice,
    String updatedAt
) {
}
