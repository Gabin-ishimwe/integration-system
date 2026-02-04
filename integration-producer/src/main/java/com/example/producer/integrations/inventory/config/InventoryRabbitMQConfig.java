package com.example.producer.integrations.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryRabbitMQConfig {

    @Value("${rabbitmq.queues.inventory-data}")
    private String inventoryQueue;

    @Value("${rabbitmq.routing-keys.inventory}")
    private String inventoryRoutingKey;

    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(inventoryQueue).build();
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, DirectExchange integrationExchange) {
        return BindingBuilder.bind(inventoryQueue)
            .to(integrationExchange)
            .with(inventoryRoutingKey);
    }
}
