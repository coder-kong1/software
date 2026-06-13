package org.example.backend.dto.billing;

import org.example.backend.domain.Bill;
import org.example.backend.domain.PenaltyBill;

public record BillingItem(
    long id,
    String billNo,
    String billType,
    String carId,
    String pileId,
    double chargeAmount,
    double chargeDuration,
    double chargeFee,
    double serviceFee,
    double penaltyFee,
    double totalFee,
    String status,
    String description,
    String startTime,
    String endTime,
    String createdAt,
    String paidAt
) {
    public static BillingItem charging(Bill bill) {
        return new BillingItem(
            bill.id(),
            bill.billNo(),
            "CHARGING",
            bill.carId(),
            bill.pileId(),
            bill.chargeAmount(),
            bill.chargeDuration(),
            bill.chargeFee(),
            bill.serviceFee(),
            0,
            bill.totalFee(),
            bill.status(),
            "充电费用",
            bill.startTime(),
            bill.endTime(),
            bill.createdAt(),
            bill.paidAt()
        );
    }

    public static BillingItem penalty(PenaltyBill bill, String description) {
        return new BillingItem(
            bill.id(),
            bill.billNo(),
            "PENALTY",
            bill.carId(),
            null,
            0,
            0,
            0,
            0,
            bill.amount(),
            bill.amount(),
            bill.status(),
            description,
            null,
            null,
            bill.createdAt(),
            bill.paidAt()
        );
    }
}
