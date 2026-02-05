package com.example.analytics.web;

import com.example.analytics.dto.AddCustomerSoapResponse;
import com.example.analytics.dto.AnalyticsDtos;
import com.example.analytics.dto.CustomerDTO;
import com.example.analytics.entity.CustomerEntity;
import com.example.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Analytics", description = "Analytics data management endpoints")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Ingest analytics batch", description = "Receives and processes a batch of merged customer-product analytics data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Batch accepted for processing"),
        @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
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

    @Operation(summary = "Get all customers", description = "Retrieves all customers with their associated products")
    @ApiResponse(responseCode = "200", description = "List of customers with products")
    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> getAnalyticsData() {
        List<Map<String, Object>> data = analyticsService.getAllCustomersWithProducts();
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Trigger full refresh", description = "Triggers a full refresh of customers and products data via consumer service")
    @ApiResponse(responseCode = "200", description = "Refresh triggered successfully")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAnalytics() {
        Map response = analyticsService.triggerRefresh();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "refresh-triggered");
        result.put("producer_response", response);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Trigger customer refresh", description = "Triggers a refresh of customer data only")
    @ApiResponse(responseCode = "200", description = "Customer refresh triggered successfully")
    @PostMapping("/refresh/customers")
    public ResponseEntity<Map<String, Object>> refreshCustomers() {
        log.info("Triggering customer refresh");
        Map response = analyticsService.triggerFetchCustomers();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "customers-refresh-triggered");
        result.put("producer_response", response);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Trigger product refresh", description = "Triggers a refresh of product data only")
    @ApiResponse(responseCode = "200", description = "Product refresh triggered successfully")
    @PostMapping("/refresh/products")
    public ResponseEntity<Map<String, Object>> refreshProducts() {
        log.info("Triggering product refresh");
        Map response = analyticsService.triggerFetchProducts();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "products-refresh-triggered");
        result.put("producer_response", response);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Add customer", description = "Creates a new customer via SOAP integration and stores it locally")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid customer data")
    })
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

    @Operation(summary = "Export customers CSV", description = "Exports all customers data as a CSV file")
    @ApiResponse(responseCode = "200", description = "CSV file download", content = @Content(mediaType = "text/csv"))
    @GetMapping("/customers/export")
    public ResponseEntity<byte[]> exportCustomersCsv() {
        String csv = analyticsService.exportCustomersToCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customers.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }

    @Operation(summary = "Export products CSV", description = "Exports all products data as a CSV file")
    @ApiResponse(responseCode = "200", description = "CSV file download", content = @Content(mediaType = "text/csv"))
    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportProductsCsv() {
        String csv = analyticsService.exportProductsToCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }
}
