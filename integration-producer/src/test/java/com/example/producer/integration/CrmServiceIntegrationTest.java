package com.example.producer.integration;

import com.example.producer.integrations.crm.model.Customer;
import com.example.producer.integrations.crm.model.CustomerMessage;
import com.example.producer.integrations.crm.service.CrmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CrmService.
 * Tests the full flow: fetch from API -> publish to RabbitMQ.
 */
@SpringBootTest
@Testcontainers
class CrmServiceIntegrationTest {

    private static WireMockServer wireMockServer;

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private CrmService crmService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8081);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8081);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("mock-service.base-url", () -> "http://localhost:8081");
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    @DisplayName("Should publish customers to RabbitMQ queue")
    void shouldPublishCustomersToRabbitMQ() {
        // Arrange
        List<Customer> customers = Arrays.asList(
                createCustomer("CUST001", "John", "Doe", "john@example.com"),
                createCustomer("CUST002", "Jane", "Smith", "jane@example.com")
        );

        // Act
        crmService.publishCustomers(customers);

        // Assert: Verify message was published to the queue
        Object received = rabbitTemplate.receiveAndConvert("customer.data.queue", 5000);
        assertNotNull(received, "Message should be received from queue");
        assertTrue(received instanceof CustomerMessage);

        CustomerMessage message = (CustomerMessage) received;
        assertEquals(2, message.getData().size());
        assertEquals("integration-producer", message.getSource());
        assertNotNull(message.getCorrelationId());
        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("Should fetch and publish customers successfully")
    void shouldFetchAndPublishCustomers() {
        // Arrange: Mock auth endpoint
        stubFor(post(urlEqualTo("/auth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"test-token\", \"expires_in\": 3600}")));

        // Arrange: Mock customers endpoint
        String customersResponse = """
            {
                "content": [
                    {
                        "customer_id": "CUST001",
                        "first_name": "John",
                        "last_name": "Doe",
                        "email": "john@example.com",
                        "phone": "+1234567890",
                        "status": "ACTIVE"
                    }
                ],
                "page": 0,
                "size": 10,
                "total_elements": 1,
                "total_pages": 1
            }
            """;

        stubFor(get(urlPathEqualTo("/crm/api/customers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(customersResponse)));

        // Act
        int count = crmService.fetchAndPublishCustomers(0, 10);

        // Assert
        assertEquals(1, count);

        // Verify message in queue
        Object received = rabbitTemplate.receiveAndConvert("customer.data.queue", 5000);
        assertNotNull(received);
        assertTrue(received instanceof CustomerMessage);

        CustomerMessage message = (CustomerMessage) received;
        assertEquals(1, message.getData().size());
        assertEquals("CUST001", message.getData().get(0).getCustomerId());
    }

    @Test
    @DisplayName("Should handle API error gracefully and return zero")
    void shouldHandleApiErrorGracefully() {
        // Arrange: Mock auth endpoint
        stubFor(post(urlEqualTo("/auth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"test-token\", \"expires_in\": 3600}")));

        // Arrange: Mock customers endpoint to return error
        stubFor(get(urlPathEqualTo("/crm/api/customers"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act
        int count = crmService.fetchAndPublishCustomers(0, 10);

        // Assert: Should return 0 on error, not throw exception
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Should include correct metadata in published message")
    void shouldIncludeCorrectMetadataInMessage() {
        // Arrange
        List<Customer> customers = List.of(
                createCustomer("CUST001", "Test", "User", "test@example.com")
        );

        // Act
        crmService.publishCustomers(customers);

        // Assert
        Object received = rabbitTemplate.receiveAndConvert("customer.data.queue", 5000);
        assertNotNull(received);

        CustomerMessage message = (CustomerMessage) received;

        // Verify metadata
        assertNotNull(message.getCorrelationId(), "Should have correlationId");
        assertFalse(message.getCorrelationId().isEmpty());
        assertNotNull(message.getTimestamp(), "Should have timestamp");
        assertEquals("integration-producer", message.getSource());
    }

    private Customer createCustomer(String id, String firstName, String lastName, String email) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone("+1234567890");
        customer.setStatus("ACTIVE");
        return customer;
    }
}
