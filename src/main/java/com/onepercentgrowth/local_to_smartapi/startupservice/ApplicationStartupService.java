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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final FnoUniverseService fnoUniverseService;
    private final ApplicationProperties applicationProperties;

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
            LeverageService leverageService,
            FnoUniverseService fnoUniverseService,
            ApplicationProperties applicationProperties
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
        this.fnoUniverseService = fnoUniverseService;
        this.applicationProperties = applicationProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        log.info("Application startup sequence initiated");

//        // ---- Scrip master ----
////        scripMasterStorageService.loadFromFile();
////
////        if (scripMasterStorageService.getCachedRawList() != null &&
////                scripMasterStorageService.isFileFromToday()) {
////
////            scripMasterService.setRawScripList(
////                    scripMasterStorageService.getCachedRawList()
////            );
////        } else {
////            scripMasterService.setRawScripList(scripMasterService.downloadRawScripMaster().block());
////        }
//
////        Set<String> fnoSet = fnoUniverseService.loadCached();
////        log.info("FNO universe loaded on startup. Size={}", fnoSet.size());
//
//        scripMasterStorageService.loadFilteredScripmasterFromFile();
//
//        if (scripMasterStorageService.getCachedFilteredList() != null &&
//                scripMasterStorageService.isFileFromToday()) {
//
//            scripMasterService.setNseEquityMap(
//                    scripMasterStorageService.getCachedFilteredList()
//            );
//        } else {
//            scripMasterService.setNseEquityMap(scripMasterService.downloadFilteredScripMaster().block());
//        }

//        boolean fnoLoaded = fnoUniverseService.loadIfPresent();

        // --------------------------------------------------
        // 2️⃣ Scrip master
        // --------------------------------------------------

//        scripMasterStorageService.loadFilteredScripmasterFromFile();
//
//        if (scripMasterStorageService.getCachedFilteredList() != null &&
//                scripMasterStorageService.isFileFromToday()) {
//
//            log.info("Using cached filtered ScripMaster");
//
//            scripMasterService.setNseEquityMap(
//                    scripMasterStorageService.getCachedFilteredList()
//            );
//
//            // Build FNO ONLY if file was missing
//            if (!fnoLoaded) {
//                fnoUniverseService.buildAndPersist(
//                        scripMasterStorageService.getCachedFilteredList()
//                );
//            }
//
//        } else {
//
//            log.warn("Filtered ScripMaster missing or stale. Downloading...");
//
//            Map<String, String> filtered =
//                    scripMasterService.downloadFilteredScripMaster().block();
//
//            scripMasterService.setNseEquityMap(filtered);
//
//            // Build FNO ONLY if file was missing
//            if (!fnoLoaded) {
//                fnoUniverseService.buildAndPersist(filtered);
//            }
//        }

        log.info("Startup: ScripMaster + FNO initialization");

        // 1️⃣ Try loading derived artifacts
        scripMasterStorageService.loadFilteredScripmasterFromFile();
        boolean filteredOk =
                scripMasterStorageService.getCachedFilteredList() != null &&
                        scripMasterStorageService.isFileFromToday();

        boolean fnoOk = fnoUniverseService.loadIfPresent();

        // 2️⃣ If both are OK → use them
        if (filteredOk && fnoOk) {

            log.info("Using cached Filtered ScripMaster and FNO universe");

            scripMasterService.setNseEquityMap(
                    scripMasterStorageService.getCachedFilteredList()
            );
        } else {

            // 3️⃣ Else → RAW FLOW
            log.warn("Derived data missing. Downloading RAW ScripMaster");

            scripMasterService
                    .downloadRawScripMaster()
                    .doOnSuccess(rawList -> {

                        // Build filtered NSE EQ
//                    Map<String, String> filtered =
//                            scripMasterService.filterOnlyEquityNse(rawList);
//
//                    scripMasterService.setNseEquityMap(filtered);
//                    scripMasterStorageService.saveFilteredScripMaster(filtered);
//
//                    // Build FNO universe (from RAW)
//                    fnoUniverseService.buildFromRaw(rawList);
//
//                    // IMPORTANT: drop RAW list
//                    scripMasterService.clearRaw();
//
//                    log.info("Startup build complete");

                        fnoUniverseService.buildFromRaw(rawList);
                        Set<String> fnoUniverse = fnoUniverseService.loadCached();

                        // 2️⃣ Build NSE Equity map
                        Map<String, String> equityMap =
                                scripMasterService.filterOnlyEquityNse(rawList);

                        // 3️⃣ Apply scripmasterOnlyFnoStocks logic
                        if (applicationProperties.isScripmasterOnlyFnoStocks()) {

                            log.info("Filtering ScripMaster to ONLY FNO stocks");

                            equityMap = equityMap.entrySet()
                                    .stream()
                                    .filter(e ->
                                            fnoUniverse.contains(
                                                    e.getKey().trim().toUpperCase()
                                            )
                                    )
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue
                                    ));

                            log.info("ScripMaster after FNO filter size={}", equityMap.size());
                        } else {
                            log.info("ScripMaster contains ALL NSE equity stocks");
                        }

                        // 4️⃣ Save final ScripMaster
                        scripMasterService.setNseEquityMap(equityMap);
                        scripMasterStorageService.saveFilteredScripMaster(equityMap);

                        // 5️⃣ Cleanup
                        scripMasterService.clearRaw();

                        log.info("Startup build complete");

                    })
                    .doOnError(e -> log.error("Startup failed", e))
                    .subscribe();
        }

        // ---- SL Order store ----
//        slOrderStore.init(slOrderBaseDir);
//        slOrderStore.loadFromFile();

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
                        log.error("Failed to fetch RMS on startup: {}", err.getMessage())
                )
                .subscribe();

        leverageStorageService.load();

        if (leverageStorageService.getCached() != null && leverageStorageService.isFileFromToday()) {
            leverageService.loadFromCache();
        } else {
            leverageService.refreshNow()
                    .doOnError(e -> log.error("Leverage refresh failed", e))
                    .subscribe();
        }

//        log.info("Application properties are: {}", applicationProperties.toString());

        logApplicationProperties();
    }

    private void logApplicationProperties() {

        log.info("========== Application Properties (myapp.*) ==========");

        log.info("---- Files ----");
        log.info("myapp.token-file-path={}", applicationProperties.getTokenFilePath());
        log.info("myapp.scripmaster-file-path={}", applicationProperties.getScripmasterFilePath());
        log.info("myapp.filtered-scripmaster-file-path={}", applicationProperties.getFilteredScripmasterFilePath());
        log.info("myapp.sl-orderstore-file-path={}", applicationProperties.getSlOrderstoreFilePath());
        log.info("myapp.rms-balance-file-path={}", applicationProperties.getRmsBalanceFilePath());
        log.info("myapp.scripmaster-fno-list-file-path={}", applicationProperties.getScripmasterFnoListFilePath());
        log.info("myapp.leverage-list-file-path={}", applicationProperties.getLeverageListFilePath());

        log.info("---- Trading ----");
        log.info("myapp.trading-window-enable={}", applicationProperties.isTradingWindowEnable());
        log.info("myapp.trading-window-start-time={}", applicationProperties.getTradingWindowStartTime());
        log.info("myapp.trading-window-end-time={}", applicationProperties.getTradingWindowEndTime());
        log.info("myapp.trading-window-time-zone={}", applicationProperties.getTradingWindowTimeZone());
        log.info("myapp.trading-stoploss-percent={}", applicationProperties.getTradingStoplossPercent());
        log.info("myapp.trading-stoploss-buffer-percent={}", applicationProperties.getTradingStoplossBufferPercent());

        log.info("---- Capital & Risk ----");
        log.info("myapp.balance-minimum-allowed={}", applicationProperties.getBalanceMinimumAllowed());
        log.info("myapp.percent-balance-use={}", applicationProperties.getPercentBalanceUse());
        log.info("myapp.stock-buy-minimum-quantity-required={}", applicationProperties.getStockBuyMinimumQuantityRequired());
        log.info("myapp.number-of-stocks-buy-less={}", applicationProperties.getNumberOfStocksBuyLess());
        log.info("myapp.fixed-quantity-flag={}", applicationProperties.isFixedQuantityFlag());
        log.info("myapp.fixed-quantity={}", applicationProperties.getFixedQuantity());

        log.info("---- Profit / SL ----");
        log.info("myapp.profit-percentage-multiplier={}", applicationProperties.getProfitPercentageMultiplier());
        log.info("myapp.stoploss-percentage-multiplier={}", applicationProperties.getStoplossPercentageMultiplier());
        log.info("myapp.order-book-retry-milliseconds={}", applicationProperties.getOrderBookRetryMilliseconds());

        log.info("---- RMS ----");
        log.info("myapp.rms-auto-refresh-enable={}", applicationProperties.isRmsAutoRefreshEnable());
        log.info("myapp.rms-file-overwrite-enabled={}", applicationProperties.isRmsFileOverwriteEnabled());
        log.info("myapp.rms-refresh-interval-minutes={}", applicationProperties.getRmsRefreshIntervalMinutes());

        log.info("---- ScripMaster / FNO ----");
        log.info("myapp.scripmaster-only-fno-stocks={}", applicationProperties.isScripmasterOnlyFnoStocks());
        log.info("myapp.scripmaster-enable-fno-universe={}", applicationProperties.isScripmasterEnableFnoUniverse());

        log.info("---- Leverage ----");
        log.info("myapp.leverage-enable={}", applicationProperties.isLeverageEnable());
        log.info("myapp.leverage-universe={}", applicationProperties.isLeverageUniverse());
        log.info("myapp.leverage-exchange={}", applicationProperties.getLeverageExchange());
        log.info("myapp.leverage-multiplier-to-use={}", applicationProperties.getLeverageMultiplierToUse());

        log.info("=======================================================");
    }

}

