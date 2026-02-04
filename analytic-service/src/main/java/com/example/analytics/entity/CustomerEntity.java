package com.example.analytics.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External customer id coming from upstream systems (e.g. CUST_001).
     */
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "status")
    private String status;

    @Column(name = "last_analytics_timestamp")
    private Instant lastAnalyticsTimestamp;

    @Column(name = "last_batch_number")
    private String lastBatchNumber;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductEntity> products = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastAnalyticsTimestamp() {
        return lastAnalyticsTimestamp;
    }

    public void setLastAnalyticsTimestamp(Instant lastAnalyticsTimestamp) {
        this.lastAnalyticsTimestamp = lastAnalyticsTimestamp;
    }

    public String getLastBatchNumber() {
        return lastBatchNumber;
    }

    public void setLastBatchNumber(String lastBatchNumber) {
        this.lastBatchNumber = lastBatchNumber;
    }

    public List<ProductEntity> getProducts() {
        return products;
    }

    public void setProducts(List<ProductEntity> products) {
        this.products = products;
    }
}

