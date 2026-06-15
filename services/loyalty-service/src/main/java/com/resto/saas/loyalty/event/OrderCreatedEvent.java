package com.resto.saas.loyalty.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String tenantId,
        String customerName,
        BigDecimal totalAmount,
        Instant createdAt
) {}
