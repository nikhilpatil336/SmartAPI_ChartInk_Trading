package com.onepercentgrowth.local_to_smartapi.backtest.strategy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyDecision {

    private String direction;          // LONG / SHORT / NO_TRADE
    private EntryTiming entryTiming;
    private double breakoutPrice;      // for BREAKOUT — price level that triggers entry
    private String reason;             // human-readable explanation for Excel output

    public static StrategyDecision noTrade(String reason) {
        StrategyDecision d = new StrategyDecision();
        d.direction = "NO_TRADE";
        d.reason = reason;
        return d;
    }

    public boolean isTrade() { return !"NO_TRADE".equals(direction); }
    public boolean isLong()  { return "LONG".equals(direction); }
    public boolean isShort() { return "SHORT".equals(direction); }
}
