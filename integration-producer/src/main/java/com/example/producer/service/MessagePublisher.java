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

    public void publishCustomer(Customer customer) {
        CustomerMessage message = CustomerMessage.builder()
            .correlationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .source("integration-producer")
            .data(customer)
            .build();

        rabbitTemplate.convertAndSend(exchangeName, customerRoutingKey, message);
        log.debug("Published customer: {}", customer.getCustomerId());
    }

    public void publishProduct(Product product) {
        ProductMessage message = ProductMessage.builder()
            .correlationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .source("integration-producer")
            .data(product)
            .build();

        rabbitTemplate.convertAndSend(exchangeName, inventoryRoutingKey, message);
        log.debug("Published product: {}", product.getProductId());
    }
}
