package com.example.producer.integrations.inventory.service;

import com.example.producer.common.model.PagedResponse;
import com.example.producer.integrations.inventory.client.InventoryClient;
import com.example.producer.integrations.inventory.model.Product;
import com.example.producer.integrations.inventory.model.ProductMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryClient inventoryClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchanges.integration}")
    private String exchangeName;

    @Value("${rabbitmq.routing-keys.inventory}")
    private String inventoryRoutingKey;

    public InventoryService(InventoryClient inventoryClient, RabbitTemplate rabbitTemplate) {
        this.inventoryClient = inventoryClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    public int fetchAndPublishProducts(int page, int size) {
        try {
            PagedResponse<Product> response = inventoryClient.getProducts(page, size);
            List<Product> products = response.getContent();
            publishProducts(products);
            log.info("Published {} products", products.size());
            return products.size();
        } catch (Exception e) {
            log.error("Failed to fetch/publish products", e);
            return 0;
        }
    }

    public List<Product> fetchProducts(int page, int size) {
        PagedResponse<Product> response = inventoryClient.getProducts(page, size);
        return response.getContent();
    }

    public void publishProducts(List<Product> products) {
        ProductMessage message = ProductMessage.builder()
            .correlationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .source("integration-producer")
            .data(products)
            .build();

        rabbitTemplate.convertAndSend(exchangeName, inventoryRoutingKey, message);
        log.info("Published {} products", products.size());
    }
}
