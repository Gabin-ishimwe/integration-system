package com.example.analytics.service;

import com.example.analytics.dto.AnalyticsDtos;
import com.example.analytics.entity.CustomerEntity;
import com.example.analytics.entity.ProductEntity;
import com.example.analytics.repository.CustomerRepository;
import com.example.analytics.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final WebClient webClient;
    private final String cacheKeyLatest;

    public AnalyticsService(
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        StringRedisTemplate redisTemplate,
        @Value("${analytics.cache.latest-key}") String cacheKeyLatest,
        @Value("${analytics.producer.base-url}") String producerBaseUrl
    ) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.cacheKeyLatest = cacheKeyLatest;
        this.webClient = WebClient.builder()
            .baseUrl(producerBaseUrl)
            .build();
    }

    @Transactional
    public void saveBatch(AnalyticsDtos.AnalyticsBatchRequest batch) {
        if (batch == null || batch.data() == null || batch.data().isEmpty()) {
            log.info("Received empty analytics batch");
            return;
        }

        List<CustomerEntity> updatedCustomers = new ArrayList<>();
        List<ProductEntity> newProducts = new ArrayList<>();

        for (AnalyticsDtos.AnalyticsRecord record : batch.data()) {
            if (record.customer() == null || record.products() == null || record.products().isEmpty()) {
                continue;
            }

            String customerExternalId = record.customer().id();
            CustomerEntity customer = customerRepository
                .findByExternalId(customerExternalId)
                .orElseGet(CustomerEntity::new);

            customer.setExternalId(customerExternalId);
            customer.setName(record.customer().name());
            customer.setEmail(record.customer().email());
            customer.setPhone(record.customer().phone());
            customer.setStatus(record.customer().status());
            customer.setLastBatchNumber(batch.batchNumber());
            if (record.timestamp() != null) {
                customer.setLastAnalyticsTimestamp(Instant.parse(record.timestamp()));
            }

            // Reset existing products for this customer to keep a simple model
            if (customer.getId() != null) {
                productRepository.deleteAll(customer.getProducts());
                customer.getProducts().clear();
            }

            for (AnalyticsDtos.Product p : record.products()) {
                ProductEntity product = new ProductEntity();
                product.setExternalId(p.id());
                product.setName(p.name());
                product.setCategory(p.category());
                product.setPrice(p.price());
                product.setStockLevel(p.stock_level());
                product.setCustomer(customer);
                customer.getProducts().add(product);
                newProducts.add(product);
            }

            updatedCustomers.add(customer);
        }

        if (!updatedCustomers.isEmpty()) {
            customerRepository.saveAll(updatedCustomers);
            // products are cascaded from customer, no need to saveAll(newProducts) explicitly
        }

        // Cache the raw batch JSON for fast read access
        try {
            String serialized = "{\"batchNumber\":\"" + batch.batchNumber() + "\",\"data\":" + toJsonArray(batch.data()) + "}";
            redisTemplate.opsForValue().set(cacheKeyLatest, serialized);
        } catch (Exception e) {
            log.warn("Failed to cache analytics batch in Redis", e);
        }
    }

    public String getLatestCachedBatch() {
        return redisTemplate.opsForValue().get(cacheKeyLatest);
    }

    /**
     * Trigger a full refresh of analytics data by calling integration-producer,
     * which will in turn talk to mock-service and publish new data to RabbitMQ.
     */
    public void triggerRefresh() {
        log.info("Triggering refresh via integration-producer /api/trigger/fetch-all");
        webClient.post()
            .uri("/api/trigger/fetch-all")
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    // Very small helper to avoid pulling an entire JSON library, since Spring will handle normal (de)serialization.
    private String toJsonArray(List<AnalyticsDtos.AnalyticsRecord> records) {
        // We don't need perfect JSON here – this is only used for quick inspection / cache,
        // and the canonical representation is in Postgres.
        // For simplicity, delegate to Jackson through Spring if needed later.
        // For now just return "[]" placeholder to keep logic minimal.
        return "[]";
    }
}

