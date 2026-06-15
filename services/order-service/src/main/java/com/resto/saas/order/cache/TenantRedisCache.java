package com.resto.saas.order.cache;

import com.resto.saas.shared.tenant.TenantContext;
import com.resto.saas.shared.tenant.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TenantRedisCache {

    private final StringRedisTemplate redisTemplate;
    private final TenantFilter tenantFilter;

    public void set(String key, String value, Duration ttl) {

        String tenantKey = prefix(key);
        redisTemplate.opsForValue().set(tenantKey, value, ttl);

    }

    public String get(String key) {

        return redisTemplate.opsForValue().get(prefix(key));
    }

    public void delete(String pattern) {

        String tenantPattern = prefix(pattern);
        Set<String> keys = redisTemplate.keys(tenantPattern);
        if (keys != null && !keys.isEmpty()){
            redisTemplate.delete(keys);
        }
    }

    private String prefix(String key) {

        String tenant = TenantContext.isSet() ? TenantContext.getTenant() : "public";
        return tenant + ":" + key;
    }
}
