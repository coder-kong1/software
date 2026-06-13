package org.example.backend.dto.billing;

import org.example.backend.domain.AbnormalEvent;
import org.example.backend.domain.PenaltyBill;

public record UserAbnormalEventView(
    long id,
    String carId,
    String eventType,
    String description,
    double penaltyFee,
    String status,
    String createdAt,
    String resolvedAt,
    String penaltyBillNo,
    String penaltyStatus,
    String notification
) {
    public static UserAbnormalEventView from(
        AbnormalEvent event,
        PenaltyBill penaltyBill
    ) {
        String notification = "RESOLVED".equals(event.status())
            ? "管理员已完成该异常事件的处理"
            : "管理员已登记异常事件，请及时查看处理状态";
        if (penaltyBill != null && "UNPAID".equals(penaltyBill.status())) {
            notification += "；存在待支付罚款";
        } else if (penaltyBill != null) {
            notification += "；罚款已支付";
        }
        return new UserAbnormalEventView(
            event.id(),
            event.carId(),
            event.eventType(),
            event.description(),
            event.penaltyFee(),
            event.status(),
            event.createdAt(),
            event.resolvedAt(),
            penaltyBill == null ? null : penaltyBill.billNo(),
            penaltyBill == null ? null : penaltyBill.status(),
            notification
        );
    }
}
