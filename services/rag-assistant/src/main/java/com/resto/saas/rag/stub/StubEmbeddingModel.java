package com.resto.saas.rag.stub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Детерминированная заглушка модели эмбеддингов (вместо bge-m3).
 *
 * Все методы делегируют на embedText(String) — единую логику генерации.
 * Это важно, потому что Spring AI VectorStore вызывает embed(Document),
 * а не call(EmbeddingRequest).
 */
@Slf4j
@Component
public class StubEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 1024;  // Как у bge-m3

    /**
     * Основной метод для EmbeddingRequest.
     * Вызывается при батчевой обработке.
     */
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> texts = request.getInstructions();

        for (int i = 0; i < texts.size(); i++) {
            float[] vector = embedText(texts.get(i));
            embeddings.add(new Embedding(vector, i));
        }

        return new EmbeddingResponse(embeddings);
    }

    /**
     * ⚠️ КРИТИЧНЫЙ МЕТОД — его вызывает PgVectorStore при сохранении!
     * Извлекает текст из Document и делегирует на embedText.
     */
    @Override
    public float[] embed(Document document) {
        return embedText(document.getContent());
    }

    /**
     * Для одного текста (удобно для тестов).
     */
    @Override
    public float[] embed(String text) {
        return embedText(text);
    }

    /**
     * Для батча текстов.
     */
    @Override
    public List<float[]> embed(List<String> texts) {
        return texts.stream()
                .map(this::embedText)
                .toList();
    }

    /**
     * Размерность вектора. Должна совпадать с настройкой pgvector.
     */
    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    // ==================== ОСНОВНАЯ ЛОГИКА ====================

    /**
     * Превращает текст в вектор размерности 1024.
     *
     * Алгоритм:
     * 1. SHA-256 хэш текста → базовая "уникальность"
     * 2. Keyword boosting → смысловые кластеры
     * 3. L2-нормализация → для cosine similarity
     */
    private float[] embedText(String text) {
        try {
            if (text == null || text.isBlank()) {
                return new float[DIMENSIONS];  // Нулевой вектор для пустого текста
            }

            // 1. Базовый хэш текста
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.toLowerCase().getBytes(StandardCharsets.UTF_8));

            // 2. Генерируем 1024 float-значений из хэша
            float[] vector = new float[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) {
                byte b = hash[i % hash.length];
                vector[i] = (float) b / 127.0f;
            }

            // 3. Смысловые кластеры
            boostKeyword(vector, text, "glovo", 0.3f, 0);
            boostKeyword(vector, text, "wolt", 0.3f, 100);
            boostKeyword(vector, text, "яндекс", 0.3f, 200);
            boostKeyword(vector, text, "меню", 0.2f, 300);
            boostKeyword(vector, text, "заказ", 0.2f, 400);
            boostKeyword(vector, text, "ошибка", 0.2f, 500);
            boostKeyword(vector, text, "синхронизация", 0.2f, 600);
            boostKeyword(vector, text, "webhook", 0.2f, 700);

            // 4. L2-нормализация
            normalizeVector(vector);

            return vector;

        } catch (Exception e) {
            log.error("Ошибка генерации эмбеддинга: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    private void boostKeyword(float[] vector, String text, String keyword,
                              float boost, int startIndex) {
        if (text.toLowerCase().contains(keyword)) {
            for (int i = startIndex; i < startIndex + 50 && i < vector.length; i++) {
                vector[i] += boost;
            }
        }
    }

    private void normalizeVector(float[] vector) {
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }
}