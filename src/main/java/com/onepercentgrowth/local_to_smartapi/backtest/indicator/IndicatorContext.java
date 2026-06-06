package com.onepercentgrowth.local_to_smartapi.backtest.indicator;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

public class IndicatorContext {

    // Key format: "<TIMEFRAME>:<INDICATOR_NAME>_<PERIOD>:<OFFSET>"
    // Example:    "5M:RSI_14:0", "15M:EMA_9:1", "5M:VWAP_0:0"
    private final Map<String, Double> values = new HashMap<>();

    public void put(String key, double value) {
        values.put(key, value);
    }

    public void merge(Map<String, Double> map) {
        values.putAll(map);
    }

    public OptionalDouble get(String key) {
        Double v = values.get(key);
        return v == null ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    public double getOrDefault(String key, double defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Map<String, Double> all() {
        return Map.copyOf(values);
    }
}
