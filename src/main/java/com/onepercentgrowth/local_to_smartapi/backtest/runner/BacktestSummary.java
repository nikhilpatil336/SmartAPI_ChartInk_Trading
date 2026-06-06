package com.onepercentgrowth.local_to_smartapi.backtest.runner;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
public class BacktestSummary {

    private static final int MIN_SAMPLE_SIZE = 30;

    private String strategyName;
    private int totalAlerts;
    private int tradesTaken;
    private int wins;
    private int losses;
    private int squareoffs;
    private double winRate;              // wins / tradesTaken (squareoffs count in denominator)
    private double expectancy;           // avgPnlPerTrade — true expected value per trade (includes all outcomes)
    private double avgWinRR;             // average R:R on winning trades only (secondary metric)
    private double totalPnlPct;
    private double avgPnlPerTrade;
    private double totalPnlAfterChargesPct;
    private double avgPnlAfterChargesPct;
    private double maxDrawdown;          // largest peak-to-trough drop in cumulative P&L%
    private int maxConsecutiveLosses;    // longest losing run (LOSS + negative SQUAREOFF_EXIT)
    private double sharpeRatio;          // avgPnlAfterChargesPct / stdDev(pnlAfterChargesPct); 0 if stdDev=0
    private double compoundedReturnPct;  // geometric product of all per-trade returns
    private double niftyReturnPct;       // Nifty 50 return over the same alert date range
    private String niftyDateRange;       // e.g. "2025-02-03 → 2025-05-30"
    private boolean beatsNifty;          // totalPnlAfterChargesPct > niftyReturnPct
    private double niftyAvgPacePct;      // 12.5% × (days/365) for the alert date range
    private boolean beatsNiftyAvgPace;   // totalPnlAfterChargesPct > niftyAvgPacePct
    private String verdict;              // PROFITABLE / LOSS-MAKING / INSUFFICIENT_DATA
    private List<BacktestResult> trades = new ArrayList<>();
    private Map<String, MonthlyStats> monthlyStats = new LinkedHashMap<>();

    public Map<String, MonthlyStats> getMonthlyStats() { return monthlyStats; }

    @Data
    @NoArgsConstructor
    public static class MonthlyStats {
        private String month;
        private int tradesTaken;
        private int wins;
        private int losses;
        private int squareoffs;
        private double winRate;
        private double totalPnlAfterChargesPct;
        private double compoundedReturnPct;
        private double niftyReturnPct;
    }

    public void addResult(BacktestResult r) {
        trades.add(r);
        totalAlerts++;
        if ("NO_TRADE".equals(r.getTradeOutcome())) return;
        tradesTaken++;
        switch (r.getTradeOutcome()) {
            case "WIN"            -> wins++;
            case "LOSS"           -> losses++;
            case "SQUAREOFF_EXIT" -> squareoffs++;
        }
    }

    public void computeStats() {
        if (tradesTaken == 0) {
            verdict = "INSUFFICIENT_DATA";
            return;
        }

        // Win rate: wins as fraction of all trades taken (squareoffs in denominator)
        winRate = (double) wins / tradesTaken;

        avgWinRR = trades.stream()
                .filter(r -> "WIN".equals(r.getTradeOutcome()))
                .mapToDouble(BacktestResult::getRiskRewardRatio)
                .average()
                .orElse(0);

        totalPnlPct = trades.stream()
                .filter(r -> !"NO_TRADE".equals(r.getTradeOutcome()))
                .mapToDouble(BacktestResult::getPnlPct)
                .sum();
        avgPnlPerTrade = totalPnlPct / tradesTaken;

        totalPnlAfterChargesPct = trades.stream()
                .filter(r -> !"NO_TRADE".equals(r.getTradeOutcome()))
                .mapToDouble(BacktestResult::getPnlAfterChargesPct)
                .sum();
        avgPnlAfterChargesPct = totalPnlAfterChargesPct / tradesTaken;

        double compoundFactor = trades.stream()
                .filter(r -> !"NO_TRADE".equals(r.getTradeOutcome()))
                .mapToDouble(r -> 1.0 + r.getPnlAfterChargesPct() / 100.0)
                .reduce(1.0, (a, b) -> a * b);
        compoundedReturnPct = (compoundFactor - 1.0) * 100.0;

        // Expectancy = avg P&L after charges per trade — matches verdict basis
        expectancy = avgPnlAfterChargesPct;

        // Max drawdown: largest peak-to-trough drop in cumulative P&L%
        double cumPnl = 0, peak = 0;
        maxDrawdown = 0;
        int consec = 0;
        maxConsecutiveLosses = 0;
        for (BacktestResult r : trades) {
            if ("NO_TRADE".equals(r.getTradeOutcome())) continue;
            cumPnl += r.getPnlAfterChargesPct();
            if (cumPnl > peak) peak = cumPnl;
            double dd = peak - cumPnl;
            if (dd > maxDrawdown) maxDrawdown = dd;

            boolean isNegative = "LOSS".equals(r.getTradeOutcome())
                    || ("SQUAREOFF_EXIT".equals(r.getTradeOutcome()) && r.getPnlAfterChargesPct() < 0);
            if (isNegative) {
                consec++;
                if (consec > maxConsecutiveLosses) maxConsecutiveLosses = consec;
            } else {
                consec = 0;
            }
        }

        // Sharpe ratio: avgPnlAfterChargesPct / stdDev(pnlAfterChargesPct) — per-trade, no annualization
        if (tradesTaken > 1) {
            double variance = trades.stream()
                    .filter(r -> !"NO_TRADE".equals(r.getTradeOutcome()))
                    .mapToDouble(r -> Math.pow(r.getPnlAfterChargesPct() - avgPnlAfterChargesPct, 2))
                    .sum() / (tradesTaken - 1);
            double stdDev = Math.sqrt(variance);
            sharpeRatio = stdDev > 0 ? avgPnlAfterChargesPct / stdDev : 0;
        }

        if (tradesTaken < MIN_SAMPLE_SIZE) {
            verdict = "INSUFFICIENT_DATA";
        } else {
            verdict = avgPnlAfterChargesPct > 0 ? "PROFITABLE" : "LOSS-MAKING";
        }
    }

    public void computeMonthlyStats(Map<String, Double> niftyByMonth) {
        Map<String, MonthlyStats> temp = new TreeMap<>();
        Map<String, Double> factorMap = new HashMap<>();
        for (BacktestResult r : trades) {
            if ("NO_TRADE".equals(r.getTradeOutcome())) continue;
            String month = r.getTradeDate().substring(0, 7);
            MonthlyStats ms = temp.computeIfAbsent(month, k -> {
                MonthlyStats m = new MonthlyStats();
                m.setMonth(k);
                return m;
            });
            ms.setTradesTaken(ms.getTradesTaken() + 1);
            switch (r.getTradeOutcome()) {
                case "WIN"            -> ms.setWins(ms.getWins() + 1);
                case "LOSS"           -> ms.setLosses(ms.getLosses() + 1);
                case "SQUAREOFF_EXIT" -> ms.setSquareoffs(ms.getSquareoffs() + 1);
            }
            ms.setTotalPnlAfterChargesPct(ms.getTotalPnlAfterChargesPct() + r.getPnlAfterChargesPct());
            factorMap.merge(month, 1.0 + r.getPnlAfterChargesPct() / 100.0, (a, b) -> a * b);
        }
        temp.forEach((month, ms) -> {
            if (ms.getTradesTaken() > 0)
                ms.setWinRate((double) ms.getWins() / ms.getTradesTaken());
            ms.setCompoundedReturnPct((factorMap.getOrDefault(month, 1.0) - 1.0) * 100.0);
            ms.setNiftyReturnPct(niftyByMonth.getOrDefault(month, 0.0));
        });
        monthlyStats.clear();
        monthlyStats.putAll(temp);
    }
}
