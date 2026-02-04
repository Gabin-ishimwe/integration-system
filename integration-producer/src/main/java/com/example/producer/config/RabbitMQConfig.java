package com.example.producer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queues.customer-data}")
    private String customerQueue;

    @Value("${rabbitmq.queues.inventory-data}")
    private String inventoryQueue;

    @Value("${rabbitmq.exchanges.integration}")
    private String exchangeName;

    @Value("${rabbitmq.routing-keys.customer}")
    private String customerRoutingKey;

    @Value("${rabbitmq.routing-keys.inventory}")
    private String inventoryRoutingKey;

    @Bean
    public Queue customerQueue() {
        return QueueBuilder.durable(customerQueue).build();
    }

    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(inventoryQueue).build();
    }

    @Bean
    public DirectExchange integrationExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding customerBinding(Queue customerQueue, DirectExchange integrationExchange) {
        return BindingBuilder.bind(customerQueue)
            .to(integrationExchange)
            .with(customerRoutingKey);
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, DirectExchange integrationExchange) {
        return BindingBuilder.bind(inventoryQueue)
            .to(integrationExchange)
            .with(inventoryRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
