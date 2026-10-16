package com.resto.saas.loyalty.dto;

import com.resto.saas.loyalty.entity.LoyaltyAccount;

import java.util.UUID;

public record LoyaltyAccountDto(
        UUID id,
        String tenantId,
        String customerName,
        int points
) {
    public static LoyaltyAccountDto fromEntity(LoyaltyAccount account) {
        return new LoyaltyAccountDto(
                account.getId(),
                account.getTenantId(),
                account.getCustomerName(),
                account.getPoints()
        );
    }
}
