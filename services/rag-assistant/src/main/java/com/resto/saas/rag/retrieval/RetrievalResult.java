package com.resto.saas.rag.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Результат поиска по векторному хранилищу.
 *
 * Почему отдельный record, а не просто List<Document>: дальше по пайплайну нужны
 * ДВЕ вещи: 1) сами чанки (контент и метаданные идут в промпт и в список источников);
 * 2) максимальная релевантность — она определяет стратегию ответа.
 *
 * ВАЖНО про скор (проверено по байткоду PgVectorStore 1.0.0-M3):
 * Document в этой версии НЕ имеет поля score(). Вместо этого PgVectorStore
 * кладёт distance (значение из SQL "embedding <=> query AS distance")
 * в метаданные каждого документа под ключом "distance".
 * Для COSINE_DISTANCE distance ∈ [0..2], где 0 = идентичные векторы.
 */
public record RetrievalResult(List<Document> chunks, String query) {

    public static final String DISTANCE_KEY = "distance";

    /** Максимальная релевантность среди найденных чанков (0..1, выше = лучше). */
    public double maxSimilarity() {
        return chunks.stream()
                .mapToDouble(RetrievalResult::similarityOf)
                .max()
                .orElse(0.0);
    }

    public boolean isEmpry() {
        return chunks.isEmpty();
    }

    /**
     * Сходство одного чанка с запросом: 1 - distance (косинусное расстояние).
     * Ограничиваем снизу нулём: distance > 1 означает «векторы почти противоположны»,
     * отрицательное сходство нам не нужно — приведём к 0.
     */
    public static double similarityOf(Document doc) {
        Object d = doc.getMetadata().get(DISTANCE_KEY);
        if (!(d instanceof Number n)) {
            return 0.0;
        }
        return Math.max(0.0, 1.0 - n.doubleValue());
    }
}
