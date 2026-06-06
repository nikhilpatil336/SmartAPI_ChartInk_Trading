package com.onepercentgrowth.local_to_smartapi.backtest.cache;

import com.google.gson.JsonObject;
import com.onepercentgrowth.local_to_smartapi.backtest.runner.TimeframeMapper;
import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;
import com.onepercentgrowth.local_to_smartapi.historicdata.HistoricalDataResponse;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class CandleCacheService {

    private static final Logger log = LoggerFactory.getLogger(CandleCacheService.class);
    private static final DateTimeFormatter API_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BrokerApiClient brokerApiClient;
    private final TokenManager tokenManager;
    private final ApplicationProperties applicationProperties;

    public CandleCacheService(BrokerApiClient brokerApiClient,
                               TokenManager tokenManager,
                               ApplicationProperties applicationProperties) {
        this.brokerApiClient = brokerApiClient;
        this.tokenManager = tokenManager;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Load candles for (symbol, date, timeframe) from disk cache.
     * On cache miss, fetch from broker API, save to disk, return candles.
     * Returns null on failure.
     *
     * @param lookbackDays how many days before alertDate to include in the fetch (for SMA seeding)
     */
    public List<Candle> loadOrFetch(String symbol, String symbolToken,
                                    LocalDate alertDate, String timeframe, int lookbackDays) {
        String cachePath = applicationProperties.getBacktestCachePath();
        File cacheFile = new File(cachePath + symbol + File.separator
                + TimeframeMapper.toCacheSegment(alertDate.toString(), timeframe) + ".json");

        CandleCache cached = CandleCache.load(cacheFile);
        if (cached != null && cached.getCandles() != null && !cached.getCandles().isEmpty()) {
            log.info("Cache hit: {} {} {}", symbol, alertDate, timeframe);
            return cached.getCandles();
        }

        log.info("Cache miss: fetching {} {} {} from API", symbol, alertDate, timeframe);
        return fetchAndCache(symbol, symbolToken, alertDate, timeframe, lookbackDays, cacheFile);
    }

    private List<Candle> fetchAndCache(String symbol, String symbolToken,
                                       LocalDate alertDate, String timeframe,
                                       int lookbackDays, File cacheFile) {
        LocalDateTime from = alertDate.minusDays(lookbackDays).atTime(9, 15);
        LocalDateTime to   = alertDate.atTime(15, 30);

        JsonObject req = new JsonObject();
        req.addProperty("exchange", "NSE");
        req.addProperty("symboltoken", symbolToken);
        req.addProperty("interval", TimeframeMapper.toApiInterval(timeframe));
        req.addProperty("fromdate", from.format(API_FMT));
        req.addProperty("todate", to.format(API_FMT));

        HistoricalDataResponse response = fetchWithRetry(req);
        if (response == null || !response.isStatus() || response.getData() == null || response.getData().isEmpty()) {
            log.warn("No data returned for {} {} {}", symbol, alertDate, timeframe);
            return null;
        }

        List<Candle> candles = response.getData().stream()
                .map(this::mapToCandle)
                .sorted(Comparator.comparing(Candle::getTimestamp))
                .toList();

        CandleCache cache = new CandleCache(symbol, alertDate.toString(),
                TimeframeMapper.toApiInterval(timeframe), candles);
        CandleCache.save(cacheFile, cache);
        log.info("Cached {} candles for {} {} {}", candles.size(), symbol, alertDate, timeframe);
        return candles;
    }

    private HistoricalDataResponse fetchWithRetry(JsonObject req) {
        long sleepMs = applicationProperties.getBacktestRequestSleepMs();
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                long start = System.currentTimeMillis();
                HistoricalDataResponse r = brokerApiClient.getHistoricalCandleData(req,
                        tokenManager.getValidJwtToken()).block();
                if (r != null && r.isStatus() && r.getData() != null) {
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed < sleepMs) {
                        try { Thread.sleep(sleepMs - elapsed); } catch (InterruptedException ignored) {}
                    }
                    return r;
                }
            } catch (Exception e) {
                log.warn("API attempt {}: {}", attempt + 1, e.getMessage());
            }
            try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    private Candle mapToCandle(List<Object> raw) {
        Candle c = new Candle();
        c.setTimestamp((String) raw.get(0));
        c.setOpen(Double.parseDouble(raw.get(1).toString()));
        c.setHigh(Double.parseDouble(raw.get(2).toString()));
        c.setLow(Double.parseDouble(raw.get(3).toString()));
        c.setClose(Double.parseDouble(raw.get(4).toString()));
        c.setVolume(Long.parseLong(raw.get(5).toString()));
        return c;
    }
}
