package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;

public class AtrIndicator implements Indicator {

    private final int period;

    public AtrIndicator(int period) {
        this.period = period;
    }

    @Override
    public String name() { return "ATR"; }

    // ATR = Wilder-smoothed true range
    // TR = max(high-low, |high-prevClose|, |low-prevClose|)
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < period || endIdx >= candles.size()) return Double.NaN;

        int startFrom = Math.max(1, endIdx - period * 3);

        // Seed: simple average of first `period` true ranges
        double atr = 0;
        for (int i = startFrom; i < startFrom + period && i <= endIdx; i++) {
            atr += trueRange(candles, i);
        }
        atr /= period;

        // Wilder smoothing
        for (int i = startFrom + period; i <= endIdx; i++) {
            atr = (atr * (period - 1) + trueRange(candles, i)) / period;
        }
        return atr;
    }

    private double trueRange(List<Candle> candles, int i) {
        Candle cur  = candles.get(i);
        Candle prev = candles.get(i - 1);
        return Math.max(cur.getHigh() - cur.getLow(),
               Math.max(Math.abs(cur.getHigh() - prev.getClose()),
                        Math.abs(cur.getLow()  - prev.getClose())));
    }
}
