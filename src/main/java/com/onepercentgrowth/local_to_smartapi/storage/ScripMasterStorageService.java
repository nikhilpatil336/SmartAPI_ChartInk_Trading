package com.onepercentgrowth.local_to_smartapi.storage;

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

@Service
public class ScripMasterStorageService {

    private static final Logger log = LoggerFactory.getLogger(ScripMasterStorageService.class);

    @Value("${myapp.scripmaster.file-path}")
    private String SCRIP_FILE;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile List<Map<String, Object>> cachedRawList = null;
    private volatile long lastUpdatedEpoch = 0;

    public synchronized void saveRawScripMaster(List<Map<String, Object>> list) {
        try {
            File file = new File(SCRIP_FILE);
            file.getParentFile().mkdirs();

            long now = System.currentTimeMillis();
            ScripFileModel model = new ScripFileModel(list, now);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, model);

            this.cachedRawList = list;
            this.lastUpdatedEpoch = now;

            log.info("✔ ScripMaster saved to {}", SCRIP_FILE);

        } catch (Exception e) {
            log.error("❌ Failed to save ScripMaster file: {}", e.getMessage(), e);
        }
    }

    public synchronized void loadFromFile() {
        try {
            File file = new File(SCRIP_FILE);

            if (!file.exists()) {
                log.warn("⚠ ScripMaster file does not exist.");
                return;
            }

            ScripFileModel model = objectMapper.readValue(file, ScripFileModel.class);

            this.cachedRawList = model.items();
            this.lastUpdatedEpoch = model.lastUpdatedEpoch();

            log.info("✔ ScripMaster loaded from {}", SCRIP_FILE);

        } catch (Exception e) {
            log.error("❌ Failed to load ScripMaster file: {}", e.getMessage(), e);
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

    /** Internal storage model */
    private record ScripFileModel(
            List<Map<String, Object>> items,
            long lastUpdatedEpoch
    ) {}
}
