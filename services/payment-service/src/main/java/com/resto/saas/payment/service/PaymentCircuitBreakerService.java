package com.resto.saas.payment.service;

import com.resto.saas.payment.dto.PaymentResult;
import com.resto.saas.payment.provider.PaymentProvider;
import com.resto.saas.payment.provider.StripePaymentProvider;
import com.resto.saas.payment.provider.YookassaPaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCircuitBreakerService {

    private final StripePaymentProvider stripeProvider;
    private final YookassaPaymentProvider yookassaProvider;

    @CircuitBreaker(name = "stripe", fallbackMethod = "stripeFallback")
    public PaymentResult chargeStripe(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return stripeProvider.charge(idempotencyKey, amount);
    }

    public PaymentResult stripeFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Stripe {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Stripe temporarily unavailable:" + provider.getCode());
    }

    @CircuitBreaker(name = "yookassa", fallbackMethod = "yookassaFallback")
    public PaymentResult chargeYookassa(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return yookassaProvider.charge(idempotencyKey, amount);
    }

    public PaymentResult yookassaFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Yookassa {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Yookassa temporarily unavailable:" + provider.getCode());
    }
}
