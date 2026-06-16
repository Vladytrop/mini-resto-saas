package com.resto.saas.order;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@OpenAPIDefinition(info = @Info(title = "Order Service", version = "1.0"))
@SpringBootApplication
@ComponentScan(basePackages = {"com.resto.saas.order", "com.resto.saas.shared"})
@EntityScan(basePackages = "com.resto.saas.order.entity")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
