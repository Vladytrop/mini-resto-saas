package com.resto.saas.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RagAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagAssistantApplication.class, args);
    }
}
