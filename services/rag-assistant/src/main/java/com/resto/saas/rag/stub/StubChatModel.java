package com.resto.saas.rag.stub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Детерминированная заглушка LLM (вместо DeepSeek/OpenAI).
 *
 * Для Spring AI 1.0.0-M3+. Реализует ChatModel интерфейс.
 */
@Slf4j
@Component
public class StubChatModel implements ChatModel {

    private static final int MIN_CONTEXT_LENGTH = 100;

    @Override
    public ChatResponse call(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();

        String context = extractContext(messages);
        String question = extractQuestion(messages);

        log.debug("StubChatModel: вопрос='{}', контекст={} символов",
                question, context.length());

        String answer = generateAnswer(question, context);

        // ✅ Правильные конструкторы для Spring AI 1.0.0-M3+
        AssistantMessage assistantMessage = new AssistantMessage(answer);
        Generation generation = new Generation(assistantMessage);

        return new ChatResponse(List.of(generation));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }

    /**
     * Опции по умолчанию. Возвращаем OpenAiChatOptions как конкретную реализацию.
     * Используем анонимный класс, чтобы не зависеть от версии Spring AI
     */
    @Override
    public ChatOptions getDefaultOptions() {
        return new ChatOptions() {
            @Override
            public String getModel() {
                return "stub-llm-v1";
            }

            @Override
            public Double getFrequencyPenalty() {
                return null;
            }

            @Override
            public Integer getMaxTokens() {
                return 1000;
            }

            @Override
            public Double getPresencePenalty() {
                return null;
            }

            @Override
            public List<String> getStopSequences() {
                return List.of();
            }

            @Override
            public Double getTemperature() {
                return 0.3;  // Низкая температура для фактических ответов
            }

            @Override
            public Integer getTopK() {
                return null;
            }

            @Override
            public Double getTopP() {
                return 0.9;
            }

            @Override
            public ChatOptions copy() {
                return this;  // Для заглушки достаточно вернуть себя
            }
        };
    }

    /**
     * Извлекает контекст из SystemMessage.
     * ✅ Используем getContent() вместо getText()
     */
    private String extractContext(List<Message> messages) {
        return messages.stream()
                .filter(m -> m instanceof SystemMessage)
                .map(m -> String.valueOf(m.getContent()))  // getContent() вместо getText()
                .filter(content -> content.contains("КОНТЕКСТ") || content.contains("CONTEXT"))
                .findFirst()
                .orElse("");
    }

    /**
     * Извлекает вопрос из UserMessage.
     */
    private String extractQuestion(List<Message> messages) {
        return messages.stream()
                .filter(m -> m instanceof UserMessage)
                .map(m -> String.valueOf(m.getContent()))  // ✅ getContent()
                .findFirst()
                .orElse("");
    }

    private String generateAnswer(String question, String context) {
        if (context == null || context.isBlank()) {
            log.info("LLM fallback: контекст пуст, предлагаем оператора");
            return """
                   К сожалению, я не нашёл информации по вашему вопросу в базе знаний.
                   
                   Рекомендую:
                   1. Переформулировать вопрос другими словами
                   2. Обратиться к живому оператору через кнопку "Позвать оператора"
                   """;
        }

        if (context.length() < MIN_CONTEXT_LENGTH) {
            log.info("LLM fallback: низкая релевантность ({} символов)", context.length());
            String sources = extractSources(context);
            return String.format("""
                   Я нашёл некоторые материалы, но их недостаточно для точного ответа.
                   
                   📚 Найденные материалы:
                   %s
                   
                   💡 Если не помогло, нажмите "Позвать оператора".
                   """, sources);
        }

        String answer = extractKeyPhrases(context);
        String sources = extractSources(context);

        return String.format("""
                На основе найденной информации в базе знаний:
                
                %s
                
                ---
                📚 Источники:
                %s
                
                ---
                💡 Если этот ответ не помог, нажмите "Позвать оператора".
                """, answer, sources);
    }

    private String extractKeyPhrases(String context) {
        String truncated = context.length() > 500
                ? context.substring(0, 500) + "..."
                : context;
        return "**Основная информация:**\n\n" + truncated;
    }

    private String extractSources(String context) {
        return context.lines()
                .filter(line -> line.contains("https://") || line.contains("confluence.kwaaka.com"))
                .map(line -> "• " + line.trim())
                .limit(3)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("• База знаний Kwaaka (Confluence)");
    }
}