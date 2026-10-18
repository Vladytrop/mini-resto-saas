package com.resto.saas.rag.retrieval;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieval — вторая ступень RAG: по вопросу пользователя ищем топ-K самых
 * похожих чанков в pgvector.
 *
 * Под капотом (PgVectorStore 1.0.0-M3):
 *   1) вопрос → StubEmbeddingModel.embed(question) → вектор 1024D;
 *   2) SQL: SELECT ... embedding <=> ?::vector AS distance FROM rag_documents
 *      ORDER BY distance LIMIT topK  (<=> — косинусное расстояние, оператор pgvector);
 *   3) строки → Document, distance кладётся в метаданные (см. RetrievalResult).
 *
 * Фильтр по агрегатору превращается в SQL-условие на JSONB:
 *   "aggregator == 'GLOVO'"  →  metadata @> '{"aggregator":"GLOVO"}'
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;
    /** Сколько чанков брать (из application.yml: rag.retrieval.top-k). */
    @Value("${rag.retrieval.top-k}")
    private int topK;

    /**
     * Поиск без фильтра. Порог similarity применим сами в оркестраторе
     */
    public RetrievalResult search(String question, String aggregator) {
        return doSearch(question, aggregator);
    }

    /**
     * Поиск с фильтром по агрегатору (Glovo, Wolt, Yandex_Eda и тд)
     */
    private RetrievalResult doSearch(String question, String aggregator) {
        Timer.Sample sample = Timer.start(meterRegistry);

        // SearchRequest - билдер
        // factory query(String) + withXxx методы, возвращают this для цепочки
        var request = SearchRequest.query(question)
                .withTopK(topK)
                .withSimilarityThresholdAll(); // берем всё до topK, порог применим сами

        if (aggregator != null && !aggregator.isBlank()) {
            // Экранирование кавычек: значение идет в DSL фильтр, одинарные кавычки ломают парсер
            String safe = aggregator.toUpperCase().replace("'", "''");
            request = request.withFilterExpression("aggregator == '" + safe + "'");
        }

        List<Document> results = vectorStore.similaritySearch(request);
        if (results == null) {
            results = List.of(); // контракт bge-m3 может вернуть null при ошибке конвертера
        }

        double maxSim = results.stream()
                .mapToDouble(RetrievalResult::similarityOf)
                .max()
                .orElse(0.0);

        log.info("Retrieval: вопрос='{}', агрегатор={}, найдено {} чанков, maxSimilarity={}",
                question, aggregator, results.size(), String.format("%.3f", maxSim));

        sample.stop(Timer.builder("rag.retrieval.duration")
                .description("Время поиска по векторному хранилищу")
                .register(meterRegistry));
        meterRegistry.counter("rag.retrieval.requests").increment();

        return new RetrievalResult(List.copyOf(results), question);
    }
}
