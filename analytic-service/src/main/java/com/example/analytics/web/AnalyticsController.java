package com.example.analytics.web;

import com.example.analytics.dto.AddCustomerSoapResponse;
import com.example.analytics.dto.AnalyticsDtos;
import com.example.analytics.dto.CustomerDTO;
import com.example.analytics.entity.CustomerEntity;
import com.example.analytics.service.AnalyticsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        Map response = analyticsService.triggerRefresh();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "refresh-triggered");
        result.put("producer_response", response);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh/customers")
    public ResponseEntity<Map<String, Object>> refreshCustomers() {
        log.info("Triggering customer refresh");
        Map response = analyticsService.triggerFetchCustomers();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "customers-refresh-triggered");
        result.put("producer_response", response);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh/products")
    public ResponseEntity<Map<String, Object>> refreshProducts() {
        log.info("Triggering product refresh");
        Map response = analyticsService.triggerFetchProducts();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "products-refresh-triggered");
        result.put("producer_response", response);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/customers")
    public ResponseEntity<Map<String, Object>> addCustomer(@Valid @RequestBody CustomerDTO request) {
        AddCustomerSoapResponse soapResponse = analyticsService.addCustomerViaSoap(
            request.first_name(), request.last_name(), request.email(), request.phone());

        String customerId = soapResponse.customerId() != null ? soapResponse.customerId() : "CUST_UNKNOWN";

        CustomerEntity saved = analyticsService.saveCustomer(
            customerId, request.first_name(), request.last_name(), request.email(), request.phone());

        Map<String, Object> result = new HashMap<>();
        result.put("status", "created");
        result.put("customer_id", saved.getExternalId());
        result.put("soap_success", soapResponse.soapSuccess());
        result.put("soap_message", soapResponse.soapMessage());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customers/export")
    public ResponseEntity<byte[]> exportCustomersCsv() {
        String csv = analyticsService.exportCustomersToCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customers.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }

    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportProductsCsv() {
        String csv = analyticsService.exportProductsToCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }
}
