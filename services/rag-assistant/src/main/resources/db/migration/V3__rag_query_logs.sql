-- V3: таблица аудита RAG-запросов (Этап 2.5).
--
-- Зачем отдельная таблица, а не лог-файл:
--   1) SQL-аналитика: «топ-20 вопросов с escalated=true за неделю» = список статей,
--      которых НЕ ХВАТАЕТ в базе знаний (data-driven пополнение KB);
--   2) hit ratio кэша: select count(*) filter (where from_cache) / count(*);
--   3) разметка для будущего дообучения/оценки качества ответов.
--
-- Индексы под два главных запроса: аналитика по времени и разбор эскалаций.

CREATE TABLE IF NOT EXISTS rag_query_logs (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     VARCHAR(64)  NOT NULL,
    question      VARCHAR(2048) NOT NULL,
    aggregator    VARCHAR(32),
    from_cache    BOOLEAN      NOT NULL DEFAULT FALSE,
    escalated     BOOLEAN      NOT NULL DEFAULT FALSE,
    confidence    DOUBLE PRECISION,
    latency_ms    BIGINT       NOT NULL,
    chunks_found  INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

-- Аналитика по периоду (самый частый фильтр) + покрытие tenant'ом
CREATE INDEX IF NOT EXISTS rag_query_logs_created_idx
    ON rag_query_logs (tenant_id, created_at DESC);

-- Выборка проблемных запросов (низкая релевантность) — частично-индекс:
-- занимает места ровно столько, сколько эскалаций, и ускоряет именно этот запрос
CREATE INDEX IF NOT EXISTS rag_query_logs_escalated_idx
    ON rag_query_logs (created_at DESC)
    WHERE escalated = TRUE;