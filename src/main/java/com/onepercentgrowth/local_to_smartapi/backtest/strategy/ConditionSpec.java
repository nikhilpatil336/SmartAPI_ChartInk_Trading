package com.onepercentgrowth.local_to_smartapi.backtest.strategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionSpec {

    // Indicator name: RSI, EMA, SMA, SMA_VOL, VWAP, ATR, MACD, MACD_SIGNAL,
    //                 BB_UPPER, BB_LOWER, SUPERTREND, ADX, STOCH_K, STOCH_D, CANDLE_COLOR,
    //                 PREV_DAY_LOW, PREV_DAY_HIGH, DAY_LOW, DAY_HIGH,
    //                 CANDLE_RANGE_PCT, CANDLE_CLOSED_AT_HIGH, CANDLE_CLOSED_AT_LOW
    private String indicator;

    // Period: 14 for RSI_14, 9 for EMA_9, 0 for VWAP / CANDLE_COLOR / day-level / candle-structure indicators
    private int period;

    // Timeframe code: 1M, 5M, 15M, 30M, 60M, 1D
    private String timeframe;

    // Operator: LT, GT, LTE, GTE, EQ,
    //           CLOSE_ABOVE_INDICATOR, CLOSE_BELOW_INDICATOR,
    //           VOL_GT_INDICATOR, VOL_LT_INDICATOR,
    //           LOW_LT_INDICATOR, LOW_GT_INDICATOR, LOW_EQUALS_INDICATOR,
    //           HIGH_LT_INDICATOR, HIGH_GT_INDICATOR, HIGH_EQUALS_INDICATOR
    private String op;

    // Threshold value — used by LT/GT/LTE/GTE/EQ; 0 for price/vol comparison ops
    private double value;

    // 0 = alert candle, 1 = one candle before alert, etc.
    private int offset;

    public String indicatorKey() {
        return timeframe.toUpperCase() + ":" + indicator.toUpperCase() + "_" + period + ":" + offset;
    }
}
