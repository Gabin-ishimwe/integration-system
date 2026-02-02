package com.example.producer.controller;

import com.example.producer.client.CrmClient;
import com.example.producer.client.CrmSoapClient;
import com.example.producer.client.InventoryClient;
import com.example.producer.model.Customer;
import com.example.producer.model.PagedResponse;
import com.example.producer.model.Product;
import com.example.producer.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class ClientTestController {

    private static final Logger log = LoggerFactory.getLogger(ClientTestController.class);

    private final AuthService authService;
    private final CrmClient crmClient;
    private final InventoryClient inventoryClient;
    private final CrmSoapClient crmSoapClient;

    public ClientTestController(AuthService authService, CrmClient crmClient,
                                InventoryClient inventoryClient, CrmSoapClient crmSoapClient) {
        this.authService = authService;
        this.crmClient = crmClient;
        this.inventoryClient = inventoryClient;
        this.crmSoapClient = crmSoapClient;
    }

    @GetMapping("/auth/token")
    public ResponseEntity<Map<String, Object>> testAuthToken() {
        log.info("Testing auth token endpoint");

        Map<String, Object> result = new HashMap<>();
        try {
            String token = authService.getToken();
            result.put("success", true);
            result.put("token", token);
            result.put("token_length", token.length());
        } catch (Exception e) {
            log.error("Auth token test failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/auth/clear-cache")
    public ResponseEntity<Map<String, Object>> clearTokenCache() {
        log.info("Clearing token cache");

        authService.clearTokenCache();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Token cache cleared");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/crm/customers")
    public ResponseEntity<Map<String, Object>> testCrmCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Testing CRM customers endpoint - page: {}, size: {}", page, size);

        Map<String, Object> result = new HashMap<>();
        try {
            PagedResponse<Customer> response = crmClient.getCustomers(page, size);
            result.put("success", true);
            result.put("total_elements", response.getTotalElements());
            result.put("total_pages", response.getTotalPages());
            result.put("page", response.getPage());
            result.put("size", response.getSize());
            result.put("content", response.getContent());
        } catch (Exception e) {
            log.error("CRM customers test failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/inventory/products")
    public ResponseEntity<Map<String, Object>> testInventoryProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Testing Inventory products endpoint - page: {}, size: {}", page, size);

        Map<String, Object> result = new HashMap<>();
        try {
            PagedResponse<Product> response = inventoryClient.getProducts(page, size);
            result.put("success", true);
            result.put("total_elements", response.getTotalElements());
            result.put("total_pages", response.getTotalPages());
            result.put("page", response.getPage());
            result.put("size", response.getSize());
            result.put("content", response.getContent());
        } catch (Exception e) {
            log.error("Inventory products test failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/crm/soap/add-customer")
    public ResponseEntity<Map<String, Object>> testSoapAddCustomer(@RequestBody AddCustomerRequest request) {
        log.info("Testing SOAP AddCustomer endpoint");

        Map<String, Object> result = new HashMap<>();
        try {
            CrmSoapClient.SoapResponse response = crmSoapClient.addCustomer(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone()
            );

            result.put("success", response.success());
            result.put("customer_id", response.customerId());
            result.put("message", response.message());
        } catch (Exception e) {
            log.error("SOAP AddCustomer test failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> testAllClients() {
        log.info("Testing all client connections");

        Map<String, Object> result = new HashMap<>();

        // Test Auth
        Map<String, Object> authResult = new HashMap<>();
        try {
            String token = authService.getToken();
            authResult.put("status", "OK");
            authResult.put("token_obtained", true);
        } catch (Exception e) {
            authResult.put("status", "FAILED");
            authResult.put("error", e.getMessage());
        }
        result.put("auth", authResult);

        // Test CRM
        Map<String, Object> crmResult = new HashMap<>();
        try {
            PagedResponse<Customer> response = crmClient.getCustomers(0, 1);
            crmResult.put("status", "OK");
            crmResult.put("total_customers", response.getTotalElements());
        } catch (Exception e) {
            crmResult.put("status", "FAILED");
            crmResult.put("error", e.getMessage());
        }
        result.put("crm", crmResult);

        // Test Inventory
        Map<String, Object> inventoryResult = new HashMap<>();
        try {
            PagedResponse<Product> response = inventoryClient.getProducts(0, 1);
            inventoryResult.put("status", "OK");
            inventoryResult.put("total_products", response.getTotalElements());
        } catch (Exception e) {
            inventoryResult.put("status", "FAILED");
            inventoryResult.put("error", e.getMessage());
        }
        result.put("inventory", inventoryResult);

        // Test SOAP (with dummy data)
        Map<String, Object> soapResult = new HashMap<>();
        try {
            CrmSoapClient.SoapResponse response = crmSoapClient.addCustomer(
                "Test", "User", "test@example.com", "+1234567890"
            );
            soapResult.put("status", response.success() ? "OK" : "FAILED");
            soapResult.put("response_message", response.message());
        } catch (Exception e) {
            soapResult.put("status", "FAILED");
            soapResult.put("error", e.getMessage());
        }
        result.put("soap", soapResult);

        return ResponseEntity.ok(result);
    }

    public record AddCustomerRequest(
        String firstName,
        String lastName,
        String email,
        String phone
    ) {}
}
