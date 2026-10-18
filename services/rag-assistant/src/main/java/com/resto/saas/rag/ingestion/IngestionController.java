package com.resto.saas.rag.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST-эндпоинты для загрузки и статистики векторного хранилища
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;
    private final RagAnswerCache answerCache;

    /**
     * POST /api/rag/ingest - запустить полный ETL цикл
     * После успешной загрузки из базы знаний инвалидируем весь Редис кеш
     */
    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        log.info("POST /api/rag/ingest - запуск ETL пайплайна");
        Map<String, Object> result = ingestionService.ingestAll();
        answerCache.invalidateAll();
        return result;
    }

    /**
     * GET /api/rag/stats - сводка по векторному хранилищу
     */
    @GetMapping("stats")
    public Map<String, Object> stats() {
        return ingestionService.getStats();
    }
}
