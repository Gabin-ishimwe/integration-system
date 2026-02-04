package com.example.analytics.service;

import com.example.analytics.dto.AnalyticsDtos;
import com.example.analytics.entity.CustomerEntity;
import com.example.analytics.entity.ProductEntity;
import com.example.analytics.repository.CustomerRepository;
import com.example.analytics.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;
    private final String producerBaseUrl;

    public AnalyticsService(
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        RestTemplate restTemplate,
        @Value("${analytics.producer.base-url}") String producerBaseUrl
    ) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.restTemplate = restTemplate;
        this.producerBaseUrl = producerBaseUrl;
    }

    @Transactional
    public void saveBatch(AnalyticsDtos.AnalyticsBatchRequest batch) {
        if (batch == null || batch.data() == null || batch.data().isEmpty()) {
            log.info("Received empty analytics batch");
            return;
        }

        List<CustomerEntity> updatedCustomers = new ArrayList<>();

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
            }

            updatedCustomers.add(customer);
        }

        if (!updatedCustomers.isEmpty()) {
            customerRepository.saveAll(updatedCustomers);
            log.info("Saved {} customers to database", updatedCustomers.size());
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllCustomersWithProducts() {
        List<CustomerEntity> customers = customerRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (CustomerEntity customer : customers) {
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("id", customer.getExternalId());
            customerData.put("name", customer.getName());
            customerData.put("email", customer.getEmail());
            customerData.put("phone", customer.getPhone());
            customerData.put("status", customer.getStatus());
            customerData.put("lastBatchNumber", customer.getLastBatchNumber());

            List<Map<String, Object>> products = new ArrayList<>();
            for (ProductEntity product : customer.getProducts()) {
                Map<String, Object> productData = new HashMap<>();
                productData.put("id", product.getExternalId());
                productData.put("name", product.getName());
                productData.put("category", product.getCategory());
                productData.put("price", product.getPrice());
                productData.put("stockLevel", product.getStockLevel());
                products.add(productData);
            }
            customerData.put("products", products);

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalProducts", products.size());
            summary.put("totalValue", customer.getProducts().stream()
                .map(ProductEntity::getPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
            customerData.put("summary", summary);

            result.add(customerData);
        }

        return result;
    }

    public void triggerRefresh() {
        log.info("Triggering refresh via integration-producer /api/trigger/fetch-all");
        restTemplate.postForObject(producerBaseUrl + "/api/trigger/fetch-all", null, Map.class);
    }

    public Map<String, Object> addCustomerViaSoap(String firstName, String lastName, String email, String phone) {
        log.info("Adding customer via SOAP: {} {}", firstName, lastName);

        Map<String, String> request = new HashMap<>();
        request.put("first_name", firstName);
        request.put("last_name", lastName);
        request.put("email", email);
        request.put("phone", phone);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
            producerBaseUrl + "/api/trigger/add-customer-soap",
            request,
            Map.class
        );

        return response;
    }
}
