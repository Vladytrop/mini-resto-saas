package com.resto.saas.rag.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Тело запроса POST /api/rag/ask.
 *
 * @NotBlank + @Size — bean validation. Контроллер помечаем @Valid, и кривой
 * запрос отбивается до того, как мы потратим ресурсы на эмбеддинг и поиск.
 *
 * aggregator — необязательный фильтр: если оператор общается с партнёром
 * конкретного агрегатора, чат может подтировать контекст и сузить поиск.
 */
public record RagAskRequest(
        @NotBlank(message = "Вопрос не может быть пустым")
        @Size(max = 1000, message = "Вопрос не может содержать более 1000 символов")
        String question,
        String aggregator
) {
}
