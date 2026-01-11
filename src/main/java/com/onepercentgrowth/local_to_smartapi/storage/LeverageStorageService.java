package com.onepercentgrowth.local_to_smartapi.storage;

import com.onepercentgrowth.local_to_smartapi.model.LeverageInfo;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Service
public class LeverageStorageService {

    @Autowired
    private ApplicationProperties applicationProperties;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile Map<String, LeverageInfo> cached = Map.of();
    private volatile long lastUpdatedEpoch = 0;

//    private final String FILE_PATH = ;
//            "data/leverage/nse_intraday.json";

    public synchronized void save(Map<String, LeverageInfo> map) {
        try {
            File file = new File(applicationProperties.getLeverageListFilePath());
            file.getParentFile().mkdirs();

            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file,
                            new LeverageFileModel(
                                    map,
                                    System.currentTimeMillis()
                            ));

            cached = map;
            lastUpdatedEpoch = System.currentTimeMillis();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save leverage file", e);
        }
    }

    public synchronized void load() {
        File file = new File(applicationProperties.getLeverageListFilePath());
        if (!file.exists()) return;

        try {
            LeverageFileModel model =
                    mapper.readValue(file, LeverageFileModel.class);

            cached = model.items();
            lastUpdatedEpoch = model.lastUpdatedEpoch();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load leverage file", e);
        }
    }

    public boolean isFileFromToday() {
        return Instant.ofEpochMilli(lastUpdatedEpoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .equals(LocalDate.now());
    }

    public Map<String, LeverageInfo> getCached() {
        return cached;
    }

    private record LeverageFileModel(
            Map<String, LeverageInfo> items,
            long lastUpdatedEpoch
    ) {}
}

