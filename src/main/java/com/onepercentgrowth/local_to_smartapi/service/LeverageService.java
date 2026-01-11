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
    public Mono<Void> refreshNow() {

        if (!applicationProperties.isLeverageEnable()) {
            log.info("Leverage system disabled by config");
            return Mono.empty();
        }

        String token = tokenStorageService.getJwtToken();
        if (token == null) {
            return Mono.error(new RuntimeException("Login required"));
        }

        Set<String> symbols = resolveUniverse();

        log.info("Refreshing leverage for {} symbols (universe={})",
                symbols.size(),
                applicationProperties.isLeverageUniverse() ? "FNO" : "ALL");

        return brokerApiClient
                .fetchNseIntradayLeverage(token)
                .map(this::parseResponse)
                .map(map ->
                        map.entrySet().stream()
                                .filter(e -> symbols.contains(e.getKey()))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue
                                ))
                )
                .doOnSuccess(map -> {
                    leverageMap = map;
                    storageService.save(map);
                    log.info("Leverage cache updated. Size={}", map.size());
                })
                .then();
    }

    /**
     * Resolve universe based on config
     */
    private Set<String> resolveUniverse() {

        if (applicationProperties.isLeverageUniverse()) {
            return fnoUniverseService.loadCached();
        }

        return scripMasterService
                .getNseEquityMap()
                .keySet();
    }

    /**
     * Parse API response → POJO map
     */
    @SuppressWarnings("unchecked")
    private Map<String, LeverageInfo> parseResponse(
            Map<String, Object> response
    ) {

        List<Map<String, Object>> data =
                (List<Map<String, Object>>) response.get("data");

        Map<String, LeverageInfo> map = new HashMap<>();

        for (Map<String, Object> item : data) {
            String exchange = item.get("Exchange").toString();
            String symbol   = item.get("SymbolName").toString();
            double mult     =
                    Double.parseDouble(item.get("Multiplier").toString());

            map.put(symbol,
                    new LeverageInfo(symbol, mult));
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

    private String normalize(String s) {
        return s.trim().toUpperCase();
    }
}

