package com.resto.saas.rag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Конфигурация RAG сервиса - сплиттер текста и векторное хранилище
 *
 * VectorStore создаем вручную через публичный конструктор
 * Таблицу не создаем - у нас воя миграция V1__rag_init.sql
 * с таблицей rag_documents (initializeSchema - false)
 */
@Configuration
public class RagConfig {

    /**
     * Чанкер текста для ETL пайплайна
     * chunkSize 800 токенов - размер одного чанка
     * minChunkSize 20 - маленькие хвосты, меньше 20 токенов отбрасываются
     * keepSeparator true - сохраняем переводы строк, чтобы МаркДавн структура не терялась
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(
                800,
                20,
                5,
                800,
                true
        );
    }

    /**
     * Векторное хранилище поверх pgvector
     * @param jdbcTemplate - тот же DataSource что у Спринг Дата/Флайвей
     * @param embeddingModel - наш StubEmbeddingModel - детерминированные векторы 1024D
     * @param dimensions 1024 - размерность как у bge-m3
     * COSINE_DISTANCE - косинусное расстояние (согласовано с vector_cosine_ops в индексе)
     * IVFFLAT lists по умолчанию - согласовано с  V1__rag_init.sql
     * initializeSchema false - не пересоздавать схему, она уже в Флайвей
     */
    @Bean
    public VectorStore vectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.pgvector.dimensions:1024}") int dimensions) {
        return new PgVectorStore(
                "vector_store",
                jdbcTemplate,
                embeddingModel,
                dimensions,
                PgVectorStore.PgDistanceType.COSINE_DISTANCE,
                false, //не удалять существующую таблицу
                PgVectorStore.PgIndexType.IVFFLAT,
                false //initializeSchema - схема принадлежит Flyway
        );
    }
}
