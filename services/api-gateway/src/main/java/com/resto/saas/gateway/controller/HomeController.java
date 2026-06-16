package com.resto.saas.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String health() {
        return "Resto SaaS API Gateway OK";
    }
}