package org.example.backend.controller;

import org.example.backend.common.ApiResponse;
import org.example.backend.dto.account.AccountResponse;
import org.example.backend.dto.account.CreateAccountRequest;
import org.example.backend.dto.account.LoginRequest;
import org.example.backend.dto.account.UpdateAccountRequest;
import org.example.backend.service.AccountService;
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
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> create(
        @Valid @RequestBody CreateAccountRequest request
    ) {
        return ApiResponse.ok(accountService.create(request));
    }

    @PostMapping("/login")
    public ApiResponse<AccountResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(accountService.login(request));
    }

    @GetMapping("/{carId}")
    public ApiResponse<AccountResponse> get(@PathVariable String carId) {
        return ApiResponse.ok(accountService.get(carId));
    }

    @PutMapping("/{carId}")
    public ApiResponse<AccountResponse> update(
        @PathVariable String carId,
        @Valid @RequestBody UpdateAccountRequest request
    ) {
        return ApiResponse.ok(accountService.update(carId, request));
    }

    @DeleteMapping("/{carId}")
    public ApiResponse<Void> delete(@PathVariable String carId) {
        accountService.delete(carId);
        return ApiResponse.ok();
    }
}
