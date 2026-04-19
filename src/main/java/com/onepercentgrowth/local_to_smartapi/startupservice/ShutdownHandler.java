package com.onepercentgrowth.local_to_smartapi.startupservice;

import com.onepercentgrowth.local_to_smartapi.storage.OrderContextStorageService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class ShutdownHandler {

    private final OrderContextStorageService storage;

    public ShutdownHandler(OrderContextStorageService storage) {
        this.storage = storage;
    }

    @PreDestroy
    public void onShutdown() {
        storage.saveToday();
    }
}
