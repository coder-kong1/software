package org.example.backend.controller;

import java.util.List;

import org.example.backend.common.ApiResponse;
import org.example.backend.domain.Bill;
import org.example.backend.domain.Payment;
import org.example.backend.dto.billing.BillingItem;
import org.example.backend.dto.billing.ChargingDetailResponse;
import org.example.backend.dto.billing.PaymentRequest;
import org.example.backend.dto.billing.UserAbnormalEventView;
import org.example.backend.service.BillingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/charging")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/details/{carId}")
    public ApiResponse<ChargingDetailResponse> getDetail(@PathVariable String carId) {
        return ApiResponse.ok(billingService.getDetail(carId));
    }

    @GetMapping("/bills/{carId}")
    public ApiResponse<List<BillingItem>> getBills(
        @PathVariable String carId,
        @RequestParam(required = false) String date
    ) {
        return ApiResponse.ok(billingService.getBills(carId, date));
    }

    @GetMapping("/bills/detail/{billNo}")
    public ApiResponse<BillingItem> getBillDetail(@PathVariable String billNo) {
        return ApiResponse.ok(billingService.getBillDetail(billNo));
    }

    @PostMapping("/bills/pay")
    public ApiResponse<Payment> pay(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.ok(billingService.pay(request));
    }

    @GetMapping("/payments/{carId}")
    public ApiResponse<List<Payment>> getPayments(@PathVariable String carId) {
        return ApiResponse.ok(billingService.getPayments(carId));
    }

    @GetMapping("/abnormal-events/{carId}")
    public ApiResponse<List<UserAbnormalEventView>> getAbnormalEvents(
        @PathVariable String carId
    ) {
        return ApiResponse.ok(billingService.getAbnormalEvents(carId));
    }
}
