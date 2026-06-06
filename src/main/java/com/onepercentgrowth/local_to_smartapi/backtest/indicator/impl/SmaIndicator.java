package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;

public class SmaIndicator implements Indicator {

    private final int period;

    public SmaIndicator(int period) {
        this.period = period;
    }

    @Override
    public String name() { return "SMA"; }

    @Override
    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < period - 1 || endIdx >= candles.size()) return Double.NaN;
        double sum = 0;
        for (int i = endIdx - period + 1; i <= endIdx; i++) {
            sum += candles.get(i).getClose();
        }
        return sum / period;
    }
}
