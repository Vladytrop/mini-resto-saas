package com.resto.saas.loyalty.consumer;

import com.resto.saas.loyalty.event.OrderCreatedEvent;
import com.resto.saas.loyalty.service.LoyaltyService;
import com.resto.saas.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final LoyaltyService loyaltyService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "true",
            topicSuffixingStrategy =
                    org.springframework.kafka.retrytopic.TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            kafkaTemplate = "retryableTopicKafkaTemplate"
    )
    @KafkaListener(topics = "orders", groupId = "loyalty-service")
    public void handleOrderCreated(OrderCreatedEvent event) {

        log.info("Получено событие: orderId={}, tenant={}, amount={}",
                event.orderId(),
                event.tenantId(),
                event.totalAmount()
        );

        if (event.orderId() == null || event.tenantId() == null || event.totalAmount() == null) {
            log.info("Невалидное событие, пропускаем: {}", event);
            return;
        }

        TenantContext.setTenant(event.tenantId());
        try {
            log.info("Обработка события: orderId={}, tenant={}, totalAmount{}",
                    event.orderId(),
                    event.tenantId(),
                    event.totalAmount()
            );
            loyaltyService.processOrderCreated(event);
        } finally {
            TenantContext.clear();
        }
    }

    @DltHandler
    public void handleDlt(
            OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {

        log.error("!!! DLQ(Dead Letter Queue, «очередь мертвых сообщений): orderId={}, tenant={}, amount={}." +
                        " Требуется ручная обработка!",
                event.orderId(), event.tenantId(), event.totalAmount());
    }
}
