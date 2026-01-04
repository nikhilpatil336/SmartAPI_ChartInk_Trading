package com.onepercentgrowth.local_to_smartapi.storage;

import com.onepercentgrowth.local_to_smartapi.model.SlOrderMeta;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusWebSocketHandler;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SlOrderStore {

    private static final Logger log = LoggerFactory.getLogger(SlOrderStore.class);

    private final Map<String, SlOrderMeta> cache = new ConcurrentHashMap<>();

    private Path storeFile;

    public void init(String baseDir) {
        if (baseDir == null || baseDir.isBlank()) {
            throw new IllegalArgumentException("SL order store path is blank");
        }

        try {
            Path dir = Paths.get(baseDir);
            Files.createDirectories(dir);

            this.storeFile = dir.resolve(
                    "sl-orders-" + LocalDate.now() + ".json"
            );

            log.info("SL Order Store initialized at: " + storeFile);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to init SL Order Store", e);
        }
    }

    public void loadFromFile() {
        if (storeFile == null || !Files.exists(storeFile)) {
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(storeFile);
            Map<String, SlOrderMeta> loaded =
                    new ObjectMapper().readValue(
                            bytes,
                            new tools.jackson.core.type.TypeReference<>() {}
                    );
            cache.putAll(loaded);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load SL orders", e);
        }
    }

    public void put(String key, SlOrderMeta meta) {
        cache.put(key, meta);
        persistAsync();
    }

    public SlOrderMeta get(String key) {
        return cache.get(key);
    }

    public Map<String, SlOrderMeta> getMap()
    {
        return cache;
    }

    public void remove(String key) {
        cache.remove(key);
        persistAsync();
    }

    private void persistAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                Files.write(
                        storeFile,
                        new ObjectMapper().writeValueAsBytes(cache),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (Exception e) {
                // log & alert — do NOT swallow
            }
        });
    }
}
