package com.resto.saas.payment.repository;

import com.resto.saas.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderIdAndTenantId(UUID orderId, String tenantId);
}
