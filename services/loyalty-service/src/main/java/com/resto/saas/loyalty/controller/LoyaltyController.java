package com.resto.saas.loyalty.controller;

import com.resto.saas.loyalty.dto.LoyaltyAccountDto;
import com.resto.saas.loyalty.entity.LoyaltyAccount;
import com.resto.saas.loyalty.repository.LoyaltyAccountRepository;
import com.resto.saas.loyalty.service.LoyaltyService;
import com.resto.saas.shared.dto.ApiResponse;
import com.resto.saas.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyAccountRepository repository;
    private final LoyaltyService loyaltyService;

    @GetMapping("/{customerName}")
    public ApiResponse<List<LoyaltyAccountDto>> getBalance(
            @PathVariable String customerName,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        return ApiResponse.success(loyaltyService.getBalance(customerName));
    }

    @GetMapping
    public ResponseEntity<List<Object>> getAllAccounts(
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        try {
            log.info("Запрос баллов для аккаунта {}", tenantId);

            List<Object> accounts = repository.findAll()
                    .stream()
                    .map(this::toDto)
                    .toList();

            return ResponseEntity.ok(accounts);
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/{customerName}")
    public ResponseEntity<Object> getAccountByCustomer(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String customerName
    ) {
        TenantContext.setTenant(tenantId);
        try {
            log.info("Запрос баллов для аккаунта {}(tenant: {})", customerName, tenantId);

            return repository.findByTenantIdAndCustomerName(tenantId, customerName)
                    .map(this::toDto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } finally {
            TenantContext.clear();
        }
    }

    private Object toDto(LoyaltyAccount account) {

        return new LoyaltyAccountDto(
                account.getId(),
                account.getTenantId(),
                account.getCustomerName(),
                account.getPoints()
        );
    }

}
