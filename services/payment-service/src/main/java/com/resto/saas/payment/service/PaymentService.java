package com.resto.saas.payment.service;

import com.resto.saas.payment.dto.CreatePaymentRequest;
import com.resto.saas.payment.dto.PaymentResult;
import com.resto.saas.shared.event.PaymentProcessedEvent;
import com.resto.saas.payment.entity.Payment;
import com.resto.saas.payment.entity.PaymentStatus;
import com.resto.saas.payment.provider.PaymentProvider;
import com.resto.saas.payment.repository.PaymentRepository;
import com.resto.saas.shared.tenant.TenantContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final List<PaymentProvider> providers;
    private Map<String, PaymentProvider> providerMap;
    private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;
    private final PaymentCircuitBreakerService paymentCircuitBreakerService;

    @PostConstruct
    public void init() {
        providerMap = providers.stream()
                .collect(Collectors.toMap(PaymentProvider::getCode,
                        Function.identity()));
    }

    @Transactional
    public Payment processPayment(CreatePaymentRequest request) {

        var tenantId = TenantContext.getTenant();

        var provider = providerMap.get(request.getProviderCode());

        if (provider == null) {
            throw new IllegalArgumentException("Invalid provider code");
        }

        var payment = Payment.builder()
                .orderId(request.getOrderId())
                .tenantId(tenantId)
                .providerCode(request.getProviderCode())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .transactionId(null)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        PaymentResult result;
        switch (provider.getCode()) {
            case "stripe"   -> result = paymentCircuitBreakerService
                    .chargeStripe(provider, request.getIdempotencyKey(), request.getAmount());

            case "yookassa" -> result = paymentCircuitBreakerService
                    .chargeYookassa(provider, request.getIdempotencyKey(), request.getAmount());

            default ->
                    throw new IllegalArgumentException("Unknown provider: " + provider.getCode());
        }

        if (result.isSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(result.getTransactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);

        var event = new PaymentProcessedEvent(
                payment.getId(),
                payment.getOrderId(),
                tenantId,
                provider.getCode(),
                payment.getAmount(),
                payment.getStatus().name()
        );

        kafkaTemplate.send("payments",payment.getId().toString() , event);

        return payment;
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrder(UUID orderId) {

        return paymentRepository.findByOrderIdAndTenantId(orderId, TenantContext.getTenant());
    }
}
