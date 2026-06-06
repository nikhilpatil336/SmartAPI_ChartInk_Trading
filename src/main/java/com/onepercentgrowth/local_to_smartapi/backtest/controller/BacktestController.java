package com.onepercentgrowth.local_to_smartapi.backtest.controller;

import com.onepercentgrowth.local_to_smartapi.backtest.config.StrategyConfigLoader;
import com.onepercentgrowth.local_to_smartapi.backtest.report.BacktestReportWriter;
import com.onepercentgrowth.local_to_smartapi.backtest.runner.AlertEntry;
import com.onepercentgrowth.local_to_smartapi.backtest.runner.BacktestRunner;
import com.onepercentgrowth.local_to_smartapi.backtest.runner.BacktestSummary;
import com.onepercentgrowth.local_to_smartapi.backtest.strategy.StrategyConfig;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/backtest")
public class BacktestController {

    private static final Logger log = LoggerFactory.getLogger(BacktestController.class);
    private static final DateTimeFormatter ALERT_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH);

    private final BacktestRunner runner;
    private final BacktestReportWriter reportWriter;
    private final StrategyConfigLoader configLoader;
    private final ApplicationProperties applicationProperties;

    public BacktestController(BacktestRunner runner,
                               BacktestReportWriter reportWriter,
                               StrategyConfigLoader configLoader,
                               ApplicationProperties applicationProperties) {
        this.runner = runner;
        this.reportWriter = reportWriter;
        this.configLoader = configLoader;
        this.applicationProperties = applicationProperties;
    }

    /** List all loaded strategy configs */
    @GetMapping("/strategies")
    public ResponseEntity<List<StrategyConfig>> listStrategies() {
        return ResponseEntity.ok(configLoader.loadAll());
    }

    /**
     * Run backtest for all alerts in the Excel input file.
     * Optional date filters for in-sample / out-of-sample splits:
     *   ?fromDate=YYYY-MM-DD  — include only alerts on or after this date
     *   ?toDate=YYYY-MM-DD    — include only alerts on or before this date
     */
    @PostMapping("/run-historical")
    public ResponseEntity<String> runHistorical(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
            LocalDate to   = toDate   != null ? LocalDate.parse(toDate)   : null;
            List<AlertEntry> alerts = readAlerts(null, from, to);
            if (alerts.isEmpty()) return ResponseEntity.ok("No alerts found in input file.");
            List<BacktestSummary> summaries = runner.run(alerts);
            String outputFile = reportWriter.write(summaries);
            String range = (from != null || to != null)
                    ? " | Range: " + (from != null ? from : "start") + " → " + (to != null ? to : "end")
                    : "";
            return ResponseEntity.ok("Backtest complete. Report: " + outputFile
                    + " | Strategies: " + summaries.size()
                    + " | Alerts: " + alerts.size() + range);
        } catch (Exception e) {
            log.error("Backtest failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Backtest failed: " + e.getMessage());
        }
    }

    /** Run backtest for a specific date only */
    @PostMapping("/run-date")
    public ResponseEntity<String> runDate(@RequestParam String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            List<AlertEntry> alerts = readAlerts(targetDate, null, null);
            if (alerts.isEmpty()) return ResponseEntity.ok("No alerts found for " + date);
            List<BacktestSummary> summaries = runner.run(alerts);
            String outputFile = reportWriter.write(summaries);
            return ResponseEntity.ok("Backtest for " + date + " complete. Report: " + outputFile
                    + " | Alerts: " + alerts.size());
        } catch (Exception e) {
            log.error("Backtest for {} failed: {}", date, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Backtest failed: " + e.getMessage());
        }
    }

    // ─── Alert readers ────────────────────────────────────────────────────────

    /**
     * Reads alerts from the Excel input file.
     * @param exactDate  if non-null, only this date is included (used by run-date)
     * @param fromDate   if non-null, alerts before this date are excluded
     * @param toDate     if non-null, alerts after this date are excluded
     */
    private List<AlertEntry> readAlerts(LocalDate exactDate,
                                        LocalDate fromDate,
                                        LocalDate toDate) throws Exception {
        String inputPath = applicationProperties.getBacktestInputAlertPath();
        List<AlertEntry> result = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(inputPath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String triggeredAt = cellStr(row.getCell(0));
                if (triggeredAt == null || triggeredAt.isBlank()) continue;

                try {
                    LocalDate alertDate = LocalDateTime.parse(triggeredAt, ALERT_FMT).toLocalDate();
                    if (exactDate != null && !alertDate.equals(exactDate)) continue;
                    if (fromDate  != null && alertDate.isBefore(fromDate))  continue;
                    if (toDate    != null && alertDate.isAfter(toDate))     continue;
                } catch (Exception ex) {
                    continue;
                }

                String stocks = cellStr(row.getCell(2));
                if (stocks == null || stocks.isBlank()) continue;
                double firstAlertPrice = cellNum(row.getCell(3));  // col D

                for (String s : stocks.split("\\s*,\\s*")) {
                    s = s.trim().toUpperCase();
                    if (!s.isEmpty()) result.add(new AlertEntry(triggeredAt, s, firstAlertPrice));
                }
            }
        }
        return result;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> null;
        };
    }

    private double cellNum(Cell cell) {
        if (cell == null) return 0.0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> { try { yield Double.parseDouble(cell.getStringCellValue()); } catch (NumberFormatException e) { yield 0.0; } }
            default      -> 0.0;
        };
    }
}
