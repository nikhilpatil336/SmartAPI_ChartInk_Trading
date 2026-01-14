package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.model.LeverageInfo;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.storage.LeverageStorageService;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LeverageService {

    private static final Logger log =
            LoggerFactory.getLogger(LeverageService.class);

    private final BrokerApiClient brokerApiClient;
    private final TokenStorageService tokenStorageService;
    private final ScripMasterService scripMasterService;
    private final FnoUniverseService fnoUniverseService;
    private final LeverageStorageService storageService;
    private final ApplicationProperties applicationProperties;

    private volatile Map<String, LeverageInfo> leverageMap = new HashMap<>();

    public LeverageService(
            BrokerApiClient brokerApiClient,
            TokenStorageService tokenStorageService,
            ScripMasterService scripMasterService,
            FnoUniverseService fnoUniverseService,
            LeverageStorageService storageService,
            ApplicationProperties applicationProperties
    ) {
        this.brokerApiClient = brokerApiClient;
        this.tokenStorageService = tokenStorageService;
        this.scripMasterService = scripMasterService;
        this.fnoUniverseService = fnoUniverseService;
        this.storageService = storageService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Entry point for startup / manual refresh
     */
//    public Mono<Void> refreshNow() {
//
//        if (!applicationProperties.isLeverageEnable()) {
//            log.info("Leverage system disabled by config");
//            return Mono.empty();
//        }
//
//        String token = tokenStorageService.getJwtToken();
//        if (token == null) {
//            return Mono.error(new RuntimeException("Login required"));
//        }
//
//        Set<String> symbols = resolveUniverse();
//
//        log.info("Refreshing leverage for {} symbols (universe={})",
//                symbols.size(),
//                applicationProperties.isLeverageUniverse() ? "FNO" : "ALL");
//
//        return brokerApiClient
//                .fetchNseIntradayLeverage(token)
//                .map(this::parseResponse)
//                .map(map ->
//                        map.entrySet().stream()
//                                .filter(e -> symbols.contains(e.getKey()))
//                                .collect(Collectors.toMap(
//                                        Map.Entry::getKey,
//                                        Map.Entry::getValue
//                                ))
//                )
//                .doOnSuccess(map -> {
//                    leverageMap = map;
//                    storageService.save(map);
//                    log.info("Leverage cache updated. Size={}", map.size());
//                })
//                .doOnNext(map ->
//                        log.info("Leverage API returned {} symbols",
//                                map.size()))
//                .then();
//    }

//    public Mono<Void> refreshNow() {
//
//        if (!applicationProperties.isLeverageEnable()) {
//            log.info("Leverage system disabled by config");
//            return Mono.empty();
//        }
//
//        String token = tokenStorageService.getJwtToken();
//        if (token == null) {
//            return Mono.error(new RuntimeException("Login required"));
//        }
//
//        // -------- 1️⃣ Resolve universe ----------
//        Set<String> symbols = resolveUniverse();
//
//        // -------- 2️⃣ Guard: empty universe ----------
//        if (symbols == null || symbols.isEmpty()) {
//            log.warn("Leverage universe is EMPTY. Skipping leverage refresh.");
//            return Mono.empty();
//        }
//
//        log.info("Refreshing leverage for {} symbols (universe={})",
//                symbols.size(),
//                applicationProperties.isLeverageUniverse() ? "FNO" : "ALL");
//
//        // -------- 3️⃣ Fetch leverage ----------
//        return brokerApiClient
//                .fetchNseIntradayLeverage(token)
//
//                // ---------- log raw count ----------
//                .doOnNext(resp -> {
//                    Object d = resp.get("data");
//                    if (d instanceof List<?>) {
//                        log.info("Leverage API returned {} records",
//                                ((List<?>) d).size());
//                    }
//                })
//
//                // ---------- parse ----------
//                .map(this::parseResponse)
//
//                // ---------- filter by universe ----------
//                .map(map -> {
//                    Map<String, LeverageInfo> filtered =
//                            map.entrySet().stream()
//                                    .filter(e ->
//                                            symbols.contains(
//                                                    normalize(e.getKey())
//                                            )
//                                    )
//                                    .collect(Collectors.toMap(
//                                            e -> normalize(e.getKey()),
//                                            Map.Entry::getValue
//                                    ));
//
//                    log.info("Leverage after universe filter = {}",
//                            filtered.size());
//
//                    return filtered;
//                })
//
//                // ---------- guard: do NOT overwrite with empty ----------
//                .doOnSuccess(map -> {
//                    if (map.isEmpty()) {
//                        log.warn("Filtered leverage map is EMPTY. Cache NOT updated.");
//                        return;
//                    }
//
//                    leverageMap = map;
//                    storageService.save(map);
//
//                    log.info("Leverage cache updated successfully. Size={}",
//                            map.size());
//                })
//                .then();
//    }

    public Mono<Void> refreshNow() {

        if (!applicationProperties.isLeverageEnable()) {
            log.info("Leverage system disabled by config");
            return Mono.empty();
        }

        String token = tokenStorageService.getJwtToken();
        if (token == null) {
            return Mono.error(new RuntimeException("Login required"));
        }

        Set<String> universe = resolveUniverse();

        if (universe == null || universe.isEmpty()) {
            log.warn("Leverage universe is EMPTY or NULL. Skipping leverage refresh.");
            return Mono.empty();
        }

        log.info("Refreshing leverage. Universe size={}, mode={}",
                universe.size(),
                applicationProperties.isLeverageUniverse() ? "FNO" : "ALL");

        return brokerApiClient
                .fetchNseIntradayLeverage(token)
                .doOnNext(resp -> {
                    Object data = resp.get("data");
                    if (data instanceof List<?> list) {
                        log.info("Leverage API returned {} records", list.size());
                    } else {
                        log.warn("Leverage API returned unexpected response format");
                    }
                })
                .map(this::parseResponse)
                .doOnNext(map ->
                        log.info("Parsed leverage symbols count={}", map.size())
                )
                .map(map ->
                        map.entrySet()
                                .stream()
                                .filter(e ->
                                        universe.contains(
                                                normalize(e.getKey())
                                        )
                                )
                                .collect(Collectors.toMap(
                                        e -> normalize(e.getKey()),
                                        Map.Entry::getValue
                                ))
                )
                .doOnNext(filtered ->
                        log.info("Leverage after universe filter = {}", filtered.size())
                )
                .doOnSuccess(filtered -> {

                    if (filtered == null || filtered.isEmpty()) {
                        log.warn("Filtered leverage EMPTY. Cache NOT updated.");
                        return;
                    }

                    leverageMap = filtered;
                    storageService.save(filtered);
                    log.info("Leverage cache updated. Size={}", filtered.size());
                })
                .then();
    }



    /**
     * Resolve universe based on config
     */
//    private Set<String> resolveUniverse() {
//
//        if (applicationProperties.isLeverageUniverse()) {
////            return fnoUniverseService.loadCached();
//            return null;
//        }
//
//        return scripMasterService
//                .getNseEquityMap()
//                .keySet();
//    }

    private Set<String> resolveUniverse() {

        if (applicationProperties.isLeverageUniverse()) {
            Set<String> fno = fnoUniverseService.loadCached(); // after adding method
            return fno != null ? fno : Set.of();
        }

        Set<String> all = scripMasterService.getNseEquityMap().keySet();
        return all != null ? all : Set.of();
    }

    /**
     * Parse API response → POJO map
     */
    @SuppressWarnings("unchecked")
//    private Map<String, LeverageInfo> parseResponse(
//            Map<String, Object> response
//    ) {
//
//        List<Map<String, Object>> data =
//                (List<Map<String, Object>>) response.get("data");
//
//        Map<String, LeverageInfo> map = new HashMap<>();
//
//        for (Map<String, Object> item : data) {
//            String exchange = item.get("Exchange").toString();
//            String symbol   = item.get("SymbolName").toString();
//            double mult     =
//                    Double.parseDouble(item.get("Multiplier").toString());
//
//            map.put(symbol,
//                    new LeverageInfo(symbol, mult));
//        }
//
//        return map;
//    }

//    private Map<String, LeverageInfo> parseResponse(
//            Map<String, Object> response
//    ) {
//
//        List<Map<String, Object>> data =
//                (List<Map<String, Object>>) response.get("data");
//
//        Map<String, LeverageInfo> map = new HashMap<>();
//
//        for (Map<String, Object> item : data) {
//            String exchange = item.get("Exchange").toString();
//            if (!"NSE".equals(exchange)) continue;
//
//            String symbol =
//                    normalizeSymbol(item.get("SymbolName").toString());
//
//            double mult =
//                    Double.parseDouble(item.get("Multiplier").toString());
//
//            map.put(symbol, new LeverageInfo(symbol, mult));
//        }
//
//        log.info("Parsed leverage symbols count={}", map.size());
//        return map;
//    }

    private Map<String, LeverageInfo> parseResponse(
            Map<String, Object> response
    ) {
        List<Map<String, Object>> data =
                (List<Map<String, Object>>) response.get("data");

        Map<String, LeverageInfo> map = new HashMap<>();

        for (Map<String, Object> item : data) {

            String symbol = normalize(
                    item.get("SymbolName").toString()
            );

            double multiplier =
                    Double.parseDouble(item.get("Multiplier").toString());

            map.put(symbol, new LeverageInfo(symbol, multiplier));
        }

        return map;
    }



    public LeverageInfo get(String symbol) {
        return leverageMap.get(symbol);
    }

    public void loadFromCache() {
        leverageMap = storageService.getCached();
        log.info("Leverage loaded from cache. Size={}", leverageMap.size());
    }

    private String normalizeSymbol(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

    private String normalize(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

}

