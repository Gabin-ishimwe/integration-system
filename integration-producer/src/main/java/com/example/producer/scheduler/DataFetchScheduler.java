package com.example.producer.scheduler;

import com.example.producer.client.CrmClient;
import com.example.producer.client.InventoryClient;
import com.example.producer.model.Customer;
import com.example.producer.model.PagedResponse;
import com.example.producer.model.Product;
import com.example.producer.service.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DataFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataFetchScheduler.class);

    private final CrmClient crmClient;
    private final InventoryClient inventoryClient;
    private final MessagePublisher messagePublisher;

    public DataFetchScheduler(CrmClient crmClient, InventoryClient inventoryClient,
                              MessagePublisher messagePublisher) {
        this.crmClient = crmClient;
        this.inventoryClient = inventoryClient;
        this.messagePublisher = messagePublisher;
    }

    @Scheduled(cron = "${scheduler.customer-fetch-cron}")
    public void fetchAndPublishCustomers() {
        log.info("Starting customer data fetch");

        try {
            PagedResponse<Customer> response = crmClient.getCustomers(0, 100);

            for (Customer customer : response.getContent()) {
                messagePublisher.publishCustomer(customer);
            }

            log.info("Published {} customers to queue", response.getContent().size());
        } catch (Exception e) {
            log.error("Failed to fetch/publish customers", e);
        }
    }

    @Scheduled(cron = "${scheduler.inventory-fetch-cron}")
    public void fetchAndPublishProducts() {
        log.info("Starting product data fetch");

        try {
            PagedResponse<Product> response = inventoryClient.getProducts(0, 100);

            for (Product product : response.getContent()) {
                messagePublisher.publishProduct(product);
            }

            log.info("Published {} products to queue", response.getContent().size());
        } catch (Exception e) {
            log.error("Failed to fetch/publish products", e);
        }
    }
}
