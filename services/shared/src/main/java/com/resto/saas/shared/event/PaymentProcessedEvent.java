package com.resto.saas.shared.event; // или shared.event, но унифицируй

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private UUID paymentId;
    private UUID orderId;
    private String tenantId;
    private String providerCode;
    private BigDecimal amount;
    private String status;
}