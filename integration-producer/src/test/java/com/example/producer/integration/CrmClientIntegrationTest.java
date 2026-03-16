package com.example.producer.integration;

import com.example.producer.common.model.PagedResponse;
import com.example.producer.integrations.crm.client.CrmRestClient;
import com.example.producer.integrations.crm.model.Customer;
import com.example.producer.integrations.crm.service.CrmAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CrmRestClient.
 * Tests API fetching with WireMock and token caching with Redis Testcontainer.
 */
@SpringBootTest
@Testcontainers
class CrmClientIntegrationTest {

    private static WireMockServer wireMockServer;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private CrmRestClient crmRestClient;

    @Autowired
    private CrmAuthService crmAuthService;

    @Autowired
    private StringRedisTemplate redisTemplate;

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
        // Clear Redis cache
        redisTemplate.delete("crm-service:token");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("mock-service.base-url", () -> "http://localhost:8081");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // Disable RabbitMQ for these tests
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> "5672");
    }

    @Test
    @DisplayName("Should fetch customers successfully from CRM API")
    void shouldFetchCustomersSuccessfully() {
        // Arrange: Mock the auth token endpoint
        stubFor(post(urlEqualTo("/auth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"test-token\", \"expires_in\": 3600}")));

        // Arrange: Mock the customers endpoint
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
                    },
                    {
                        "customer_id": "CUST002",
                        "first_name": "Jane",
                        "last_name": "Smith",
                        "email": "jane@example.com",
                        "phone": "+9876543210",
                        "status": "ACTIVE"
                    }
                ],
                "page": 0,
                "size": 10,
                "total_elements": 2,
                "total_pages": 1
            }
            """;

        stubFor(get(urlPathEqualTo("/crm/api/customers"))
                .withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(customersResponse)));

        // Act
        PagedResponse<Customer> response = crmRestClient.getCustomers(0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("CUST001", response.getContent().get(0).getCustomerId());
        assertEquals("John", response.getContent().get(0).getFirstName());
        assertEquals("Doe", response.getContent().get(0).getLastName());
    }

    @Test
    @DisplayName("Should include Bearer token in request header")
    void shouldIncludeBearerTokenInRequest() {
        // Arrange: Mock the auth token endpoint
        stubFor(post(urlEqualTo("/auth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"my-secret-token\", \"expires_in\": 3600}")));

        // Arrange: Mock customers endpoint that verifies the token
        stubFor(get(urlPathEqualTo("/crm/api/customers"))
                .withHeader("Authorization", equalTo("Bearer my-secret-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"content\": [], \"page\": 0, \"size\": 10, \"total_elements\": 0, \"total_pages\": 0}")));

        // Act
        PagedResponse<Customer> response = crmRestClient.getCustomers(0, 10);

        // Assert
        assertNotNull(response);
        verify(getRequestedFor(urlPathEqualTo("/crm/api/customers"))
                .withHeader("Authorization", equalTo("Bearer my-secret-token")));
    }

    @Test
    @DisplayName("Should handle empty customer list")
    void shouldHandleEmptyCustomerList() {
        // Arrange
        stubFor(post(urlEqualTo("/auth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"test-token\", \"expires_in\": 3600}")));

        stubFor(get(urlPathEqualTo("/crm/api/customers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"content\": [], \"page\": 0, \"size\": 10, \"total_elements\": 0, \"total_pages\": 0}")));

        // Act
        PagedResponse<Customer> response = crmRestClient.getCustomers(0, 10);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    @DisplayName("Should pass pagination parameters correctly")
    void shouldPassPaginationParameters() {
        // Arrange
        stubFor(post(urlEqualTo("/auth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"test-token\", \"expires_in\": 3600}")));

        stubFor(get(urlPathEqualTo("/crm/api/customers"))
                .withQueryParam("page", equalTo("2"))
                .withQueryParam("size", equalTo("25"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"content\": [], \"page\": 2, \"size\": 25, \"totalElements\": 50, \"totalPages\": 2}")));

        // Act
        PagedResponse<Customer> response = crmRestClient.getCustomers(2, 25);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getPage());
        assertEquals(25, response.getSize());
    }
}
