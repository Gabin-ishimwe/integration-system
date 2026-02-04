package com.example.analytics.web;

import com.example.analytics.dto.AnalyticsDtos;
import com.example.analytics.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/analytics/api")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Endpoint that consumer-service posts its merged batch to.
     * Expected body:
     * {
     *   "batchNumber": "BATCH_001",
     *   "data": [ { merge_id, customer, products, summary, timestamp }, ... ]
     * }
     */
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

    /**
     * Simple endpoint to read the latest cached analytics batch from Redis.
     */
    @GetMapping("/data/latest")
    public ResponseEntity<?> getLatestAnalytics() {
        String cached = analyticsService.getLatestCachedBatch();
        if (cached == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(cached);
    }

    /**
     * Endpoint to request a full refresh of analytics data.
     * This will call integration-producer, which in turn calls mock-service
     * and publishes new data onto RabbitMQ for consumer-service to process.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAnalytics() {
        analyticsService.triggerRefresh();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "refresh-triggered");
        return ResponseEntity.ok(result);
    }
}

