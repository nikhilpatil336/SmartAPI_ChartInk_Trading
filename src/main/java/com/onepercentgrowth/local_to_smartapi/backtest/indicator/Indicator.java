package com.onepercentgrowth.local_to_smartapi.backtest.indicator;

import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;
import java.util.Map;

public interface Indicator {

    // Unique name — e.g., "RSI", "EMA", "VWAP". Combined with period in context key.
    String name();

    // Compute primary value at endIdx. Returns Double.NaN if insufficient data.
    double compute(List<Candle> candles, int endIdx);

    // Multi-value indicators (MACD, BB) override this to return all components.
    // Key format matches context key suffix: e.g., "MACD_12", "BB_UPPER_20"
    default Map<String, Double> computeAll(List<Candle> candles, int endIdx) {
        return Map.of(name(), compute(candles, endIdx));
    }
}
