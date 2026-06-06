package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;

public class CandleColorIndicator implements Indicator {

    @Override
    public String name() { return "CANDLE_COLOR"; }

    // Returns: 1 = GREEN (close > open), -1 = RED (close < open), 0 = DOJI
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < 0 || endIdx >= candles.size()) return Double.NaN;
        Candle c = candles.get(endIdx);
        if (c.getClose() > c.getOpen()) return 1;
        if (c.getClose() < c.getOpen()) return -1;
        return 0;
    }
}
