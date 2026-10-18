package com.resto.saas.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync // нужен, чтобы @Async у RagQueryLogger реально работал (иначе аннотация игнорируется)
public class RagAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagAssistantApplication.class, args);
    }
}
