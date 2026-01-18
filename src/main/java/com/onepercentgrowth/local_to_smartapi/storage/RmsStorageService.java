package com.onepercentgrowth.local_to_smartapi.storage;

import com.onepercentgrowth.local_to_smartapi.model.RmsData;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;

@Service
public class RmsStorageService {

    private static final Logger log = LoggerFactory.getLogger(RmsStorageService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationProperties applicationProperties;

    private volatile RmsFileModel cachedRms;

    public RmsStorageService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public synchronized void save(RmsData data) {

        if (!applicationProperties.isRmsFileOverwriteEnabled()) {
            return;
        }

        try {
            File file = new File(applicationProperties.getRmsBalanceFilePath());
            file.getParentFile().mkdirs();

            RmsFileModel model = new RmsFileModel(data, System.currentTimeMillis());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, model);

            cachedRms = model;

            log.info("Saved RMS file and the last update date is {}", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Failed to save RMS", e);
        }
    }

    public RmsData getCached() {
        return cachedRms != null ? cachedRms.data() : null;
    }

    private record RmsFileModel(RmsData data, long lastUpdatedEpoch) {}
}

