package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.storage.ScripMasterStorageService;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScripMasterService {

    private static final Logger log = LoggerFactory.getLogger(ScripMasterService.class);

    private final BrokerApiClient brokerApiClient;
    private final TokenStorageService tokenStorageService;
    private final ScripMasterStorageService scripMasterStorageService;
    private final ApplicationProperties applicationProperties;
    private final FnoUniverseService fnoUniverseService;
    private volatile Map<String, String> nseEquityMap = new HashMap<>();
    private volatile List<Map<String, Object>> rawScripList = null;
//    private volatile RmsData rmsData = null;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScripMasterService(BrokerApiClient brokerApiClient,
                              TokenStorageService tokenStorageService, ScripMasterStorageService scripMasterStorageService, ApplicationProperties applicationProperties, FnoUniverseService fnoUniverseService) {
        this.brokerApiClient = brokerApiClient;
        this.tokenStorageService = tokenStorageService;
        this.scripMasterStorageService = scripMasterStorageService;
        this.applicationProperties = applicationProperties;
        this.fnoUniverseService = fnoUniverseService;
    }

    public Mono<Map<String, String>> fetchNseScripMaster() {

        log.info("Starting fetchNseScripMaster()");

        String accessToken = tokenStorageService.getJwtToken();

        if (accessToken == null) {
            log.error("No access token found in TokenStorageService. Login required.");
            return Mono.error(new RuntimeException("No access token found. Please login first."));
        }

        return brokerApiClient
                .downloadScripMaster(accessToken)
                .doOnSubscribe(sub -> log.info("Calling BrokerApiClient.downloadScripMaster()"))
                .doOnSuccess(list ->
                {
                    this.rawScripList = list;
                    log.info("Successfully fetched raw ScripMaster list. Count={}", list.size());
                })
                .doOnError(err -> log.error("Error while downloading ScripMaster: {}", err.getMessage()))
                .map(list -> {
                    log.info("Filtering only NSE symbols from ScripMaster...");
                    Map<String, String> result = filterOnlyEquityNse(list);
                    this.nseEquityMap = result;
                    log.info("NSE filter complete. NSE count={}", result.size());
                    return result;
                });
    }

    public Mono<List<Map<String, Object>>> downloadRawScripMaster() {

        String accessToken = tokenStorageService.getJwtToken();

        if (accessToken == null) {
            return Mono.error(new RuntimeException("Login required. No token found."));
        }

        return brokerApiClient
                .downloadScripMaster(accessToken)
                .doOnSuccess(list -> {
                    this.rawScripList = list;
                    this.nseEquityMap = filterOnlyEquityNse(this.rawScripList);
                    scripMasterStorageService.saveRawScripMaster(this.rawScripList);
                    log.info("Raw ScripMaster stored in memory. Size={}", list.size());
                });
    }

//    public Mono<Map<String, String>> downloadFilteredScripMaster() {
//
//        String accessToken = tokenStorageService.getJwtToken();
//
//        if (accessToken == null) {
//            return Mono.error(new RuntimeException("Login required. No token found."));
//        }
//
//        return brokerApiClient
//                .downloadScripMaster(accessToken)
//                .map(list -> {
//                    this.rawScripList = list;
//
//                    Map<String, String> filteredMap = filterOnlyEquityNse(list);
//                    this.nseEquityMap = filteredMap;
//
//                    scripMasterStorageService.saveFilteredScripMaster(filteredMap);
//
//                    log.info("Filtered ScripMaster stored in memory. Size={}", filteredMap.size());
//
//                    return filteredMap; // ✅ IMPORTANT
//                });
//    }

//    public Mono<Map<String, String>> downloadFilteredScripMaster() {
//
//        String token = tokenStorageService.getJwtToken();
//        if (token == null) {
//            return Mono.error(new RuntimeException("Login required"));
//        }
//
//        return brokerApiClient
//                .downloadScripMasterStream(token)
//                .filter(item -> "NSE".equals(item.get("exch_seg")))
//                .filter(item -> {
//                    String symbol = (String) item.get("symbol");
//                    return symbol != null && symbol.endsWith("-EQ");
//                })
//                .collect(Collectors.toMap(
//                        item -> item.get("name").toString(),
//                        item -> item.get("token").toString(),
//                        (a, b) -> a
//                ))
//                .doOnSuccess(map -> {
//                    this.nseEquityMap = map;
//                    scripMasterStorageService.saveFilteredScripMaster(map);
//                    log.info("Filtered NSE EQ count={}", map.size());
//                });
//    }

//    public Mono<Map<String, String>> downloadFilteredScripMaster() {
//
//        String token = tokenStorageService.getJwtToken();
//        if (token == null) {
//            return Mono.error(new RuntimeException("Login required"));
//        }
//
//        return brokerApiClient
//                .downloadScripMasterStream(token)
//                .collectList()   // collect ONCE per day
//                .map(rawList -> {
//
//                    // ---------- 1️⃣ Build / load FNO universe ----------
////                    Set<String> fnoSet =
////                            fnoUniverseService.loadIfPresent();
//
//                    boolean fnoListIsPresent =
//                            fnoUniverseService.loadIfPresent();
//
//                    if (applicationProperties.isScripmasterOnlyFnoStocks()
//                            && !fnoListIsPresent) {
//
////                        build the list here and store it locally
//                        rawList.stream()
//                        .filter(item -> "NSE".equals(item.get("exch_seg")))
//                        .filter(item -> {
//                            String symbol = (String) item.get("symbol");
//                            return symbol != null && symbol.endsWith("-EQ");
//                        })
//                        .collect(Collectors.toMap(
//                                item -> item.get("name").toString(),
//                                item -> item.get("token").toString(),
//                                (a, b) -> a
//                        ))
//                        .then(map -> {
//                            this.nseEquityMap = map;
//                            scripMasterStorageService.saveFilteredScripMaster(map);
//                            log.info("Filtered NSE EQ count={}", map.size());
//                        });
//
//                        log.warn(
//                                "FNO-only mode ENABLED but FNO universe is EMPTY. " +
//                                        "Filtered NSE EQ result may be empty."
//                        );
//                    }
//
//                    // ---------- 2️⃣ Filter NSE EQ ----------
//                    return rawList.stream()
//                            .filter(item -> "NSE".equals(item.get("exch_seg")))
//                            .filter(item -> {
//                                String symbol = (String) item.get("symbol");
//                                return symbol != null && symbol.endsWith("-EQ");
//                            })
//                            .filter(item -> {
//                                if (!applicationProperties.isScripmasterOnlyFnoStocks()) {
//                                    return true;
//                                }
//                                return fnoSet.contains(
//                                        normalize(item.get("symbol").toString().replace("-EQ", ""))
//                                );
//                            })
//                            .collect(Collectors.toMap(
////                                    item -> item.get("name").toString(),
//                                    item -> normalize(item.get("symbol").toString().replace("-EQ", "")),
//                                    item -> item.get("token").toString(),
//                                    (a, b) -> a
//                            ));
//                })
//                .doOnSuccess(map -> {
//                    this.nseEquityMap = map;
//                    scripMasterStorageService.saveFilteredScripMaster(map);
//                    log.info("Filtered NSE EQ count={}", map.size());
//                });
//    }

    public Map<String, String> filterOnlyEquityNse(List<Map<String, Object>> rawJsonList) {

        return rawJsonList.stream()
                .filter(item -> "NSE".equals(item.get("exch_seg")))
                .filter(item -> {
                    String symbol = (String) item.get("symbol");
                    return symbol != null && symbol.endsWith("-EQ");
                })
                .collect(Collectors.toMap(
                        item -> item.get("name").toString(),
                        item -> item.get("token").toString(),
                        (existing, duplicate) -> existing
                ));
    }

    public void rebuildNseCacheFromRaw() {
        if (rawScripList != null) {
            this.nseEquityMap = filterOnlyEquityNse(rawScripList);
        }
    }

    public Map<String, String> getNseEquityMap() {
        return nseEquityMap;
    }

    public void setNseEquityMap(Map<String, String> nseEquityMap) {
        this.nseEquityMap = nseEquityMap;
    }

    public void setRawScripList(List<Map<String, Object>> rawList) {
        this.rawScripList = rawList;
        rebuildNseCacheFromRaw();
    }

    public List<Map<String, Object>> getRawScripList() {
        return rawScripList;
    }

//    private void saveFile(String path, Object model) {
//        try {
//            new File(path).getParentFile().mkdirs();
//            objectMapper.writerWithDefaultPrettyPrinter()
//                    .writeValue(new File(path), model);
//            log.info("Saved {}", path);
//        } catch (Exception e) {
//            log.error("Failed saving {}", path, e);
//        }
//    }

//    public Mono<RmsResponse> getCurrentBalance() {
//        String jwtToken = tokenStorageService.getJwtToken();
//
//        if (jwtToken == null) {
//            return Mono.error(new RuntimeException("No JWT token found. Please log in."));
//        }
//
//        return brokerApiClient.fetchRmsBalance(jwtToken)
//                .doOnNext(rmsResponse -> {
//                    if (rmsResponse != null && rmsResponse.getData() != null) {
//                        this.rmsData = rmsResponse.getData(); // store for later use
//                        log.info("Stored RMS data locally: {}", rmsData);
//                    } else {
//                        log.warn("RMS response data is null");
//                    }
//                });
//    }

    public String getTokenForName(String name) {
        return nseEquityMap.get(name);
    }

//    public RmsData getRmsData() {
//        return rmsData;
//    }
//
//    public void setRmsData(RmsData rmsData) {
//        this.rmsData = rmsData;
//    }

    private String normalize(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

    public void clearRaw() {
        this.rawScripList = null;
        System.gc(); // optional but useful on RPi
    }
}
