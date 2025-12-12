package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.model.RmsData;
import com.onepercentgrowth.local_to_smartapi.model.RmsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScripMasterService {

    private static final Logger log = LoggerFactory.getLogger(ScripMasterService.class);

    private final BrokerApiClient brokerApiClient;
    private final TokenStorageService tokenStorageService;

    private volatile Map<String, String> nseEquityMap = new HashMap<>();

    private volatile List<Map<String, Object>> rawScripList = null;

    private volatile RmsData rmsData = null;

    public ScripMasterService(BrokerApiClient brokerApiClient,
                              TokenStorageService tokenStorageService) {
        this.brokerApiClient = brokerApiClient;
        this.tokenStorageService = tokenStorageService;
    }

    public Mono<Map<String, String>> fetchNseScripMaster() {

        log.info("➡ Starting fetchNseScripMaster()");

        String accessToken = tokenStorageService.getJwtToken();

        if (accessToken == null) {
            log.error("❌ No access token found in TokenStorageService. Login required.");
            return Mono.error(new RuntimeException("No access token found. Please login first."));
        }

        return brokerApiClient
                .downloadScripMaster(accessToken)
                .doOnSubscribe(sub -> log.info("➡ Calling BrokerApiClient.downloadScripMaster()"))
                .doOnSuccess(list ->
                {
                    this.rawScripList = list;
                    log.info("✔ Successfully fetched raw ScripMaster list. Count={}", list.size());
                })
                .doOnError(err -> log.error("❌ Error while downloading ScripMaster: {}", err.getMessage(), err))
                .map(list -> {
                    log.info("➡ Filtering only NSE symbols from ScripMaster...");
                    Map<String, String> result = filterOnlyEquityNse(list);
                    this.nseEquityMap = result;
                    log.info("✔ NSE filter complete. NSE count={}", result.size());
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
                    log.info("✔ Raw ScripMaster stored in memory. Size={}", list.size());
                });
    }

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

    public void setRawScripList(List<Map<String, Object>> rawList) {
        this.rawScripList = rawList;
        rebuildNseCacheFromRaw();
    }

    public List<Map<String, Object>> getRawScripList() {
        return rawScripList;
    }

    public Mono<RmsResponse> getCurrentBalance() {
        String jwtToken = tokenStorageService.getJwtToken();

        if (jwtToken == null) {
            return Mono.error(new RuntimeException("No JWT token found. Please log in."));
        }

        return brokerApiClient.fetchRmsBalance(jwtToken)
                .doOnNext(rmsResponse -> {
                    if (rmsResponse != null && rmsResponse.getData() != null) {
                        this.rmsData = rmsResponse.getData(); // store for later use
                        log.info("✔ Stored RMS data locally: {}", rmsData);
                    } else {
                        log.warn("⚠ RMS response data is null");
                    }
                });
    }

    public String getTokenForName(String name) {
        return nseEquityMap.get(name);
    }

    public RmsData getRmsData() {
        return rmsData;
    }

    public void setRmsData(RmsData rmsData) {
        this.rmsData = rmsData;
    }

    public void setNseEquityMap(Map<String, String> nseEquityMap) {
        this.nseEquityMap = nseEquityMap;
    }
}
