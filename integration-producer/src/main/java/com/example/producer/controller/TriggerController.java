package com.example.producer.controller;

import com.example.producer.client.CrmClient;
import com.example.producer.client.CrmSoapClient;
import com.example.producer.client.InventoryClient;
import com.example.producer.model.Customer;
import com.example.producer.model.PagedResponse;
import com.example.producer.model.Product;
import com.example.producer.service.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trigger")
public class TriggerController {

    private static final Logger log = LoggerFactory.getLogger(TriggerController.class);

    private final CrmClient crmClient;
    private final CrmSoapClient crmSoapClient;
    private final InventoryClient inventoryClient;
    private final MessagePublisher messagePublisher;

    public TriggerController(CrmClient crmClient, CrmSoapClient crmSoapClient,
                             InventoryClient inventoryClient, MessagePublisher messagePublisher) {
        this.crmClient = crmClient;
        this.crmSoapClient = crmSoapClient;
        this.inventoryClient = inventoryClient;
        this.messagePublisher = messagePublisher;
    }

    @PostMapping("/fetch-all")
    public ResponseEntity<Map<String, Object>> triggerFetchAll() {
        log.info("Manual trigger: fetching all data");

        int customersPublished = fetchAndPublishCustomers();
        int productsPublished = fetchAndPublishProducts();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("customers_published", customersPublished);
        result.put("products_published", productsPublished);
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/fetch-customers")
    public ResponseEntity<Map<String, Object>> triggerFetchCustomers() {
        log.info("Manual trigger: fetching customers");

        int count = fetchAndPublishCustomers();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("customers_published", count);
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/fetch-products")
    public ResponseEntity<Map<String, Object>> triggerFetchProducts() {
        log.info("Manual trigger: fetching products");

        int count = fetchAndPublishProducts();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("products_published", count);
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-customer-soap")
    public ResponseEntity<Map<String, Object>> addCustomerViaSoap(@RequestBody Map<String, String> request) {
        log.info("Manual trigger: adding customer via SOAP");

        String firstName = request.getOrDefault("first_name", "John");
        String lastName = request.getOrDefault("last_name", "Doe");
        String email = request.getOrDefault("email", "john.doe@example.com");
        String phone = request.getOrDefault("phone", "+1234567890");

        // Step 1: Add customer via SOAP
        CrmSoapClient.SoapResponse soapResponse = crmSoapClient.addCustomer(firstName, lastName, email, phone);

        Map<String, Object> result = new HashMap<>();
        result.put("soap_success", soapResponse.success());
        result.put("customer_id", soapResponse.customerId());
        result.put("soap_message", soapResponse.message());
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-customer-and-fetch")
    public ResponseEntity<Map<String, Object>> addCustomerAndFetch(@RequestBody Map<String, String> request) {
        log.info("Manual trigger: adding customer via SOAP and re-fetching");

        String firstName = request.getOrDefault("first_name", "John");
        String lastName = request.getOrDefault("last_name", "Doe");
        String email = request.getOrDefault("email", "john.doe@example.com");
        String phone = request.getOrDefault("phone", "+1234567890");

        // Step 1: Add customer via SOAP
        CrmSoapClient.SoapResponse soapResponse = crmSoapClient.addCustomer(firstName, lastName, email, phone);

        // Step 2: Re-fetch customers and publish to queue
        int customersPublished = 0;
        if (soapResponse.success()) {
            customersPublished = fetchAndPublishCustomers();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("step1_soap_add", Map.of(
            "success", soapResponse.success(),
            "customer_id", soapResponse.customerId() != null ? soapResponse.customerId() : "N/A",
            "message", soapResponse.message() != null ? soapResponse.message() : "N/A"
        ));
        result.put("step2_fetch_customers", Map.of(
            "success", customersPublished > 0,
            "customers_published", customersPublished
        ));
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    private int fetchAndPublishCustomers() {
        try {
            PagedResponse<Customer> response = crmClient.getCustomers(0, 100);
            for (Customer customer : response.getContent()) {
                messagePublisher.publishCustomer(customer);
            }
            log.info("Published {} customers", response.getContent().size());
            return response.getContent().size();
        } catch (Exception e) {
            log.error("Failed to fetch/publish customers", e);
            return 0;
        }
    }

    private int fetchAndPublishProducts() {
        try {
            PagedResponse<Product> response = inventoryClient.getProducts(0, 100);
            for (Product product : response.getContent()) {
                messagePublisher.publishProduct(product);
            }
            log.info("Published {} products", response.getContent().size());
            return response.getContent().size();
        } catch (Exception e) {
            log.error("Failed to fetch/publish products", e);
            return 0;
        }
    }
}
