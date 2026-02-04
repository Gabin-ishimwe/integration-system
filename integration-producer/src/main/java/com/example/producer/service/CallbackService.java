package com.example.producer.service;

import com.example.producer.client.CrmClient;
import com.example.producer.client.InventoryClient;
import com.example.producer.model.Customer;
import com.example.producer.model.PagedResponse;
import com.example.producer.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class CallbackService {

    private static final Logger log = LoggerFactory.getLogger(CallbackService.class);

    private final CrmClient crmClient;
    private final InventoryClient inventoryClient;
    private final MessagePublisher messagePublisher;
    private final Executor executor;

    public CallbackService(
            CrmClient crmClient,
            InventoryClient inventoryClient,
            MessagePublisher messagePublisher,
            @Qualifier("callbackExecutor") Executor executor) {
        this.crmClient = crmClient;
        this.inventoryClient = inventoryClient;
        this.messagePublisher = messagePublisher;
        this.executor = executor;
    }

    public CompletableFuture<CallbackResult> fetchAndPublishAsync() {
        log.info("Starting async fetch from CRM and Inventory");

        CompletableFuture<List<Customer>> customersFuture = fetchCustomersAsync();
        CompletableFuture<List<Product>> productsFuture = fetchProductsAsync();

        return customersFuture.thenCombine(productsFuture, (customers, products) -> {
            log.info("Both fetches completed. Customers: {}, Products: {}",
                customers.size(), products.size());

            messagePublisher.publishCustomers(customers);
            messagePublisher.publishProducts(products);

            return new CallbackResult(customers.size(), products.size());
        });
    }

    private CompletableFuture<List<Customer>> fetchCustomersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Fetching customers async");
            PagedResponse<Customer> response = crmClient.getCustomers(0, 100);
            return response.getContent();
        }, executor);
    }

    private CompletableFuture<List<Product>> fetchProductsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Fetching products async");
            PagedResponse<Product> response = inventoryClient.getProducts(0, 100);
            return response.getContent();
        }, executor);
    }

    public record CallbackResult(int customersCount, int productsCount) {}
}
