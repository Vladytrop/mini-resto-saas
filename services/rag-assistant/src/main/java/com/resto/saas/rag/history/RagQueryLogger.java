package com.resto.saas.rag.history;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Асинхронная запись RAG-запросов в rag_query_logs.
 *
 * Почему @Async (fire-and-forget), а не синхронный INSERT:
 * логирование НЕ должно добавлять latency пользователю, и ошибка записи
 * не должна ломать пользовательский запрос — поэтому всё в try/catch.
 *
 * Требуемые условия работы @Async (иначе аннотация молча игнорируется!):
 *   1) @EnableAsync в конфигурации (RagAssistantApplication);
 *   2) метод public и вызывается ИЗ ДРУГОГО бина (не this.method() —
 *      та же self-invocation ловушка, что мы чинили для @CircuitBreaker);
 *   3) возвращает void/CompletableFuture.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryLogger {

    private final RagQueryLogRepository repository;
    private final MeterRegistry meterRegistry;

    @Async("ragLogExecutor")
    public void log(String tenantId, String question, String aggregator,
                    boolean fromCache, boolean escalated, Double confidence,
                    long latencyMs, int chunksFound) {
        try {
            repository.save(RagQueryLog.builder()
                    .tenantId(tenantId)
                    // 2048 — лимит колонки; обрезаем, а не падаем с DataException
                    .question(truncate(question, 2048))
                    .aggregator(aggregator)
                    .fromCache(fromCache)
                    .escalated(escalated)
                    .confidence(confidence)
                    .latencyMs(latencyMs)
                    .chunksFound(chunksFound)
                    .build());

            // Метрика p95 latency — именно по «холодным» запросам она интересна:
            // кэш-хиты дают почти константные ~1 мс и размывают распределение.
            meterRegistry.timer("rag.query.latency",
                            "cache", fromCache ? "hit" : "miss",
                            "escalated", String.valueOf(escalated))
                    .record(latencyMs, TimeUnit.MILLISECONDS);

            meterRegistry.counter("rag.query.total",
                    "cache", fromCache ? "hit" : "miss").increment();
        } catch (Exception e) {
            // Лог упал — бизнес не виноват: только warn + счётчик деградации
            log.warn("Failed to persist RAG query log: {}", e.getMessage());
            meterRegistry.counter("rag.query.log.errors").increment();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}