package com.example.producer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("category")
    private String category;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("stock_level")
    private Integer stockLevel;

    @JsonProperty("last_updated")
    private Instant lastUpdated;
}
