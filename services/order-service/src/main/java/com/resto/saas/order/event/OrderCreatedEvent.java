package com.resto.saas.order.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String tenantId,
        String customerName,
        BigDecimal totalAmount,
        String status
) {}
