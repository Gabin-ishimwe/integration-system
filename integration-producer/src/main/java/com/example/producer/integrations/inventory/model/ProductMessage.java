package com.example.producer.integrations.inventory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("source")
    private String source;

    @JsonProperty("data")
    private List<Product> data;
}
