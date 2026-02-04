package com.example.producer.integrations.crm.service;

import com.example.producer.common.model.PagedResponse;
import com.example.producer.integrations.crm.client.CrmRestClient;
import com.example.producer.integrations.crm.client.CrmSoapClient;
import com.example.producer.integrations.crm.model.AddCustomerSoapResponse;
import com.example.producer.integrations.crm.model.Customer;
import com.example.producer.integrations.crm.model.CustomerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CrmService {

    private static final Logger log = LoggerFactory.getLogger(CrmService.class);

    private final CrmRestClient crmRestClient;
    private final CrmSoapClient crmSoapClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchanges.integration}")
    private String exchangeName;

    @Value("${rabbitmq.routing-keys.customer}")
    private String customerRoutingKey;

    public CrmService(CrmRestClient crmRestClient, CrmSoapClient crmSoapClient, RabbitTemplate rabbitTemplate) {
        this.crmRestClient = crmRestClient;
        this.crmSoapClient = crmSoapClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    public int fetchAndPublishCustomers(int page, int size) {
        try {
            PagedResponse<Customer> response = crmRestClient.getCustomers(page, size);
            List<Customer> customers = response.getContent();
            publishCustomers(customers);
            log.info("Published {} customers", customers.size());
            return customers.size();
        } catch (Exception e) {
            log.error("Failed to fetch/publish customers", e);
            return 0;
        }
    }

    public List<Customer> fetchCustomers(int page, int size) {
        PagedResponse<Customer> response = crmRestClient.getCustomers(page, size);
        return response.getContent();
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

    public AddCustomerSoapResponse addCustomerViaSoap(String firstName, String lastName, String email, String phone) {
        CrmSoapClient.SoapResponse soapResponse = crmSoapClient.addCustomer(firstName, lastName, email, phone);

        return new AddCustomerSoapResponse(
            soapResponse.success(),
            soapResponse.customerId(),
            soapResponse.message(),
            Instant.now().toString()
        );
    }
}
