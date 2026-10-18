package com.resto.saas.rag.ingestion;

import com.resto.saas.rag.stub.StubConflunceLoader;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final StubConflunceLoader conflunceLoader;
    private final TokenTextSplitter textSplitter;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private MeterRegistry meterRegistry;

    /**
     * Полный цикл загрузки из базы знаний
     * Возвращает сводку для REST-ответа
     */
    public Map<String, Object> ingestAll() {

        Timer.Sample sample = Timer.start(meterRegistry);

        List<StubConflunceLoader.Article> articles = conflunceLoader.loadArticles();
        log.info("Extract: загружено {} статей из Conflunce(заглушка)", articles.size());

        List<Document> chunks = new ArrayList<>();
        for (StubConflunceLoader.Article article : articles) {
            Document doc = new Document(article.content(),
                    Map.of(
                            "title", article.title(),
                            "aggregator", article.aggregation(),
                            "document_type", article.documentType(),
                            "source_url", article.url()
                    ));
            // split() возвращает список чанков,
            // метаданные исходного документа копируются в каждый
            List<Document> articleChunks = textSplitter.split(doc);
            log.debug("Chunking '{}': {} чанков", article.title(), articleChunks.size());
            chunks.addAll(articleChunks);
        }
        log.info("Transform: статьи нарезаны на {} чанков", chunks.size());

        // add() внутри: embed каждого Document, INSERT ... vector(...) в rag_documents
        vectorStore.add(chunks);
        log.info("Load: {} чанков векторизованы и сохранены в pgvector", chunks.size());

        long durationMs = sample.stop(
                Timer
                .builder("rag.ingestion.duration")
                .description("Полный цикл ETL загрузки из базы знаний")
                .register(meterRegistry));

        meterRegistry.counter("rag.ingestion.articles").increment(articles.size());
        meterRegistry.counter("rag.ingestion.chunks").increment(chunks.size());

        return Map.of(
                "articlesLoaded", articles.size(),
                "chunksIngested", chunks.size(),
                "durationMs", durationMs,
                "stats", getStats());
    }

    /**
     * Статистика по таблице rag_documents - сколько чанков на агрегатор/тип
     * Считаем напрямую SQL-ом, так как VectorStore в bge-m3 не предоставляет count()
     */
    public Map<String, Object> getStats() {
        List<Map<String, Object>> byAggregator = jdbcTemplate.queryForList(
                """
                    SELECT COALESCE(aggregator, 'UNKNOWN') AS aggregator,
                           COUNT(*) AS chunks
                    FROM rag_documents
                    GROUP BY aggregator
                    ORDER BY chunks DESC
                    """
        );
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rag_documents", Integer.class);

        return Map.of(
                "totalChunks", total == null ? 0 : total,
                "byAggregator", byAggregator
        );
    }
}
