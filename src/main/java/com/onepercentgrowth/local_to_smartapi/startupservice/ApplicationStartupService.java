package com.onepercentgrowth.local_to_smartapi.startupservice;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.service.*;
import com.onepercentgrowth.local_to_smartapi.storage.LeverageStorageService;
import com.onepercentgrowth.local_to_smartapi.storage.ScripMasterStorageService;
import com.onepercentgrowth.local_to_smartapi.storage.SlOrderStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ApplicationStartupService {

    private static final Logger log =
            LoggerFactory.getLogger(ApplicationStartupService.class);

    private final ScripMasterStorageService scripMasterStorageService;
    private final ScripMasterService scripMasterService;
    private final SlOrderStore slOrderStore;
    private final OrderStatusWebSocketService orderStatusWebSocketService;
    private final TokenManager tokenManager;
    private final RmsService rmsService;
    private final BalanceService balanceService;
    private final LeverageStorageService leverageStorageService;
    private final LeverageService leverageService;

    @Value("${myapp.sl-orderstore-file-path}")
    private String slOrderBaseDir;

    public ApplicationStartupService(
            ScripMasterStorageService scripMasterStorageService,
            ScripMasterService scripMasterService,
            SlOrderStore slOrderStore,
            OrderStatusWebSocketService orderStatusWebSocketService,
            TokenManager tokenManager,
            RmsService rmsService,
            BalanceService balanceService,
            LeverageStorageService leverageStorageService,
            LeverageService leverageService
    ) {
        this.scripMasterStorageService = scripMasterStorageService;
        this.scripMasterService = scripMasterService;
        this.slOrderStore = slOrderStore;
        this.orderStatusWebSocketService = orderStatusWebSocketService;
        this.tokenManager = tokenManager;
        this.rmsService = rmsService;
        this.balanceService = balanceService;
        this.leverageStorageService = leverageStorageService;
        this.leverageService = leverageService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        log.info("Application startup sequence initiated");

        // ---- Scrip master ----
//        scripMasterStorageService.loadFromFile();
//
//        if (scripMasterStorageService.getCachedRawList() != null &&
//                scripMasterStorageService.isFileFromToday()) {
//
//            scripMasterService.setRawScripList(
//                    scripMasterStorageService.getCachedRawList()
//            );
//        } else {
//            scripMasterService.setRawScripList(scripMasterService.downloadRawScripMaster().block());
//        }

        scripMasterStorageService.loadFilteredScripmasterFromFile();

        if (scripMasterStorageService.getCachedFilteredList() != null &&
                scripMasterStorageService.isFileFromToday()) {

            scripMasterService.setNseEquityMap(
                    scripMasterStorageService.getCachedFilteredList()
            );
        } else {
            scripMasterService.setNseEquityMap(scripMasterService.downloadFilteredScripMaster().block());
        }

        // ---- SL Order store ----
        slOrderStore.init(slOrderBaseDir);
        slOrderStore.loadFromFile();

        // ---- WebSocket ----
        log.info("Starting Order Status WebSocket");
        orderStatusWebSocketService.start();

        // ---- RMS BALANCE ----
//        log.info("Fetching RMS balance on startup");
//
//        Mono.fromRunnable(() -> tokenManager.getValidJwtToken())
//                .then(scripMasterService.getCurrentBalance())
//                .doOnSuccess(resp ->
//                        log.info("RMS balance loaded successfully on startup")
//                )
//                .doOnError(err ->
//                        log.error("Failed to fetch RMS on startup", err)
//                )
//                .subscribe();

        log.info("Fetching RMS balance on startup");

        Mono.fromRunnable(() -> tokenManager.getValidJwtToken())
                .then(rmsService.refreshNow())
                .doOnSuccess(rms ->
                        log.info("RMS loaded and balance synced on startup")
                )
                .doOnError(err ->
                        log.error("Failed to fetch RMS on startup", err)
                )
                .subscribe();

        leverageStorageService.load();

        if (leverageStorageService.isFileFromToday()) {
            leverageService.loadFromCache();
        } else {
            leverageService.refreshNow()
                    .doOnError(e -> log.error("Leverage refresh failed", e))
                    .subscribe();
        }
    }
}

