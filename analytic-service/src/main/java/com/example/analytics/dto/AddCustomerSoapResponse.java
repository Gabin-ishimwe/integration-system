package com.example.analytics.dto;

public record AddCustomerSoapResponse(
    boolean soapSuccess,
    String customerId,
    String soapMessage,
    String timestamp
) {}
