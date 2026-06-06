package com.onepercentgrowth.local_to_smartapi.backtest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.backtest.strategy.StrategyConfig;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class StrategyConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(StrategyConfigLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ApplicationProperties applicationProperties;

    public StrategyConfigLoader(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public List<StrategyConfig> loadAll() {
        String dir = applicationProperties.getBacktestStrategiesPath();
        File folder = new File(dir);
        List<StrategyConfig> configs = new ArrayList<>();

        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Backtest strategies directory not found: {}", dir);
            return configs;
        }

        File[] files = folder.listFiles((f, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            log.warn("No .json strategy files found in: {}", dir);
            return configs;
        }

        for (File file : files) {
            try {
                StrategyConfig cfg = MAPPER.readValue(file, StrategyConfig.class);
                if (cfg.getName() == null || cfg.getName().isBlank()) {
                    log.warn("Strategy file {} has no name — skipping", file.getName());
                    continue;
                }
                configs.add(cfg);
                log.info("Loaded strategy: {} (enabled={})", cfg.getName(), cfg.isEnabled());
            } catch (Exception e) {
                log.warn("Failed to load strategy {}: {}", file.getName(), e.getMessage());
            }
        }

        log.info("Loaded {}/{} strategy configs from {}", configs.size(), files.length, dir);
        return configs;
    }
}
