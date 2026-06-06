package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;
import java.util.Map;

public class BollingerBandsIndicator implements Indicator {

    private final int period;
    private final double multiplier;

    public BollingerBandsIndicator(int period, double multiplier) {
        this.period = period;
        this.multiplier = multiplier;
    }

    public BollingerBandsIndicator() {
        this(20, 2.0);
    }

    @Override
    public String name() { return "BB"; }

    // compute() returns the middle band (SMA)
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        return sma(candles, endIdx);
    }

    @Override
    public Map<String, Double> computeAll(List<Candle> candles, int endIdx) {
        if (endIdx < period - 1 || endIdx >= candles.size()) {
            return Map.of("BB_UPPER_" + period, Double.NaN,
                          "BB_MID_"   + period, Double.NaN,
                          "BB_LOWER_" + period, Double.NaN);
        }
        double mid = sma(candles, endIdx);
        double std = stdDev(candles, endIdx, mid);
        return Map.of(
            "BB_UPPER_" + period, mid + multiplier * std,
            "BB_MID_"   + period, mid,
            "BB_LOWER_" + period, mid - multiplier * std
        );
    }

    private double sma(List<Candle> candles, int endIdx) {
        if (endIdx < period - 1 || endIdx >= candles.size()) return Double.NaN;
        double sum = 0;
        for (int i = endIdx - period + 1; i <= endIdx; i++) sum += candles.get(i).getClose();
        return sum / period;
    }

    private double stdDev(List<Candle> candles, int endIdx, double mean) {
        double variance = 0;
        for (int i = endIdx - period + 1; i <= endIdx; i++) {
            double diff = candles.get(i).getClose() - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / period);
    }
}
