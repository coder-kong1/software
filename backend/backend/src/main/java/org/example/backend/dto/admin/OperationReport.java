package org.example.backend.dto.admin;

public record OperationReport(
    int billCount,
    int paidBillCount,
    int unpaidBillCount,
    double totalChargeAmount,
    double totalChargeDuration,
    double totalRevenue,
    int pendingAbnormalCount,
    int resolvedAbnormalCount
) {
}
