package com.example.producer.model;

public record AddCustomerSoapResponse(
    boolean soapSuccess,
    String customerId,
    String soapMessage,
    String timestamp
) {}
