package org.example.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.AbnormalEvent;
import org.example.backend.domain.Bill;
import org.example.backend.domain.PenaltyBill;
import org.example.backend.dto.admin.CreateAbnormalEventRequest;
import org.example.backend.dto.admin.OperationReport;
import org.example.backend.repository.AbnormalEventRepository;
import org.example.backend.repository.BillRepository;
import org.example.backend.repository.PenaltyBillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOperationService {

    private static final Set<String> EVENT_TYPES = Set.of(
        "NO_SHOW",
        "OCCUPY_WITHOUT_CHARGE",
        "OVERSTAY",
        "QUEUE_JUMP"
    );

    private final AbnormalEventRepository abnormalEventRepository;
    private final BillRepository billRepository;
    private final AccountService accountService;
    private final PenaltyBillRepository penaltyBillRepository;

    public AdminOperationService(
        AbnormalEventRepository abnormalEventRepository,
        BillRepository billRepository,
        AccountService accountService,
        PenaltyBillRepository penaltyBillRepository
    ) {
        this.abnormalEventRepository = abnormalEventRepository;
        this.billRepository = billRepository;
        this.accountService = accountService;
        this.penaltyBillRepository = penaltyBillRepository;
    }

    @Transactional
    public AbnormalEvent createAbnormalEvent(CreateAbnormalEventRequest request) {
        String carId = AccountService.normalizeCarId(request.carId());
        accountService.requireAccount(carId);
        String eventType = request.eventType().trim().toUpperCase(Locale.ROOT);
        if (!EVENT_TYPES.contains(eventType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的异常类型");
        }

        double penaltyFee = money(request.penaltyFee());
        abnormalEventRepository.insert(
            carId,
            eventType,
            request.description() == null ? "" : request.description().trim(),
            penaltyFee
        );
        AbnormalEvent event = abnormalEventRepository.findLatestByCarId(carId)
            .orElseThrow(() -> new IllegalStateException("异常事件创建失败"));
        if (penaltyFee > 0) {
            penaltyBillRepository.insert(
                "P" + event.id() + "-" + System.currentTimeMillis(),
                event.id(),
                carId,
                penaltyFee
            );
        }
        return event;
    }

    public List<AbnormalEvent> getAbnormalEvents() {
        return abnormalEventRepository.findAll();
    }

    @Transactional
    public AbnormalEvent resolveAbnormalEvent(long id) {
        AbnormalEvent event = abnormalEventRepository.findById(id)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "异常事件不存在"));
        if ("RESOLVED".equals(event.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "异常事件已处理");
        }
        abnormalEventRepository.resolve(id);
        return abnormalEventRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("异常事件更新失败"));
    }

    public OperationReport getOperationReport() {
        List<Bill> bills = billRepository.findAll();
        List<PenaltyBill> penaltyBills = penaltyBillRepository.findAll();
        int paidCount = (int) bills.stream()
            .filter(bill -> "PAID".equals(bill.status()))
            .count();
        int paidPenaltyCount = (int) penaltyBills.stream()
            .filter(bill -> "PAID".equals(bill.status()))
            .count();
        double chargeAmount = bills.stream().mapToDouble(Bill::chargeAmount).sum();
        double chargeDuration = bills.stream().mapToDouble(Bill::chargeDuration).sum();
        double revenue = bills.stream()
            .filter(bill -> "PAID".equals(bill.status()))
            .mapToDouble(Bill::totalFee)
            .sum()
            + penaltyBills.stream()
                .filter(bill -> "PAID".equals(bill.status()))
                .mapToDouble(PenaltyBill::amount)
                .sum();
        int totalBillCount = bills.size() + penaltyBills.size();
        int totalPaidCount = paidCount + paidPenaltyCount;

        return new OperationReport(
            totalBillCount,
            totalPaidCount,
            totalBillCount - totalPaidCount,
            decimal(chargeAmount),
            decimal(chargeDuration),
            money(revenue),
            abnormalEventRepository.countByStatus("PENDING"),
            abnormalEventRepository.countByStatus("RESOLVED")
        );
    }

    private double money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
