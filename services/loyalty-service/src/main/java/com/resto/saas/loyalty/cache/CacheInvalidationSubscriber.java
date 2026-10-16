package com.resto.saas.loyalty.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resto.saas.shared.cache.CacheInvalidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate; // @Primary — master/replica, для удаления ключей
    private final CacheManager cacheManager; // удаляет из L1

    @Override
    @SneakyThrows
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        CacheInvalidationEvent event = objectMapper.readValue(body, CacheInvalidationEvent.class);
        String tenantKey = event.getTenantId() + ":" + event.getCacheKeyPattern();

        // Удаляем из L1 Caffeine через CacheManager
        Cache loyaltyCache = cacheManager.getCache("loyalty");
        if (loyaltyCache != null) {
            loyaltyCache.evict(tenantKey);
            log.debug("L1 (Caffeine): ключ '{}' удален", tenantKey);
        }

        // Удаляем из L2 Redis через SCAN вместо KEYS
        List<String> keysToDelete = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(tenantKey)
                .count(100) // батч 100 ключей не блокирует Редис
                .build();

        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
            }
        }

        if (!keysToDelete.isEmpty()) {
            stringRedisTemplate.delete(keysToDelete);
            log.info("pub/sub: invalidated {} keys for tenant={}, pattern={}",
                    keysToDelete.size(), event.getTenantId(), event.getCacheKeyPattern());
        } else {
            log.debug("pub/sub: no keys found for tenant={}, pattern={}",
                    event.getTenantId(), event.getCacheKeyPattern());
        }

//        String tenantKey = event.getTenantId() + ":" + event.getCacheKeyPattern();
//        Set<String> keys = stringRedisTemplate.keys(tenantKey);
//
//        if (keys != null && !keys.isEmpty()) {
//            stringRedisTemplate.delete(keys);
//            log.info(">>> Pub/Sub: invalidated {} keys for tenant={}, pattern={}",
//                    keys.size(), event.getTenantId(), event.getCacheKeyPattern());
//        } else {
//            log.debug(">>> Pub/Sub: no keys found for tenant={}, pattern={}",
//                    event.getTenantId(), event.getCacheKeyPattern());
//        }
    }
}