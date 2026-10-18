package com.resto.saas.rag.orchestration;

import com.resto.saas.rag.api.RagAskRequest;
import com.resto.saas.rag.api.RagAskResponse;
import com.resto.saas.rag.cache.RagAnswerCache;
import com.resto.saas.rag.history.RagQueryLogger;
import com.resto.saas.rag.retrieval.RetrievalResult;
import com.resto.saas.rag.retrieval.RetrievalService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagOrchestrationService {

    private final RetrievalService retrievalService;
    private final LlmGenerationService llmGenerationService; // отдельный бин — чтобы AOP-прокси CB реально работал
    private final RagAnswerCache answerCache;                // Этап 2.5: Redis-кэш частых вопросов
    private final RagQueryLogger queryLogger;                // Этап 2.5: асинхронный аудит запросов
    private final MeterRegistry meterRegistry;
    private final HandlerMapping defaultServletHandlerMapping;
    /** Порог релевантности из application.yml: rag.retrieval.min-similarity */
    @Value("${rag.retrieval.min-similarity}")
    private  double minSimilarity;

    public RagAskResponse ask(RagAskRequest request) {
        long start = System.nanoTime();

        // Проверка кэша ДО retrieval: весь пайплайн (эмбеддинг вопроса + ANN-поиск
        // + LLM) — это ~300-800 мс и деньги за API; хит в Redis — ~1 мс.
        String cacheKey = answerCache.buildKey(request);
        var cashed = answerCache.get(cacheKey);
        if (cashed.isPresent()) {
            RagAskResponse hit = withLatency(cashed.get(), millisSince(start));
            queryLogger.log("demo", request.question(), request.aggregator(),
                    true, hit.escalated(), hit.confidence(), hit.latencyMs(), 0);
            return hit;
        }
        // 1. RETRIEVAL
        RetrievalResult retrieval = retrievalService.search(request.question(), request.aggregator());
        // 2. Анти галлюцинационный контроль
        if (retrieval.isEmpty()) {
            log.info("RAG: ничего не найдено по вопросу '{}'", request.question());
            return finish(request, cacheKey,
                    noAnswerFallback(retrieval, start, "В базе знаний ничего не найдено по вашему вопросу."));
        }
        double confidence = retrieval.maxSimilarity();
        if (confidence < minSimilarity) {
            log.info("RAG: низкая релевантность {} < {}, эскалация без LLM", confidence, minSimilarity);
            meterRegistry.counter("rag.escalations", "reason", "low_similarity").increment();
            String answer = "Я нашёл похожие материалы, но недостаточно уверен, чтобы отвечать автоматически.\n\n"
                    + sourcesBlock(retrieval.chunks())
                    + "\n💡 Пожалуйста, обратитесь к живому оператору или переформулируйте вопрос.";
            return finish(request, cacheKey, new RagAskResponse(answer, true, round(confidence),
                    toSources(retrieval.chunks()), false, millisSince(start)));
        }
        // 3. Generation (LLM под CircuitBreaker - вызов через отдельный бин)
        String answer = llmGenerationService.generate(request.question(), retrieval.chunks());

        return finish(request, cacheKey,
                new RagAskResponse(answer, false, round(confidence),
                        toSources(retrieval.chunks()), false, millisSince(start)));
    }

    /**
     * Единая «точка выхода» запроса: кладём ответ в кэш (put сам решает TTL —
     * эскалации живут меньше) и пишем аудит-лог (асинхронно, не блокирует ответ).
     * Вынесли в метод, чтобы ни одна из трёх веток не забыла логирование.
     */
    private RagAskResponse finish(RagAskRequest request, String cacheKey, RagAskResponse response) {
        answerCache.put(cacheKey, response);
        queryLogger.log("demo", request.question(), request.aggregator(),
                false, response.escalated(), response.confidence(),
                response.latencyMs(), countChunksQuietly(response));
        return response;
    }

    private int countChunksQuietly(RagAskResponse response) {
        return response.sources().size();
    }

    private static RagAskResponse withLatency(RagAskResponse r, long latencyMs) {
        return new RagAskResponse(r.answer(), r.escalated(), r.confidence(),
                r.sources(), true, latencyMs);
    }

    // ---------------- helpers ----------------

    private String sourcesBlock(List<Document> chunks) {
        return toSources(chunks).stream()
                .map(s -> "📚 %s — %s [%s]".formatted(s.title(), s.url(), s.aggregator()))
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    /** Уникальные источники по чанкам (несколько чанков одной статьи → одна ссылка). */
    private List<RagAskResponse.Source> toSources(List<Document> chunks) {
        return chunks.stream()
                .map(doc -> {
                    Map<String, Object> md = doc.getMetadata();
                    return new RagAskResponse.Source(
                            String.valueOf(md.getOrDefault("title", "Без названия")),
                            String.valueOf(md.getOrDefault("source_url", "")),
                            String.valueOf(md.getOrDefault("aggregator", "UNKNOWN")));
                })
                .distinct()
                .toList();
    }

    private RagAskResponse noAnswerFallback(RetrievalResult r, long start, String lead) {
        meterRegistry.counter("rag.escalations", "reason", "no_results").increment();
        return new RagAskResponse(
                lead + "\n\nРекомендации:\n1. Попробуйте переформулировать вопрос.\n"
                        + "2. Или нажмите \"Позвать оператора\" — живой специалист ответит быстрее.",
                true, 0.0, List.of(), false, millisSince(start));
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
