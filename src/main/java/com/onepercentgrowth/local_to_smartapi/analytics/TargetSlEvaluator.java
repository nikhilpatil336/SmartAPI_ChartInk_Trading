package com.onepercentgrowth.local_to_smartapi.analytics;

import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TargetSlEvaluator {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_DATE_TIME;
    private static final double TRAILING_GAP_PCT = 0.001; // 0.1% trail gap

    /**
     * @param evalCandles      candle list used for evaluation (1-min or 5-min)
     * @param entryStartIndex  index of the first candle to treat as the entry (0 for pre-filtered 1-min list;
     *                         alertCandleIndex+1 for the 5-min fallback list)
     */
    public static EvalResult evaluate(
            List<Candle> evalCandles,
            int entryStartIndex,
            double targetPrice,
            double slPrice,
            boolean isLong,
            LocalTime analyticsSquareoffTime) {

        List<Candle> allCandles = evalCandles;
        EvalResult result = new EvalResult();

        int entryIndex = entryStartIndex;
        if (entryIndex >= allCandles.size()) {
            result.hitFirst = "SQUAREOFF";
            result.squareoffReason = "AUTO_SQUAREOFF";
            result.tradeOutcome = "SQUAREOFF_EXIT";
            result.trailingOutcome = "NOT_ACTIVATED";
            return result;
        }

        LocalDateTime entryTime = LocalDateTime.parse(allCandles.get(entryIndex).getTimestamp(), TS_FMT);

        boolean trailingActive = false;
        double trailingMaxFavorable = isLong ? 0.0 : Double.MAX_VALUE;
        double trailingSl = 0.0;

        for (int i = entryIndex; i < allCandles.size(); i++) {
            Candle candle = allCandles.get(i);
            LocalDateTime candleTime = LocalDateTime.parse(candle.getTimestamp(), TS_FMT);

            // Squareoff cutoff check (time-of-day comparison)
            if (!candleTime.toLocalTime().isBefore(analyticsSquareoffTime)) {
                result.squareoffPrice = candle.getOpen();
                result.squareoffReason = "AUTO_SQUAREOFF";
                result.hitFirst = trailingActive ? "SQUAREOFF" : "SQUAREOFF";
                result.tradeOutcome = "SQUAREOFF_EXIT";
                result.timeToHitMins = (int) Duration.between(entryTime, candleTime).toMinutes();
                if (trailingActive) {
                    result.trailingActivated = 1;
                    result.trailingMaxFavorable = trailingMaxFavorable;
                    result.trailingSlExitPrice = trailingSl;
                    result.trailingOutcome = "SQUAREOFF_AFTER_TRAILING";
                } else {
                    result.trailingOutcome = "NOT_ACTIVATED";
                }
                return result;
            }

            int diffMins = (int) Duration.between(entryTime, candleTime).toMinutes();

            if (!trailingActive) {
                boolean targetHit = isLong ? candle.getHigh() >= targetPrice : candle.getLow() <= targetPrice;
                boolean slHit     = isLong ? candle.getLow()  <= slPrice     : candle.getHigh() >= slPrice;

                if (targetHit && slHit) {
                    // Both in same candle — cannot determine order; record as WIN, no trailing
                    result.targetHit = 1;
                    result.slHit = 1;
                    result.hitFirst = "SAME_CANDLE";
                    result.squareoffReason = "SAME_CANDLE";
                    result.tradeOutcome = "WIN";
                    result.timeToHitMins = diffMins;
                    result.trailingOutcome = "NOT_ACTIVATED";
                    result.hitFirstCandleOpen   = candle.getOpen();
                    result.hitFirstCandleHigh   = candle.getHigh();
                    result.hitFirstCandleLow    = candle.getLow();
                    result.hitFirstCandleClose  = candle.getClose();
                    result.hitFirstCandleVolume = candle.getVolume();
                    return result;
                } else if (targetHit) {
                    // Target hit — activate trailing
                    result.targetHit = 1;
                    result.hitFirst = "TARGET";
                    result.timeToHitMins = diffMins;
                    result.hitFirstCandleOpen   = candle.getOpen();
                    result.hitFirstCandleHigh   = candle.getHigh();
                    result.hitFirstCandleLow    = candle.getLow();
                    result.hitFirstCandleClose  = candle.getClose();
                    result.hitFirstCandleVolume = candle.getVolume();
                    trailingActive = true;
                    trailingMaxFavorable = isLong ? candle.getHigh() : candle.getLow();
                    trailingSl = isLong
                            ? trailingMaxFavorable * (1 - TRAILING_GAP_PCT)
                            : trailingMaxFavorable * (1 + TRAILING_GAP_PCT);

                    // Check if trailing SL is already triggered on this same candle
                    boolean trailingHitSameCandle = isLong
                            ? candle.getLow() <= trailingSl
                            : candle.getHigh() >= trailingSl;
                    if (trailingHitSameCandle) {
                        result.trailingActivated = 1;
                        result.trailingMaxFavorable = trailingMaxFavorable;
                        result.trailingSlExitPrice = trailingSl;
                        result.trailingOutcome = "TRAILING_EXIT";
                        result.squareoffReason = "TRAILING_SL";
                        result.squareoffPrice = trailingSl;
                        result.tradeOutcome = "WIN";
                        return result;
                    }
                    // Continue loop with trailing active
                } else if (slHit) {
                    result.slHit = 1;
                    result.hitFirst = "SL";
                    result.squareoffReason = "SL_HIT";
                    result.tradeOutcome = "LOSS";
                    result.timeToHitMins = diffMins;
                    result.trailingOutcome = "NOT_ACTIVATED";
                    result.hitFirstCandleOpen   = candle.getOpen();
                    result.hitFirstCandleHigh   = candle.getHigh();
                    result.hitFirstCandleLow    = candle.getLow();
                    result.hitFirstCandleClose  = candle.getClose();
                    result.hitFirstCandleVolume = candle.getVolume();
                    return result;
                }
            } else {
                // Trailing is active — update max favorable and check trailing SL
                if (isLong) {
                    if (candle.getHigh() > trailingMaxFavorable) {
                        trailingMaxFavorable = candle.getHigh();
                        trailingSl = trailingMaxFavorable * (1 - TRAILING_GAP_PCT);
                    }
                    if (candle.getLow() <= trailingSl) {
                        result.trailingActivated = 1;
                        result.trailingMaxFavorable = trailingMaxFavorable;
                        result.trailingSlExitPrice = trailingSl;
                        result.trailingOutcome = "TRAILING_EXIT";
                        result.squareoffReason = "TRAILING_SL";
                        result.squareoffPrice = trailingSl;
                        result.tradeOutcome = "WIN";
                        result.timeToHitMins = diffMins;
                        result.hitFirstCandleOpen   = candle.getOpen();
                        result.hitFirstCandleHigh   = candle.getHigh();
                        result.hitFirstCandleLow    = candle.getLow();
                        result.hitFirstCandleClose  = candle.getClose();
                        result.hitFirstCandleVolume = candle.getVolume();
                        return result;
                    }
                } else {
                    if (candle.getLow() < trailingMaxFavorable) {
                        trailingMaxFavorable = candle.getLow();
                        trailingSl = trailingMaxFavorable * (1 + TRAILING_GAP_PCT);
                    }
                    if (candle.getHigh() >= trailingSl) {
                        result.trailingActivated = 1;
                        result.trailingMaxFavorable = trailingMaxFavorable;
                        result.trailingSlExitPrice = trailingSl;
                        result.trailingOutcome = "TRAILING_EXIT";
                        result.squareoffReason = "TRAILING_SL";
                        result.squareoffPrice = trailingSl;
                        result.tradeOutcome = "WIN";
                        result.timeToHitMins = diffMins;
                        result.hitFirstCandleOpen   = candle.getOpen();
                        result.hitFirstCandleHigh   = candle.getHigh();
                        result.hitFirstCandleLow    = candle.getLow();
                        result.hitFirstCandleClose  = candle.getClose();
                        result.hitFirstCandleVolume = candle.getVolume();
                        return result;
                    }
                }
            }
        }

        // All candles exhausted before squareoff time (data gap / short day)
        Candle last = allCandles.get(allCandles.size() - 1);
        result.squareoffPrice = last.getClose();
        result.squareoffReason = "AUTO_SQUAREOFF";
        result.hitFirst = "SQUAREOFF";
        result.tradeOutcome = "SQUAREOFF_EXIT";
        if (trailingActive) {
            result.trailingActivated = 1;
            result.trailingMaxFavorable = trailingMaxFavorable;
            result.trailingSlExitPrice = trailingSl;
            result.trailingOutcome = "SQUAREOFF_AFTER_TRAILING";
        } else {
            result.trailingOutcome = "NOT_ACTIVATED";
        }
        return result;
    }

    public static class EvalResult {
        public int targetHit = 0;
        public int slHit = 0;
        public String hitFirst = "NONE";
        public double squareoffPrice = 0;
        public String squareoffReason = "NONE";
        public int timeToHitMins = 0;
        public String tradeOutcome = "SQUAREOFF_EXIT";
        // trailing SL
        public int trailingActivated = 0;
        public double trailingMaxFavorable = 0;
        public double trailingSlExitPrice = 0;
        public String trailingOutcome = "NOT_ACTIVATED";
        public double hitFirstCandleOpen   = 0;
        public double hitFirstCandleHigh   = 0;
        public double hitFirstCandleLow    = 0;
        public double hitFirstCandleClose  = 0;
        public long   hitFirstCandleVolume = 0;
    }
}
