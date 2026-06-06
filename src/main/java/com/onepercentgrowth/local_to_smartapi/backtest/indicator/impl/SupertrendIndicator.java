package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;

public class SupertrendIndicator implements Indicator {

    private final int atrPeriod;
    private final double multiplier;

    public SupertrendIndicator(int atrPeriod, double multiplier) {
        this.atrPeriod = atrPeriod;
        this.multiplier = multiplier;
    }

    public SupertrendIndicator() {
        this(10, 3.0);
    }

    @Override
    public String name() { return "SUPERTREND"; }

    // Returns: 1.0 = uptrend (bullish), -1.0 = downtrend (bearish)
    // Uses Wilder ATR internally and tracks trend flip state from candle 0 to endIdx.
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < atrPeriod || endIdx >= candles.size()) return Double.NaN;

        AtrIndicator atrCalc = new AtrIndicator(atrPeriod);

        // We must iterate from the beginning to properly track trend flips
        double supertrend = 0;
        boolean uptrend = true;
        double prevUpperBand = 0, prevLowerBand = 0;

        for (int i = atrPeriod; i <= endIdx; i++) {
            double atr = atrCalc.compute(candles, i);
            if (Double.isNaN(atr)) continue;

            Candle c = candles.get(i);
            double hl2 = (c.getHigh() + c.getLow()) / 2.0;
            double upperBand = hl2 + multiplier * atr;
            double lowerBand = hl2 - multiplier * atr;

            // Adjust bands to not widen against trend
            if (i > atrPeriod) {
                upperBand = (upperBand < prevUpperBand || candles.get(i - 1).getClose() > prevUpperBand)
                        ? upperBand : prevUpperBand;
                lowerBand = (lowerBand > prevLowerBand || candles.get(i - 1).getClose() < prevLowerBand)
                        ? lowerBand : prevLowerBand;
            }

            // Determine trend direction
            if (uptrend) {
                if (c.getClose() < lowerBand) {
                    uptrend = false;
                    supertrend = upperBand;
                } else {
                    supertrend = lowerBand;
                }
            } else {
                if (c.getClose() > upperBand) {
                    uptrend = true;
                    supertrend = lowerBand;
                } else {
                    supertrend = upperBand;
                }
            }

            prevUpperBand = upperBand;
            prevLowerBand = lowerBand;
        }

        return uptrend ? 1.0 : -1.0;
    }
}
