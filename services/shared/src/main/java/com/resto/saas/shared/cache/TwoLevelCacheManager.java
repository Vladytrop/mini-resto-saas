package com.resto.saas.shared.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Менеджер двухуровенвых кэшей
 * для каждого имени кеша создает TwoLevelCache
 * который внутри управляет л1 кеш кафеин и л2 кедис
 */
public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager l1CacheManager;
    private final CacheManager l2CacheManager;
    private final ConcurrentMap<String, TwoLevelCache> cacheMap = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(CacheManager l1CacheManager, CacheManager l2CacheManager) {
        this.l1CacheManager = l1CacheManager;
        this.l2CacheManager = l2CacheManager;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, cacheName -> {
            Cache l1 = l1CacheManager.getCache(cacheName);
            Cache l2 = l2CacheManager.getCache(cacheName);
            if (l1 == null || l2 == null) {
                throw new IllegalStateException(
                        "Не удалось создать двухуровневый кеш '" + cacheName + "': " +
                                "L1=" + (l1 != null) + ", L2=" + (l2 != null)
                );
            }
            return new TwoLevelCache(cacheName, l1, l2);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheMap.keySet());
    }
}
