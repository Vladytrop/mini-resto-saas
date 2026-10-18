package com.resto.saas.rag.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resto.saas.rag.api.RagAskRequest;
import com.resto.saas.rag.api.RagAskResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Кэш частых вопросов в Redis (Этап 2.5).
 *
 * Архитектурное решение: НЕ используем аннотацию @Cacheable, а пишем
 * read-through / write-through логику вручную через StringRedisTemplate. Почему:
 *
 * 1) Контроль что кэшируем. Ответы с escalated=true — «временная правда»:
 *    завтра оператор добавит статью, и эскалация станет неверным ответом на 12 часов.
 *    Такие ответы кладём с укороченным TTL (1 час), нормальные — с полным (12 часов).
 *
 * 2) Контроль ключа. Ключ = SHA-256 от канонизированного "aggregator|question":
 *    канонизация (trim + lower + схлопание пробелов) склеивает варианты одного
 *    вопроса; aggregator обязателен (ответ для GLOVO неверен для WOLT);
 *    хэш вместо сырого текста — безопасное имя Redis-ключа любой длины.
 *
 * 3) Устойчивость к отказам Redis. Если Redis недоступен, get() вернёт empty,
 *    put() проигнорирует ошибку (warn). Кэш — оптимизация, а не зависимость —
 *    та же философия, что у L1 Caffeine в loyalty-service.
 *
 * 4) Сериализация — JSON через ObjectMapper, а не Java-сериализация: значение
 *    можно прочитать глазами через redis-cli GET (отладка!).
 */
@Slf4j
@Component
public class RagAnswerCache {

    private static final String KEY_PREFIX = "rag:answer:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Duration ttl;
    private final Duration escalatedTtl;

    public RagAnswerCache(StringRedisTemplate redis,
                          ObjectMapper objectMapper,
                          MeterRegistry meterRegistry,
                          @Value("${rag.cache.ttl-hours:12}") long ttlHours,
                          @Value("${rag.cache.escalated-ttl-hours:1}") long escalatedTtlHours) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.ttl = Duration.ofHours(ttlHours);
        this.escalatedTtl = Duration.ofHours(escalatedTtlHours);
    }

    /** Ключ: rag:answer:<sha256(aggregator|normalized question)>. */
    public String buildKey(RagAskRequest request) {
        String canonical = "%s|%s".formatted(
                request.aggregator() == null ? "" : request.aggregator().trim().toUpperCase(),
                normalize(request.question()));
        return KEY_PREFIX + sha256Hex(canonical);
    }

    /** Read-through: попал — возвращаем DTO, промахнулся/ошибка — Optional.empty(). */
    public java.util.Optional<RagAskResponse> get(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                meterRegistry.counter("rag.cache", "result", "miss").increment();
                return java.util.Optional.empty();
            }
            RagAnswerCacheEntry entry = objectMapper.readValue(json, RagAnswerCacheEntry.class);
            meterRegistry.counter("rag.cache", "result", "hit").increment();
            return java.util.Optional.of(entry.toDto(0));
        } catch (Exception e) {
            // Не роняем запрос из-за кэша: битый JSON / Redis недоступен → как промах
            log.warn("RAG cache get failed, treating as miss: {}", e.getMessage());
            meterRegistry.counter("rag.cache", "result", "error").increment();
            return java.util.Optional.empty();
        }
    }

    /** Write-through: TTL зависит от типа ответа (эскалации живут меньше). */
    public void put(String key, RagAskResponse response) {
        Duration effectiveTtl = response.escalated() ? escalatedTtl : ttl;
        try {
            String json = objectMapper.writeValueAsString(new RagAnswerCacheEntry(response));
            redis.opsForValue().set(key, json, effectiveTtl);
            meterRegistry.counter("rag.cache.put",
                    "type", response.escalated() ? "escalated" : "normal").increment();
        } catch (JsonProcessingException e) {
            log.warn("RAG cache serialize failed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("RAG cache put failed (Redis down?): {}", e.getMessage());
        }
    }

    /** Инвалидация после реинджеста базы знаний: старые ответы больше не валидны. */
    public void invalidateAll() {
        // KEYS запрещён в проде (блокирует Redis на O(N)) — сканируем инкрементально.
        var options = org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(KEY_PREFIX + "*").count(500).build();
        long deleted = redis.execute((org.springframework.data.redis.core.RedisCallback<Long>) connection -> {
            long n = 0;
            try (var cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    connection.keyCommands().del(cursor.next());
                    n++;
                }
            }
            return n;
        });
        log.info("RAG cache invalidated: {} keys removed", deleted);
    }

    // ---------------- helpers ----------------

    /** trim → lower → все последовательности whitespace в один пробел. */
    private static String normalize(String text) {
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is mandatory in every JDK", e);
        }
    }
}