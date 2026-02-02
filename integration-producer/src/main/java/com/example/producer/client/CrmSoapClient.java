package com.example.producer.client;

import com.example.producer.soap.generated.AddCustomerRequest;
import com.example.producer.soap.generated.AddCustomerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

@Component
public class CrmSoapClient {

    private static final Logger log = LoggerFactory.getLogger(CrmSoapClient.class);

    private final WebServiceTemplate webServiceTemplate;

    @Value("${mock-service.base-url}")
    private String mockServiceUrl;

    public CrmSoapClient(WebServiceTemplate crmWebServiceTemplate) {
        this.webServiceTemplate = crmWebServiceTemplate;
    }

    public SoapResponse addCustomer(String firstName, String lastName, String email, String phone) {
        log.info("Sending SOAP AddCustomer request for: {} {}", firstName, lastName);

        try {
            AddCustomerRequest request = new AddCustomerRequest();
            request.setFirstName(firstName);
            request.setLastName(lastName);
            request.setEmail(email);
            request.setPhone(phone);

            String endpoint = mockServiceUrl + "/crm/soap/customers";

            AddCustomerResponse response = (AddCustomerResponse) webServiceTemplate.marshalSendAndReceive(
                endpoint,
                request
            );

            log.info("SOAP AddCustomer response - Status: {}, CustomerId: {}",
                response.getStatus(), response.getCustomerId());

            boolean success = "SUCCESS".equalsIgnoreCase(response.getStatus());
            return new SoapResponse(success, response.getCustomerId(), response.getMessage());

        } catch (Exception e) {
            log.error("SOAP AddCustomer failed", e);
            return new SoapResponse(false, null, e.getMessage());
        }
    }

    public record SoapResponse(boolean success, String customerId, String message) {}
}
