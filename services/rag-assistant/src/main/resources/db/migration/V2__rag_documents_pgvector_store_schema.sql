-- V2: приводим таблицу rag_documents к схеме, которую ожидает Spring AI PgVectorStore (1.0.0-M3).
--
-- PgVectorStore в bge-m3 НЕ умеет работать с «плоскими» колонками метаданных —
-- он читает/пишет ВСЮ документную структуру в одну JSONB-колонку metadata.
-- Поэтому:
--   1) переименовываем source_title -> name (стандартное поле Document в Spring AI);
--   2) добавляем JSONB-колонку metadata;
--   3) добавляем UNIQUE(id) + GIN-индекс на metadata (GIN ускоряет фильтры вида
--      "metadata @> '{"aggregator":"GLOVO"}'" — именно так работает FilterExpressionConverter);
--   4) старые плоские индексы по метаданным больше не нужны — удаляем.

ALTER TABLE rag_documents RENAME COLUMN source_title TO name;

ALTER TABLE rag_documents ADD COLUMN IF NOT EXISTS metadata JSONB;

-- Уникальный id обязателен: PgVectorStore использует ON CONFLICT (id) DO UPDATE
-- при повторной загрузке одних и тех же документов (идемпотентность инджестии).
CREATE UNIQUE INDEX IF NOT EXISTS rag_documents_id_unique_idx ON rag_documents (id);

-- GIN-индекс для JSONB-фильтрации метаданных (поиск по aggregator/document_type)
CREATE INDEX IF NOT EXISTS rag_documents_metadata_gin_idx ON rag_documents USING GIN (metadata);

-- Плоские индексы под колонки, которые больше не участвуют в запросах VectorStore
DROP INDEX IF EXISTS rag_documents_aggregator_idx;
DROP INDEX IF EXISTS rag_documents_type_idx;
DROP INDEX IF EXISTS rag_documents_agg_type_idx;