package com.onepercentgrowth.local_to_smartapi.backtest.runner;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BacktestResult {

    private String strategyName;
    private String tradeDate;
    private String stock;
    private String direction;          // LONG / SHORT / NO_TRADE
    private String entryTiming;
    private double entryPrice;
    private double targetPrice;
    private double slPrice;
    private double riskRewardRatio;
    private String tradeOutcome;       // WIN / LOSS / SQUAREOFF_EXIT / NO_TRADE
    private String hitFirst;           // TARGET / SL / SAME_CANDLE / SQUAREOFF / NONE
    private double squareoffPrice;
    private String squareoffReason;
    private int timeToHitMins;
    private int trailingActivated;
    private double trailingMaxFavorable;
    private double trailingSlExitPrice;
    private String trailingOutcome;
    private String reason;             // from StrategyDecision — why the decision was made
    private String alertTime;
    private String timeframe;          // primary timeframe used for evaluation
    private double exitPrice;
    private double pnlPoints;          // exitPrice - entryPrice (long) or entryPrice - exitPrice (short)
    private double pnlPct;             // pnlPoints / entryPrice * 100
    private double totalChargesPct;    // all-in round-trip charges as % of entry price
    private double pnlAfterChargesPct; // pnlPct − totalChargesPct
}
