package com.resto.saas.loyalty.service;

import com.resto.saas.loyalty.dto.LoyaltyAccountDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyCircuitBreakerService {

    private final LoyaltyService loyaltyService;

    @CircuitBreaker(name = "getBalance", fallbackMethod = "getBalanceFallback")
    public LoyaltyAccountDto getBalance(String customerName) {

        return loyaltyService.getBalanceInternal(customerName);
    }

    public LoyaltyAccountDto getBalanceFallback(String customerName, Throwable ex) {
        log.warn("Loyalty getBalance fallback: {}", ex.getMessage());
        return new LoyaltyAccountDto(null, null, customerName, 0);
    }
}
