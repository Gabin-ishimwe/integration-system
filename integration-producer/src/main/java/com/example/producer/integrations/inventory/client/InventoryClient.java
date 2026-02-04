package com.example.producer.integrations.inventory.client;

import com.example.producer.common.model.PagedResponse;
import com.example.producer.integrations.inventory.model.Product;
import com.example.producer.integrations.inventory.service.InventoryAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestTemplate restTemplate;
    private final InventoryAuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${mock-service.base-url}")
    private String mockServiceUrl;

    public InventoryClient(InventoryAuthService authService, ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    public PagedResponse<Product> getProducts(int page, int size) {
        String url = mockServiceUrl + "/inventory/api/products?page=" + page + "&size=" + size;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.info("Fetching products from Inventory - page: {}, size: {}", page, size);

        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, String.class);

        try {
            return objectMapper.readValue(response.getBody(),
                new TypeReference<PagedResponse<Product>>() {});
        } catch (Exception e) {
            log.error("Failed to parse Inventory response", e);
            throw new RuntimeException("Failed to parse Inventory response", e);
        }
    }
}
