package com.resto.saas.loyalty.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.resto.saas.loyalty.dto.LoyaltyAccountDto;
import com.resto.saas.loyalty.entity.LoyaltyAccount;
import com.resto.saas.loyalty.repository.LoyaltyAccountRepository;
import com.resto.saas.shared.event.OrderCreatedEvent;
import com.resto.saas.shared.event.PaymentProcessedEvent;
import com.resto.saas.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository repository;

    @Cacheable(value = "loyalty",
            key = "T(com.resto.saas.shared.tenant.TenantContext).getTenant() + ':' + #customerName")
    @Transactional(readOnly = true)
    public LoyaltyAccountDto getBalanceInternal(String customerName) {
//        throw new RuntimeException("Simulated DB failure"); // TODO: временная проверка, потом удалить
        String tenantId = TenantContext.getTenant();
        return (LoyaltyAccountDto) repository.findByTenantIdAndCustomerName(tenantId, customerName)
                .map(this::toDto)
                .orElse(new LoyaltyAccountDto(null, tenantId, customerName, 0));
    }

    @CacheEvict(value = "loyalty",
            key = "T(com.resto.saas.shared.tenant.TenantContext).getTenant() + ':' + #event.customerName")
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

    public void creditPoints(UUID orderId, String tenantId, BigDecimal amount) {

        log.info("Credited {} points for tenant={}", amount.multiply(BigDecimal.valueOf(0.1)), tenantId);
    }

    @KafkaListener(topics = "orders", groupId = "loyalty-service")
    public void handleOrderCreated(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            log.info("Получено событие создания заказа: {}", event);

            TenantContext.setTenant(event.tenantId());
            try {
                processOrderCreated(event);
            } finally {
                TenantContext.clear();
            }
        } catch (Exception e) {
            log.error("Failed to parse OrderCreatedEvent: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payments", groupId = "loyalty-service")
    public void handlePaymentProcessed(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            PaymentProcessedEvent event = objectMapper.readValue(message, PaymentProcessedEvent.class);

            if ("SUCCESS".equals(event.getStatus())) {
                log.info("Payment success for order={}, crediting loyalty points", event.getOrderId());
                creditPoints(event.getOrderId(), event.getTenantId(), event.getAmount());
            }
        } catch (Exception e) {
            log.error("Failed to parse PaymentProcessedEvent: {}", e.getMessage());
        }
    }

    private LoyaltyAccount createNewAccount(OrderCreatedEvent event) {
        return LoyaltyAccount.builder()
                .tenantId(event.tenantId())
                .customerName(event.customerName())
                .points(0)
                .build();
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
