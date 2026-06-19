package com.resto.saas.payment.service;

import com.resto.saas.payment.dto.PaymentResult;
import com.resto.saas.payment.provider.*;
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
    private final ApplePayPaymentProvider applepayProvider;
    private final CryptoPaymentProvider cryptoProvider;
    private final GooglePayPaymentProvider googlepayProvider;
    private final PayPalPaymentProvider paypalProvider;
    private final SberPayPaymentProvider sberpayProvider;
    private final TinkoffPaymentProvider tinkoffProvider;

//    Stripe
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

//    Yookassa
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

//    Applepay
    @CircuitBreaker(name = "applepay", fallbackMethod = "applepayFallback")
    public PaymentResult chargeApplepay(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return applepayProvider.charge(idempotencyKey, amount);
    }

    public PaymentResult applepayFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Applepay {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Applepay temporarily unavailable:" + provider.getCode());
    }

//    Crypto
    @CircuitBreaker(name = "crypto", fallbackMethod = "cryptoFallback")
    public PaymentResult chargeCrypto(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return cryptoProvider.charge(idempotencyKey, amount);
    }

    public PaymentResult cryptoFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Crypto {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Crypto temporarily unavailable:" + provider.getCode());
    }

//    Googlepay
    @CircuitBreaker(name = "googlepay", fallbackMethod = "googlepayFallback")
    public PaymentResult chargeGooglepay(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return googlepayProvider.charge(idempotencyKey, amount);
    }

    public PaymentResult googlepayFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Googlepay {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Googlepay temporarily unavailable:" + provider.getCode());
    }

//    Paypal
    @CircuitBreaker(name = "paypal", fallbackMethod = "paypalFallback")
    public PaymentResult chargePaypal(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return paypalProvider.charge(idempotencyKey, amount);
    }

    public PaymentResult paypalFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Paypal {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Paypal temporarily unavailable:" + provider.getCode());
    }

    //    Sberpay
    @CircuitBreaker(name = "sberpay", fallbackMethod = "sberpayFallback")
    public PaymentResult chargeSberpay(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return sberpayProvider.charge(idempotencyKey, amount);
    }

    public PaymentResult sberpayFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Sberpay {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Sberpay temporarily unavailable:" + provider.getCode());
    }

//    Tinkoff
    @CircuitBreaker(name = "tinkoff", fallbackMethod = "tinkoffFallback")
    public PaymentResult chargeTinkoff(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount
    ) {

        return tinkoffProvider.charge(idempotencyKey, amount);
    }

    public PaymentResult tinkoffFallback(
            PaymentProvider provider,
            String idempotencyKey,
            BigDecimal amount,
            Throwable ex
    ) {
        log.warn("Tinkoff {} fallback: {}", provider.getCode(), ex.getMessage());
        return PaymentResult.failure("Tinkoff temporarily unavailable:" + provider.getCode());
    }
}
