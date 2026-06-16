package com.resto.saas.loyalty.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Loyalty Service API")
                        .version("1.0.0")
                        .description(("Resto SaaS - Multi-tenant Order Management (PostgreSQL RLS + Kafka + Redis)")))
                .addServersItem(new Server().url("http://localhost:8082"));
    }

    @Bean
    public OperationCustomizer operationCustomizer() {

        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Tenant-Id")
                    .description("Tenant идентифицирован для Row-Level Security изоляции")
                    .required(true));
            return operation;
        };
    }
}
