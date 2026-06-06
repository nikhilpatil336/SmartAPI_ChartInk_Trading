package com.onepercentgrowth.local_to_smartapi.backtest.strategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrategyConfig {

    private String name;
    private String description;
    private boolean enabled;

    // ─── Entry ───────────────────────────────────────────────────────────────
    // NEXT_CANDLE_OPEN / CURRENT_CLOSE / BREAKOUT
    private String entryTiming = "NEXT_CANDLE_OPEN";
    // For BREAKOUT: offset added to alert candle high (LONG) or subtracted from low (SHORT)
    private double breakoutBuffer = 0.0;

    // ─── Stop-loss ────────────────────────────────────────────────────────────
    // FIXED_PCT / ATR_MULTIPLE
    private String slMethod = "FIXED_PCT";
    private double slPct = 0.005;           // 0.5% — used by FIXED_PCT
    private double slAtrMultiple = 1.5;     // used by ATR_MULTIPLE
    private int slAtrPeriod = 14;
    private String slAtrTimeframe = "5M";

    // ─── Target ───────────────────────────────────────────────────────────────
    // FIXED_PCT / ATR_MULTIPLE / RR_MULTIPLE
    private String targetMethod = "RR_MULTIPLE";
    private double targetPct = 0.010;       // 1% — used by FIXED_PCT
    private double targetAtrMultiple = 3.0; // used by ATR_MULTIPLE
    private String targetAtrTimeframe = "5M";
    private double targetRr = 2.0;          // used by RR_MULTIPLE (2:1 R/R)

    // ─── Conditions ───────────────────────────────────────────────────────────
    private List<ConditionSpec> longConditions;
    private List<ConditionSpec> shortConditions;
    private boolean requireAllLong = true;   // true = AND logic, false = OR logic
    private boolean requireAllShort = true;

    // ─── Take-all overrides (bypass condition evaluation entirely) ────────────
    private boolean takeAllLong = false;
    private boolean takeAllShort = false;

    // ─── Optional overrides ───────────────────────────────────────────────────
    // Override global squareoff time for this strategy (null = use global config)
    private String squareoffTime;

    // Latest allowed entry candle time (HH:mm). Trades after this time are skipped. null = no restriction.
    private String latestEntryTime;

    // ─── Target ATR period (null = fall back to slAtrPeriod) ─────────────────
    private Integer targetAtrPeriod;

    // ─── Configurable indicator params (null = use framework defaults) ────────
    private Integer macdFastPeriod;      // default 12
    private Integer macdSlowPeriod;      // default 26
    private Integer macdSignalPeriod;    // default 9
    private Double supertrendMultiplier; // default 3.0
    private Double bbStdDev;             // default 2.0
}
