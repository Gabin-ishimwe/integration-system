package com.example.producer.common.config;

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
    public OpenAPI integrationProducerOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Integration Producer API")
                .description("REST API for triggering data fetches from external systems (CRM, Inventory) and publishing to RabbitMQ queues.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Integration System Team")))
            .servers(List.of(
                new Server().url("http://localhost:8082").description("Local development"),
                new Server().url("http://41.186.186.166:8082").description("Docker environment")
            ));
    }
}
