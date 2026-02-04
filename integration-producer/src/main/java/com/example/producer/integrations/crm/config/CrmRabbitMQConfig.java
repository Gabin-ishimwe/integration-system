package com.example.producer.integrations.crm.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrmRabbitMQConfig {

    @Value("${rabbitmq.queues.customer-data}")
    private String customerQueue;

    @Value("${rabbitmq.routing-keys.customer}")
    private String customerRoutingKey;

    @Bean
    public Queue customerQueue() {
        return QueueBuilder.durable(customerQueue).build();
    }

    @Bean
    public Binding customerBinding(Queue customerQueue, DirectExchange integrationExchange) {
        return BindingBuilder.bind(customerQueue)
            .to(integrationExchange)
            .with(customerRoutingKey);
    }
}
