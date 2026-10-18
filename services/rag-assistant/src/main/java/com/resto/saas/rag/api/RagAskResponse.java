package com.resto.saas.rag.api;

import java.util.List;

/**
 * Ответ эндпоинта POST /api/rag/ask.
 *
 * record — потому что это неизменяемые данные для сериализации в JSON (Jackson).
 *   answer        — текст ответа (LLM или fallback);
 *   escalated     — true → показать кнопку «Позвать оператора» prominently;
 *   confidence    — maxSimilarity 0..1, можно вывести как «уверенность ответа»;
 *   sources       — заголовки + ссылки найденных статей (прозрачность = анти-галлюцинации);
 *   fromCache     — true если ответ пришёл из Redis-кэша (Этап 2.5);
 *   latencyMs     — p95 мониторинг на стороне клиента.
 */
public record RagAskResponse(
        String answer,
        boolean escalated,
        double confidence,
        List<Source> sources,
        boolean fromCache,
        long latencyMs
) {
    /** Источник: название статьи + ссылка на Confluence. */
    public record Source(
            String title,
            String url,
            String aggregator
    ) {
    }
}
