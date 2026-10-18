package com.resto.saas.rag.cache;

import com.resto.saas.rag.api.RagAskResponse;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Снимок ответа для хранения в Redis (кэш Этапа 2.5).
 *
 * Почему отдельный класс, а не RagAskResponse напрямую:
 * 1) стабильность контракта с хранилищем: если завтра добавим поле в API-DTO,
 *    старые значения в Redis станут нечитаемыми. CacheEntry — «версионируемая» модель;
 * 2) Jackson при восстановлении из JSON требует public конструктор без аргументов
 *    + сеттеры — у record'а с этим сложнее (нужен @JsonCreator).
 *
 * latencyMs сюда НЕ кладём: он про конкретный вызов, а не про данные ответа.
 */
@Setter
@Getter
public class RagAnswerCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String answer;
    private boolean escalated;
    private double confidence;
    private List<SourceEntry> sources = List.of();

    /** Plain-класс для источника — те же причины стабильности, что и снаружи. */
    @Setter
    @Getter
    public static class SourceEntry implements Serializable {

        private static final long serialVersionUID = 1L;

        private String title;
        private String url;
        private String aggregator;

        public SourceEntry() {
        }

        public SourceEntry(String title, String url, String aggregator) {
            this.title = title;
            this.url = url;
            this.aggregator = aggregator;
        }

        public RagAskResponse.Source toDto() {
            return new RagAskResponse.Source(title, url, aggregator);
        }

        public static SourceEntry from(RagAskResponse.Source s) {
            return new SourceEntry(s.title(), s.url(), s.aggregator());
        }
    }

    public RagAnswerCacheEntry() {
    }

    public RagAnswerCacheEntry(RagAskResponse response) {
        this.answer = response.answer();
        this.escalated = response.escalated();
        this.confidence = response.confidence();
        this.sources = response.sources().stream()
                .map(SourceEntry::from)
                .toList();
    }

    public RagAskResponse toDto(long latencyMs) {
        return new RagAskResponse(
                answer, escalated, confidence,
                sources.stream().map(SourceEntry::toDto)
                        .toList(),
                true, latencyMs);
    }
}
