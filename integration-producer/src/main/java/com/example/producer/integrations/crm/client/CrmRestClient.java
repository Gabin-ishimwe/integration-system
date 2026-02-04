package com.example.producer.integrations.crm.client;

import com.example.producer.common.model.PagedResponse;
import com.example.producer.integrations.crm.model.Customer;
import com.example.producer.integrations.crm.service.CrmAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CrmRestClient {

    private static final Logger log = LoggerFactory.getLogger(CrmRestClient.class);

    private final RestTemplate restTemplate;
    private final CrmAuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${mock-service.base-url}")
    private String mockServiceUrl;

    public CrmRestClient(CrmAuthService authService, ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    public PagedResponse<Customer> getCustomers(int page, int size) {
        String url = mockServiceUrl + "/crm/api/customers?page=" + page + "&size=" + size;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.info("Fetching customers from CRM - page: {}, size: {}", page, size);

        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, String.class);

        try {
            return objectMapper.readValue(response.getBody(),
                new TypeReference<PagedResponse<Customer>>() {});
        } catch (Exception e) {
            log.error("Failed to parse CRM response", e);
            throw new RuntimeException("Failed to parse CRM response", e);
        }
    }
}
