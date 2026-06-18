package com.resto.saas.payment.service;

import com.resto.saas.payment.dto.PaymentResult;
import com.resto.saas.payment.provider.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCircuitBreakerService {

    @CircuitBreaker(name = "stripe", fallbackMethod = "chargeFallback")
    public PaymentResult charge(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return provider.charge(idempotencyKey, amount);
    }

    public PaymentResult chargeFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Provider {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Provider temporarily unavailable:" + provider.getCode());
    }
}
