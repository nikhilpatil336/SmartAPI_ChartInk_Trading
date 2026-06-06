package com.onepercentgrowth.local_to_smartapi.backtest.strategy;

import com.onepercentgrowth.local_to_smartapi.backtest.indicator.IndicatorContext;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.OptionalDouble;
import java.util.StringJoiner;

public class ConfigurableBacktestStrategy {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableBacktestStrategy.class);

    private final StrategyConfig config;

    public ConfigurableBacktestStrategy(StrategyConfig config) {
        this.config = config;
    }

    public String name() { return config.getName(); }
    public StrategyConfig getConfig() { return config; }

    public StrategyDecision decide(List<Candle> alertCandles, int alertIdx, IndicatorContext ctx) {
        if (!config.isEnabled()) return StrategyDecision.noTrade("strategy disabled");

        boolean longSignal  = config.isTakeAllLong()  || evaluate(config.getLongConditions(),  config.isRequireAllLong(),  alertCandles, alertIdx, ctx);
        boolean shortSignal = config.isTakeAllShort() || evaluate(config.getShortConditions(), config.isRequireAllShort(), alertCandles, alertIdx, ctx);

        if (longSignal && !shortSignal) {
            StrategyDecision d = new StrategyDecision();
            d.setDirection("LONG");
            d.setEntryTiming(EntryTiming.valueOf(config.getEntryTiming()));
            d.setBreakoutPrice(alertIdx < alertCandles.size()
                    ? alertCandles.get(alertIdx).getHigh() + config.getBreakoutBuffer() : 0);
            d.setReason(config.isTakeAllLong() ? "take-all-long" : "LONG conditions met");
            return d;
        }
        if (shortSignal && !longSignal) {
            StrategyDecision d = new StrategyDecision();
            d.setDirection("SHORT");
            d.setEntryTiming(EntryTiming.valueOf(config.getEntryTiming()));
            d.setBreakoutPrice(alertIdx < alertCandles.size()
                    ? alertCandles.get(alertIdx).getLow() - config.getBreakoutBuffer() : 0);
            d.setReason(config.isTakeAllShort() ? "take-all-short" : "SHORT conditions met");
            return d;
        }
        return StrategyDecision.noTrade(longSignal ? "both-take-all-conflict" : "no conditions met");
    }

    private boolean evaluate(List<ConditionSpec> conditions, boolean requireAll,
                             List<Candle> candles, int alertIdx, IndicatorContext ctx) {
        if (conditions == null || conditions.isEmpty()) return false;
        for (ConditionSpec cond : conditions) {
            boolean result = evaluateCondition(cond, candles, alertIdx, ctx);
            if (requireAll && !result) return false;
            if (!requireAll && result)  return true;
        }
        return requireAll;
    }

    private boolean evaluateCondition(ConditionSpec cond, List<Candle> candles,
                                      int alertIdx, IndicatorContext ctx) {
        int targetIdx = alertIdx - cond.getOffset();
        if (targetIdx < 0 || targetIdx >= candles.size()) return false;

        Candle candle = candles.get(targetIdx);
        String key = cond.indicatorKey();
        OptionalDouble indVal = ctx.get(key);

        if (indVal.isEmpty()) {
            log.warn("Indicator key not found in context: {}", key);
            return false;
        }

        double iv = indVal.getAsDouble();
        double threshold = cond.getValue();

        return switch (cond.getOp().toUpperCase()) {
            case "LT"  -> iv < threshold;
            case "GT"  -> iv > threshold;
            case "LTE" -> iv <= threshold;
            case "GTE" -> iv >= threshold;
            case "EQ"  -> Double.compare(iv, threshold) == 0;
            case "CLOSE_ABOVE_INDICATOR", "PRICE_ABOVE_INDICATOR" -> candle.getClose() > iv;
            case "CLOSE_BELOW_INDICATOR", "PRICE_BELOW_INDICATOR" -> candle.getClose() < iv;
            case "VOL_GT_INDICATOR"      -> candle.getVolume() > iv;
            case "VOL_LT_INDICATOR"      -> candle.getVolume() < iv;
            case "LOW_LT_INDICATOR"      -> candle.getLow() < iv;
            case "LOW_GT_INDICATOR"      -> candle.getLow() > iv;
            case "LOW_EQUALS_INDICATOR"  -> Double.compare(candle.getLow(), iv) == 0;
            case "HIGH_LT_INDICATOR"     -> candle.getHigh() < iv;
            case "HIGH_GT_INDICATOR"     -> candle.getHigh() > iv;
            case "HIGH_EQUALS_INDICATOR" -> Double.compare(candle.getHigh(), iv) == 0;
            default -> {
                log.warn("Unknown op: {}", cond.getOp());
                yield false;
            }
        };
    }
}
