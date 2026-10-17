package com.resto.saas.rag.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Автозагрузка знаний при старте сервиса
 * Включается флагом rag.ingestion.auto-run-on-startup=true
 * Удобно для показа пайплайна на собеседовании одним докер компоузом
 * В проде флаг выключен, загрузка знаний идет по расписанию/вебхуку через REST
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.ai.ingestion.auto-run-on-startup", havingValue = "true")
public class IngestionRunner {

    private final IngestionService ingestionService;

    @Bean
    public ApplicationRunner autoIngestOnStartup() {
        return args -> {
            log.info("rag.ingestion.auto-run-on-startup=true - автозагрузка знаний при старте");
            try {
                var summary = ingestionService.ingestAll();
                log.info("Загрузка знаний завершена: {}", summary);
            } catch (Exception e) {
                // Не роняем сервис, если БД еще не поднялась -
                // можно повторить вручную через POST /api/rag/ingest
                log.error("Ошибка при загрузке знаний(сервис продолжает работу): {}",
                        e.getMessage(), e);
            }
        };
    }
}
