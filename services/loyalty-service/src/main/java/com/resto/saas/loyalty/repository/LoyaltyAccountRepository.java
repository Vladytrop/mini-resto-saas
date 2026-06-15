package com.resto.saas.loyalty.repository;

import com.resto.saas.loyalty.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {
    Optional<LoyaltyAccount> findByTenantIdAndCustomerName(String tenantId, String customerName);
}
