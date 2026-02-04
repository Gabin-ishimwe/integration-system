package com.example.analytics.web;

import com.example.analytics.dto.AnalyticsDtos;
import com.example.analytics.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics/api")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> ingestAnalytics(@RequestBody AnalyticsDtos.AnalyticsBatchRequest batch) {
        log.info("Received analytics batch, batchNumber={}", batch.batchNumber());
        analyticsService.saveBatch(batch);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "accepted");
        result.put("batchNumber", batch.batchNumber());
        result.put("records", batch.data() != null ? batch.data().size() : 0);
        return ResponseEntity.accepted().body(result);
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> getAnalyticsData() {
        List<Map<String, Object>> data = analyticsService.getAllCustomersWithProducts();
        return ResponseEntity.ok(data);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAnalytics() {
        analyticsService.triggerRefresh();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "refresh-triggered");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/customers")
    public ResponseEntity<Map<String, Object>> addCustomer(@RequestBody Map<String, String> request) {
        String firstName = request.getOrDefault("first_name", "");
        String lastName = request.getOrDefault("last_name", "");
        String email = request.getOrDefault("email", "");
        String phone = request.getOrDefault("phone", "");

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "first_name, last_name, and email are required");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> soapResponse = analyticsService.addCustomerViaSoap(firstName, lastName, email, phone);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "created");
        result.put("soap_response", soapResponse);
        return ResponseEntity.ok(result);
    }
}
