package com.onepercentgrowth.local_to_smartapi.storage;

import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ScripMasterStorageService {

    private static final Logger log = LoggerFactory.getLogger(ScripMasterStorageService.class);

//    @Value("${myapp.scripmaster.file-path}")
//    private String SCRIP_FILE;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApplicationProperties applicationProperties;
    private volatile List<Map<String, Object>> cachedRawList = null;
    private volatile Map<String, String> cachedFilteredList = null;
    private volatile long lastUpdatedEpoch = 0;

    public ScripMasterStorageService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public synchronized void saveRawScripMaster(List<Map<String, Object>> list) {
        try {
            File file = new File(applicationProperties.getScripmasterFilePath());
            file.getParentFile().mkdirs();

            long now = System.currentTimeMillis();
            ScripFileModel model = new ScripFileModel(list, now);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, model);

            this.cachedRawList = list;
            this.lastUpdatedEpoch = now;

            log.info("ScripMaster saved to {}", applicationProperties.getScripmasterFilePath());

        } catch (Exception e) {
            log.error("Failed to save ScripMaster file: {}", e.getMessage(), e);
        }
    }

    public synchronized void saveFilteredScripMaster(Map<String, String> list) {
        try {
            File file = new File(applicationProperties.getFilteredScripmasterFilePath());
            file.getParentFile().mkdirs();

            long now = System.currentTimeMillis();
            FilteredScripFileModel model = new FilteredScripFileModel(list, now);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, model);

            this.cachedFilteredList = list;
            this.lastUpdatedEpoch = now;

            log.info("ScripMaster saved to {}", applicationProperties.getFilteredScripmasterFilePath());

        } catch (Exception e) {
            log.error("Failed to save ScripMaster file: {}", e.getMessage(), e);
        }
    }

    public synchronized void loadFromFile() {
        try {
            File file = new File(applicationProperties.getScripmasterFilePath());

            if (!file.exists()) {
                log.warn("ScripMaster file does not exist.");
                return;
            }

            ScripFileModel model = objectMapper.readValue(file, ScripFileModel.class);

            this.cachedRawList = model.items();
            this.lastUpdatedEpoch = model.lastUpdatedEpoch();

            log.info("ScripMaster loaded from {}", applicationProperties.getScripmasterFilePath());

        } catch (Exception e) {
            log.error("Failed to load ScripMaster file: {}", e.getMessage(), e);
        }
    }

//    public synchronized void loadFilteredScripmasterFromFile() {
//        try {
//            File file = new File(applicationProperties.getFilteredScripmasterFilePath());
//
//            if (!file.exists()) {
//                log.warn("ScripMaster file does not exist.");
//                return;
//            }
//
//            ScripFileModel model = objectMapper.readValue(file, ScripFileModel.class);
//
//            this.cachedRawList = model.items();
//            this.lastUpdatedEpoch = model.lastUpdatedEpoch();
//
//            log.info("ScripMaster loaded from {}", applicationProperties.getFilteredScripmasterFilePath());
//
//        } catch (Exception e) {
//            log.error("Failed to load ScripMaster file: {}", e.getMessage(), e);
//        }
//    }

    public synchronized void loadFilteredScripmasterFromFile() {
        try {
            File file = new File(applicationProperties.getFilteredScripmasterFilePath());

            if (!file.exists()) {
                log.warn("Filtered ScripMaster file does not exist.");
                return;
            }

            FilteredScripFileModel model =
                    objectMapper.readValue(file, FilteredScripFileModel.class);

            this.cachedFilteredList = model.items();
            this.lastUpdatedEpoch = model.lastUpdatedEpoch();

            log.info("Filtered ScripMaster loaded from {}",
                    applicationProperties.getFilteredScripmasterFilePath());

        } catch (Exception e) {
            log.error("Failed to load Filtered ScripMaster file: {}", e.getMessage(), e);
        }
    }


    public boolean isFileFromToday() {
        LocalDate fileDay = Instant.ofEpochMilli(lastUpdatedEpoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        return fileDay.equals(LocalDate.now());
    }


    public List<Map<String, Object>> getCachedRawList() {
        return cachedRawList;
    }

    public Map<String, String> getCachedFilteredList() {
        return cachedFilteredList;
    }

    public void setCachedFilteredList(Map<String, String> cachedFilteredList) {
        this.cachedFilteredList = cachedFilteredList;
    }

    public void saveFnoUniverse(Set<String> fnoSet) {
        try {
            File file = new File(applicationProperties.getScripmasterFnoListFilePath());
            file.getParentFile().mkdirs();

            objectMapper.writeValue(file,
                    new FnoUniverseModel(fnoSet, System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Failed to save FNO universe", e);
        }
    }

    public Set<String> loadFnoUniverse() {
        File file = new File(applicationProperties.getScripmasterFnoListFilePath());
        if (!file.exists()) return Set.of();

        try {
            FnoUniverseModel model =
                    objectMapper.readValue(file, FnoUniverseModel.class);
            return model.items();
        } catch (Exception e) {
            log.error("Failed to load FNO universe", e);
            return Set.of();
        }
    }

    private record FnoUniverseModel(
            Set<String> items,
            long lastUpdatedEpoch
    ) {}


    /** Internal storage model */
    private record ScripFileModel(
            List<Map<String, Object>> items,
            long lastUpdatedEpoch
    ) {}

    private record FilteredScripFileModel(
            Map<String, String> items,
            long lastUpdatedEpoch
    ) {}
}
