package com.example.producer.integrations.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration
public class CrmSoapConfig {

    @Value("${mock-service.base-url}")
    private String mockServiceUrl;

    @Bean
    public Jaxb2Marshaller crmMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.example.producer.soap.generated");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate crmWebServiceTemplate(Jaxb2Marshaller crmMarshaller) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(crmMarshaller);
        template.setUnmarshaller(crmMarshaller);
        template.setDefaultUri(mockServiceUrl + "/crm/soap/customers");
        return template;
    }
}
