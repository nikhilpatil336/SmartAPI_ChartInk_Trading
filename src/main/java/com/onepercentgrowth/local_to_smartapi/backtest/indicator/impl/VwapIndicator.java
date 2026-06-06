package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VwapIndicator implements Indicator {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public String name() { return "VWAP"; }

    // Intraday cumulative VWAP. Resets whenever the date changes.
    // Typical price = (H + L + C) / 3
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        if (endIdx < 0 || endIdx >= candles.size()) return Double.NaN;

        LocalDateTime endTime = LocalDateTime.parse(candles.get(endIdx).getTimestamp(), TS_FMT);
        String endDate = endTime.toLocalDate().toString();

        double cumTpv = 0;
        long   cumVol = 0;

        for (int i = 0; i <= endIdx; i++) {
            LocalDateTime t = LocalDateTime.parse(candles.get(i).getTimestamp(), TS_FMT);
            // Reset when we cross into a new day
            if (!t.toLocalDate().toString().equals(endDate)) {
                cumTpv = 0;
                cumVol = 0;
                continue;
            }
            Candle c = candles.get(i);
            double tp = (c.getHigh() + c.getLow() + c.getClose()) / 3.0;
            cumTpv += tp * c.getVolume();
            cumVol += c.getVolume();
        }

        return cumVol == 0 ? Double.NaN : cumTpv / cumVol;
    }
}
