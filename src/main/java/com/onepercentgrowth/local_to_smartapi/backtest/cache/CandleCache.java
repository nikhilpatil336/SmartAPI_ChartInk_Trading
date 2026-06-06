package com.onepercentgrowth.local_to_smartapi.backtest.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandleCache {

    private static final Logger log = LoggerFactory.getLogger(CandleCache.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String symbol;
    private String date;      // yyyy-MM-dd
    private String interval;  // e.g., FIVE_MINUTE
    private List<Candle> candles;

    public static void save(File file, CandleCache cache) {
        try {
            file.getParentFile().mkdirs();
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, cache);
        } catch (IOException e) {
            log.warn("Failed to save candle cache {}: {}", file.getPath(), e.getMessage());
        }
    }

    public static CandleCache load(File file) {
        if (!file.exists()) return null;
        try {
            return MAPPER.readValue(file, CandleCache.class);
        } catch (IOException e) {
            log.warn("Failed to load candle cache {}: {}", file.getPath(), e.getMessage());
            return null;
        }
    }
}
