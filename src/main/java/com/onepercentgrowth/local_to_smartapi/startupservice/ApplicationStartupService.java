package com.onepercentgrowth.local_to_smartapi.startupservice;

import com.onepercentgrowth.local_to_smartapi.service.OrderStatusWebSocketService;
import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import com.onepercentgrowth.local_to_smartapi.storage.ScripMasterStorageService;
import com.onepercentgrowth.local_to_smartapi.storage.SlOrderStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ApplicationStartupService {

    private static final Logger log =
            LoggerFactory.getLogger(ApplicationStartupService.class);

    private final ScripMasterStorageService scripMasterStorageService;
    private final ScripMasterService scripMasterService;
    private final SlOrderStore slOrderStore;
    private final OrderStatusWebSocketService orderStatusWebSocketService;

    @Value("${myapp.sl_orderstore.file-path}")
    private String slOrderBaseDir;

    public ApplicationStartupService(
            ScripMasterStorageService scripMasterStorageService,
            ScripMasterService scripMasterService,
            SlOrderStore slOrderStore,
            OrderStatusWebSocketService orderStatusWebSocketService
    ) {
        this.scripMasterStorageService = scripMasterStorageService;
        this.scripMasterService = scripMasterService;
        this.slOrderStore = slOrderStore;
        this.orderStatusWebSocketService = orderStatusWebSocketService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        log.info("🚀 Application startup sequence initiated");

        // ---- Scrip master ----
        scripMasterStorageService.loadFromFile();

        if (scripMasterStorageService.getCachedRawList() != null &&
                scripMasterStorageService.isFileFromToday()) {

            scripMasterService.setRawScripList(
                    scripMasterStorageService.getCachedRawList()
            );
        } else {
            scripMasterService.downloadRawScripMaster().block();
        }

        // ---- SL Order store ----
        slOrderStore.init(slOrderBaseDir);
        slOrderStore.loadFromFile();

        // ---- WebSocket ----
        log.info("🔌 Starting Order Status WebSocket");
        orderStatusWebSocketService.start();
    }
}

