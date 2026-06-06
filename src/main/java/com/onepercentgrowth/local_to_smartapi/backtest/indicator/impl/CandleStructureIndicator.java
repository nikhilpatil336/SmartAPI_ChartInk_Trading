package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;

public class CandleStructureIndicator {

    public enum Type { CANDLE_RANGE_PCT, CANDLE_CLOSED_AT_HIGH, CANDLE_CLOSED_AT_LOW }

    private final Type type;

    public CandleStructureIndicator(Type type) {
        this.type = type;
    }

    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < 0 || endIdx >= candles.size()) return Double.NaN;
        Candle c = candles.get(endIdx);
        return switch (type) {
            case CANDLE_RANGE_PCT      -> c.getClose() > 0
                    ? (c.getHigh() - c.getLow()) / c.getClose() * 100.0 : Double.NaN;
            case CANDLE_CLOSED_AT_HIGH -> Double.compare(c.getClose(), c.getHigh()) == 0 ? 1.0 : 0.0;
            case CANDLE_CLOSED_AT_LOW  -> Double.compare(c.getClose(), c.getLow())  == 0 ? 1.0 : 0.0;
        };
    }
}
