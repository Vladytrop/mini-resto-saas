package com.resto.saas.loyalty.service;

import com.resto.saas.loyalty.entity.LoyaltyAccount;
import com.resto.saas.loyalty.event.OrderCreatedEvent;
import com.resto.saas.loyalty.repository.LoyaltyAccountRepository;
import com.resto.saas.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository repository;

    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {

        log.info("Обработка события: orderId={}, tenant={}, totalAmount{}",
                event.orderId(),
                event.tenantId(),
                event.totalAmount()
        );

//        проверка DLQ
//        if (event.totalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
//            throw new RuntimeException("Имитация ошибки для теста DLQ");
//        }

        if (!TenantContext.isSet()){
            throw new IllegalArgumentException("TenantContext не установлен перед @Transactional");
        }

        int pointToAdd = event.totalAmount()
                .divide(BigDecimal.TEN, RoundingMode.DOWN)
                .intValue();

        LoyaltyAccount account = repository
                .findByTenantIdAndCustomerName(event.tenantId(), event.customerName())
                .orElseGet(() -> createNewAccount(event));

        account.setPoints(account.getPoints() + pointToAdd);
        repository.save(account);

        log.info("Начислено {} баллов.Итого: {} для {}(tenant: {})",
                pointToAdd, account.getPoints(), event.customerName(), event.tenantId());
    }

    private LoyaltyAccount createNewAccount(OrderCreatedEvent event) {
        return LoyaltyAccount.builder()
                .tenantId(event.tenantId())
                .customerName(event.customerName())
                .points(0)
                .build();
    }
}
