package org.example.backend.service;

import java.util.Locale;

import org.example.backend.common.BusinessException;
import org.example.backend.domain.UserAccount;
import org.example.backend.dto.account.AccountResponse;
import org.example.backend.dto.account.CreateAccountRequest;
import org.example.backend.dto.account.LoginRequest;
import org.example.backend.dto.account.UpdateAccountRequest;
import org.example.backend.repository.ChargingRequestRepository;
import org.example.backend.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AccountService {

    private final UserAccountRepository userAccountRepository;
    private final ChargingRequestRepository chargingRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
        UserAccountRepository userAccountRepository,
        ChargingRequestRepository chargingRequestRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.chargingRequestRepository = chargingRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        String carId = normalizeCarId(request.carId());
        if (userAccountRepository.existsByCarId(carId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该车号已注册");
        }

        userAccountRepository.insert(
            carId,
            request.userName().trim(),
            passwordEncoder.encode(request.password()),
            request.carCapacity()
        );
        return AccountResponse.from(requireAccount(carId));
    }

    public AccountResponse login(LoginRequest request) {
        UserAccount account = requireAccount(normalizeCarId(request.carId()));
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "车号或密码错误");
        }
        return AccountResponse.from(account);
    }

    public AccountResponse get(String carId) {
        return AccountResponse.from(requireAccount(normalizeCarId(carId)));
    }

    @Transactional
    public AccountResponse update(String carId, UpdateAccountRequest request) {
        String normalizedCarId = normalizeCarId(carId);
        UserAccount current = requireAccount(normalizedCarId);

        String userName = StringUtils.hasText(request.userName())
            ? request.userName().trim()
            : current.userName();
        String passwordHash = StringUtils.hasText(request.password())
            ? passwordEncoder.encode(request.password())
            : current.passwordHash();
        double carCapacity = request.carCapacity() != null
            ? request.carCapacity()
            : current.carCapacity();

        userAccountRepository.update(
            normalizedCarId,
            userName,
            passwordHash,
            carCapacity
        );
        return AccountResponse.from(requireAccount(normalizedCarId));
    }

    @Transactional
    public void delete(String carId) {
        String normalizedCarId = normalizeCarId(carId);
        requireAccount(normalizedCarId);
        if (chargingRequestRepository.existsActiveByCarId(normalizedCarId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "存在未完成的充电申请，不能删除账号");
        }
        userAccountRepository.deleteByCarId(normalizedCarId);
    }

    UserAccount requireAccount(String carId) {
        return userAccountRepository.findByCarId(carId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "车辆账号不存在"));
    }

    static String normalizeCarId(String carId) {
        return carId.trim().toUpperCase(Locale.ROOT);
    }
}
