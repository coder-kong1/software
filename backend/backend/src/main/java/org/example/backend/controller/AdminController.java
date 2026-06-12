package org.example.backend.controller;

import org.example.backend.common.ApiResponse;
import org.example.backend.dto.admin.StationSnapshot;
import org.example.backend.service.PileService;
import org.example.backend.service.SchedulingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SchedulingService schedulingService;
    private final PileService pileService;

    public AdminController(
        SchedulingService schedulingService,
        PileService pileService
    ) {
        this.schedulingService = schedulingService;
        this.pileService = pileService;
    }

    @GetMapping("/snapshot")
    public ApiResponse<StationSnapshot> snapshot() {
        return ApiResponse.ok(schedulingService.snapshot());
    }

    @PostMapping("/piles/{pileId}/power-on")
    public ApiResponse<Void> powerOn(@PathVariable String pileId) {
        pileService.powerOn(pileId);
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
}
