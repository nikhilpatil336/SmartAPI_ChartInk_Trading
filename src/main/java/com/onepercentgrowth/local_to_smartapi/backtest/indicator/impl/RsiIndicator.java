package com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.Indicator;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.util.List;

public class RsiIndicator implements Indicator {

    private final int period;

    public RsiIndicator(int period) {
        this.period = period;
    }

    @Override
    public String name() { return "RSI"; }

    // Wilder smoothing RSI: seed with simple average of first `period` gains/losses,
    // then use Wilder's smoothing for the rest.
    @Override
    public double compute(List<Candle> candles, int endIdx) {
        // Need at least period+1 candles to compute first avg gain/loss
        if (endIdx < period || endIdx >= candles.size()) return Double.NaN;

        // Start as early as possible for better Wilder smoothing accuracy
        int startFrom = Math.max(1, endIdx - period * 5);

        // Seed: simple average of first `period` up/down moves starting at startFrom
        double avgGain = 0, avgLoss = 0;
        for (int i = startFrom; i < startFrom + period && i <= endIdx; i++) {
            double change = candles.get(i).getClose() - candles.get(i - 1).getClose();
            if (change > 0) avgGain += change;
            else             avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;

        // Wilder smoothing from seedStart+period to endIdx
        for (int i = startFrom + period; i <= endIdx; i++) {
            double change = candles.get(i).getClose() - candles.get(i - 1).getClose();
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? Math.abs(change) : 0;
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }

        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}
