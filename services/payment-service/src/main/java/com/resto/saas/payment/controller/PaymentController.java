package com.resto.saas.payment.controller;

import com.resto.saas.payment.dto.CreatePaymentRequest;
import com.resto.saas.payment.entity.Payment;
import com.resto.saas.payment.service.PaymentService;
import com.resto.saas.shared.dto.ApiResponse;
import com.resto.saas.shared.tenant.TenantContext;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Payment>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {

        TenantContext.setTenant(tenantId);
        try {
            Payment payment = paymentService.processPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(payment));
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<List<Payment>>> getPayments(
            @PathVariable UUID orderId,
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {

        TenantContext.setTenant(tenantId);
        try {
            return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentsByOrder(orderId)));
        } finally {
            TenantContext.clear();
        }
    }
}
