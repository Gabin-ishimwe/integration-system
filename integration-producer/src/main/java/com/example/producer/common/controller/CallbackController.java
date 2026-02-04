package com.example.producer.common.controller;

import com.example.producer.integrations.crm.service.CrmService;
import com.example.producer.integrations.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/callback")
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);

    private final CrmService crmService;
    private final InventoryService inventoryService;

    public CallbackController(CrmService crmService, InventoryService inventoryService) {
        this.crmService = crmService;
        this.inventoryService = inventoryService;
    }

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
}
