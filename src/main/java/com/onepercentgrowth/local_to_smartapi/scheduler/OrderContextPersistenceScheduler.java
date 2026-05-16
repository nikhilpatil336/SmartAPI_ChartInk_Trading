package com.onepercentgrowth.local_to_smartapi.scheduler;

import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.storage.OrderContextStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderContextPersistenceScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(OrderContextPersistenceScheduler.class);

    private final OrderRegistry registry;
    private final OrderContextStorageService storageService;

    public OrderContextPersistenceScheduler(
            OrderRegistry registry,
            OrderContextStorageService storageService
    ) {
        this.registry = registry;
        this.storageService = storageService;
    }

    /**
     * 🔥 Save every 10 seconds (adjust as needed)
     */
    @Scheduled(fixedDelayString = "${myapp.ordercontext-save-interval-ms}")
    public void persistOrderContexts() {

        try {
            storageService.saveToday();

        } catch (Exception e) {
            log.error("❌ Failed to persist OrderContexts | error: {}", e.getMessage());
        }
    }
}
