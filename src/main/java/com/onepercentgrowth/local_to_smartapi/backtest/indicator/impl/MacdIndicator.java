package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MacdIndicator implements Indicator {

    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;

    public MacdIndicator(int fastPeriod, int slowPeriod, int signalPeriod) {
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }

    public MacdIndicator() {
        this(12, 26, 9);
    }

    @Override
    public String name() { return "MACD"; }

    // Returns MACD line only via compute(); use computeAll() for signal + histogram
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < slowPeriod - 1 || endIdx >= candles.size()) return Double.NaN;
        return ema(candles, endIdx, fastPeriod) - ema(candles, endIdx, slowPeriod);
    }

    @Override
    public Map<String, Double> computeAll(List<Candle> candles, int endIdx) {
        if (endIdx < slowPeriod + signalPeriod - 2 || endIdx >= candles.size()) {
            return Map.of("MACD_0", Double.NaN, "MACD_SIGNAL_0", Double.NaN, "MACD_HIST_0", Double.NaN);
        }

        // Build MACD line series from (endIdx - signalPeriod*2) to endIdx for signal EMA seeding
        List<Double> macdSeries = new ArrayList<>();
        int seriesStart = Math.max(slowPeriod - 1, endIdx - signalPeriod * 4);
        for (int i = seriesStart; i <= endIdx; i++) {
            macdSeries.add(ema(candles, i, fastPeriod) - ema(candles, i, slowPeriod));
        }

        double macdLine = macdSeries.get(macdSeries.size() - 1);
        double signal   = emaOfSeries(macdSeries, signalPeriod);
        double hist     = macdLine - signal;

        return Map.of("MACD_0", macdLine, "MACD_SIGNAL_0", signal, "MACD_HIST_0", hist);
    }

    private double ema(List<Candle> candles, int endIdx, int period) {
        if (endIdx < period - 1) return Double.NaN;
        int startFrom = Math.max(0, endIdx - period * 4);
        double k = 2.0 / (period + 1);
        double e = 0;
        for (int i = startFrom; i < startFrom + period && i < candles.size(); i++) {
            e += candles.get(i).getClose();
        }
        e /= period;
        for (int i = startFrom + period; i <= endIdx; i++) {
            e = candles.get(i).getClose() * k + e * (1 - k);
        }
        return e;
    }

    private double emaOfSeries(List<Double> series, int period) {
        if (series.size() < period) return Double.NaN;
        double k = 2.0 / (period + 1);
        double e = 0;
        for (int i = 0; i < period; i++) e += series.get(i);
        e /= period;
        for (int i = period; i < series.size(); i++) {
            e = series.get(i) * k + e * (1 - k);
        }
        return e;
    }
}
