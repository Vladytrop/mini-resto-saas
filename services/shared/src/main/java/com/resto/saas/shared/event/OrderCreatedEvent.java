package com.resto.saas.shared.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String tenantId,
        String customerName,
        BigDecimal totalAmount,
        String status
) {
}