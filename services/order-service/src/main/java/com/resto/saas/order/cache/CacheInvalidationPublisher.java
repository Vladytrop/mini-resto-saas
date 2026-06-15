package com.resto.saas.order.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resto.saas.shared.cache.CacheInvalidationEvent;
import com.resto.saas.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationPublisher {

    private static final String CHANNEL = "cache:invalidate";

    @Qualifier("pubSubRedisTemplate")
    private final StringRedisTemplate pubSubRedisTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void invalidate(String cacheKeyPattern) {
        String tenantId = TenantContext.isSet() ? TenantContext.getTenant() : "public";
        CacheInvalidationEvent event = new CacheInvalidationEvent(tenantId, cacheKeyPattern);

        String message = objectMapper.writeValueAsString(event);
        pubSubRedisTemplate.convertAndSend(CHANNEL, message);
        log.info(">>> Pub/Sub: published invalidation for tenant={}, pattern={}", tenantId, cacheKeyPattern);
    }
}