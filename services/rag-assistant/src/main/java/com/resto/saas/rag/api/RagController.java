package com.resto.saas.rag.api;

import com.resto.saas.rag.orchestration.RagOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-фасад над RAG-оркестратором.
 * Вся логика (retrieval, пороги, LLM) — в RagOrchestrationService, чтобы её
 * можно было переиспользовать, например, из Kafka-listener'а без HTTP.
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Assistant", description = "AI-ассистент службы поддержки партнёров")
public class RagController {

    private final RagOrchestrationService orchestrationService;

    /**
     * POST /api/rag/ask
     * Body: {"question": "Как вывести деньги с Glovo?", "aggregator": "GLOVO"}
     * aggregator необязателен — без него ищем по всей базе знаний.
     */
    @Operation(summary = "Задать вопрос ассистенту (RAG: retrieval + generation)")
    @PostMapping("/ask")
    public ResponseEntity<RagAskResponse> ask(@Valid @RequestBody RagAskRequest request) {
        return ResponseEntity.ok(orchestrationService.ask(request));
    }
}