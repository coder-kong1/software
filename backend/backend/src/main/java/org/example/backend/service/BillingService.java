package org.example.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.Bill;
import org.example.backend.domain.ChargingPile;
import org.example.backend.domain.ChargingRequest;
import org.example.backend.domain.ChargingRequestState;
import org.example.backend.domain.Payment;
import org.example.backend.domain.PriceRule;
import org.example.backend.dto.billing.ChargingDetailResponse;
import org.example.backend.dto.billing.PaymentRequest;
import org.example.backend.dto.billing.PriceRuleRequest;
import org.example.backend.dto.charging.ChargingRequestResponse;
import org.example.backend.repository.BillRepository;
import org.example.backend.repository.ChargingPileRepository;
import org.example.backend.repository.ChargingRequestRepository;
import org.example.backend.repository.PaymentRepository;
import org.example.backend.repository.PriceRuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private static final DateTimeFormatter SQLITE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Shanghai");

    private final ChargingRequestRepository chargingRequestRepository;
    private final ChargingPileRepository chargingPileRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final SchedulingService schedulingService;
    private final TariffCalculator tariffCalculator;

    public BillingService(
        ChargingRequestRepository chargingRequestRepository,
        ChargingPileRepository chargingPileRepository,
        PriceRuleRepository priceRuleRepository,
        BillRepository billRepository,
        PaymentRepository paymentRepository,
        AccountService accountService,
        SchedulingService schedulingService,
        TariffCalculator tariffCalculator
    ) {
        this.chargingRequestRepository = chargingRequestRepository;
        this.chargingPileRepository = chargingPileRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.accountService = accountService;
        this.schedulingService = schedulingService;
        this.tariffCalculator = tariffCalculator;
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
        return ChargingRequestResponse.from(requireActive(normalizedCarId));
    }

    public ChargingDetailResponse getDetail(String carId) {
        String normalizedCarId = normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        ChargingRequest request = requireActive(normalizedCarId);
        Usage usage = currentUsage(request, nowUtc());
        PriceRule rule = priceRuleRepository.get();
        TariffCalculator.FeeBreakdown fee = calculateFee(usage, rule);

        return new ChargingDetailResponse(
            request.carId(),
            position(request.state()),
            request.requestMode(),
            request.requestAmount(),
            usage.chargeAmount(),
            request.queueNum(),
            request.pileId(),
            request.startTime(),
            usage.durationHours(),
            fee.chargeFee(),
            fee.serviceFee(),
            fee.totalFee()
        );
    }

    @Transactional
    public Bill endCharging(String carId) {
        String normalizedCarId = normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        ChargingRequest request = requireActive(normalizedCarId);
        if (request.state() != ChargingRequestState.CHARGING || request.pileId() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "车辆当前未在充电");
        }

        LocalDateTime endTime = nowUtc();
        Usage usage = currentUsage(request, endTime);
        PriceRule rule = priceRuleRepository.get();
        TariffCalculator.FeeBreakdown fee = calculateFee(usage, rule);

        chargingRequestRepository.finish(request.id(), usage.chargeAmount());
        String billNo = billNo(request);
        billRepository.insert(
            billNo,
            request.id(),
            request.carId(),
            request.pileId(),
            usage.chargeAmount(),
            usage.durationHours(),
            fee.chargeFee(),
            fee.serviceFee(),
            fee.totalFee()
        );
        chargingPileRepository.addChargingStatistics(
            request.pileId(),
            usage.durationHours(),
            usage.chargeAmount()
        );
        schedulingService.schedule();
        return requireBill(billNo);
    }

    public List<Bill> getBills(String carId, String date) {
        String normalizedCarId = normalizeCarId(carId);
        accountService.requireAccount(normalizedCarId);
        return billRepository.findByCarId(normalizedCarId, date);
    }

    public Bill getBillDetail(String billNo) {
        return requireBill(billNo.trim().toUpperCase(Locale.ROOT));
    }

    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    @Transactional
    public Payment pay(PaymentRequest request) {
        String billNo = request.billNo().trim().toUpperCase(Locale.ROOT);
        String carId = normalizeCarId(request.carId());
        accountService.requireAccount(carId);
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
        return paymentRepository.findByCarId(normalizedCarId);
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
            request.servicePrice()
        );
        return priceRuleRepository.get();
    }

    private Usage currentUsage(ChargingRequest request, LocalDateTime endTime) {
        if (request.state() != ChargingRequestState.CHARGING || request.startTime() == null) {
            return new Usage(endTime, endTime, 0, 0);
        }

        ChargingPile pile = chargingPileRepository.findById(request.pileId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "充电桩不存在"));
        LocalDateTime startTime = parseTime(request.startTime());
        double durationHours = Math.max(
            0,
            Duration.between(startTime, endTime).toSeconds() / 3600.0
        );
        double calculatedAmount = Math.min(
            request.requestAmount(),
            durationHours * pile.powerKw()
        );
        double chargeAmount = Math.max(request.chargedAmount(), calculatedAmount);
        return new Usage(
            startTime,
            endTime,
            decimal(chargeAmount, 4),
            decimal(durationHours, 4)
        );
    }

    private ChargingRequest requireActive(String carId) {
        return chargingRequestRepository.findActiveByCarId(carId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到进行中的充电申请"));
    }

    private Bill requireBill(String billNo) {
        return billRepository.findByBillNo(billNo)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "账单不存在"));
    }

    private LocalDateTime parseTime(String value) {
        try {
            return LocalDateTime.parse(value, SQLITE_TIME);
        } catch (DateTimeParseException exception) {
            return LocalDateTime.parse(value);
        }
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

    private TariffCalculator.FeeBreakdown calculateFee(Usage usage, PriceRule rule) {
        LocalDateTime localStart = usage.startTime()
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(BILLING_ZONE)
            .toLocalDateTime();
        LocalDateTime localEnd = usage.endTime()
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(BILLING_ZONE)
            .toLocalDateTime();
        return tariffCalculator.calculate(localStart, localEnd, usage.chargeAmount(), rule);
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

    private double decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private record Usage(
        LocalDateTime startTime,
        LocalDateTime endTime,
        double chargeAmount,
        double durationHours
    ) {
    }
}
