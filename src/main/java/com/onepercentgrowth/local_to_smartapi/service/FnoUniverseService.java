package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.storage.ScripMasterStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FnoUniverseService {

    private static final Logger log = LoggerFactory.getLogger(FnoUniverseService.class);

    private final ScripMasterStorageService storageService;
    private final ApplicationProperties applicationProperties;

    private volatile Set<String> cachedFnoSet = Set.of();

    public FnoUniverseService(
            ScripMasterStorageService storageService,
            ApplicationProperties applicationProperties
    ) {
        this.storageService = storageService;
        this.applicationProperties = applicationProperties;
    }

    public Set<String> buildAndPersistIfEnabled(
            List<Map<String, Object>> rawList
    ) {

        if (!applicationProperties.isScripmasterEnableFnoUniverse()) {
            log.info("FNO universe generation disabled by config");
            return Set.of();
        }

        Set<String> fnoSet = rawList.stream()
                .filter(item ->
                        "NFO".equals(item.get("exch_seg")) &&
                                "FUTSTK".equals(item.get("instrumenttype"))
                )
                .map(item -> item.get("name").toString())
                .collect(Collectors.toSet());

        storageService.saveFnoUniverse(fnoSet);
        cachedFnoSet = fnoSet;

        log.info("FNO universe built. Size={}", fnoSet.size());
        return fnoSet;
    }

    public Set<String> loadCached() {
        if (!cachedFnoSet.isEmpty()) {
            return cachedFnoSet;
        }

        cachedFnoSet = storageService.loadFnoUniverse();
        log.info("Loaded cached FNO universe. Size={}", cachedFnoSet.size());
        return cachedFnoSet;
    }
}

