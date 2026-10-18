package com.resto.saas.rag.orchestration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Отдельный бин генерации ответа через LLM.
 *
 * ПОЧЕМУ ВЫНЕСЛИ ИЗ ОРКЕСТРАТОРА (тема для собеседования!):
 * Spring вешает @CircuitBreaker через AOP-прокси: когда ДРУГОЙ бин вызывает
 * наш метод, вызов идёт через обёртку, которая считает ошибки и размыкает цепь.
 * Но self-invocation (вызов this.generate() из того же класса) идёт МИМО прокси —
 * это обычный Java-вызов, без всякого AOP. Значит CB молча не работал бы.
 * Решение: вынести генерацию в отдельный бин и вызывать его ЧЕРЕЗ DI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmGenerationService {

    private final ChatModel chatModel;
    private final MeterRegistry meterRegistry;
    private final UrlPathHelper urlPathHelper;

    @CircuitBreaker(name = "llmApi", fallbackMethod = "generateFallback")
    public String generate(String question, List<Document> chunks) {

        // Context-инженеринг: текст вопроса + нумерованные чанки со ссылками.
        // Жёсткое правило в системной части («отвечай ТОЛЬКО по контексту»)
        // снижает шанс фантазий модели — так делают вендорские промпты OpenAI/Anthropic.
        String systemPrompt = """
                Ты — ассистент службы поддержки ресторанных партнёров агрегаторов доставки.
                                Отвечай ТОЛЬКО на основе предоставленного КОНТЕКСТА. Если информации
                                в контексте нет — честно скажи об этом и предложи позвать оператора.
                                Не выдумывай факты, суммы комиссий и сроки.
                
                                КОНТЕКСТ:
                """ + buildContext(chunks);

        // Prompt в M3 принимает List<Message>. Порядок важен: сначала система, потом вопрос.
        List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(question)
        );

        ChatResponse response = chatModel.call(new Prompt(messages));

        // В M3 ответ достаётся так: getResult() -> первое Generation, getOutput() -> AssistantMessage.
        return response.getResult().getOutput().getContent();
    }

    @SuppressWarnings("unused") // параметр Throwable обязателен по контракту fallback
    String generateFallback(String question, List<Document> chunks, Throwable t) {
        log.warn("CircuitBreaker llmApi: LLM недоступна ({}), отвечаю списком статей", t.getMessage());

        // Счётчик эскалаций с лейблом reason — увидим долю таких ответов в Grafana.
        meterRegistry.counter("rag.escalations", "reason", "llm_unavailable").increment();

        return "Сервис генерации ответов временно недоступен. Вот материалы, которые могут помочь:\n\n"
                + sourcesBlock(chunks)
                + "\n💡 Если нужно срочное решение — нажмите \"Позвать оператора\".";
    }

    /** Собираем из чанков нумерованный блок КОНТЕКСТА для промпта. */
    private String buildContext(List<Document> chunks) {
        return chunks.stream()
                .map(document -> {
                    Map<String, Object> md = document.getMetadata();// метаданные мы сами положили при загрузке из базы знаний
                    String title = String.valueOf(md.getOrDefault("title", "Без названия"));
                    String url = String.valueOf(md.getOrDefault("source_url", ""));
                    return "### %s (%s)\n%s\n".formatted(title, url, document.getContent());
                })
                .collect(Collectors.joining("\n"));
    }

    /** Человекочитаемый блок «📚 Статья — ссылка [агрегатор]» без дублей. */
    private String sourcesBlock(List<Document> chunks) {
        return chunks.stream()
                .map(doc -> {
                    Map<String, Object> md = doc.getMetadata();
                    String title = String.valueOf(md.getOrDefault("title", "Без названия"));
                    String url = String.valueOf(md.getOrDefault("source_url", ""));
                    String agg = String.valueOf(md.getOrDefault("aggregator", "UNKNOWN"));
                    return "📚 %s — %s [%s]".formatted(title, url, agg);
                })
                .distinct()
                .collect(Collectors.joining("\n"));
    }
}
