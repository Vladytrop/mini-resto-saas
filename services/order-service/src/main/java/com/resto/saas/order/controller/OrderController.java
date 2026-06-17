package com.resto.saas.order.controller;

import com.resto.saas.order.dto.CreateOrderRequest;
import com.resto.saas.order.dto.OrderResponse;
import com.resto.saas.order.entity.Order;
import com.resto.saas.order.service.OrderService;
import com.resto.saas.shared.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @CircuitBreaker(name = "getAllOrders", fallbackMethod = "getAllOrdersFallback")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders()));
    }

    @CircuitBreaker(name = "getOrder", fallbackMethod = "getOrderFallback")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrder(id)));
    }

    @CircuitBreaker(name = "createOrder", fallbackMethod = "createOrderFallback")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(orderService.createOrder(request)));
    }

    @CircuitBreaker(name = "updateStatus", fallbackMethod = "updateStatusFallback")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.updateStatus(id, status)));
    }

//    Fallback методы для CircuitBreaker + Exception

    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrdersFallback(Exception ex) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Order service temporarily unavailable. Fallback activated"));
    }

    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrderFallback(Exception ex) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Order service temporarily unavailable. Fallback activated"));
    }

    public ResponseEntity<ApiResponse<List<OrderResponse>>> createOrderFallback(Exception ex) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Unable to create order. Service temporarily unavailable"));
    }

    public ResponseEntity<ApiResponse<List<OrderResponse>>> updateStatusFallback(Exception ex) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Unable to update status. Service temporarily unavailable"));
    }
}
