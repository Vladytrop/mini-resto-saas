package com.resto.saas.shared.cache;


import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * Двухуровневый кеш L1 Caffeine локальный, L2 Redis общий
 *
 */
public class TwoLevelCache implements Cache {
    private final String name;
    private final Cache l1Cache; // Caffeine быстрый локальный
    private final Cache l2Cache; // Redis медленнее но общий для всех инстансов

    public TwoLevelCache(String name, Cache l1Cache, Cache l2Cache) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        // Пробуем получить из L1 Caffeine
        ValueWrapper value = l1Cache.get(key);
        if (value != null) {
            return value;
        }
        // Если не нашли в L1, пытаемся получить из L2
        value = l2Cache.get(key);
        if (value != null) {
            // Если нашли в L2, кэшируем(прогреваем) в L1
            l1Cache.put(key, value.get());
        }
        // Если нигде не нашли, возвращаем null и @Cacheable пойдет в БД
        return value;
    }

    @Override
    @Nullable
    public <T> T get(Object key, @Nullable Class<T> type) {
        T value = l1Cache.get(key, type);
        if (value != null) {
            return value;
        }
        value = l2Cache.get(key, type);
        if (value != null) {
            l1Cache.put(key, value);
        }
        return value;
    }

    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        // l1 через get(object) возвращаем Valuewrapper
        ValueWrapper wrapper = l1Cache.get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        // пробуем l2
        wrapper = l2Cache.get(key);
        if (wrapper != null) {
            l1Cache.put(key, wrapper.get());
            return (T) wrapper.get();
        }
        // если нет нигде то вызываем valueLoader и кладем оба
        T value;
        try {
            value = valueLoader.call();
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
        put(key, value);
        return value;
    }

    /**
     * Запись в оба кеша одновременно
     * Вызывается когда @Cacheable не нашел данные и выполнил метод
     */
    @Override
    public void put(Object key, @Nullable Object value) {
        l1Cache.put(key, value);
        l2Cache.put(key, value);
    }

    /**
     * удаление из обоих уровней
     * вызывается при @CacheEvict
     */
    @Override
    public void evict(Object key) {
        l1Cache.evict(key);
        l2Cache.evict(key);
    }

    /**
     * полная очистка оба уровня
     */
    @Override
    public void clear() {
        l1Cache.clear();
        l2Cache.clear();
    }
}
