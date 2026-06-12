package org.example.backend.controller;

import org.example.backend.common.ApiResponse;
import org.example.backend.dto.charging.ChargingRequestResponse;
import org.example.backend.dto.charging.CreateChargingRequest;
import org.example.backend.dto.charging.StartChargingRequest;
import org.example.backend.dto.charging.UpdateAmountRequest;
import org.example.backend.dto.charging.UpdateModeRequest;
import org.example.backend.domain.Bill;
import org.example.backend.service.BillingService;
import org.example.backend.service.ChargingRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/charging/requests")
public class ChargingController {

    private final ChargingRequestService chargingRequestService;
    private final BillingService billingService;

    public ChargingController(
        ChargingRequestService chargingRequestService,
        BillingService billingService
    ) {
        this.chargingRequestService = chargingRequestService;
        this.billingService = billingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChargingRequestResponse> create(
        @Valid @RequestBody CreateChargingRequest request
    ) {
        return ApiResponse.ok(chargingRequestService.create(request));
    }

    @PutMapping("/{carId}/amount")
    public ApiResponse<ChargingRequestResponse> updateAmount(
        @PathVariable String carId,
        @Valid @RequestBody UpdateAmountRequest request
    ) {
        return ApiResponse.ok(
            chargingRequestService.updateAmount(carId, request.amount())
        );
    }

    @PutMapping("/{carId}/mode")
    public ApiResponse<ChargingRequestResponse> updateMode(
        @PathVariable String carId,
        @Valid @RequestBody UpdateModeRequest request
    ) {
        return ApiResponse.ok(chargingRequestService.updateMode(carId, request.mode()));
    }

    @DeleteMapping("/{carId}")
    public ApiResponse<Void> cancel(@PathVariable String carId) {
        chargingRequestService.cancel(carId);
        return ApiResponse.ok();
    }

    @GetMapping("/{carId}/state")
    public ApiResponse<ChargingRequestResponse> getState(@PathVariable String carId) {
        return ApiResponse.ok(chargingRequestService.getState(carId));
    }

    @PostMapping("/{carId}/start")
    public ApiResponse<ChargingRequestResponse> startCharging(
        @PathVariable String carId,
        @Valid @RequestBody StartChargingRequest request
    ) {
        return ApiResponse.ok(billingService.startCharging(carId, request.pileId()));
    }

    @PostMapping("/{carId}/end")
    public ApiResponse<Bill> endCharging(@PathVariable String carId) {
        return ApiResponse.ok(billingService.endCharging(carId));
    }
}
