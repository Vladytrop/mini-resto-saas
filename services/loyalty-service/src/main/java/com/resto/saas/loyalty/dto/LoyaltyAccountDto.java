package com.resto.saas.loyalty.dto;

import java.util.UUID;

public record LoyaltyAccountDto(
        UUID id,
        String tenantId,
        String customerName,
        int points
) {
}
