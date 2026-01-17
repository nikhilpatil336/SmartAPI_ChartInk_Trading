package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.storage.ScripMasterStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//@Service
//public class FnoUniverseService {
//
////    private static final Logger log = LoggerFactory.getLogger(FnoUniverseService.class);
////
////    private final ScripMasterStorageService storageService;
////    private final ApplicationProperties applicationProperties;
////
////    private volatile Set<String> cachedFnoSet = Set.of();
////
////    public FnoUniverseService(
////            ScripMasterStorageService storageService,
////            ApplicationProperties applicationProperties
////    ) {
////        this.storageService = storageService;
////        this.applicationProperties = applicationProperties;
////    }
////
////    public Set<String> buildAndPersistIfEnabled(
////            List<Map<String, Object>> rawList
////    ) {
////
////        if (!applicationProperties.isScripmasterEnableFnoUniverse()) {
////            log.info("FNO universe generation disabled by config");
////            return Set.of();
////        }
////
////        Set<String> fnoSet = rawList.stream()
////                .filter(item ->
////                        "NFO".equals(item.get("exch_seg")) &&
////                                "FUTSTK".equals(item.get("instrumenttype"))
////                )
////                .map(item -> normalize(item.get("name").toString()))
//////                .map(item -> item.get("symbol").toString().toUpperCase())
////                .collect(Collectors.toSet());
////
////        storageService.saveFnoUniverse(fnoSet);
////        cachedFnoSet = fnoSet;
////
////        log.info("FNO universe built. Size={}", fnoSet.size());
////        return fnoSet;
////    }
////
////    public Set<String> loadCached() {
////        if (!cachedFnoSet.isEmpty()) {
////            return cachedFnoSet;
////        }
////
//////        cachedFnoSet = storageService.loadFnoUniverse();
////
////        cachedFnoSet = storageService.loadFnoUniverse()
////                .stream()
////                .map(this::normalize)
////                .collect(Collectors.toSet());
////        log.info("Loaded cached FNO universe. Size={}", cachedFnoSet.size());
////        return cachedFnoSet;
////    }
////
////    private String normalize(Object o) {
////        return o == null ? null : o.toString().trim().toUpperCase();
////    }
//
//    private static final Logger log =
//            LoggerFactory.getLogger(FnoUniverseService.class);
//
//    private final FnoUniverseStorageService storage;
//    private final ApplicationProperties properties;
//
//    private volatile Set<String> cached = Set.of();
//
//    public FnoUniverseService(
//            FnoUniverseStorageService storage,
//            ApplicationProperties properties
//    ) {
//        this.storage = storage;
//        this.properties = properties;
//    }
//
//    public Set<String> loadOrBuild(List<Map<String, Object>> rawList) {
//
//        Set<String> existing = loadCached();
//        if (!existing.isEmpty()) {
//            return existing;
//        }
//
//        log.info("FNO universe missing. Building from raw scrip master");
//        return buildAndPersist(rawList);
//    }
//
//    /**
//     * Build FNO universe from raw scrip master and persist it
//     */
//    public Set<String> buildAndPersist(List<Map<String, Object>> rawList) {
//
//        if (!properties.isScripmasterEnableFnoUniverse()) {
//            log.info("FNO universe generation disabled by config");
//            return Set.of();
//        }
//
//        Set<String> set = rawList.stream()
//                .filter(i -> "NFO".equals(i.get("exch_seg")))
//                .filter(i -> "FUTSTK".equals(i.get("instrumenttype")))
//                .map(i -> normalize(i.get("name")))
//                .collect(Collectors.toSet());
//
//        storage.save(set);
//        cached = set;
//
//        log.info("FNO universe built. Size={}", set.size());
//        return set;
//    }
//
//    /**
//     * Load cached FNO universe (memory → disk → empty)
//     */
//    public Set<String> loadCached() {
//
//        if (!cached.isEmpty()) {
//            return cached;
//        }
//
//        var model = storage.load();
//        if (model == null) {
//            return Set.of();
//        }
//
//        cached = model.items()
//                .stream()
//                .map(this::normalize)
//                .collect(Collectors.toSet());
//
//        log.info("FNO universe loaded from storage. Size={}", cached.size());
//        return cached;
//    }
//
//    private String normalize(Object o) {
//        return o == null ? null : o.toString().trim().toUpperCase();
//    }
//}

@Service
public class FnoUniverseService {

    private static final Logger log =
            LoggerFactory.getLogger(FnoUniverseService.class);
//
//    private final ScripMasterStorageService storageService;
//
//    private volatile Set<String> cached = Set.of();
//
//    public FnoUniverseService(ScripMasterStorageService storageService) {
//        this.storageService = storageService;
//    }
//
//    /**
//     * Startup entry point.
//     * Loads FNO universe ONLY if file exists and is non-empty.
//     */
//    public boolean loadIfPresent() {
//
//        Set<String> stored = storageService.loadFnoUniverse();
//
//        if (stored == null || stored.isEmpty()) {
//            log.warn("FNO universe file missing or empty");
//            return false;
//        }
//
//        cached = stored;
//        log.info("FNO universe loaded from file. Size={}", cached.size());
//        return true;
//    }
//
//    /**
//     * Build FNO universe from filtered ScripMaster
//     * and persist it.
//     */
//    public void buildAndPersist(Map<String, String> nseEqMap) {
//
//        if (nseEqMap == null || nseEqMap.isEmpty()) {
//            log.warn("Cannot build FNO universe: NSE EQ map empty");
//            return;
//        }
//
//        Set<String> fnoSet = nseEqMap.keySet()
//                .stream()
//                .map(this::normalize)
//                .collect(Collectors.toSet());
//
//        storageService.saveFnoUniverse(fnoSet);
//        cached = fnoSet;
//
//        log.info("FNO universe built & stored. Size={}", cached.size());
//    }
//
//    /**
//     * Used by LeverageService
//     */
//    public Set<String> getCached() {
//        return cached;
//    }
//
//    private String normalize(String s) {
//        return s == null ? null : s.trim().toUpperCase();
//    }

    private final ScripMasterStorageService storage;
    private volatile Set<String> cached = Set.of();

    public FnoUniverseService(ScripMasterStorageService storage) {
        this.storage = storage;
    }

    public boolean loadIfPresent() {
        Set<String> set = storage.loadFnoUniverse();
        if (set == null || set.isEmpty()) return false;
        cached = normalize(set);
        return true;
    }

    public void buildFromRaw(List<Map<String, Object>> rawList) {

        Set<String> fnoSet = rawList.stream()
                .filter(i -> "NFO".equals(i.get("exch_seg")))
                .filter(i -> "FUTSTK".equals(i.get("instrumenttype")))
                .map(i -> normalize(i.get("name")))
                .collect(Collectors.toSet());

        storage.saveFnoUniverse(fnoSet);
        cached = fnoSet;

        log.info("FNO universe built. Size={}", cached.size());
    }

    public Set<String> getCached() {
        return cached;
    }

    private Set<String> normalize(Set<String> set) {
        return set.stream()
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());
    }

    private String normalize(Object o) {
        return o == null ? null : o.toString().trim().toUpperCase();
    }

    public void updateUniverse(Set<String> symbols) {
        this.cached = symbols != null ? symbols : Set.of();
    }

    public Set<String> loadCached() {
        return cached;
    }

}

