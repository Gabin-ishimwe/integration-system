package com.example.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI analyticsServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Analytic Service API")
                .description("REST API for managing analytics data. Stores and retrieves merged customer-product data from the integration pipeline.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Integration System Team")))
            .servers(List.of(
                new Server().url("http://localhost:8083").description("Local development"),
                new Server().url("http://41.186.186.166:8083").description("Docker environment")
            ));
    }
}
