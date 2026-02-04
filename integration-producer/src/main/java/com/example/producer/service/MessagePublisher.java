package com.example.producer.service;

import com.example.producer.model.Customer;
import com.example.producer.model.CustomerMessage;
import com.example.producer.model.Product;
import com.example.producer.model.ProductMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchanges.integration}")
    private String exchangeName;

    @Value("${rabbitmq.routing-keys.customer}")
    private String customerRoutingKey;

    @Value("${rabbitmq.routing-keys.inventory}")
    private String inventoryRoutingKey;

    public MessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCustomers(List<Customer> customers) {
        CustomerMessage message = CustomerMessage.builder()
            .correlationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .source("integration-producer")
            .data(customers)
            .build();

        rabbitTemplate.convertAndSend(exchangeName, customerRoutingKey, message);
        log.info("Published {} customers", customers.size());
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
