package com.onepercentgrowth.local_to_smartapi.storage;

import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

@Service
public class FnoUniverseStorageService {

    private static final Logger log =
            LoggerFactory.getLogger(FnoUniverseStorageService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationProperties properties;

    public FnoUniverseStorageService(ApplicationProperties properties) {
        this.properties = properties;
    }

    public void save(Set<String> fnoSet) {
        try {
            File file = new File(properties.getScripmasterFnoListFilePath());
            file.getParentFile().mkdirs();

            objectMapper.writeValue(file,
                    new FnoUniverseModel(fnoSet, System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Failed to save FNO universe | error: {}", e.getMessage());
        }
    }

    public FnoUniverseModel load() {
        File file = new File(properties.getScripmasterFnoListFilePath());
        if (!file.exists()) return null;

        try {
            return objectMapper.readValue(file, FnoUniverseModel.class);
        } catch (Exception e) {
            log.error("Failed to load FNO universe | error: {}", e.getMessage());
            return null;
        }
    }

    public boolean isFromToday(FnoUniverseModel model) {
        if (model == null) return false;

        LocalDate fileDay = Instant.ofEpochMilli(model.lastUpdatedEpoch())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        return fileDay.equals(LocalDate.now());
    }

    public record FnoUniverseModel(
            Set<String> items,
            long lastUpdatedEpoch
    ) {}
}

