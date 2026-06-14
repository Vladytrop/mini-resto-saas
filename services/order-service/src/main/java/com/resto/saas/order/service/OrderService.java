package com.resto.saas.order.service;

import com.resto.saas.order.dto.CreateOrderRequest;
import com.resto.saas.order.dto.OrderResponse;
import com.resto.saas.order.entity.Order;
import com.resto.saas.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(OrderResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
            .customerName(request.getCustomerName())
            .totalAmount(request.getTotalAmount())
            .build();

        Order saved = orderRepository.save(order);
        log.info("Created order: {} for customer: {}", saved.getId(), saved.getCustomerName());

        return OrderResponse.fromEntity(saved);
    }

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
