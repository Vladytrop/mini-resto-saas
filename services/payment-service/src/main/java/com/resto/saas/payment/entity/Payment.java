package com.resto.saas.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false)
    UUID orderId;
    @Column(name = "tenant_id", nullable = false)
    String tenantId;
    @Column(name = "provider_code", nullable = false)
    String providerCode;
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    BigDecimal amount;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    PaymentStatus status;
    @Column(name = "transaction_id")
    String transactionId;
    @Column(name = "idempotency_key")
    String idempotencyKey;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
