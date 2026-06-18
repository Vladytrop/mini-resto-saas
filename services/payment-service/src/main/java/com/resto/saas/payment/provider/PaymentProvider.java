package com.resto.saas.payment.provider;

import com.resto.saas.payment.dto.PaymentResult;

import java.math.BigDecimal;

public interface PaymentProvider {

    String getCode();
    PaymentResult charge(String idempotencyKey, BigDecimal amount);
    PaymentResult refund(String transactionId, BigDecimal amount);

}
