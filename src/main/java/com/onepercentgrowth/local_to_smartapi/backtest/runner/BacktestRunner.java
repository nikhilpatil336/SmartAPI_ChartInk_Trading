package com.onepercentgrowth.local_to_smartapi.backtest.runner;

import com.google.gson.JsonObject;
import com.onepercentgrowth.local_to_smartapi.analytics.TargetSlEvaluator;
import com.onepercentgrowth.local_to_smartapi.backtest.cache.CandleCache;
import com.onepercentgrowth.local_to_smartapi.backtest.cache.CandleCacheService;
import com.onepercentgrowth.local_to_smartapi.backtest.config.StrategyConfigLoader;
import com.onepercentgrowth.local_to_smartapi.backtest.indicator.IndicatorContext;
import com.onepercentgrowth.local_to_smartapi.backtest.indicator.impl.*;
import com.onepercentgrowth.local_to_smartapi.backtest.strategy.*;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BacktestRunner {

    private static final Logger log = LoggerFactory.getLogger(BacktestRunner.class);
    private static final DateTimeFormatter ALERT_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_DATE_TIME;

    private final CandleCacheService cacheService;
    private final StrategyConfigLoader configLoader;
    private final ScripMasterService scripMasterService;
    private final ApplicationProperties applicationProperties;

    public BacktestRunner(CandleCacheService cacheService,
                          StrategyConfigLoader configLoader,
                          ScripMasterService scripMasterService,
                          ApplicationProperties applicationProperties) {
        this.cacheService = cacheService;
        this.configLoader = configLoader;
        this.scripMasterService = scripMasterService;
        this.applicationProperties = applicationProperties;
    }

    public List<BacktestSummary> run(List<AlertEntry> alertEntries) {
        // Deduplicate: keep only first alert per (stock, date) — re-entries not modeled
        Set<String> seenStockDate = new LinkedHashSet<>();
        List<AlertEntry> dedupedAlerts = new ArrayList<>();
        for (AlertEntry a : alertEntries) {
            try {
                String key = a.stock() + "|" + LocalDateTime.parse(a.triggeredAt(), ALERT_FMT).toLocalDate();
                if (seenStockDate.add(key)) dedupedAlerts.add(a);
            } catch (Exception ignored) { dedupedAlerts.add(a); }
        }
        alertEntries = dedupedAlerts;

        // Extract date range from deduped alerts for Nifty benchmark
        LocalDate minDate = null, maxDate = null;
        for (AlertEntry a : alertEntries) {
            try {
                LocalDate d = LocalDateTime.parse(a.triggeredAt(), ALERT_FMT).toLocalDate();
                if (minDate == null || d.isBefore(minDate)) minDate = d;
                if (maxDate == null || d.isAfter(maxDate)) maxDate = d;
            } catch (Exception ignored) {}
        }

        List<StrategyConfig> configs = configLoader.loadAll();
        if (configs.isEmpty()) {
            log.warn("No strategy configs loaded — aborting backtest");
            return List.of();
        }

        List<ConfigurableBacktestStrategy> strategies = configs.stream()
                .filter(StrategyConfig::isEnabled)
                .map(ConfigurableBacktestStrategy::new)
                .toList();

        Map<String, BacktestSummary> summaries = new LinkedHashMap<>();
        for (ConfigurableBacktestStrategy s : strategies) {
            BacktestSummary summary = new BacktestSummary();
            summary.setStrategyName(s.name());
            summaries.put(s.name(), summary);
        }

        LocalTime squareoffTime = LocalTime.parse(applicationProperties.getAnalyticsSquareoffTime());

        for (AlertEntry entry : alertEntries) {
            String alertTimeStr    = entry.triggeredAt();
            String stock           = entry.stock();
            double firstAlertPrice = entry.firstAlertPrice();

            String token = scripMasterService.getTokenForName(stock);
            if (token == null) {
                log.warn("No token for {} — skipping", stock);
                continue;
            }

            LocalDateTime alertTime;
            try {
                alertTime = LocalDateTime.parse(alertTimeStr, ALERT_FMT);
            } catch (Exception e) {
                log.warn("Cannot parse alert time '{}' — skipping", alertTimeStr);
                continue;
            }

            LocalDate alertDate = alertTime.toLocalDate();

            // Determine all timeframes needed across all strategies
            Set<String> neededTfs = collectNeededTimeframes(strategies);

            // Load candles per timeframe
            Map<String, List<Candle>> candlesByTf = new HashMap<>();
            Map<String, Integer> alertIdxByTf     = new HashMap<>();

            for (String tf : neededTfs) {
                // 1M only needs the alert day itself; other TFs need lookback for SMA seeding
                int lookback = "1M".equals(tf) ? 0 : applicationProperties.getHistoricDataDays();
                List<Candle> candles = cacheService.loadOrFetch(stock, token, alertDate, tf, lookback);
                if (candles == null || candles.isEmpty()) {
                    log.warn("No candles for {} {} {} — strategies using this tf will skip", stock, alertDate, tf);
                    continue;
                }
                candlesByTf.put(tf, candles);
                int idx = findNearestIdx(candles, alertTime);
                alertIdxByTf.put(tf, idx);
            }

            // Primary timeframe for signal is 5M; fall back to first available
            String primaryTf = candlesByTf.containsKey("5M") ? "5M"
                    : candlesByTf.keySet().stream().findFirst().orElse(null);
            if (primaryTf == null) continue;

            List<Candle> primaryCandles = candlesByTf.get(primaryTf);
            int primaryAlertIdx = alertIdxByTf.getOrDefault(primaryTf, -1);
            if (primaryAlertIdx < 0) continue;

            // Build IndicatorContext for this alert
            IndicatorContext ctx = buildIndicatorContext(strategies, candlesByTf, alertIdxByTf);

            // Fan out to all strategies
            for (ConfigurableBacktestStrategy strategy : strategies) {
                BacktestSummary summary = summaries.get(strategy.name());
                StrategyDecision decision = strategy.decide(primaryCandles, primaryAlertIdx, ctx);

                if (!decision.isTrade()) {
                    BacktestResult noTradeResult = new BacktestResult();
                    noTradeResult.setStrategyName(strategy.name());
                    noTradeResult.setTradeDate(alertDate.toString());
                    noTradeResult.setStock(stock);
                    noTradeResult.setAlertTime(alertTimeStr);
                    noTradeResult.setDirection("NO_TRADE");
                    noTradeResult.setTradeOutcome("NO_TRADE");
                    noTradeResult.setReason(decision.getReason());
                    summary.addResult(noTradeResult);
                    continue;
                }

                // Resolve entry candle
                Pair<Integer, Double> entryResolved = resolveEntry(primaryCandles, primaryAlertIdx, decision, firstAlertPrice);
                if (entryResolved == null) {
                    BacktestResult noEntry = new BacktestResult();
                    noEntry.setStrategyName(strategy.name());
                    noEntry.setTradeDate(alertDate.toString());
                    noEntry.setStock(stock);
                    noEntry.setAlertTime(alertTimeStr);
                    noEntry.setDirection(decision.getDirection());
                    noEntry.setTradeOutcome("NO_TRADE");
                    noEntry.setReason("breakout never triggered");
                    summary.addResult(noEntry);
                    continue;
                }
                int entryIdx = entryResolved.getLeft();
                double entryPrice = entryResolved.getRight();

                // Resolve SL + target
                StrategyConfig cfg = strategy.getConfig();

                // Reject entry if candle is past the strategy's latest allowed entry time
                if (cfg.getLatestEntryTime() != null) {
                    LocalTime entryCandleTime = LocalDateTime.parse(
                            primaryCandles.get(entryIdx).getTimestamp(), TS_FMT).toLocalTime();
                    if (entryCandleTime.isAfter(LocalTime.parse(cfg.getLatestEntryTime()))) {
                        BacktestResult late = new BacktestResult();
                        late.setStrategyName(strategy.name());
                        late.setTradeDate(alertDate.toString());
                        late.setStock(stock);
                        late.setAlertTime(alertTimeStr);
                        late.setDirection(decision.getDirection());
                        late.setTradeOutcome("NO_TRADE");
                        late.setReason("entry after latest-entry " + cfg.getLatestEntryTime());
                        summary.addResult(late);
                        continue;
                    }
                }
                double slPrice     = resolveSl(entryPrice, decision.isLong(), cfg, ctx, primaryTf);
                double targetPrice = resolveTarget(entryPrice, slPrice, decision.isLong(), cfg, ctx, primaryTf);
                double rr = Math.abs(entryPrice - slPrice) > 0
                        ? Math.abs(targetPrice - entryPrice) / Math.abs(entryPrice - slPrice) : 0;

                // Use 1-min candles for exit evaluation if available, else primary
                List<Candle> evalCandles = candlesByTf.getOrDefault("1M", primaryCandles);
                int evalEntryIdx = entryIdx;
                if (evalCandles != primaryCandles) {
                    LocalDateTime entryTime = LocalDateTime.parse(primaryCandles.get(entryIdx).getTimestamp(), TS_FMT);
                    evalEntryIdx = findNearestIdx(evalCandles, entryTime);
                }

                LocalTime sqTime = cfg.getSquareoffTime() != null
                        ? LocalTime.parse(cfg.getSquareoffTime()) : squareoffTime;

                TargetSlEvaluator.EvalResult eval = TargetSlEvaluator.evaluate(
                        evalCandles, evalEntryIdx, targetPrice, slPrice, decision.isLong(), sqTime);

                BacktestResult result = new BacktestResult();
                result.setStrategyName(strategy.name());
                result.setTradeDate(alertDate.toString());
                result.setStock(stock);
                result.setAlertTime(alertTimeStr);
                result.setDirection(decision.getDirection());
                result.setEntryTiming(decision.getEntryTiming().name());
                result.setEntryPrice(entryPrice);
                result.setTargetPrice(targetPrice);
                result.setSlPrice(slPrice);
                result.setRiskRewardRatio(rr);
                result.setTradeOutcome(eval.tradeOutcome);
                result.setHitFirst(eval.hitFirst);
                result.setSquareoffPrice(eval.squareoffPrice);
                result.setSquareoffReason(eval.squareoffReason);
                result.setTimeToHitMins(eval.timeToHitMins);
                result.setTrailingActivated(eval.trailingActivated);
                result.setTrailingMaxFavorable(eval.trailingMaxFavorable);
                result.setTrailingSlExitPrice(eval.trailingSlExitPrice);
                result.setTrailingOutcome(eval.trailingOutcome);
                result.setReason(decision.getReason());
                result.setTimeframe(primaryTf);

                double exitPrice = switch (eval.tradeOutcome) {
                    case "WIN"  -> "TRAILING_EXIT".equals(eval.trailingOutcome)
                                   ? eval.trailingSlExitPrice : targetPrice;
                    case "LOSS" -> slPrice;
                    default     -> eval.squareoffPrice > 0 ? eval.squareoffPrice : entryPrice;
                };
                double pnlPoints = decision.isLong() ? (exitPrice - entryPrice) : (entryPrice - exitPrice);
                double pnlPct    = entryPrice > 0 ? (pnlPoints / entryPrice) * 100.0 : 0.0;
                result.setExitPrice(exitPrice);
                result.setPnlPoints(pnlPoints);
                result.setPnlPct(pnlPct);

                // Indian NSE intraday (MIS) charges — round trip
                double brokAbs    = applicationProperties.getBacktestBrokeragePct() * (entryPrice + exitPrice);
                double stt        = 0.00025   * exitPrice;                   // 0.025% sell only
                double exchCharge = 0.0000325 * (entryPrice + exitPrice);    // 0.00325% both sides
                double sebi       = 0.000001  * (entryPrice + exitPrice);    // 0.0001% both sides
                double stampDuty  = 0.00003   * entryPrice;                  // 0.003% buy only
                double gst        = 0.18 * (brokAbs + exchCharge + sebi);
                double totalChargesAbs = brokAbs + stt + exchCharge + sebi + stampDuty + gst;
                double totalChargesPct = entryPrice > 0 ? totalChargesAbs / entryPrice * 100.0 : 0.0;
                result.setTotalChargesPct(totalChargesPct);
                result.setPnlAfterChargesPct(pnlPct - totalChargesPct);

                summary.addResult(result);
            }
        }

        summaries.values().forEach(BacktestSummary::computeStats);

        // Stamp Nifty benchmark on every summary (same range as the alerts)
        final LocalDate finalMinDate = minDate;
        final LocalDate finalMaxDate = maxDate;
        List<Candle> niftyCandles = fetchNiftyCandles(finalMinDate, finalMaxDate);
        double niftyReturn = computeOverallNiftyReturn(niftyCandles, finalMinDate, finalMaxDate);
        Map<String, Double> monthlyNiftyMap = computeMonthlyNiftyReturns(niftyCandles);
        summaries.values().forEach(s -> s.computeMonthlyStats(monthlyNiftyMap));
        String niftyRange  = (finalMinDate != null && finalMaxDate != null) ? finalMinDate + " → " + finalMaxDate : "N/A";
        double avgPace = (finalMinDate != null && finalMaxDate != null)
                ? 12.5 * (finalMaxDate.toEpochDay() - finalMinDate.toEpochDay()) / 365.0 : 0;
        summaries.values().forEach(s -> {
            s.setNiftyReturnPct(Double.isNaN(niftyReturn) ? 0 : niftyReturn);
            s.setNiftyDateRange(Double.isNaN(niftyReturn) ? "N/A" : niftyRange);
            s.setBeatsNifty(!Double.isNaN(niftyReturn) && s.getTotalPnlAfterChargesPct() > niftyReturn);
            s.setNiftyAvgPacePct(avgPace);
            s.setBeatsNiftyAvgPace(s.getTotalPnlAfterChargesPct() > avgPace);
        });

        return new ArrayList<>(summaries.values());
    }

    // ─── Nifty benchmark ──────────────────────────────────────────────────────

    private List<Candle> fetchNiftyCandles(LocalDate minDate, LocalDate maxDate) {
        if (minDate == null || maxDate == null) return List.of();
        String token = applicationProperties.getNiftySymbolToken();
        if (token == null || token.isBlank()) {
            log.warn("nifty-symbol-token not configured — skipping Nifty benchmark");
            return List.of();
        }
        int lookback = (int)(maxDate.toEpochDay() - minDate.toEpochDay()) + 5;
        List<Candle> candles = cacheService.loadOrFetch("NIFTY50", token, maxDate, "1D", lookback);
        if (candles == null || candles.isEmpty()) {
            log.warn("No Nifty candles fetched for {} → {}", minDate, maxDate);
            return List.of();
        }
        return candles;
    }

    private double computeOverallNiftyReturn(List<Candle> candles, LocalDate minDate, LocalDate maxDate) {
        if (candles.isEmpty() || minDate == null || maxDate == null || minDate.equals(maxDate)) return Double.NaN;
        Candle first = candles.stream()
                .filter(c -> !LocalDateTime.parse(c.getTimestamp(), TS_FMT).toLocalDate().isBefore(minDate))
                .findFirst().orElse(null);
        Candle last = null;
        for (int i = candles.size() - 1; i >= 0; i--) {
            if (!LocalDateTime.parse(candles.get(i).getTimestamp(), TS_FMT).toLocalDate().isAfter(maxDate)) {
                last = candles.get(i);
                break;
            }
        }
        if (first == null || last == null || first == last) return Double.NaN;
        log.info("Nifty benchmark: {} close={} → {} close={}", minDate, first.getClose(), maxDate, last.getClose());
        return (last.getClose() - first.getClose()) / first.getClose() * 100.0;
    }

    private Map<String, Double> computeMonthlyNiftyReturns(List<Candle> candles) {
        Map<String, List<Candle>> byMonth = new TreeMap<>();
        for (Candle c : candles) {
            String month = c.getTimestamp().substring(0, 7);
            byMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(c);
        }
        Map<String, Double> result = new TreeMap<>();
        byMonth.forEach((month, list) -> {
            if (list.size() < 2) return;
            double firstClose = list.get(0).getClose();
            double lastClose  = list.get(list.size() - 1).getClose();
            if (firstClose > 0)
                result.put(month, (lastClose - firstClose) / firstClose * 100.0);
        });
        return result;
    }

    // ─── Entry resolution ─────────────────────────────────────────────────────

    private Pair<Integer, Double> resolveEntry(List<Candle> candles, int alertIdx,
                                               StrategyDecision decision, double firstAlertPrice) {
        EntryTiming timing = decision.getEntryTiming() != null ? decision.getEntryTiming() : EntryTiming.NEXT_CANDLE_OPEN;
        switch (timing) {
            case CURRENT_CLOSE -> {
                double price = firstAlertPrice > 0 ? firstAlertPrice : candles.get(alertIdx).getClose();
                return Pair.of(alertIdx, price);
            }
            case BREAKOUT -> {
                double bp = decision.getBreakoutPrice();
                String alertDayStr = LocalDateTime.parse(candles.get(alertIdx).getTimestamp(), TS_FMT)
                        .toLocalDate().toString();
                for (int i = alertIdx + 1; i < candles.size(); i++) {
                    String candleDayStr = LocalDateTime.parse(candles.get(i).getTimestamp(), TS_FMT)
                            .toLocalDate().toString();
                    if (!candleDayStr.equals(alertDayStr)) return null; // past alert day
                    boolean triggered = decision.isLong()
                            ? candles.get(i).getHigh() >= bp
                            : candles.get(i).getLow() <= bp;
                    if (triggered) return Pair.of(i, bp);
                }
                return null; // breakout never triggered on alert day
            }
            default -> { // NEXT_CANDLE_OPEN
                if (alertIdx + 1 >= candles.size()) return null;
                return Pair.of(alertIdx + 1, candles.get(alertIdx + 1).getOpen());
            }
        }
    }

    // ─── SL / Target resolution ───────────────────────────────────────────────

    private double resolveSl(double entry, boolean isLong, StrategyConfig cfg,
                              IndicatorContext ctx, String primaryTf) {
        if ("ATR_MULTIPLE".equals(cfg.getSlMethod())) {
            String tf = cfg.getSlAtrTimeframe() != null ? cfg.getSlAtrTimeframe() : primaryTf;
            String key = tf + ":ATR_" + cfg.getSlAtrPeriod() + ":0";
            double atr = ctx.getOrDefault(key, 0);
            return isLong ? entry - cfg.getSlAtrMultiple() * atr : entry + cfg.getSlAtrMultiple() * atr;
        }
        // FIXED_PCT
        return isLong ? entry * (1 - cfg.getSlPct()) : entry * (1 + cfg.getSlPct());
    }

    private double resolveTarget(double entry, double sl, boolean isLong, StrategyConfig cfg,
                                 IndicatorContext ctx, String primaryTf) {
        if ("ATR_MULTIPLE".equals(cfg.getTargetMethod())) {
            String tf = cfg.getTargetAtrTimeframe() != null ? cfg.getTargetAtrTimeframe() : primaryTf;
            int atrPeriod = cfg.getTargetAtrPeriod() != null ? cfg.getTargetAtrPeriod() : cfg.getSlAtrPeriod();
            String key = tf + ":ATR_" + atrPeriod + ":0";
            double atr = ctx.getOrDefault(key, 0);
            return isLong ? entry + cfg.getTargetAtrMultiple() * atr : entry - cfg.getTargetAtrMultiple() * atr;
        }
        if ("RR_MULTIPLE".equals(cfg.getTargetMethod())) {
            double slDist = Math.abs(entry - sl);
            return isLong ? entry + cfg.getTargetRr() * slDist : entry - cfg.getTargetRr() * slDist;
        }
        // FIXED_PCT
        return isLong ? entry * (1 + cfg.getTargetPct()) : entry * (1 - cfg.getTargetPct());
    }

    // ─── Indicator context builder ────────────────────────────────────────────

    private IndicatorContext buildIndicatorContext(List<ConfigurableBacktestStrategy> strategies,
                                                   Map<String, List<Candle>> candlesByTf,
                                                   Map<String, Integer> alertIdxByTf) {
        IndicatorContext ctx = new IndicatorContext();

        // Collect all unique (key, tf, indicator, period, offset, cfg) needed — cfg enables per-strategy params for MACD/ST/BB
        // Using a Map so last-writer-wins for shared keys; acceptable since mixed params across strategies is rare
        Map<String, String[]> neededKeyMap = new LinkedHashMap<>();
        for (ConfigurableBacktestStrategy s : strategies) {
            StrategyConfig cfg = s.getConfig();
            String cfgIdx = strategies.indexOf(s) + "";
            for (ConditionSpec cond : safeList(cfg.getLongConditions()))
                neededKeyMap.put(cond.indicatorKey(), new String[]{cond.indicatorKey(), cond.getTimeframe(), cond.getIndicator(), String.valueOf(cond.getPeriod()), String.valueOf(cond.getOffset()), cfgIdx});
            for (ConditionSpec cond : safeList(cfg.getShortConditions()))
                neededKeyMap.put(cond.indicatorKey(), new String[]{cond.indicatorKey(), cond.getTimeframe(), cond.getIndicator(), String.valueOf(cond.getPeriod()), String.valueOf(cond.getOffset()), cfgIdx});
        }

        // Ensure ATR is in context for strategies using ATR_MULTIPLE SL or target
        for (ConfigurableBacktestStrategy s : strategies) {
            StrategyConfig cfg = s.getConfig();
            String cfgIdx = strategies.indexOf(s) + "";
            if ("ATR_MULTIPLE".equals(cfg.getSlMethod())) {
                String tf = cfg.getSlAtrTimeframe() != null ? cfg.getSlAtrTimeframe() : "5M";
                int period = cfg.getSlAtrPeriod();
                String ctxKey = tf + ":ATR_" + period + ":0";
                neededKeyMap.put(ctxKey, new String[]{ctxKey, tf, "ATR", String.valueOf(period), "0", cfgIdx});
            }
            if ("ATR_MULTIPLE".equals(cfg.getTargetMethod())) {
                String tf = cfg.getTargetAtrTimeframe() != null ? cfg.getTargetAtrTimeframe() : "5M";
                int period = cfg.getTargetAtrPeriod() != null ? cfg.getTargetAtrPeriod() : cfg.getSlAtrPeriod();
                String ctxKey = tf + ":ATR_" + period + ":0";
                neededKeyMap.put(ctxKey, new String[]{ctxKey, tf, "ATR", String.valueOf(period), "0", cfgIdx});
            }
        }

        for (String[] parts : neededKeyMap.values()) {
            String ctxKey   = parts[0];
            String tf       = parts[1];
            String indName  = parts[2];
            int period      = Integer.parseInt(parts[3]);
            int offset      = Integer.parseInt(parts[4]);
            int sIdx        = Integer.parseInt(parts[5]);
            StrategyConfig cfg = strategies.get(sIdx).getConfig();

            List<Candle> candles = candlesByTf.get(tf);
            Integer alertIdx     = alertIdxByTf.get(tf);
            if (candles == null || alertIdx == null) continue;

            int targetIdx = alertIdx - offset;
            if (targetIdx < 0) continue;

            double value = computeIndicator(indName, period, candles, targetIdx, cfg);
            if (!Double.isNaN(value)) ctx.put(ctxKey, value);
        }

        return ctx;
    }

    private double computeIndicator(String name, int period, List<Candle> candles, int endIdx, StrategyConfig cfg) {
        int macdFast   = cfg != null && cfg.getMacdFastPeriod()   != null ? cfg.getMacdFastPeriod()   : 12;
        int macdSlow   = cfg != null && cfg.getMacdSlowPeriod()   != null ? cfg.getMacdSlowPeriod()   : 26;
        int macdSignal = cfg != null && cfg.getMacdSignalPeriod() != null ? cfg.getMacdSignalPeriod() : 9;
        double stMult  = cfg != null && cfg.getSupertrendMultiplier() != null ? cfg.getSupertrendMultiplier() : 3.0;
        double bbStd   = cfg != null && cfg.getBbStdDev()             != null ? cfg.getBbStdDev()             : 2.0;

        return switch (name.toUpperCase()) {
            case "RSI"          -> new RsiIndicator(period).compute(candles, endIdx);
            case "EMA"          -> new EmaIndicator(period).compute(candles, endIdx);
            case "SMA"          -> new SmaIndicator(period).compute(candles, endIdx);
            case "SMA_VOL"      -> new SmaVolumeIndicator(period).compute(candles, endIdx);
            case "VWAP"         -> new VwapIndicator().compute(candles, endIdx);
            case "ATR"          -> new AtrIndicator(period).compute(candles, endIdx);
            case "MACD"         -> new MacdIndicator(macdFast, macdSlow, macdSignal).compute(candles, endIdx);
            case "MACD_SIGNAL"  -> new MacdIndicator(macdFast, macdSlow, macdSignal).computeAll(candles, endIdx)
                                       .getOrDefault("MACD_SIGNAL_0", Double.NaN);
            case "MACD_HIST"    -> new MacdIndicator(macdFast, macdSlow, macdSignal).computeAll(candles, endIdx)
                                       .getOrDefault("MACD_HIST_0", Double.NaN);
            case "BB_UPPER"     -> new BollingerBandsIndicator(period, bbStd).computeAll(candles, endIdx)
                                       .getOrDefault("BB_UPPER_" + period, Double.NaN);
            case "BB_LOWER"     -> new BollingerBandsIndicator(period, bbStd).computeAll(candles, endIdx)
                                       .getOrDefault("BB_LOWER_" + period, Double.NaN);
            case "SUPERTREND"   -> new SupertrendIndicator(period, stMult).compute(candles, endIdx);
            case "ADX"          -> new AdxIndicator(period).compute(candles, endIdx);
            case "STOCH_K"      -> new StochIndicator(period, 3).compute(candles, endIdx);
            case "STOCH_D"      -> new StochIndicator(period, 3).computeAll(candles, endIdx)
                                       .getOrDefault("STOCH_D_" + period, Double.NaN);
            case "CANDLE_COLOR"          -> new CandleColorIndicator().compute(candles, endIdx);
            case "PREV_DAY_LOW"          -> new PrevDayLevelIndicator(PrevDayLevelIndicator.Type.PREV_DAY_LOW).compute(candles, endIdx);
            case "PREV_DAY_HIGH"         -> new PrevDayLevelIndicator(PrevDayLevelIndicator.Type.PREV_DAY_HIGH).compute(candles, endIdx);
            case "DAY_LOW"               -> new PrevDayLevelIndicator(PrevDayLevelIndicator.Type.DAY_LOW).compute(candles, endIdx);
            case "DAY_HIGH"              -> new PrevDayLevelIndicator(PrevDayLevelIndicator.Type.DAY_HIGH).compute(candles, endIdx);
            case "CANDLE_RANGE_PCT"      -> new CandleStructureIndicator(CandleStructureIndicator.Type.CANDLE_RANGE_PCT).compute(candles, endIdx);
            case "CANDLE_CLOSED_AT_HIGH" -> new CandleStructureIndicator(CandleStructureIndicator.Type.CANDLE_CLOSED_AT_HIGH).compute(candles, endIdx);
            case "CANDLE_CLOSED_AT_LOW"  -> new CandleStructureIndicator(CandleStructureIndicator.Type.CANDLE_CLOSED_AT_LOW).compute(candles, endIdx);
            default -> { log.warn("Unknown indicator: {}", name); yield Double.NaN; }
        };
    }

    private Set<String> collectNeededTimeframes(List<ConfigurableBacktestStrategy> strategies) {
        Set<String> tfs = new LinkedHashSet<>();
        tfs.add("5M"); // always load 5M for signal evaluation
        tfs.add("1M"); // always attempt 1M for precision exit eval; falls back to 5M if unavailable
        for (ConfigurableBacktestStrategy s : strategies) {
            safeList(s.getConfig().getLongConditions()).forEach(c -> tfs.add(c.getTimeframe()));
            safeList(s.getConfig().getShortConditions()).forEach(c -> tfs.add(c.getTimeframe()));
        }
        return tfs;
    }

    private int findNearestIdx(List<Candle> candles, LocalDateTime target) {
        int best = -1;
        long minDiff = Long.MAX_VALUE;
        for (int i = 0; i < candles.size(); i++) {
            LocalDateTime t = LocalDateTime.parse(candles.get(i).getTimestamp(), TS_FMT);
            long diff = Math.abs(java.time.Duration.between(t, target).toMinutes());
            if (diff < minDiff) { minDiff = diff; best = i; }
        }
        return best;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
