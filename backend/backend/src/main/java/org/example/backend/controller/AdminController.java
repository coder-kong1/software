package org.example.backend.controller;

import java.util.List;

import org.example.backend.common.ApiResponse;
import org.example.backend.domain.AbnormalEvent;
import org.example.backend.domain.PriceRule;
import org.example.backend.domain.SchedulingLog;
import org.example.backend.dto.admin.CreateAbnormalEventRequest;
import org.example.backend.dto.admin.OperationReport;
import org.example.backend.dto.admin.PileQueueView;
import org.example.backend.dto.admin.SchedulingStrategyRequest;
import org.example.backend.dto.admin.StationSnapshot;
import org.example.backend.dto.billing.PriceRuleRequest;
import org.example.backend.dto.billing.BillingItem;
import org.example.backend.service.AdminOperationService;
import org.example.backend.service.BillingService;
import org.example.backend.service.PileService;
import org.example.backend.service.SchedulingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SchedulingService schedulingService;
    private final PileService pileService;
    private final BillingService billingService;
    private final AdminOperationService adminOperationService;

    public AdminController(
        SchedulingService schedulingService,
        PileService pileService,
        BillingService billingService,
        AdminOperationService adminOperationService
    ) {
        this.schedulingService = schedulingService;
        this.pileService = pileService;
        this.billingService = billingService;
        this.adminOperationService = adminOperationService;
    }

    @GetMapping("/snapshot")
    public ApiResponse<StationSnapshot> snapshot() {
        return ApiResponse.ok(schedulingService.snapshot());
    }

    @GetMapping("/queues/{pileId}")
    public ApiResponse<PileQueueView> queueState(@PathVariable String pileId) {
        return ApiResponse.ok(schedulingService.queueState(pileId));
    }

    @GetMapping("/scheduling-strategy")
    public ApiResponse<?> getSchedulingStrategy() {
        return ApiResponse.ok(schedulingService.getStrategy());
    }

    @GetMapping("/scheduling-logs")
    public ApiResponse<List<SchedulingLog>> getSchedulingLogs() {
        return ApiResponse.ok(schedulingService.schedulingLogs());
    }

    @PutMapping("/scheduling-strategy")
    public ApiResponse<?> updateSchedulingStrategy(
        @Valid @RequestBody SchedulingStrategyRequest request
    ) {
        return ApiResponse.ok(schedulingService.updateStrategy(request));
    }

    @PostMapping("/piles/{pileId}/power-on")
    public ApiResponse<Void> powerOn(@PathVariable String pileId) {
        pileService.powerOn(pileId);
        return ApiResponse.ok();
    }

    @PostMapping("/piles/{pileId}/start")
    public ApiResponse<Void> start(@PathVariable String pileId) {
        pileService.start(pileId);
        return ApiResponse.ok();
    }

    @PostMapping("/piles/{pileId}/power-off")
    public ApiResponse<Void> powerOff(@PathVariable String pileId) {
        pileService.powerOff(pileId);
        return ApiResponse.ok();
    }

    @PostMapping("/piles/{pileId}/fault")
    public ApiResponse<Void> fault(@PathVariable String pileId) {
        pileService.reportFault(pileId);
        return ApiResponse.ok();
    }

    @PostMapping("/piles/{pileId}/recover")
    public ApiResponse<Void> recover(@PathVariable String pileId) {
        pileService.recover(pileId);
        return ApiResponse.ok();
    }

    @GetMapping("/price-rule")
    public ApiResponse<PriceRule> getPriceRule() {
        return ApiResponse.ok(billingService.getPriceRule());
    }

    @PutMapping("/price-rule")
    public ApiResponse<PriceRule> updatePriceRule(
        @Valid @RequestBody PriceRuleRequest request
    ) {
        return ApiResponse.ok(billingService.updatePriceRule(request));
    }

    @GetMapping("/reports/bills")
    public ApiResponse<List<BillingItem>> getBillReport() {
        return ApiResponse.ok(billingService.getAllBills());
    }

    @GetMapping("/reports/summary")
    public ApiResponse<OperationReport> getOperationReport() {
        return ApiResponse.ok(adminOperationService.getOperationReport());
    }

    @PostMapping("/abnormal-events")
    public ApiResponse<AbnormalEvent> createAbnormalEvent(
        @Valid @RequestBody CreateAbnormalEventRequest request
    ) {
        return ApiResponse.ok(adminOperationService.createAbnormalEvent(request));
    }

    @GetMapping("/abnormal-events")
    public ApiResponse<List<AbnormalEvent>> getAbnormalEvents() {
        return ApiResponse.ok(adminOperationService.getAbnormalEvents());
    }

    @PostMapping("/abnormal-events/{id}/resolve")
    public ApiResponse<AbnormalEvent> resolveAbnormalEvent(@PathVariable long id) {
        return ApiResponse.ok(adminOperationService.resolveAbnormalEvent(id));
    }
}


