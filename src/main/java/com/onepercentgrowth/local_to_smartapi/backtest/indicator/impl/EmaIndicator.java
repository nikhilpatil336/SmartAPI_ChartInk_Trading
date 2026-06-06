package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;

public class EmaIndicator implements Indicator {

    private final int period;

    public EmaIndicator(int period) {
        this.period = period;
    }

    @Override
    public String name() { return "EMA"; }

    @Override
    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < period - 1 || endIdx >= candles.size()) return Double.NaN;
        double k = 2.0 / (period + 1);
        // Seed with SMA of first `period` candles
        double ema = 0;
        int seedStart = endIdx - period + 1;
        // Walk from the earliest available data to endIdx to get accurate EMA
        int startFrom = Math.max(0, seedStart - period * 3); // use more history if available
        // Seed EMA from startFrom using SMA of first `period` values
        if (startFrom + period - 1 >= candles.size()) return Double.NaN;
        double sum = 0;
        for (int i = startFrom; i < startFrom + period; i++) {
            sum += candles.get(i).getClose();
        }
        ema = sum / period;
        for (int i = startFrom + period; i <= endIdx; i++) {
            ema = candles.get(i).getClose() * k + ema * (1 - k);
        }
        return ema;
    }
}
