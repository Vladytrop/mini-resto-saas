package com.resto.saas.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {

    @NotNull
    private UUID orderId;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    private String providerCode;
    @NotBlank
    private String idempotencyKey;
}
