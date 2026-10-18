package com.resto.saas.rag.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Строка лога RAG-запроса (таблица rag_query_logs, миграция V3).
 *
 * Зачем лог в PostgreSQL, а не только метрики Prometheus:
 * - метрики отвечают на «СКОЛЬКО» (RPS, p95, доля эскалаций),
 * - лог отвечает на «ЧТО ИМЕННО»: текст вопроса, confidence, id статей.
 *   Кластеризуем вопросы с низкой релевантностью → получаем список «каких статей
 *   не хватает в базе знаний» (data-driven пополнение KB).
 *
 * tenant_id пока константой 'demo' — в реальном Kwaaka брался бы из JWT.
 */
@Entity
@Table(name = "rag_query_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 2048)
    private String question;

    @Column(length = 32)
    private String aggregator;

    /** Ответ отдали из Redis-кэша? (hit ratio по этой колонне = эффективность кэша) */
    @Column(name = "from_cache", nullable = false)
    private boolean fromCache;

    /** Эскалация к оператору (низкая релевантность / ничего не найдено / CB open)? */
    @Column(nullable = false)
    private boolean escalated;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    /** Сколько чанков нашёл retrieval (0 = промах по базе знаний). */
    @Column(name = "chunks_found", nullable = false)
    private int chunksFound;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}