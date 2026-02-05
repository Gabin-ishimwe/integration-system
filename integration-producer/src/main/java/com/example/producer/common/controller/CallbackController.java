package com.example.producer.common.controller;

import com.example.producer.integrations.crm.model.AddCustomerSoapResponse;
import com.example.producer.integrations.crm.service.CrmService;
import com.example.producer.integrations.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/callback")
@Tag(name = "Callback", description = "Callback endpoints to trigger data fetching and publishing")
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);

    private final CrmService crmService;
    private final InventoryService inventoryService;

    public CallbackController(CrmService crmService, InventoryService inventoryService) {
        this.crmService = crmService;
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Fetch all data", description = "Fetches customers from CRM and products from Inventory, then publishes both to RabbitMQ")
    @ApiResponse(responseCode = "200", description = "Data fetched and published successfully")
    @PostMapping("/fetch-all")
    public ResponseEntity<Map<String, Object>> fetchAll() {
        log.info("Callback: fetching all data");

        int customersPublished = crmService.fetchAndPublishCustomers(0, 100);
        int productsPublished = inventoryService.fetchAndPublishProducts(0, 100);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("customers_published", customersPublished);
        result.put("products_published", productsPublished);
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Fetch customers", description = "Fetches customers from CRM service and publishes to RabbitMQ customer queue")
    @ApiResponse(responseCode = "200", description = "Customers fetched and published successfully")
    @PostMapping("/fetch-customers")
    public ResponseEntity<Map<String, Object>> fetchCustomers() {
        log.info("Callback: fetching customers");

        int count = crmService.fetchAndPublishCustomers(0, 100);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("customers_published", count);
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Fetch products", description = "Fetches products from Inventory service and publishes to RabbitMQ product queue")
    @ApiResponse(responseCode = "200", description = "Products fetched and published successfully")
    @PostMapping("/fetch-products")
    public ResponseEntity<Map<String, Object>> fetchProducts() {
        log.info("Callback: fetching products");

        int count = inventoryService.fetchAndPublishProducts(0, 100);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("products_published", count);
        result.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-customer-soap")
    public ResponseEntity<AddCustomerSoapResponse> addCustomerViaSoap(@RequestBody Map<String, String> request) {
        log.info("Callback: adding customer via SOAP");

        String firstName = request.get("first_name");
        String lastName = request.get("last_name");
        String email = request.get("email");
        String phone = request.get("phone");

        AddCustomerSoapResponse response = crmService.addCustomerViaSoap(firstName, lastName, email, phone);

        return ResponseEntity.ok(response);
    }
}
