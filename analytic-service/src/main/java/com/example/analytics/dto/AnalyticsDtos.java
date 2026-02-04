package com.example.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public class AnalyticsDtos {

    public record AnalyticsBatchRequest(
        String batchNumber,
        List<AnalyticsRecord> data
    ) {}

    public record AnalyticsRecord(
        String merge_id,
        Customer customer,
        List<Product> products,
        Summary summary,
        String timestamp
    ) {}

    public record Customer(
        String id,
        String name,
        String email,
        String phone,
        String status
    ) {}

    public record Product(
        String id,
        String name,
        String category,
        BigDecimal price,
        Integer stock_level
    ) {}

    public record Summary(
        Integer total_products,
        BigDecimal total_value
    ) {}
}

