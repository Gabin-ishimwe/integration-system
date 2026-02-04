package com.example.producer.controller;

import com.example.producer.service.CallbackService;
import com.example.producer.service.CallbackService.CallbackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/callback")
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);

    private final CallbackService callbackService;

    public CallbackController(CallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @PostMapping("/fetch")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> fetchData() {
        log.info("Callback received - fetching data async");

        return callbackService.fetchAndPublishAsync()
            .thenApply(result -> {
                Map<String, Object> response = Map.of(
                    "status", "completed",
                    "customers_published", result.customersCount(),
                    "products_published", result.productsCount(),
                    "timestamp", Instant.now().toString()
                );
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                log.error("Callback fetch failed", ex);
                Map<String, Object> errorResponse = Map.of(
                    "status", "failed",
                    "error", ex.getMessage(),
                    "timestamp", Instant.now().toString()
                );
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    @PostMapping("/fetch-fire-and-forget")
    public ResponseEntity<Map<String, Object>> fetchDataFireAndForget() {
        log.info("Callback received - fetching data async (fire and forget)");

        callbackService.fetchAndPublishAsync()
            .thenAccept(result ->
                log.info("Fire-and-forget completed: customers={}, products={}",
                    result.customersCount(), result.productsCount())
            )
            .exceptionally(ex -> {
                log.error("Fire-and-forget failed", ex);
                return null;
            });

        Map<String, Object> response = Map.of(
            "status", "accepted",
            "message", "Request accepted, processing in background",
            "timestamp", Instant.now().toString()
        );

        return ResponseEntity.accepted().body(response);
    }
}
