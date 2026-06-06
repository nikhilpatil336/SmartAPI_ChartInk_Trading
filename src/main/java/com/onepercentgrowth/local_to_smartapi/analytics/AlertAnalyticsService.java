package com.onepercentgrowth.local_to_smartapi.analytics;

import com.google.gson.JsonObject;
import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.historicdata.Candle;
import com.onepercentgrowth.local_to_smartapi.historicdata.HistoricalDataResponse;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AlertAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AlertAnalyticsService.class);

    private static final DateTimeFormatter ALERT_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter API_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CANDLE_TS_FMT = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter TIME_OF_DAY_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BrokerApiClient brokerApiClient;
    private final TokenManager tokenManager;
    private final ScripMasterService scripMasterService;
    private final ApplicationProperties applicationProperties;
    private final AlertAnalyticsExcelWriter excelWriter;

    public AlertAnalyticsService(BrokerApiClient brokerApiClient, TokenManager tokenManager,
                                 ScripMasterService scripMasterService,
                                 ApplicationProperties applicationProperties,
                                 AlertAnalyticsExcelWriter excelWriter) {
        this.brokerApiClient = brokerApiClient;
        this.tokenManager = tokenManager;
        this.scripMasterService = scripMasterService;
        this.applicationProperties = applicationProperties;
        this.excelWriter = excelWriter;
    }

    public void runForToday() throws Exception {
        runForDate(LocalDate.now());
    }

    public void runForDate(LocalDate date) throws Exception {
        String inputPath = applicationProperties.getEodAnalyticsInputAlertPath();
        List<Pair<String, String>> entries = readAlertsForDate(inputPath, date);
        if (entries.isEmpty()) {
            log.info("EOD Analytics: no alerts found for {}", date);
            return;
        }
        List<AlertAnalyticsRow> rows = processEntries(entries);
        excelWriter.appendRows(rows);
        log.info("EOD Analytics: appended {} rows for {}", rows.size(), date);
    }

    public void runHistorical() throws Exception {
        String inputPath = applicationProperties.getEodAnalyticsInputAlertPath();
        List<Pair<LocalDate, Pair<String, String>>> all = readAllAlerts(inputPath);

        Map<LocalDate, List<Pair<String, String>>> byDate = new LinkedHashMap<>();
        for (Pair<LocalDate, Pair<String, String>> entry : all) {
            byDate.computeIfAbsent(entry.getLeft(), k -> new ArrayList<>()).add(entry.getRight());
        }

        List<AlertAnalyticsRow> allRows = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Pair<String, String>>> e : byDate.entrySet()) {
            List<AlertAnalyticsRow> rows = processEntries(e.getValue());
            allRows.addAll(rows);
            log.info("Historical analytics: processed {} rows for {}", rows.size(), e.getKey());
        }

        excelWriter.rebuildAll(allRows);
        log.info("Historical analytics: rebuilt all sheets with {} total rows", allRows.size());
    }

    // ─── Internal helpers ──────────────────────────────────────────────────────

    private List<Pair<String, String>> readAlertsForDate(String inputPath, LocalDate date) throws Exception {
        List<Pair<String, String>> result = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(inputPath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String triggeredAt = cellStr(row.getCell(0));
                if (triggeredAt == null || triggeredAt.isBlank()) continue;
                LocalDate alertDate;
                try {
                    alertDate = LocalDateTime.parse(triggeredAt, ALERT_FMT).toLocalDate();
                } catch (Exception ex) {
                    log.warn("Skipping unparseable date: {}", triggeredAt);
                    continue;
                }
                if (!alertDate.equals(date)) continue;
                String stocks = cellStr(row.getCell(2));
                if (stocks == null || stocks.isBlank()) continue;
                for (String s : stocks.split("\\s*,\\s*")) {
                    s = s.trim().toUpperCase();
                    if (!s.isEmpty()) result.add(Pair.of(triggeredAt, s));
                }
            }
        }
        return result;
    }

    private List<Pair<LocalDate, Pair<String, String>>> readAllAlerts(String inputPath) throws Exception {
        List<Pair<LocalDate, Pair<String, String>>> result = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(inputPath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String triggeredAt = cellStr(row.getCell(0));
                if (triggeredAt == null || triggeredAt.isBlank()) continue;
                LocalDate alertDate;
                try {
                    alertDate = LocalDateTime.parse(triggeredAt, ALERT_FMT).toLocalDate();
                } catch (Exception ex) {
                    log.warn("Skipping unparseable date: {}", triggeredAt);
                    continue;
                }
                String stocks = cellStr(row.getCell(2));
                if (stocks == null || stocks.isBlank()) continue;
                for (String s : stocks.split("\\s*,\\s*")) {
                    s = s.trim().toUpperCase();
                    if (!s.isEmpty()) result.add(Pair.of(alertDate, Pair.of(triggeredAt, s)));
                }
            }
        }
        return result;
    }

    private List<AlertAnalyticsRow> processEntries(List<Pair<String, String>> entries) {
        List<AlertAnalyticsRow> rows = new ArrayList<>();
        LocalTime squareoffTime = LocalTime.parse(applicationProperties.getAnalyticsSquareoffTime());

        for (Pair<String, String> entry : entries) {
            String triggeredAt = entry.getLeft();
            String stock = entry.getRight();
            log.info("Analytics: processing {} at {}", stock, triggeredAt);

            long start = System.currentTimeMillis();
            AlertAnalyticsRow row = buildRow(stock, triggeredAt, squareoffTime);
            if (row != null) rows.add(row);

            long elapsed = System.currentTimeMillis() - start;
            if (elapsed < 1500) {
                try { Thread.sleep(1500 - elapsed); } catch (InterruptedException ignored) {}
            }
        }
        return rows;
    }

    private AlertAnalyticsRow buildRow(String stock, String triggeredAt, LocalTime squareoffTime) {
        try {
            String symbolToken = scripMasterService.getTokenForName(stock);
            if (symbolToken == null) {
                log.warn("No token for {}", stock);
                return null;
            }

            LocalDateTime alertTime = LocalDateTime.parse(triggeredAt, ALERT_FMT);
            LocalDateTime from = alertTime.minusDays(3).withHour(9).withMinute(15).withSecond(0).withNano(0);
            LocalDateTime to   = alertTime.toLocalDate().atTime(15, 30);

            JsonObject req = new JsonObject();
            req.addProperty("exchange", "NSE");
            req.addProperty("symboltoken", symbolToken);
            req.addProperty("interval", "FIVE_MINUTE");
            req.addProperty("fromdate", from.format(API_FMT));
            req.addProperty("todate", to.format(API_FMT));

            HistoricalDataResponse response = fetchWithRetry(req, tokenManager.getValidJwtToken());
            if (response == null || !response.isStatus() || response.getData() == null) {
                log.warn("No candle data for {}", stock);
                return null;
            }

            List<Candle> candles = response.getData().stream()
                    .map(this::mapToCandle)
                    .sorted(Comparator.comparing(Candle::getTimestamp))
                    .toList();

            int alertIdx = findNearestIdx(candles, alertTime);
            if (alertIdx < 0) return null;

            Candle alertCandle = candles.get(alertIdx);
            Candle prevCandle  = alertIdx > 0 ? candles.get(alertIdx - 1) : null;

            // Candle color — drives sheet routing and alertDirection
            String color;
            if (alertCandle.getClose() > alertCandle.getOpen())      color = "GREEN";
            else if (alertCandle.getClose() < alertCandle.getOpen()) color = "RED";
            else                                                       color = "DOJI";

            boolean green = "GREEN".equals(color);
            boolean red   = "RED".equals(color);
            boolean doji  = "DOJI".equals(color);

            double sma10 = calcSMA(candles, alertIdx, 10);
            double sma20 = calcSMA(candles, alertIdx, 20);

            // Volume condition determines trade decision; candle color determines sheet
            String decision = "NO_TRADE";
            if (green && alertCandle.getVolume() > sma10)     decision = "TAKE_LONG";
            else if (red && alertCandle.getVolume() < sma10)  decision = "TAKE_SHORT";

            int tradeTaken = ("TAKE_LONG".equals(decision) || "TAKE_SHORT".equals(decision)) ? 1 : 0;
            boolean isLong = "TAKE_LONG".equals(decision);

            // Second candle (entry candle)
            double secondAlertTriggerPrice = 0;
            String secondAlertTime = "";
            if (alertIdx + 1 < candles.size()) {
                Candle next = candles.get(alertIdx + 1);
                secondAlertTriggerPrice = next.getOpen();
                secondAlertTime = next.getTimestamp();
            }

            // Next candle open vs alert candle close — open confirmation
            double nextCandleOpenVsAlertClosePct = 0;
            String nextCandleOpenDirection = "N/A";
            if (secondAlertTriggerPrice > 0 && alertCandle.getClose() > 0) {
                nextCandleOpenVsAlertClosePct = (secondAlertTriggerPrice - alertCandle.getClose())
                        / alertCandle.getClose() * 100;
                nextCandleOpenDirection = nextCandleOpenVsAlertClosePct > 0.005 ? "ABOVE"
                        : nextCandleOpenVsAlertClosePct < -0.005 ? "BELOW" : "FLAT";
            }

            // Target / SL based on entry price (second candle OPEN)
            double targetPrice = 0, slPrice = 0;
            if ("TAKE_LONG".equals(decision) && secondAlertTriggerPrice > 0) {
                targetPrice = secondAlertTriggerPrice * applicationProperties.getBuyProfitPercentageMultiplier();
                slPrice     = secondAlertTriggerPrice * applicationProperties.getBuyStoplossPercentageMultiplier();
            } else if ("TAKE_SHORT".equals(decision) && secondAlertTriggerPrice > 0) {
                targetPrice = secondAlertTriggerPrice * applicationProperties.getSellProfitPercentageMultiplier();
                slPrice     = secondAlertTriggerPrice * applicationProperties.getSellStoplossPercentageMultiplier();
            }

            // Risk/reward ratio
            double riskRewardRatio = 0;
            if (tradeTaken == 1 && secondAlertTriggerPrice > 0) {
                double reward = Math.abs(targetPrice - secondAlertTriggerPrice);
                double risk   = Math.abs(secondAlertTriggerPrice - slPrice);
                riskRewardRatio = risk > 0 ? reward / risk : 0;
            }

            // Contextual metrics
            double candleBodyPct  = alertCandle.getOpen() > 0
                    ? Math.abs(alertCandle.getClose() - alertCandle.getOpen()) / alertCandle.getOpen() * 100 : 0;
            double candleRangePct = alertCandle.getOpen() > 0
                    ? (alertCandle.getHigh() - alertCandle.getLow()) / alertCandle.getOpen() * 100 : 0;
            double volVsSma10Ratio = sma10 > 0 ? alertCandle.getVolume() / sma10 : 0;
            double volVsSma20Ratio = sma20 > 0 ? alertCandle.getVolume() / sma20 : 0;
            String timeOfDay  = alertTime.format(TIME_OF_DAY_FMT);
            String dayOfWeek  = alertTime.getDayOfWeek().name().substring(0, 3);

            // Build row
            AlertAnalyticsRow row = new AlertAnalyticsRow();
            row.setTradeDate(alertTime.toLocalDate().toString());
            row.setStock(stock);
            String alertDir = "TAKE_LONG".equals(decision) ? "LONG"
                           : "TAKE_SHORT".equals(decision) ? "SHORT"
                           : "NO_TRADE";
            row.setAlertDirection(alertDir);
            row.setTradeTaken(tradeTaken);

            row.setFirstAlertTime(alertTime.toString());
            row.setFirstAlertTriggerPrice(alertCandle.getOpen());
            row.setSecondAlertTime(secondAlertTime);
            row.setSecondAlertTriggerPrice(secondAlertTriggerPrice);

            row.setCandleOpen(alertCandle.getOpen());
            row.setCandleHigh(alertCandle.getHigh());
            row.setCandleLow(alertCandle.getLow());
            row.setCandleClose(alertCandle.getClose());
            row.setCandleVolume(alertCandle.getVolume());
            row.setCandleColor(color);
            row.setIsGreen(green ? 1 : 0);
            row.setIsRed(red ? 1 : 0);
            row.setIsDoji(doji ? 1 : 0);
            row.setCandleBodyPct(candleBodyPct);
            row.setCandleRangePct(candleRangePct);

            if (prevCandle != null) {
                row.setPrevCandleOpen(prevCandle.getOpen());
                row.setPrevCandleHigh(prevCandle.getHigh());
                row.setPrevCandleLow(prevCandle.getLow());
                row.setPrevCandleClose(prevCandle.getClose());
                row.setPrevCandleVolume(prevCandle.getVolume());
            }

            row.setVolumeSma10(sma10);
            row.setVolumeSma20(sma20);
            row.setVolVsSma10Ratio(volVsSma10Ratio);
            row.setVolVsSma20Ratio(volVsSma20Ratio);
            row.setVolGtSma10(alertCandle.getVolume() > sma10 ? 1 : 0);
            row.setVolGtSma20(alertCandle.getVolume() > sma20 ? 1 : 0);
            row.setVolLtSma10(alertCandle.getVolume() < sma10 ? 1 : 0);
            row.setVolLtSma20(alertCandle.getVolume() < sma20 ? 1 : 0);
            row.setSma10LtSma20(sma10 < sma20 ? 1 : 0);

            row.setTargetPrice(targetPrice);
            row.setSlPrice(slPrice);
            row.setRiskRewardRatio(riskRewardRatio);

            row.setTimeOfDay(timeOfDay);
            row.setDayOfWeek(dayOfWeek);
            row.setNextCandleOpenVsAlertClosePct(nextCandleOpenVsAlertClosePct);
            row.setNextCandleOpenDirection(nextCandleOpenDirection);

            if (tradeTaken == 0 || secondAlertTriggerPrice == 0) {
                row.setHitFirst("NONE");
                row.setSquareoffReason("NONE");
                row.setTradeOutcome("NO_TRADE");
                row.setTrailingOutcome("NO_TRADE");
            } else {
                // Fetch 1-min candles for more accurate target/SL/trailing evaluation.
                // Falls back to 5-min if API returns empty (e.g. date > 30 days old).
                List<Candle> evalCandles = candles;
                int evalEntryStartIndex  = alertIdx + 1;

                if (alertIdx + 1 < candles.size()) {
                    LocalDateTime entryStart = LocalDateTime.parse(
                            candles.get(alertIdx + 1).getTimestamp(), CANDLE_TS_FMT);
                    LocalDateTime evalTo = alertTime.toLocalDate().atTime(15, 30);

                    JsonObject oneMinReq = new JsonObject();
                    oneMinReq.addProperty("exchange", "NSE");
                    oneMinReq.addProperty("symboltoken", symbolToken);
                    oneMinReq.addProperty("interval", "ONE_MINUTE");
                    oneMinReq.addProperty("fromdate", entryStart.format(API_FMT));
                    oneMinReq.addProperty("todate", evalTo.format(API_FMT));

                    HistoricalDataResponse oneMinResp = fetchWithRetry(oneMinReq, tokenManager.getValidJwtToken());
                    if (oneMinResp != null && oneMinResp.isStatus()
                            && oneMinResp.getData() != null && !oneMinResp.getData().isEmpty()) {
                        evalCandles = oneMinResp.getData().stream()
                                .map(this::mapToCandle)
                                .sorted(Comparator.comparing(Candle::getTimestamp))
                                .toList();
                        evalEntryStartIndex = 0; // 1-min list starts at entry candle
                        log.info("Analytics: 1-min eval ({} candles) for {}", evalCandles.size(), stock);
                    } else {
                        log.warn("Analytics: 1-min fetch unavailable for {}, falling back to 5-min", stock);
                    }
                }

                TargetSlEvaluator.EvalResult eval = TargetSlEvaluator.evaluate(
                        evalCandles, evalEntryStartIndex, targetPrice, slPrice, isLong, squareoffTime);

                row.setTargetHit(eval.targetHit);
                row.setSlHit(eval.slHit);
                row.setHitFirst(eval.hitFirst);
                row.setSquareoffReason(eval.squareoffReason);
                row.setSquareoffPrice(eval.squareoffPrice);
                row.setTimeToHitMins(eval.timeToHitMins);
                row.setTradeOutcome(eval.tradeOutcome);
                row.setTrailingActivated(eval.trailingActivated);
                row.setTrailingMaxFavorable(eval.trailingMaxFavorable);
                row.setTrailingSlExitPrice(eval.trailingSlExitPrice);
                row.setTrailingOutcome(eval.trailingOutcome);
                row.setHitFirstCandleOpen(eval.hitFirstCandleOpen);
                row.setHitFirstCandleHigh(eval.hitFirstCandleHigh);
                row.setHitFirstCandleLow(eval.hitFirstCandleLow);
                row.setHitFirstCandleClose(eval.hitFirstCandleClose);
                row.setHitFirstCandleVolume(eval.hitFirstCandleVolume);

                // MFE / MAE — max favorable and adverse excursion from entry price
                double maxHigh = secondAlertTriggerPrice, minLow = secondAlertTriggerPrice;
                for (int i = evalEntryStartIndex; i < evalCandles.size(); i++) {
                    Candle ec = evalCandles.get(i);
                    if (ec.getHigh() > maxHigh) maxHigh = ec.getHigh();
                    if (ec.getLow()  < minLow)  minLow  = ec.getLow();
                }
                double mfePct = isLong ? (maxHigh - secondAlertTriggerPrice) / secondAlertTriggerPrice * 100
                                       : (secondAlertTriggerPrice - minLow)  / secondAlertTriggerPrice * 100;
                double maePct = isLong ? (secondAlertTriggerPrice - minLow)  / secondAlertTriggerPrice * 100
                                       : (maxHigh - secondAlertTriggerPrice) / secondAlertTriggerPrice * 100;
                row.setMfePct(mfePct);
                row.setMaePct(maePct);
                row.setMfe05(mfePct >= 0.5 ? 1 : 0);
                row.setMfe10(mfePct >= 1.0 ? 1 : 0);
                row.setMfe15(mfePct >= 1.5 ? 1 : 0);
                row.setMae05(maePct >= 0.5 ? 1 : 0);
                row.setMae10(maePct >= 1.0 ? 1 : 0);
                row.setMae15(maePct >= 1.5 ? 1 : 0);
            }

            boolean win = "WIN".equals(row.getTradeOutcome());
            row.setVolGtSma10Win(row.getVolGtSma10() == 1 && win ? 1 : 0);
            row.setVolGtSma20Win(row.getVolGtSma20() == 1 && win ? 1 : 0);
            row.setVolLtSma10Win(row.getVolLtSma10() == 1 && win ? 1 : 0);
            row.setVolLtSma20Win(row.getVolLtSma20() == 1 && win ? 1 : 0);

            return row;

        } catch (Exception e) {
            log.error("Error building row for {} at {}: {}", stock, triggeredAt, e.getMessage());
            return null;
        }
    }

    // ─── Candle helpers ───────────────────────────────────────────────────────

    private Candle mapToCandle(List<Object> raw) {
        Candle c = new Candle();
        c.setTimestamp((String) raw.get(0));
        c.setOpen(Double.parseDouble(raw.get(1).toString()));
        c.setHigh(Double.parseDouble(raw.get(2).toString()));
        c.setLow(Double.parseDouble(raw.get(3).toString()));
        c.setClose(Double.parseDouble(raw.get(4).toString()));
        c.setVolume(Long.parseLong(raw.get(5).toString()));
        return c;
    }

    private double calcSMA(List<Candle> candles, int endIdx, int period) {
        if (endIdx - period + 1 < 0) return 0;
        double sum = 0;
        for (int i = endIdx; i > endIdx - period; i--) sum += candles.get(i).getVolume();
        return sum / period;
    }

    private int findNearestIdx(List<Candle> candles, LocalDateTime target) {
        int best = -1;
        long minDiff = Long.MAX_VALUE;
        for (int i = 0; i < candles.size(); i++) {
            LocalDateTime t = LocalDateTime.parse(candles.get(i).getTimestamp(), CANDLE_TS_FMT);
            long diff = Math.abs(Duration.between(t, target).toMinutes());
            if (diff < minDiff) { minDiff = diff; best = i; }
        }
        return best;
    }

    private HistoricalDataResponse fetchWithRetry(JsonObject req, String jwt) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HistoricalDataResponse r = brokerApiClient.getHistoricalCandleData(req, jwt).block();
                if (r != null && r.isStatus() && r.getData() != null) return r;
            } catch (Exception e) {
                log.warn("Candle fetch attempt {}: {}", attempt, e.getMessage());
            }
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> null;
        };
    }
}
