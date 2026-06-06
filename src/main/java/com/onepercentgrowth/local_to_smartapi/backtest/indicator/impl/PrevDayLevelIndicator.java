package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrevDayLevelIndicator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE_TIME;

    public enum Type { PREV_DAY_LOW, PREV_DAY_HIGH, DAY_LOW, DAY_HIGH }

    private final Type type;

    public PrevDayLevelIndicator(Type type) {
        this.type = type;
    }

    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < 0 || endIdx >= candles.size()) return Double.NaN;

        LocalDate alertDate = LocalDateTime.parse(candles.get(endIdx).getTimestamp(), FMT).toLocalDate();

        return switch (type) {
            case DAY_LOW -> {
                double low = Double.MAX_VALUE;
                for (int i = endIdx; i >= 0; i--) {
                    if (!LocalDateTime.parse(candles.get(i).getTimestamp(), FMT).toLocalDate().equals(alertDate)) break;
                    low = Math.min(low, candles.get(i).getLow());
                }
                yield low == Double.MAX_VALUE ? Double.NaN : low;
            }
            case DAY_HIGH -> {
                double high = -Double.MAX_VALUE;
                for (int i = endIdx; i >= 0; i--) {
                    if (!LocalDateTime.parse(candles.get(i).getTimestamp(), FMT).toLocalDate().equals(alertDate)) break;
                    high = Math.max(high, candles.get(i).getHigh());
                }
                yield high == -Double.MAX_VALUE ? Double.NaN : high;
            }
            case PREV_DAY_LOW -> {
                LocalDate prevDay = null;
                double low = Double.MAX_VALUE;
                for (int i = endIdx - 1; i >= 0; i--) {
                    LocalDate d = LocalDateTime.parse(candles.get(i).getTimestamp(), FMT).toLocalDate();
                    if (d.equals(alertDate)) continue;
                    if (prevDay == null) prevDay = d;
                    if (!d.equals(prevDay)) break;
                    low = Math.min(low, candles.get(i).getLow());
                }
                yield low == Double.MAX_VALUE ? Double.NaN : low;
            }
            case PREV_DAY_HIGH -> {
                LocalDate prevDay = null;
                double high = -Double.MAX_VALUE;
                for (int i = endIdx - 1; i >= 0; i--) {
                    LocalDate d = LocalDateTime.parse(candles.get(i).getTimestamp(), FMT).toLocalDate();
                    if (d.equals(alertDate)) continue;
                    if (prevDay == null) prevDay = d;
                    if (!d.equals(prevDay)) break;
                    high = Math.max(high, candles.get(i).getHigh());
                }
                yield high == -Double.MAX_VALUE ? Double.NaN : high;
            }
        };
    }
}
