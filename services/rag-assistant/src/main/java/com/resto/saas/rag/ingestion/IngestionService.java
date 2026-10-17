package com.resto.saas.rag.ingestion;

import com.resto.saas.rag.stub.StubConflunceLoader;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    public Map<String, Object> ingestAll() {
        return null;
    }

    public Map<String, Object> getStats() {
        return null;
    }
}
