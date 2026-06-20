package com.resto.saas.loyalty.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resto.saas.shared.cache.CacheInvalidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate; // @Primary — master/replica, для удаления ключей

    @Override
    @SneakyThrows
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        CacheInvalidationEvent event = objectMapper.readValue(body, CacheInvalidationEvent.class);

        String tenantKey = event.getTenantId() + ":" + event.getCacheKeyPattern();
        Set<String> keys = stringRedisTemplate.keys(tenantKey);

        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info(">>> Pub/Sub: invalidated {} keys for tenant={}, pattern={}",
                    keys.size(), event.getTenantId(), event.getCacheKeyPattern());
        } else {
            log.debug(">>> Pub/Sub: no keys found for tenant={}, pattern={}",
                    event.getTenantId(), event.getCacheKeyPattern());
        }
    }
}