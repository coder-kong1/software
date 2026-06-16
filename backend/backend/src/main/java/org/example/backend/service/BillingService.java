package org.example.backend.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.Bill;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;
import org.example.backend.domain.Payment;
import org.example.backend.domain.PriceRule;
import org.example.backend.domain.AbnormalEvent;
import org.example.backend.domain.PenaltyBill;
import org.example.backend.dto.billing.BillingItem;
import org.example.backend.dto.billing.ChargingDetailResponse;
import org.example.backend.dto.billing.PaymentRequest;
import org.example.backend.dto.billing.PriceRuleRequest;
import org.example.backend.dto.billing.UserAbnormalEventView;
import org.example.backend.dto.charging.ChargingRequestResponse;
import org.example.backend.repository.BillRepository;
import org.example.backend.repository.ChargingPileRepository;
import org.example.backend.repository.ChargingRequestRepository;
import org.example.backend.repository.PaymentRepository;
import org.example.backend.repository.PriceRuleRepository;
import org.example.backend.repository.AbnormalEventRepository;
import org.example.backend.repository.PenaltyBillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Shanghai");

    private final ChargingRequestRepository chargingRequestRepository;
    private final ChargingPileRepository chargingPileRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final SchedulingService schedulingService;
    private final TariffCalculator tariffCalculator;
    private final AbnormalEventRepository abnormalEventRepository;
    private final PenaltyBillRepository penaltyBillRepository;
    private final ChargingProgressService chargingProgressService;

    public BillingService(
        ChargingRequestRepository chargingRequestRepository,
        ChargingPileRepository chargingPileRepository,
        PriceRuleRepository priceRuleRepository,
        BillRepository billRepository,
        PaymentRepository paymentRepository,
        AccountService accountService,
        SchedulingService schedulingService,
        TariffCalculator tariffCalculator,
        AbnormalEventRepository abnormalEventRepository,
        PenaltyBillRepository penaltyBillRepository,
        ChargingProgressService chargingProgressService
    ) {
        this.chargingRequestRepository = chargingRequestRepository;
        this.chargingPileRepository = chargingPileRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.accountService = accountService;
        this.schedulingService = schedulingService;
        this.tariffCalculator = tariffCalculator;
        this.abnormalEventRepository = abnormalEventRepository;
        this.penaltyBillRepository = penaltyBillRepository;
        this.chargingProgressService = chargingProgressService;
    }

    @Transactional
    public ChargingRequestResponse startCharging(String carId, String pileId) {
        String normalizedCarId = normalizeCarId(carId);
        String normalizedPileId = normalizePileId(pileId);
        accountService.requireAccount(normalizedCarId);
        ChargingRequest request = requireActive(normalizedCarId);

        if (request.pileId() == null || !request.pileId().equalsIgnoreCase(normalizedPileId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "车辆未分配到指定充电桩");
        }
        if (request.state() == ChargingRequestState.WAITING_AREA) {
            throw new BusinessException(HttpStatus.CONFLICT, "车辆尚未进入充电桩队列");
        }
        if (request.state() == ChargingRequestState.QUEUING
            && chargingRequestRepository.findChargingByPile(normalizedPileId).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "该充电桩正在为其他车辆充电");
        }
        if (request.state() == ChargingRequestState.QUEUING) {
            ChargingRequest firstQueued = chargingRequestRepository
                .findFirstQueuedByPile(normalizedPileId)
                .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "充电桩队列为空"));
            if (firstQueued.id() != request.id()) {
                throw new BusinessException(HttpStatus.CONFLICT, "只有队首车辆可以开始充电");
            }
            chargingRequestRepository.startCharging(request.id());
        }
        return chargingProgressService.response(requireActive(normalizedCarId));
    }

    public ChargingDetailResponse getDetail(String carId) {
        String normalizedCarId = normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        Optional<ChargingRequest> activeRequest = chargingRequestRepository.findActiveByCarId(normalizedCarId);
        if (activeRequest.isPresent()) {
            ChargingRequest request = activeRequest.get();
            ChargingProgressService.ChargingProgress progress = chargingProgressService.progress(request);
            if (request.state() == ChargingRequestState.CHARGING
                && progress.chargedAmount() + 0.0001 >= request.requestAmount()) {
                Bill bill = finishRequest(request, progress);
                ChargingRequest finishedRequest = chargingRequestRepository.findById(request.id())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到充电申请"));
                return detailFromBill(finishedRequest, bill);
            }
            return detailFromActiveRequest(request, progress);
        }

        Bill latestBill = billRepository.findLatestByCarId(normalizedCarId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到充电详单"));
        ChargingRequest request = chargingRequestRepository.findById(latestBill.requestId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到充电申请"));
        return detailFromBill(request, latestBill);
    }
    @Transactional
    public Bill endCharging(String carId) {
        String normalizedCarId = normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        ChargingRequest request = requireActive(normalizedCarId);
        if (request.state() != ChargingRequestState.CHARGING || request.pileId() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "车辆当前未在充电");
        }

        return finishRequest(request, chargingProgressService.progress(request));
    }

    @Transactional
    public Optional<Bill> finishIfFullyCharged(String carId) {
        String normalizedCarId = normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        Optional<ChargingRequest> active = chargingRequestRepository.findActiveByCarId(normalizedCarId);
        if (active.isEmpty()) {
            return Optional.empty();
        }
        ChargingRequest request = active.get();
        if (request.state() != ChargingRequestState.CHARGING || request.pileId() == null) {
            return Optional.empty();
        }
        ChargingProgressService.ChargingProgress progress = chargingProgressService.progress(request);
        if (progress.chargedAmount() + 0.0001 < request.requestAmount()) {
            return Optional.empty();
        }
        return Optional.of(finishRequest(request, progress));
    }


    private ChargingDetailResponse detailFromActiveRequest(
        ChargingRequest request,
        ChargingProgressService.ChargingProgress progress
    ) {
        PriceRule rule = priceRuleRepository.get();
        TariffCalculator.FeeBreakdown fee = calculateFee(request, progress, rule);
        return new ChargingDetailResponse(
            request.carId(),
            position(request.state()),
            request.requestMode(),
            request.requestAmount(),
            progress.chargedAmount(),
            request.queueNum(),
            request.pileId(),
            request.startTime(),
            progress.durationHours(),
            fee.chargeFee(),
            fee.serviceFee(),
            fee.totalFee()
        );
    }

    private ChargingDetailResponse detailFromBill(ChargingRequest request, Bill bill) {
        return new ChargingDetailResponse(
            request.carId(),
            position(request.state()),
            request.requestMode(),
            request.requestAmount(),
            bill.chargeAmount(),
            request.queueNum(),
            bill.pileId(),
            bill.startTime(),
            bill.chargeDuration(),
            bill.chargeFee(),
            bill.serviceFee(),
            bill.totalFee()
        );
    }
    private Bill finishRequest(
        ChargingRequest request,
        ChargingProgressService.ChargingProgress progress
    ) {
        Optional<Bill> existingBill = billRepository.findByRequestId(request.id());
        if (existingBill.isPresent()) {
            return existingBill.get();
        }

        PriceRule rule = priceRuleRepository.get();
        TariffCalculator.FeeBreakdown fee = calculateFee(request, progress, rule);
        chargingRequestRepository.finish(request.id(), progress.chargedAmount());
        String billNo = billNo(request);
        billRepository.insert(
            billNo,
            request.id(),
            request.carId(),
            request.pileId(),
            progress.chargedAmount(),
            progress.durationHours(),
            fee.chargeFee(),
            fee.serviceFee(),
            fee.totalFee()
        );
        chargingPileRepository.addChargingStatistics(
            request.pileId(),
            progress.durationHours(),
            progress.chargedAmount()
        );
        schedulingService.schedule();
        return requireBill(billNo);
    }
    public List<BillingItem> getBills(String carId, String date) {
        String normalizedCarId = normalizeCarId(carId);
        finishIfFullyCharged(normalizedCarId);
        List<BillingItem> items = new ArrayList<>();
        billRepository.findByCarId(normalizedCarId, date).stream()
            .map(BillingItem::charging)
            .forEach(items::add);
        penaltyBillRepository.findByCarId(normalizedCarId).stream()
            .filter(bill -> date == null || date.isBlank() || bill.createdAt().startsWith(date))
            .map(bill -> BillingItem.penalty(
                bill,
                abnormalEventRepository.findById(bill.eventId())
                    .map(AbnormalEvent::description)
                    .orElse("异常罚款")
            ))
            .forEach(items::add);
        items.sort(Comparator.comparing(BillingItem::createdAt).reversed());
        return items;
    }

    public BillingItem getBillDetail(String billNo) {
        String normalizedBillNo = billNo.trim().toUpperCase(Locale.ROOT);
        return billRepository.findByBillNo(normalizedBillNo)
            .map(BillingItem::charging)
            .orElseGet(() -> penaltyBillRepository.findByBillNo(normalizedBillNo)
                .map(bill -> BillingItem.penalty(
                    bill,
                    abnormalEventRepository.findById(bill.eventId())
                        .map(AbnormalEvent::description)
                        .orElse("异常罚款")
                ))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "账单不存在")));
    }

    public List<BillingItem> getAllBills() {
        List<BillingItem> items = new ArrayList<>();
        billRepository.findAll().stream().map(BillingItem::charging).forEach(items::add);
        penaltyBillRepository.findAll().stream()
            .map(bill -> BillingItem.penalty(
                bill,
                abnormalEventRepository.findById(bill.eventId())
                    .map(AbnormalEvent::description)
                    .orElse("异常罚款")
            ))
            .forEach(items::add);
        items.sort(Comparator.comparing(BillingItem::createdAt).reversed());
        return items;
    }

    @Transactional
    public Payment pay(PaymentRequest request) {
        String billNo = request.billNo().trim().toUpperCase(Locale.ROOT);
        String carId = normalizeCarId(request.carId());
        accountService.requireAccount(carId);
        PenaltyBill penaltyBill = penaltyBillRepository.findByBillNo(billNo).orElse(null);
        if (penaltyBill != null) {
            return payPenalty(request, carId, penaltyBill);
        }
        Bill bill = requireBill(billNo);

        if (!bill.carId().equals(carId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权支付其他车辆的账单");
        }
        if ("PAID".equals(bill.status()) || paymentRepository.findByBillNo(billNo).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "该账单已支付");
        }
        if (Math.abs(request.amount() - bill.totalFee()) > 0.005) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付金额与账单金额不一致");
        }

        paymentRepository.insert(billNo, carId, bill.totalFee());
        billRepository.markPaid(billNo);
        return paymentRepository.findByBillNo(billNo)
            .orElseThrow(() -> new IllegalStateException("支付记录创建失败"));
    }

    public List<Payment> getPayments(String carId) {
        String normalizedCarId = normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        List<Payment> payments = new ArrayList<>(paymentRepository.findByCarId(normalizedCarId));
        payments.addAll(penaltyBillRepository.findPaymentsByCarId(normalizedCarId));
        payments.sort(Comparator.comparing(Payment::paidAt).reversed());
        return payments;
    }

    public List<UserAbnormalEventView> getAbnormalEvents(String carId) {
        String normalizedCarId = normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        return abnormalEventRepository.findByCarId(normalizedCarId).stream()
            .map(event -> UserAbnormalEventView.from(
                event,
                penaltyBillRepository.findByEventId(event.id()).orElse(null)
            ))
            .toList();
    }

    public PriceRule getPriceRule() {
        return priceRuleRepository.get();
    }

    @Transactional
    public PriceRule updatePriceRule(PriceRuleRequest request) {
        priceRuleRepository.update(
            request.peakPrice(),
            request.normalPrice(),
            request.valleyPrice(),
            request.fastServicePrice(),
            request.slowServicePrice()
        );
        return priceRuleRepository.get();
    }

    private ChargingRequest requireActive(String carId) {
        return chargingRequestRepository.findActiveByCarId(carId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到进行中的充电申请"));
    }

    private Bill requireBill(String billNo) {
        return billRepository.findByBillNo(billNo)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "账单不存在"));
    }

    private Payment payPenalty(
        PaymentRequest request,
        String carId,
        PenaltyBill bill
    ) {
        if (!bill.carId().equals(carId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权支付其他车辆的罚款账单");
        }
        if ("PAID".equals(bill.status())
            || penaltyBillRepository.findPaymentByBillNo(bill.billNo()).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "该罚款账单已支付");
        }
        if (Math.abs(request.amount() - bill.amount()) > 0.005) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付金额与罚款金额不一致");
        }
        penaltyBillRepository.insertPayment(bill.billNo(), carId, bill.amount());
        penaltyBillRepository.markPaid(bill.billNo());
        return penaltyBillRepository.findPaymentByBillNo(bill.billNo())
            .orElseThrow(() -> new IllegalStateException("罚款支付记录创建失败"));
    }

    private String position(ChargingRequestState state) {
        return switch (state) {
            case WAITING_AREA -> "等待区";
            case QUEUING -> "充电桩等待队列";
            case CHARGING -> "充电中";
            case FINISHED -> "已完成";
            case CANCELED -> "已取消";
        };
    }

    private String billNo(ChargingRequest request) {
        return "B" + nowUtc().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + "-" + request.id();
    }

    private TariffCalculator.FeeBreakdown calculateFee(
        ChargingRequest request,
        ChargingProgressService.ChargingProgress progress,
        PriceRule rule
    ) {
        LocalDateTime localStart = progress.startTime()
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(BILLING_ZONE)
            .toLocalDateTime();
        LocalDateTime localEnd = progress.endTime()
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(BILLING_ZONE)
            .toLocalDateTime();
        return tariffCalculator.calculate(
            localStart,
            localEnd,
            progress.chargedAmount(),
            request.requestMode(),
            rule
        );
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String normalizeCarId(String carId) {
        return AccountService.normalizeCarId(carId);
    }

    private String normalizePileId(String pileId) {
        return pileId.trim().toUpperCase(Locale.ROOT);
    }

}
