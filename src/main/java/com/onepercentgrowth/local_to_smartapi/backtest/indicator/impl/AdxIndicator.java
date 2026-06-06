package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;

public class AdxIndicator implements Indicator {

    private final int period;

    public AdxIndicator(int period) {
        this.period = period;
    }

    public AdxIndicator() {
        this(14);
    }

    @Override
    public String name() { return "ADX"; }

    // Returns ADX value (0–100). Higher = stronger trend.
    // Uses Wilder smoothing for TR, +DM, -DM.
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        // Need 2*period candles minimum: period for seed + period for DX smoothing
        if (endIdx < period * 2 || endIdx >= candles.size()) return Double.NaN;

        int startFrom = Math.max(1, endIdx - period * 5);

        // Seed averages
        double smoothedTr  = 0, smoothedPdm = 0, smoothedMdm = 0;
        for (int i = startFrom; i < startFrom + period && i <= endIdx; i++) {
            smoothedTr  += trueRange(candles, i);
            smoothedPdm += plusDm(candles, i);
            smoothedMdm += minusDm(candles, i);
        }

        // Wilder smooth from seedStart+period
        for (int i = startFrom + period; i <= endIdx; i++) {
            smoothedTr  = smoothedTr  - (smoothedTr  / period) + trueRange(candles, i);
            smoothedPdm = smoothedPdm - (smoothedPdm / period) + plusDm(candles, i);
            smoothedMdm = smoothedMdm - (smoothedMdm / period) + minusDm(candles, i);
        }

        if (smoothedTr == 0) return 0;
        double plusDi  = 100 * smoothedPdm / smoothedTr;
        double minusDi = 100 * smoothedMdm / smoothedTr;
        double diSum   = plusDi + minusDi;
        if (diSum == 0) return 0;

        // Single-period DX → ADX seed not computed here (approximation: return DX)
        return 100 * Math.abs(plusDi - minusDi) / diSum;
    }

    private double trueRange(List<Candle> candles, int i) {
        Candle c = candles.get(i), p = candles.get(i - 1);
        return Math.max(c.getHigh() - c.getLow(),
               Math.max(Math.abs(c.getHigh() - p.getClose()),
                        Math.abs(c.getLow()  - p.getClose())));
    }

    private double plusDm(List<Candle> candles, int i) {
        double up   = candles.get(i).getHigh() - candles.get(i - 1).getHigh();
        double down = candles.get(i - 1).getLow() - candles.get(i).getLow();
        return (up > down && up > 0) ? up : 0;
    }

    private double minusDm(List<Candle> candles, int i) {
        double up   = candles.get(i).getHigh() - candles.get(i - 1).getHigh();
        double down = candles.get(i - 1).getLow() - candles.get(i).getLow();
        return (down > up && down > 0) ? down : 0;
    }
}
