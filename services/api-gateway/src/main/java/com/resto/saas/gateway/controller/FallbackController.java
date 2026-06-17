package com.resto.saas.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/order")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {

        return Mono.just(
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                                "success", false,
                                "error", "Order Service temporarily unavailable. Please try again later",
                                "timestamp", Instant.now().toString()))
        );
    }

    @GetMapping("/loyalty")
    public Mono<ResponseEntity<Map<String, Object>>> loyaltyFallback() {

        return Mono.just(
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                                "success", false,
                                "error", "Loyalty Service temporarily unavailable. Please try again later",
                                "timestamp", Instant.now().toString()))
        );
    }
}
