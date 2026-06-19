package com.resto.saas.payment.provider;

import com.resto.saas.payment.dto.PaymentResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class GooglePayPaymentProvider implements PaymentProvider {

    @Override
    public String getCode() {
        return "googlepay";
    }

    @SneakyThrows
    @Override
    public PaymentResult charge(String idempotencyKey, BigDecimal amount) {

//        throw new RuntimeException("Simulated failure for GooglePay"); // TODO проверка Circuit Breaker, потом удалить
        try {
            Thread.sleep(350);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("GooglePay charge: key={}, amount={}", idempotencyKey, amount.toString());
        return PaymentResult.success(UUID.randomUUID().toString(), getCode());
    }

    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        return new PaymentResult(true,
                UUID.randomUUID().toString(),
                getCode(),
                "");
    }
}
