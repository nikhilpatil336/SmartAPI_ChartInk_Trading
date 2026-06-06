package com.onepercentgrowth.local_to_smartapi.backtest.report;

import com.onepercentgrowth.local_to_smartapi.backtest.runner.BacktestResult;
import com.onepercentgrowth.local_to_smartapi.backtest.runner.BacktestSummary;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BacktestReportWriter {

    private static final Logger log = LoggerFactory.getLogger(BacktestReportWriter.class);

    private final ApplicationProperties applicationProperties;

    public BacktestReportWriter(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String write(List<BacktestSummary> summaries) throws Exception {
        String outputDir = applicationProperties.getBacktestOutputPath();
        new File(outputDir).mkdirs();
        String fileName = outputDir + "backtest_" + LocalDate.now() + ".xlsx";

        try (Workbook wb = new XSSFWorkbook()) {
            writeSummarySheet(wb, summaries);
            for (BacktestSummary summary : summaries) {
                writeDetailSheet(wb, summary);
            }
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                wb.write(fos);
            }
        }

        log.info("Backtest report written to {}", fileName);
        return fileName;
    }

    private void writeSummarySheet(Workbook wb, List<BacktestSummary> summaries) {
        Sheet sheet = wb.createSheet("SUMMARY");

        List<Long> capitals = applicationProperties.getBacktestCapitalAmounts();
        if (capitals == null) capitals = List.of();

        // ── Overall summary table ────────────────────────────────────────────
        List<String> headerList = new ArrayList<>(List.of(
                "Strategy", "Total Alerts", "Trades Taken", "Wins", "Losses",
                "Squareoffs", "Win Rate %", "Avg Win R:R", "Expectancy (Avg Net P&L%)",
                "Total P&L %", "Avg Gross P&L/Trade %",
                "Total P&L After Charges %", "Avg After Charges %",
                "Compounded Return %",
                "Max Drawdown %", "Max Consec Losses", "Sharpe Ratio", "Verdict",
                "Nifty Return (Period) %", "Nifty Period", "Beats Nifty (Actual)?",
                "Nifty Avg Pace (Period) %", "Beats Nifty (Avg Pace)?"));
        for (long cap : capitals) headerList.add("Return ₹" + formatCapital(cap));

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headerList.size(); i++) {
            headerRow.createCell(i).setCellValue(headerList.get(i));
        }

        List<BacktestSummary> sorted = summaries.stream()
                .sorted((a, b) -> Double.compare(b.getExpectancy(), a.getExpectancy()))
                .toList();

        int rowNum = 1;
        for (BacktestSummary s : sorted) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;
            row.createCell(col++).setCellValue(s.getStrategyName());
            row.createCell(col++).setCellValue(s.getTotalAlerts());
            row.createCell(col++).setCellValue(s.getTradesTaken());
            row.createCell(col++).setCellValue(s.getWins());
            row.createCell(col++).setCellValue(s.getLosses());
            row.createCell(col++).setCellValue(s.getSquareoffs());
            row.createCell(col++).setCellValue(round(s.getWinRate() * 100, 1));
            row.createCell(col++).setCellValue(round(s.getAvgWinRR(), 2));
            row.createCell(col++).setCellValue(round(s.getExpectancy(), 3));
            row.createCell(col++).setCellValue(round(s.getTotalPnlPct(), 2));
            row.createCell(col++).setCellValue(round(s.getAvgPnlPerTrade(), 2));
            row.createCell(col++).setCellValue(round(s.getTotalPnlAfterChargesPct(), 2));
            row.createCell(col++).setCellValue(round(s.getAvgPnlAfterChargesPct(), 2));
            row.createCell(col++).setCellValue(round(s.getCompoundedReturnPct(), 2));
            row.createCell(col++).setCellValue(round(s.getMaxDrawdown(), 2));
            row.createCell(col++).setCellValue(s.getMaxConsecutiveLosses());
            row.createCell(col++).setCellValue(round(s.getSharpeRatio(), 3));
            row.createCell(col++).setCellValue(s.getVerdict());
            row.createCell(col++).setCellValue(round(s.getNiftyReturnPct(), 2));
            row.createCell(col++).setCellValue(s.getNiftyDateRange());
            row.createCell(col++).setCellValue(s.isBeatsNifty() ? "YES" : "NO");
            row.createCell(col++).setCellValue(round(s.getNiftyAvgPacePct(), 2));
            row.createCell(col++).setCellValue(s.isBeatsNiftyAvgPace() ? "YES" : "NO");
            for (long cap : capitals) {
                row.createCell(col++).setCellValue(round(s.getCompoundedReturnPct() / 100.0 * cap, 0));
            }
        }

        // ── Monthly breakdown table (5-row gap below overall) ────────────────
        int monthlyStartRow = rowNum + 5;

        List<String> mHeaders = new ArrayList<>(List.of(
                "Month", "Strategy", "Trades", "Win Rate %",
                "P&L After Charges %", "Compounded Return %", "Nifty Return %"));
        for (long cap : capitals) mHeaders.add("Return ₹" + formatCapital(cap));

        Row mHeaderRow = sheet.createRow(monthlyStartRow);
        for (int i = 0; i < mHeaders.size(); i++) {
            mHeaderRow.createCell(i).setCellValue(mHeaders.get(i));
        }

        // Collect all months across all strategies (sorted)
        java.util.TreeSet<String> allMonths = new java.util.TreeSet<>();
        for (BacktestSummary s : sorted) allMonths.addAll(s.getMonthlyStats().keySet());

        int mRow = monthlyStartRow + 1;
        for (String month : allMonths) {
            boolean wroteAny = false;
            for (BacktestSummary s : sorted) {
                BacktestSummary.MonthlyStats ms = s.getMonthlyStats().get(month);
                if (ms == null) continue;
                Row row = sheet.createRow(mRow++);
                wroteAny = true;
                int col = 0;
                row.createCell(col++).setCellValue(month);
                row.createCell(col++).setCellValue(s.getStrategyName());
                row.createCell(col++).setCellValue(ms.getTradesTaken());
                row.createCell(col++).setCellValue(round(ms.getWinRate() * 100, 1));
                row.createCell(col++).setCellValue(round(ms.getTotalPnlAfterChargesPct(), 2));
                row.createCell(col++).setCellValue(round(ms.getCompoundedReturnPct(), 2));
                row.createCell(col++).setCellValue(round(ms.getNiftyReturnPct(), 2));
                for (long cap : capitals) {
                    row.createCell(col++).setCellValue(round(ms.getCompoundedReturnPct() / 100.0 * cap, 0));
                }
            }
            if (wroteAny) mRow += 2;
        }
    }

    private String formatCapital(long amount) {
        if (amount >= 100000) return (amount / 100000) + "L";
        if (amount >= 1000)   return (amount / 1000) + "K";
        return String.valueOf(amount);
    }

    private void writeDetailSheet(Workbook wb, BacktestSummary summary) {
        String sheetName = summary.getStrategyName().length() > 31
                ? summary.getStrategyName().substring(0, 31) : summary.getStrategyName();
        Sheet sheet = wb.createSheet(sheetName);

        String[] headers = {"Date", "Stock", "Alert Time", "Direction", "Entry Timing",
                "Entry Price", "Target Price", "SL Price", "R:R",
                "Exit Price", "P&L Points", "P&L %", "Charges %", "P&L After Charges %",
                "Outcome", "Hit First", "Squareoff Reason", "Squareoff Price",
                "Time To Hit (min)", "Trailing Activated", "Trailing Max", "Trailing SL Exit",
                "Trailing Outcome", "Reason"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        int rowNum = 1;
        for (BacktestResult r : summary.getTrades()) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;
            row.createCell(col++).setCellValue(r.getTradeDate());
            row.createCell(col++).setCellValue(r.getStock());
            row.createCell(col++).setCellValue(r.getAlertTime());
            row.createCell(col++).setCellValue(r.getDirection());
            row.createCell(col++).setCellValue(r.getEntryTiming());
            row.createCell(col++).setCellValue(r.getEntryPrice());
            row.createCell(col++).setCellValue(r.getTargetPrice());
            row.createCell(col++).setCellValue(r.getSlPrice());
            row.createCell(col++).setCellValue(round(r.getRiskRewardRatio(), 2));
            row.createCell(col++).setCellValue(round(r.getExitPrice(), 2));
            row.createCell(col++).setCellValue(round(r.getPnlPoints(), 2));
            row.createCell(col++).setCellValue(round(r.getPnlPct(), 3));
            row.createCell(col++).setCellValue(round(r.getTotalChargesPct(), 4));
            row.createCell(col++).setCellValue(round(r.getPnlAfterChargesPct(), 3));
            row.createCell(col++).setCellValue(r.getTradeOutcome());
            row.createCell(col++).setCellValue(r.getHitFirst());
            row.createCell(col++).setCellValue(r.getSquareoffReason());
            row.createCell(col++).setCellValue(r.getSquareoffPrice());
            row.createCell(col++).setCellValue(r.getTimeToHitMins());
            row.createCell(col++).setCellValue(r.getTrailingActivated());
            row.createCell(col++).setCellValue(r.getTrailingMaxFavorable());
            row.createCell(col++).setCellValue(r.getTrailingSlExitPrice());
            row.createCell(col++).setCellValue(r.getTrailingOutcome());
            row.createCell(col).setCellValue(r.getReason());
        }
    }

    private double round(double v, int places) {
        double factor = Math.pow(10, places);
        return Math.round(v * factor) / factor;
    }
}
