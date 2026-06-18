package com.resto.saas.loyalty.controller;

import com.resto.saas.loyalty.dto.LoyaltyAccountDto;
import com.resto.saas.loyalty.service.LoyaltyCircuitBreakerService;
import com.resto.saas.loyalty.service.LoyaltyService;
import com.resto.saas.shared.dto.ApiResponse;
import com.resto.saas.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyCircuitBreakerService loyaltyCircuitBreakerService;

    @GetMapping("/{customerName}")
    public ApiResponse<LoyaltyAccountDto> getBalance(
            @PathVariable String customerName,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        TenantContext.setTenant(tenantId);
        try {
            log.info("Запрос баллов для customer={}, tenant={}", customerName, tenantId);
            return ApiResponse.ok(loyaltyCircuitBreakerService.getBalance(customerName));
        } finally {
            TenantContext.clear();
        }
    }
}