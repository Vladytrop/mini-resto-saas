package com.resto.saas.payment.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResult {

    private boolean success;
    private String transactionId;
    private String providerCode;
    private String errorMessage;

    public static PaymentResult success(String transactionId, String providerCode) {
        return new PaymentResult(true, transactionId, providerCode, null);
    }

    public static PaymentResult failure(String errorMessage) {
        return new PaymentResult(false, null, null, errorMessage);
    }
}
