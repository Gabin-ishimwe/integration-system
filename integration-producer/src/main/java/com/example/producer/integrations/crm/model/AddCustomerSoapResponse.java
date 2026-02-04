package com.example.producer.integrations.crm.model;

public record AddCustomerSoapResponse(
    boolean soapSuccess,
    String customerId,
    String soapMessage,
    String timestamp
) {}
