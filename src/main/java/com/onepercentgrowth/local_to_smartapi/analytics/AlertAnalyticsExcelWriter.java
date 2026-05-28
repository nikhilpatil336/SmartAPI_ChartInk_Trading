package com.onepercentgrowth.local_to_smartapi.analytics;

import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Component
public class AlertAnalyticsExcelWriter {

    private static final Logger log = LoggerFactory.getLogger(AlertAnalyticsExcelWriter.class);

    static final String SHEET_LONG  = "LONG_TRADES";
    static final String SHEET_SHORT = "SHORT_TRADES";
    static final String SHEET_DOJI  = "DOJI_TRADES";

    // Column order — 53 columns (indices 0-52)
    private static final String[] HEADERS = {
        // 0-3: identity
        "trade_date", "stock", "alert_direction", "trade_taken",
        // 4-7: timing
        "first_alert_time", "first_alert_trigger_price", "second_alert_time", "second_alert_trigger_price",
        // 8-20: 1st alert candle OHLCV + color flags + pct metrics
        "candle_open", "candle_high", "candle_low", "candle_close", "candle_volume",
        "candle_color", "is_green", "is_red", "is_doji", "candle_body_pct", "candle_range_pct",
        // 19-23: previous candle
        "prev_candle_open", "prev_candle_high", "prev_candle_low", "prev_candle_close", "prev_candle_volume",
        // 24-32: volume analysis
        "volume_sma10", "volume_sma20", "vol_vs_sma10_ratio", "vol_vs_sma20_ratio",
        "vol_gt_sma10", "vol_gt_sma20", "vol_lt_sma10", "vol_lt_sma20", "sma10_lt_sma20",
        // 33-35: trade levels
        "target_price", "sl_price", "risk_reward_ratio",
        // 36-42: outcome
        "target_hit", "sl_hit", "hit_first", "squareoff_reason", "squareoff_price",
        "time_to_hit_mins", "trade_outcome",
        // 43-44: contextual
        "time_of_day", "day_of_week",
        // 45-48: trailing SL
        "trailing_activated", "trailing_max_favorable", "trailing_sl_exit_price", "trailing_outcome",
        // 49-52: conditional win stats
        "vol_gt_sma10_win", "vol_gt_sma20_win", "vol_lt_sma10_win", "vol_lt_sma20_win"
    };

    // Key column indices for stats formulas
    private static final int COL_STOCK          = 1;
    private static final int COL_TRADE_TAKEN    = 3;
    private static final int COL_TIME_TO_HIT    = 41;
    private static final int COL_TRADE_OUTCOME  = 42;
    private static final int COL_TRAILING_OUTCOME = 48;
    private static final int COL_VOL_GT_SMA10   = 28;
    private static final int COL_VOL_GT_SMA10_WIN = 49;

    // Row layout: row 0 = stats, row 1 = headers, row 2+ = data
    private static final int ROW_STATS   = 0;
    private static final int ROW_HEADERS = 1;
    private static final int ROW_DATA    = 2;

    private final ApplicationProperties applicationProperties;

    public AlertAnalyticsExcelWriter(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /** Appends rows to the appropriate sheets; skips (trade_date, stock) already present. */
    public void appendRows(List<AlertAnalyticsRow> rows) throws Exception {
        if (rows.isEmpty()) return;
        String filePath = applicationProperties.getEodAnalyticsOutputFile();
        String tmpPath  = filePath + ".tmp";

        Workbook workbook = openOrCreate(filePath);
        ensureSheets(workbook);

        // Build dedup sets per sheet
        Map<String, Set<String>> existingKeys = buildExistingKeys(workbook);

        for (AlertAnalyticsRow r : rows) {
            String sheetName = sheetFor(r.getAlertDirection());
            Sheet sheet = workbook.getSheet(sheetName);
            Set<String> keys = existingKeys.computeIfAbsent(sheetName, k -> new HashSet<>());
            String key = r.getTradeDate() + "|" + r.getStock();
            if (keys.contains(key)) {
                log.debug("Skipping duplicate: {} on {}", r.getStock(), r.getTradeDate());
                continue;
            }
            int nextRow = sheet.getLastRowNum() + 1;
            if (nextRow < ROW_DATA) nextRow = ROW_DATA;
            writeDataRow(sheet, nextRow, r);
            keys.add(key);
        }

        updateStatsFormulas(workbook);
        save(workbook, filePath, tmpPath);
    }

    /** Clears all data rows (row 2+) from all 3 sheets and rewrites from scratch. */
    public void rebuildAll(List<AlertAnalyticsRow> rows) throws Exception {
        String filePath = applicationProperties.getEodAnalyticsOutputFile();
        String tmpPath  = filePath + ".tmp";

        Workbook workbook = openOrCreate(filePath);
        ensureSheets(workbook);
        clearDataRows(workbook);

        // Sort by tradeDate ascending before writing
        rows.sort(Comparator.comparing(AlertAnalyticsRow::getTradeDate));

        Map<String, Integer> nextRowNum = new HashMap<>();
        nextRowNum.put(SHEET_LONG,  ROW_DATA);
        nextRowNum.put(SHEET_SHORT, ROW_DATA);
        nextRowNum.put(SHEET_DOJI,  ROW_DATA);

        for (AlertAnalyticsRow r : rows) {
            String sheetName = sheetFor(r.getAlertDirection());
            Sheet sheet = workbook.getSheet(sheetName);
            int rowNum = nextRowNum.get(sheetName);
            writeDataRow(sheet, rowNum, r);
            nextRowNum.put(sheetName, rowNum + 1);
        }

        updateStatsFormulas(workbook);
        save(workbook, filePath, tmpPath);
        log.info("Analytics: rebuilt 3 sheets with {} total rows → {}", rows.size(), filePath);
    }

    // ─── Sheet / file helpers ─────────────────────────────────────────────────

    private Workbook openOrCreate(String filePath) throws Exception {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                return new XSSFWorkbook(fis);
            }
        }
        Files.createDirectories(Paths.get(file.getParent()));
        return new XSSFWorkbook();
    }

    private void ensureSheets(Workbook wb) {
        for (String name : new String[]{SHEET_LONG, SHEET_SHORT, SHEET_DOJI}) {
            if (wb.getSheet(name) == null) {
                Sheet sheet = wb.createSheet(name);
                writeHeaderRow(sheet);
                // Stats row will be written by updateStatsFormulas
            }
        }
    }

    private void clearDataRows(Workbook wb) {
        for (String name : new String[]{SHEET_LONG, SHEET_SHORT, SHEET_DOJI}) {
            Sheet sheet = wb.getSheet(name);
            if (sheet == null) continue;
            int last = sheet.getLastRowNum();
            for (int i = last; i >= ROW_DATA; i--) {
                Row row = sheet.getRow(i);
                if (row != null) sheet.removeRow(row);
            }
        }
    }

    private Map<String, Set<String>> buildExistingKeys(Workbook wb) {
        Map<String, Set<String>> result = new HashMap<>();
        for (String name : new String[]{SHEET_LONG, SHEET_SHORT, SHEET_DOJI}) {
            Set<String> keys = new HashSet<>();
            Sheet sheet = wb.getSheet(name);
            if (sheet != null) {
                for (int i = ROW_DATA; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String date  = cellStrRaw(row.getCell(0));
                    String stock = cellStrRaw(row.getCell(1));
                    if (date != null && stock != null) keys.add(date + "|" + stock);
                }
            }
            result.put(name, keys);
        }
        return result;
    }

    private static String sheetFor(String alertDirection) {
        if ("GREEN".equals(alertDirection)) return SHEET_LONG;
        if ("RED".equals(alertDirection))   return SHEET_SHORT;
        return SHEET_DOJI;
    }

    private void save(Workbook workbook, String filePath, String tmpPath) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(tmpPath)) {
            workbook.write(fos);
        }
        workbook.close();
        Files.move(Paths.get(tmpPath), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
    }

    // ─── Stats formulas row ───────────────────────────────────────────────────

    private void updateStatsFormulas(Workbook wb) {
        for (String name : new String[]{SHEET_LONG, SHEET_SHORT, SHEET_DOJI}) {
            Sheet sheet = wb.getSheet(name);
            if (sheet == null) continue;
            Row stats = sheet.getRow(ROW_STATS);
            if (stats == null) stats = sheet.createRow(ROW_STATS);
            writeStatsRow(stats);
        }
    }

    private void writeStatsRow(Row stats) {
        // Data range starts at ROW_DATA+1 in Excel (1-indexed), goes to end of sheet
        String dataStart = (ROW_DATA + 1) + "";       // "3"
        String stockCol   = col(COL_STOCK);            // B
        String takenCol   = col(COL_TRADE_TAKEN);      // D
        String outcomeCol = col(COL_TRADE_OUTCOME);    // AQ
        String timeCol    = col(COL_TIME_TO_HIT);      // AP
        String trailCol   = col(COL_TRAILING_OUTCOME); // AW
        String vgt10Col   = col(COL_VOL_GT_SMA10);     // AC
        String vgt10WinCol = col(COL_VOL_GT_SMA10_WIN); // AX

        // Build per-column ranges
        String stockRange   = stockCol   + dataStart + ":" + stockCol   + "1048576";
        String takenRange   = takenCol   + dataStart + ":" + takenCol   + "1048576";
        String outcomeRange = outcomeCol + dataStart + ":" + outcomeCol + "1048576";
        String timeRange    = timeCol    + dataStart + ":" + timeCol    + "1048576";
        String trailRange   = trailCol   + dataStart + ":" + trailCol   + "1048576";
        String vgt10Range   = vgt10Col   + dataStart + ":" + vgt10Col   + "1048576";
        String vgt10WinRange = vgt10WinCol + dataStart + ":" + vgt10WinCol + "1048576";

        int c = 0;
        setLabel(stats, c++, "Total Alerts");
        setFormula(stats, c++, "COUNTA(" + stockRange + ")");
        setLabel(stats, c++, "Trades Taken");
        setFormula(stats, c++, "COUNTIF(" + takenRange + ",1)");
        setLabel(stats, c++, "Wins");
        setFormula(stats, c++, "COUNTIF(" + outcomeRange + ",\"WIN\")");
        setLabel(stats, c++, "Losses");
        setFormula(stats, c++, "COUNTIF(" + outcomeRange + ",\"LOSS\")");
        setLabel(stats, c++, "Squareoff Exits");
        setFormula(stats, c++, "COUNTIF(" + outcomeRange + ",\"SQUAREOFF_EXIT\")");
        setLabel(stats, c++, "Win Rate %");
        // wins / trades_taken * 100 — reference the formula cells (E1=col4, D1=col3 → 1-indexed cols F and D)
        setFormula(stats, c++, "IFERROR(" + col(5) + "1/" + col(3) + "1*100,0)");
        setLabel(stats, c++, "Avg Win Mins");
        setFormula(stats, c++, "IFERROR(AVERAGEIF(" + outcomeRange + ",\"WIN\"," + timeRange + "),0)");
        setLabel(stats, c++, "Trailing Exits");
        setFormula(stats, c++, "COUNTIF(" + trailRange + ",\"TRAILING_EXIT\")");
        setLabel(stats, c++, "VolGtSma10 Win Rate %");
        setFormula(stats, c, "IFERROR(COUNTIF(" + vgt10WinRange + ",1)/COUNTIF(" + vgt10Range + ",1)*100,0)");
    }

    private static void setLabel(Row row, int colIdx, String label) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(label);
    }

    private static void setFormula(Row row, int colIdx, String formula) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellFormula(formula);
    }

    // ─── Header row ───────────────────────────────────────────────────────────

    private void writeHeaderRow(Sheet sheet) {
        Row header = sheet.getRow(ROW_HEADERS);
        if (header == null) header = sheet.createRow(ROW_HEADERS);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.getCell(i);
            if (cell == null) cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
        }
    }

    // ─── Data row writer ──────────────────────────────────────────────────────

    private void writeDataRow(Sheet sheet, int rowNum, AlertAnalyticsRow r) {
        Row row = sheet.createRow(rowNum);
        int c = 0;
        // identity
        row.createCell(c++).setCellValue(r.getTradeDate());
        row.createCell(c++).setCellValue(r.getStock());
        row.createCell(c++).setCellValue(r.getAlertDirection());
        row.createCell(c++).setCellValue(r.getTradeTaken());
        // timing
        row.createCell(c++).setCellValue(r.getFirstAlertTime());
        row.createCell(c++).setCellValue(r.getFirstAlertTriggerPrice());
        row.createCell(c++).setCellValue(r.getSecondAlertTime());
        row.createCell(c++).setCellValue(r.getSecondAlertTriggerPrice());
        // 1st candle OHLCV + flags + pct
        row.createCell(c++).setCellValue(r.getCandleOpen());
        row.createCell(c++).setCellValue(r.getCandleHigh());
        row.createCell(c++).setCellValue(r.getCandleLow());
        row.createCell(c++).setCellValue(r.getCandleClose());
        row.createCell(c++).setCellValue(r.getCandleVolume());
        row.createCell(c++).setCellValue(r.getCandleColor());
        row.createCell(c++).setCellValue(r.getIsGreen());
        row.createCell(c++).setCellValue(r.getIsRed());
        row.createCell(c++).setCellValue(r.getIsDoji());
        row.createCell(c++).setCellValue(r.getCandleBodyPct());
        row.createCell(c++).setCellValue(r.getCandleRangePct());
        // prev candle
        row.createCell(c++).setCellValue(r.getPrevCandleOpen());
        row.createCell(c++).setCellValue(r.getPrevCandleHigh());
        row.createCell(c++).setCellValue(r.getPrevCandleLow());
        row.createCell(c++).setCellValue(r.getPrevCandleClose());
        row.createCell(c++).setCellValue(r.getPrevCandleVolume());
        // volume analysis
        row.createCell(c++).setCellValue(r.getVolumeSma10());
        row.createCell(c++).setCellValue(r.getVolumeSma20());
        row.createCell(c++).setCellValue(r.getVolVsSma10Ratio());
        row.createCell(c++).setCellValue(r.getVolVsSma20Ratio());
        row.createCell(c++).setCellValue(r.getVolGtSma10());
        row.createCell(c++).setCellValue(r.getVolGtSma20());
        row.createCell(c++).setCellValue(r.getVolLtSma10());
        row.createCell(c++).setCellValue(r.getVolLtSma20());
        row.createCell(c++).setCellValue(r.getSma10LtSma20());
        // trade levels
        row.createCell(c++).setCellValue(r.getTargetPrice());
        row.createCell(c++).setCellValue(r.getSlPrice());
        row.createCell(c++).setCellValue(r.getRiskRewardRatio());
        // outcome
        row.createCell(c++).setCellValue(r.getTargetHit());
        row.createCell(c++).setCellValue(r.getSlHit());
        row.createCell(c++).setCellValue(r.getHitFirst());
        row.createCell(c++).setCellValue(r.getSquareoffReason());
        row.createCell(c++).setCellValue(r.getSquareoffPrice());
        row.createCell(c++).setCellValue(r.getTimeToHitMins());
        row.createCell(c++).setCellValue(r.getTradeOutcome());
        // contextual
        row.createCell(c++).setCellValue(r.getTimeOfDay());
        row.createCell(c++).setCellValue(r.getDayOfWeek());
        // trailing SL
        row.createCell(c++).setCellValue(r.getTrailingActivated());
        row.createCell(c++).setCellValue(r.getTrailingMaxFavorable());
        row.createCell(c++).setCellValue(r.getTrailingSlExitPrice());
        row.createCell(c++).setCellValue(r.getTrailingOutcome());
        // conditional win stats
        row.createCell(c++).setCellValue(r.getVolGtSma10Win());
        row.createCell(c++).setCellValue(r.getVolGtSma20Win());
        row.createCell(c++).setCellValue(r.getVolLtSma10Win());
        row.createCell(c).setCellValue(r.getVolLtSma20Win());
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    /** Converts 0-based column index to Excel column letter (A, B, …, AA, AB, …). */
    private static String col(int idx) {
        StringBuilder sb = new StringBuilder();
        int n = idx + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    private static String cellStrRaw(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> null;
        };
    }
}
