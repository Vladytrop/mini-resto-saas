-- Включаем расширение pgvector (добавляет тип vector и операторы сходства)
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Таблица для хранения чанков статей из Confluence
CREATE TABLE IF NOT EXISTS rag_documents (
                                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Текст чанка (500-800 слов)
    content TEXT NOT NULL,

    -- Векторное представление (1024 числа для bge-m3)
    -- Хранится в специальном типе vector из pgvector
    embedding vector(1024),

    -- Метаданные для фильтрации
    aggregator VARCHAR(50),      -- Glovo, Wolt, YandexEda, Chocofood, Uzum
    document_type VARCHAR(50),   -- ONBOARDING, TROUBLESHOOTING, FAQ, MENU_SYNC

-- Источник (ссылка на статью в Confluence)
    source_url TEXT,
    source_title TEXT,

    -- Аудит
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
    );

-- Индекс для быстрого ANN-поиска (Approximate Nearest Neighbor)
-- ivfflat: inverted file index — стандарт для pgvector
-- lists = 100: количество кластеров (обычно sqrt(N), где N — число записей)
-- Для прода: CREATE INDEX ... WITH (lists = sqrt(число_чанков))
CREATE INDEX IF NOT EXISTS rag_documents_embedding_idx
    ON rag_documents
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Индексы для фильтрации по метаданным
CREATE INDEX IF NOT EXISTS rag_documents_aggregator_idx
    ON rag_documents (aggregator);

CREATE INDEX IF NOT EXISTS rag_documents_type_idx
    ON rag_documents (document_type);

-- Композитный индекс для частых запросов: "покажи чанки по Glovo типу TROUBLESHOOTING"
CREATE INDEX IF NOT EXISTS rag_documents_agg_type_idx
    ON rag_documents (aggregator, document_type);

-- Обновление updated_at автоматически
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_rag_documents_updated_at
    BEFORE UPDATE ON rag_documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Таблица для логирования запросов к RAG (для метрик и анализа)
CREATE TABLE IF NOT EXISTS rag_query_logs (
                                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    question TEXT NOT NULL,
    answer TEXT,

    -- Метрики
    relevance_score DECIMAL(5, 4),  -- cosine similarity топ-1 чанка
    retrieval_latency_ms INTEGER,
    generation_latency_ms INTEGER,

    -- Что произошло
    outcome VARCHAR(30),  -- ANSWERED, LOW_RELEVANCE, LLM_FAILED, ESCALATED

-- Контекст
    aggregator VARCHAR(50),
    session_id VARCHAR(100),

    created_at TIMESTAMP NOT NULL DEFAULT now()
    );

-- Индекс для аналитики
CREATE INDEX IF NOT EXISTS rag_query_logs_outcome_idx
    ON rag_query_logs (outcome);

CREATE INDEX IF NOT EXISTS rag_query_logs_created_at_idx
    ON rag_query_logs (created_at DESC);