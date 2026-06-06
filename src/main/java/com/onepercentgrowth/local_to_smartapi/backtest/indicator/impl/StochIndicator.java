package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StochIndicator implements Indicator {

    private final int kPeriod;
    private final int dPeriod;

    public StochIndicator(int kPeriod, int dPeriod) {
        this.kPeriod = kPeriod;
        this.dPeriod = dPeriod;
    }

    public StochIndicator() {
        this(14, 3);
    }

    @Override
    public String name() { return "STOCH"; }

    // compute() returns %K
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        return computeK(candles, endIdx);
    }

    @Override
    public Map<String, Double> computeAll(List<Candle> candles, int endIdx) {
        if (endIdx < kPeriod + dPeriod - 2 || endIdx >= candles.size()) {
            return Map.of("STOCH_K_" + kPeriod, Double.NaN, "STOCH_D_" + kPeriod, Double.NaN);
        }
        List<Double> kSeries = new ArrayList<>();
        for (int i = endIdx - dPeriod + 1; i <= endIdx; i++) {
            kSeries.add(computeK(candles, i));
        }
        double k = kSeries.get(kSeries.size() - 1);
        double d = kSeries.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        return Map.of("STOCH_K_" + kPeriod, k, "STOCH_D_" + kPeriod, d);
    }

    private double computeK(List<Candle> candles, int endIdx) {
        if (endIdx < kPeriod - 1 || endIdx >= candles.size()) return Double.NaN;
        double high = Double.MIN_VALUE, low = Double.MAX_VALUE;
        for (int i = endIdx - kPeriod + 1; i <= endIdx; i++) {
            high = Math.max(high, candles.get(i).getHigh());
            low  = Math.min(low,  candles.get(i).getLow());
        }
        double range = high - low;
        if (range == 0) return 50;
        return 100 * (candles.get(endIdx).getClose() - low) / range;
    }
}
