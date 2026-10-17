package com.resto.saas.rag.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    /**
     * POST /api/rag/ingest - запустить полный ETL цикл
     * Ради идемпотентности, перед загрузкой можно почистить таблицу
     * для пет-проекта повторный запуск просто добавит дубликаты чанков
     */
    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        log.info("POST /api/rag/ingest - запуск ETL пайплайна");
        return ingestionService.ingestAll();
    }

    /**
     * GET /api/rag/stats - сводка по векторному хранилищу
     */
    @GetMapping("stats")
    public Map<String, Object> stats() {
        return ingestionService.getStats();
    }
}
