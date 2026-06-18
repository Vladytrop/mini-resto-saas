package com.resto.saas.order.service;

import com.resto.saas.order.dto.CreateOrderRequest;
import com.resto.saas.order.dto.OrderResponse;
import com.resto.saas.order.entity.Order;
import com.resto.saas.order.repository.OrderRepository;
import com.resto.saas.order.cache.CacheInvalidationPublisher;
import com.resto.saas.shared.event.OrderCreatedEvent;
import com.resto.saas.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    @Cacheable(value = "orders",
            key = "T(com.resto.saas.shared.tenant.TenantContext).getTenant() + ':list'")
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {

//        throw new RuntimeException("Simulated DB failure"); // TODO: временная проверка, потом удалить
        return orderRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(OrderResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "orders",
            key = "T(com.resto.saas.shared.tenant.TenantContext).getTenant() + ':' + #id")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {

        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        return OrderResponse.fromEntity(order);
    }

    @CacheEvict(value = "orders",
            key = "T(com.resto.saas.shared.tenant.TenantContext).getTenant() + ':list'")
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
            .customerName(request.getCustomerName())
            .totalAmount(request.getTotalAmount())
            .build();

        Order saved = orderRepository.save(order);
        log.info("Created order: {} for customer: {}", saved.getId(), saved.getCustomerName());

        cacheInvalidationPublisher.invalidate("orders:list");

        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                TenantContext.getTenant(),
                saved.getCustomerName(),
                saved.getTotalAmount(),
                saved.getStatus().name()
        );

        kafkaTemplate.send("orders", saved.getId().toString(), event);
        log.info("Sent in order event {}", event);

        return OrderResponse.fromEntity(saved);
    }

    @Caching(evict = {
            @CacheEvict(value = "orders",
                    key = "T(com.resto.saas.shared.tenant.TenantContext).getTenant() + ':list'"),
            @CacheEvict(value = "orders",
                    key = "T(com.resto.saas.shared.tenant.TenantContext).getTenant() + ':' + #id")
    })
    @Transactional
    public OrderResponse updateStatus(UUID id, Order.OrderStatus newStatus) {

        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);

        Order updated = orderRepository.save(order);
        log.info("Updated order {} status: {} -> {}", id, order.getStatus(), newStatus);

        return OrderResponse.fromEntity(updated);
    }

    private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus next) {

        boolean valid = switch (current) {
            case CREATED -> next == Order.OrderStatus.CONFIRMED || next == Order.OrderStatus.CANCELLED;
            case CONFIRMED -> next == Order.OrderStatus.READY || next == Order.OrderStatus.CANCELLED;
            case READY -> next == Order.OrderStatus.COMPLETED || next == Order.OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                String.format("Invalid status transition: %s -> %s", current, next)
            );
        }
    }
}
