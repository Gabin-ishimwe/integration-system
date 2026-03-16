package com.example.analytics.integration;

import com.example.analytics.dto.AnalyticsDtos;
import com.example.analytics.entity.CustomerEntity;
import com.example.analytics.repository.CustomerRepository;
import com.example.analytics.repository.ProductRepository;
import com.example.analytics.service.AnalyticsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AnalyticsService.
 * Tests database operations using PostgreSQL Testcontainer.
 */
@SpringBootTest
@Testcontainers
class AnalyticsServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("analytics_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Disable external service calls
        registry.add("analytics.producer.base-url", () -> "http://localhost:9999");
        registry.add("analytics.consumer.base-url", () -> "http://localhost:9999");
    }

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save analytics batch to database")
    void shouldSaveAnalyticsBatchToDatabase() {
        // Arrange
        AnalyticsDtos.AnalyticsBatchRequest batch = createSampleBatch();

        // Act
        analyticsService.saveBatch(batch);

        // Assert
        List<CustomerEntity> customers = customerRepository.findAll();
        assertEquals(1, customers.size());

        CustomerEntity customer = customers.get(0);
        assertEquals("CUST001", customer.getExternalId());
        assertEquals("John Doe", customer.getName());
        assertEquals("john@example.com", customer.getEmail());
    }

    @Test
    @DisplayName("Should update existing customer on duplicate external ID")
    void shouldUpdateExistingCustomerOnDuplicate() {
        // Arrange: Save initial batch
        AnalyticsDtos.AnalyticsBatchRequest batch1 = createSampleBatch();
        analyticsService.saveBatch(batch1);

        // Arrange: Create updated batch with same customer ID
        AnalyticsDtos.Customer updatedCustomer = new AnalyticsDtos.Customer(
                "CUST001", "John Updated", "john.updated@example.com", "+9999999999", "INACTIVE"
        );
        AnalyticsDtos.Product newProduct = new AnalyticsDtos.Product(
                "PROD003", "New Product", "New Category", new BigDecimal("199.99"), 75
        );
        AnalyticsDtos.AnalyticsRecord updatedRecord = new AnalyticsDtos.AnalyticsRecord(
                "MERGE_UPDATE", updatedCustomer, List.of(newProduct), null, "2024-01-01T12:00:00Z"
        );
        AnalyticsDtos.AnalyticsBatchRequest batch2 = new AnalyticsDtos.AnalyticsBatchRequest(
                "BATCH002", List.of(updatedRecord)
        );

        // Act
        analyticsService.saveBatch(batch2);

        // Assert: Should still have only 1 customer
        List<CustomerEntity> customers = customerRepository.findAll();
        assertEquals(1, customers.size());

        CustomerEntity customer = customers.get(0);
        assertEquals("John Updated", customer.getName());
        assertEquals("john.updated@example.com", customer.getEmail());
        assertEquals("INACTIVE", customer.getStatus());
    }

    @Test
    @DisplayName("Should retrieve all customers with products")
    void shouldRetrieveAllCustomersWithProducts() {
        // Arrange
        analyticsService.saveBatch(createSampleBatch());

        // Act
        List<Map<String, Object>> result = analyticsService.getAllCustomersWithProducts();

        // Assert
        assertEquals(1, result.size());

        Map<String, Object> customerData = result.get(0);
        assertEquals("CUST001", customerData.get("id"));
        assertEquals("John Doe", customerData.get("name"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) customerData.get("products");
        assertEquals(2, products.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) customerData.get("summary");
        assertEquals(2, summary.get("totalProducts"));
    }

    @Test
    @DisplayName("Should export customers to CSV")
    void shouldExportCustomersToCsv() {
        // Arrange
        analyticsService.saveBatch(createSampleBatch());

        // Act
        String csv = analyticsService.exportCustomersToCsv();

        // Assert
        assertNotNull(csv);
        assertTrue(csv.startsWith("id,name,email,phone,status,total_products,total_value"));
        assertTrue(csv.contains("CUST001"));
        assertTrue(csv.contains("John Doe"));
        assertTrue(csv.contains("john@example.com"));
    }

    @Test
    @DisplayName("Should export products to CSV")
    void shouldExportProductsToCsv() {
        // Arrange
        analyticsService.saveBatch(createSampleBatch());

        // Act
        String csv = analyticsService.exportProductsToCsv();

        // Assert
        assertNotNull(csv);
        assertTrue(csv.startsWith("id,name,category,price,stock_level,customer_id"));
        assertTrue(csv.contains("PROD001"));
        assertTrue(csv.contains("Laptop"));
        assertTrue(csv.contains("Electronics"));
    }

    @Test
    @DisplayName("Should handle empty batch gracefully")
    void shouldHandleEmptyBatch() {
        // Arrange
        AnalyticsDtos.AnalyticsBatchRequest emptyBatch = new AnalyticsDtos.AnalyticsBatchRequest(
                "BATCH_EMPTY", List.of()
        );

        // Act
        analyticsService.saveBatch(emptyBatch);

        // Assert
        List<CustomerEntity> customers = customerRepository.findAll();
        assertTrue(customers.isEmpty());
    }

    @Test
    @DisplayName("Should handle null batch gracefully")
    void shouldHandleNullBatch() {
        // Act & Assert: Should not throw exception
        assertDoesNotThrow(() -> analyticsService.saveBatch(null));

        List<CustomerEntity> customers = customerRepository.findAll();
        assertTrue(customers.isEmpty());
    }

    @Test
    @DisplayName("Should calculate correct total value for products")
    void shouldCalculateCorrectTotalValue() {
        // Arrange
        analyticsService.saveBatch(createSampleBatch());

        // Act
        List<Map<String, Object>> result = analyticsService.getAllCustomersWithProducts();

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get(0).get("summary");
        BigDecimal totalValue = (BigDecimal) summary.get("totalValue");

        // 999.99 + 29.99 = 1029.98
        assertEquals(0, totalValue.compareTo(new BigDecimal("1029.98")));
    }

    @Test
    @DisplayName("Should save customer via saveCustomer method")
    void shouldSaveCustomerDirectly() {
        // Act
        CustomerEntity saved = analyticsService.saveCustomer(
                "DIRECT001", "Direct", "Customer", "direct@example.com", "+1234567890"
        );

        // Assert
        assertNotNull(saved.getId());
        assertEquals("DIRECT001", saved.getExternalId());
        assertEquals("Direct Customer", saved.getName());
        assertEquals("ACTIVE", saved.getStatus());
    }

    private AnalyticsDtos.AnalyticsBatchRequest createSampleBatch() {
        AnalyticsDtos.Customer customer = new AnalyticsDtos.Customer(
                "CUST001", "John Doe", "john@example.com", "+1234567890", "ACTIVE"
        );

        List<AnalyticsDtos.Product> products = List.of(
                new AnalyticsDtos.Product("PROD001", "Laptop", "Electronics", new BigDecimal("999.99"), 50),
                new AnalyticsDtos.Product("PROD002", "Mouse", "Electronics", new BigDecimal("29.99"), 200)
        );

        AnalyticsDtos.Summary summary = new AnalyticsDtos.Summary(2, new BigDecimal("1029.98"));

        AnalyticsDtos.AnalyticsRecord record = new AnalyticsDtos.AnalyticsRecord(
                "MERGE_001", customer, products, summary, "2024-01-01T10:00:00Z"
        );

        return new AnalyticsDtos.AnalyticsBatchRequest("BATCH001", List.of(record));
    }
}
