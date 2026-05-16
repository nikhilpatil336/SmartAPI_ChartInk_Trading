package com.onepercentgrowth.local_to_smartapi.historicdata;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.google.gson.JsonObject;

@Service
public class HistoricalDataService {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private ScripMasterService scripMasterService;

    private final BrokerApiClient brokerApiClient;

    public HistoricalDataService(BrokerApiClient brokerApiClient) {
        this.brokerApiClient = brokerApiClient;
    }

    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    private static final DateTimeFormatter API_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Mono<HistoricalDataResponse> fetch2DayData(
            String symbolToken,
            String fromDate,
            String toDate,
            String interval
    ) {

        LocalDateTime from = LocalDateTime.parse(fromDate, INPUT_FORMAT);
        LocalDateTime to = LocalDateTime.parse(toDate, INPUT_FORMAT);


        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("exchange", "NSE");
        requestBody.addProperty("symboltoken", symbolToken);
        requestBody.addProperty("interval", interval);
        requestBody.addProperty("fromdate", from.format(API_FORMAT));
        requestBody.addProperty("todate", to.format(API_FORMAT));

        String jwtToken = tokenManager.getValidJwtToken();

        return brokerApiClient.getHistoricalCandleData(requestBody, jwtToken);
    }

    public Mono<CandleAnalysisResponse> analyzeCandle(
            String symbol,
            String inputDate,
            String interval
    ) {

        // ✅ Step 1: Get Symbol Token
        String symbolToken = scripMasterService.getTokenForName(symbol);

        // ✅ Step 2: Parse Input Date
        LocalDateTime target = LocalDateTime.parse(
                inputDate,
                DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH)
        );

        // ✅ Step 3: Create range (previous + current day)
        LocalDateTime from = target.minusDays(3).withHour(9).withMinute(15);
        LocalDateTime to = target.withHour(15).withMinute(30);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("exchange", "NSE");
        requestBody.addProperty("symboltoken", symbolToken);
        requestBody.addProperty("interval", interval);
        requestBody.addProperty("fromdate", from.format(API_FORMAT));
        requestBody.addProperty("todate", to.format(API_FORMAT));

        String jwtToken = tokenManager.getValidJwtToken();

        return brokerApiClient.getHistoricalCandleData(requestBody, jwtToken)
                .map(response -> {

                    // ✅ Step 4: Convert raw → Candle list
                    List<Candle> candles = response.getData().stream()
                            .map(this::mapToCandle)
                            .sorted(Comparator.comparing(Candle::getTimestamp))
                            .toList();

                    // ✅ Step 5: Find exact candle index
                    int index = findCandleIndex(candles, target);

                    if (index <= 0) {
                        throw new RuntimeException("Candle not found or no previous candle");
                    }

                    Candle current = candles.get(index);
//                    Candle previous = candles.get(index - 1);

                    // ✅ Step 6: Candle color

                    // ✅ Candle color (CURRENT candle)
                    boolean isGreen = current.getClose() > current.getOpen();
                    boolean isRed = current.getClose() < current.getOpen();

                    // ✅ SMA calculation
                    double sma10 = calculateSMA(candles, index, 10);

                    // ✅ Decision logic
                    String decision = "NO_TRADE";

                    if (sma10 > 0) { // safety check
                        if (isGreen && current.getVolume() > sma10) {
                            decision = "TAKE_LONG";
                        } else if (isRed && current.getVolume() < sma10) {
                            decision = "TAKE_SHORT";
                        }
                    }

                    // ✅ Step 9: Build response
                    CandleAnalysisResponse result = new CandleAnalysisResponse();
                    result.setCurrentCandle(current);
                    result.setSma10Volume(sma10);
                    result.setTradeDecision(decision);

                    return result;
                });
    }

    private int findCandleIndex(List<Candle> candles, LocalDateTime target) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        int index = -1;

        for (int i = 0; i < candles.size(); i++) {
            LocalDateTime candleTime = LocalDateTime.parse(
                    candles.get(i).getTimestamp(),
                    formatter
            );

//            if (candleTime.equals(target)) {
//                return i;
//            }
            if (!candleTime.isAfter(target)) {
                index = i;
            } else {
                break;
            }
        }

//        throw new RuntimeException("Exact candle not found for time: " + target);
        return index;
    }

    private Candle mapToCandle(List<Object> row) {
        Candle c = new Candle();
        c.setTimestamp((String) row.get(0));
        c.setOpen(Double.parseDouble(row.get(1).toString()));
        c.setHigh(Double.parseDouble(row.get(2).toString()));
        c.setLow(Double.parseDouble(row.get(3).toString()));
        c.setClose(Double.parseDouble(row.get(4).toString()));
        c.setVolume(Long.parseLong(row.get(5).toString()));
        return c;
    }

    private double calculateSMA(List<Candle> candles, int endIndex, int period) {

        if (endIndex - period + 1 < 0) {
            return 0;
        }

        double sum = 0;

        for (int i = endIndex; i > endIndex - period; i--) {
            sum += candles.get(i).getVolume();
        }

        return sum / period;
    }





//    public void processExcelAndWriteBack() throws Exception {
//
//        String filePath = "D:\\1 percent growth trading\\chartinkAlertExcels\\AllAlerts.xlsx";
//
//        FileInputStream fis = new FileInputStream(filePath);
//        Workbook workbook = new XSSFWorkbook(fis);
//        Sheet sheet = workbook.getSheetAt(0);
//
//        List<BacktestResultRow> results = new ArrayList<>();
//
//        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//
//            Row row = sheet.getRow(i);
//            if (row == null) continue;
//
//            String triggeredAt = row.getCell(0).getStringCellValue();
//            String stocksCell = row.getCell(2).getStringCellValue();
//
////            String[] stocks = stocksCell.split(",");
////
////            for (String stock : stocks) {
////
////                stock = stock.trim();
////
////                BacktestResultRow result = processSingleStockForExcel(stock, triggeredAt);
////
////                if (result != null) {
////                    results.add(result);
////                }
////
////                Thread.sleep(2000);
////            }
//
//            List<Pair<String, String>> normalizedList = new ArrayList<>();
//
//            String[] stocks = stocksCell.split("\\s*,\\s*");
//
//            for (String stock : stocks) {
//                stock = stock.trim().toUpperCase();
//
//                if (!stock.isEmpty()) {
//                    normalizedList.add(Pair.of(triggeredAt, stock));
//                }
//            }
//
//            for (Pair<String, String> entry : normalizedList) {
//
//                String triggeredAt_pair = entry.getLeft();
//                String stock = entry.getRight();
//
//                BacktestResultRow result = processSingleStockForExcel(stock, triggeredAt_pair);
//
//                if (result != null) {
//                    results.add(result);
//                }
//
//                Thread.sleep(2000);
//            }
//        }
//
//        workbook.close();
//        fis.close();
//
//        // ✅ Rewrite same file
//        writeResultsToSameExcel(filePath, results);
//    }
//
//    private BacktestResultRow processSingleStockForExcel(String symbol, String triggeredAt) {
//
//        try {
//
//            String symbolToken = scripMasterService.getTokenForName(symbol);
//
//            LocalDateTime target = LocalDateTime.parse(
//                    triggeredAt,
//                    DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH)
//            );
//
//            LocalDateTime from = target.minusDays(1).withHour(9).withMinute(15);
//            LocalDateTime to = target.withHour(15).withMinute(30);
//
//            JsonObject requestBody = new JsonObject();
//            requestBody.addProperty("exchange", "NSE");
//            requestBody.addProperty("symboltoken", symbolToken);
//            requestBody.addProperty("interval", "FIVE_MINUTE");
//            requestBody.addProperty("fromdate", from.format(API_FORMAT));
//            requestBody.addProperty("todate", to.format(API_FORMAT));
//
//            String jwtToken = tokenManager.getValidJwtToken();
//
//            HistoricalDataResponse response =
//                    brokerApiClient.getHistoricalCandleData(requestBody, jwtToken).block();
//
//            List<Candle> candles = response.getData().stream()
//                    .map(this::mapToCandle)
//                    .sorted(Comparator.comparing(Candle::getTimestamp))
//                    .toList();
//
//            int index = findCandleIndex(candles, target);
//            if (index < 0) return null;
//
//            Candle current = candles.get(index);
//            Candle previous = index > 0 ? candles.get(index - 1) : null;
//
//            // ✅ Candle color
//            String color;
//            if (current.getClose() > current.getOpen()) {
//                color = "GREEN";
//            } else if (current.getClose() < current.getOpen()) {
//                color = "RED";
//            } else {
//                color = "DOJI";
//            }
//
//            boolean isGreen = "GREEN".equals(color);
//            boolean isRed = "RED".equals(color);
//
//            // ✅ SMA
//            double sma10 = calculateSMA(candles, index, 10);
//            double sma20 = calculateSMA(candles, index, 20);
//
//            long prevVolume = previous != null ? previous.getVolume() : 0;
//
//            // ✅ Strategy Decision
//            String decision = "NO_TRADE";
//
//            if (isGreen && current.getVolume() > sma10) {
//                decision = "TAKE_LONG";
//            } else if (isRed && current.getVolume() < sma10) {
//                decision = "TAKE_SHORT";
//            }
//
//            // ✅ Build result row
//            BacktestResultRow row = new BacktestResultRow();
//
//            row.setTriggeredAt(triggeredAt);
//            row.setStock(symbol);
//
//            row.setOpen(current.getOpen());
//            row.setHigh(current.getHigh());
//            row.setLow(current.getLow());
//            row.setClose(current.getClose());
//            row.setVolume(current.getVolume());
//
//            row.setColor(color);
//            row.setPrevVolume(prevVolume);
//
//            row.setSma10(sma10);
//            row.setSma20(sma20);
//
//            // ✅ Raw comparisons (keep for analysis)
//            row.setVolGtSma10(current.getVolume() > sma10);
//            row.setVolGtSma20(current.getVolume() > sma20);
//            row.setVolLtSma10(current.getVolume() < sma10);
//            row.setVolLtSma20(current.getVolume() < sma20);
//
//            // ✅ Strategy-based conditions
//            row.setTradeDecision(decision);
//
//            return row;
//
//        } catch (Exception e) {
//            System.out.println("Error processing " + symbol + " : " + e.getMessage());
//            return null;
//        }
//    }

    private void writeResultsToSameExcel(String filePath, List<BacktestResultRow> results) throws Exception {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Backtest");

        int rowNum = 0;

        // ✅ Header
        Row header = sheet.createRow(rowNum++);

        String[] columns = {
                "Triggered At", "Stock",
                "Open", "High", "Low", "Close", "Volume",
                "Color", "Prev Volume",
                "SMA10", "SMA20",
                "Vol>SMA10", "Vol>SMA20",
                "Vol<SMA10", "Vol<SMA20"
        };

        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        // ✅ Data rows
        for (BacktestResultRow r : results) {

            Row row = sheet.createRow(rowNum++);

            int col = 0;

            row.createCell(col++).setCellValue(r.getTriggeredAt());
            row.createCell(col++).setCellValue(r.getStock());

            row.createCell(col++).setCellValue(r.getOpen());
            row.createCell(col++).setCellValue(r.getHigh());
            row.createCell(col++).setCellValue(r.getLow());
            row.createCell(col++).setCellValue(r.getClose());
            row.createCell(col++).setCellValue(r.getVolume());

            row.createCell(col++).setCellValue(r.getColor());
            row.createCell(col++).setCellValue(r.getPrevVolume());

            row.createCell(col++).setCellValue(r.getSma10());
            row.createCell(col++).setCellValue(r.getSma20());

            row.createCell(col++).setCellValue(r.isVolGtSma10());
            row.createCell(col++).setCellValue(r.isVolGtSma20());
            row.createCell(col++).setCellValue(r.isVolLtSma10());
            row.createCell(col++).setCellValue(r.isVolLtSma20());

            row.createCell(col++).setCellValue(r.getTradeDecision());
        }

        // ✅ Overwrite SAME FILE
        FileOutputStream fos = new FileOutputStream(filePath);
        workbook.write(fos);

        fos.close();
        workbook.close();
    }


    public void processExcelAndWriteBack() throws Exception {

        String filePath = "D:\\1 percent growth trading\\chartinkAlertExcels\\AllAlerts.xlsx";

        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        List<Pair<String, String>> normalizedList = new ArrayList<>();
        List<BacktestResultRow> results = new ArrayList<>();

        // ✅ STEP 1: Normalize data
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);
            if (row == null) continue;

            String triggeredAt = row.getCell(0).getStringCellValue();
            String stocksCell = row.getCell(2).getStringCellValue();

            String[] stocks = stocksCell.split("\\s*,\\s*");

            for (String stock : stocks) {
                stock = stock.trim().toUpperCase();

                if (!stock.isEmpty()) {
                    normalizedList.add(Pair.of(triggeredAt, stock));
                }
            }
        }

        workbook.close();
        fis.close();

        // ✅ STEP 2: Process each stock with retry + adaptive delay
        for (Pair<String, String> entry : normalizedList) {

            String triggeredAt = entry.getLeft();
            String stock = entry.getRight();

            System.out.println("Processing: " + stock + " at " + triggeredAt);

            long start = System.currentTimeMillis();

            BacktestResultRow result = processSingleStockForExcel(stock, triggeredAt);

            if (result != null) {
                results.add(result);
            }

            // ✅ Adaptive delay (avoid rate limit)
            long timeTaken = System.currentTimeMillis() - start;
            if (timeTaken < 1500) {
                Thread.sleep(1500 - timeTaken);
            }
        }

        // ✅ STEP 3: Write back
        writeResultsToSameExcel(filePath, results);
    }

    private BacktestResultRow processSingleStockForExcel(String symbol, String triggeredAt) {

        try {

            String symbolToken = scripMasterService.getTokenForName(symbol);

            LocalDateTime target = LocalDateTime.parse(
                    triggeredAt,
                    DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH)
            );

//            LocalDateTime from = target.minusDays(1).withHour(9).withMinute(15);
            LocalDateTime from = target.minusDays(3).withHour(9).withMinute(15);
//            LocalDateTime to = target.withHour(15).withMinute(30);
            LocalDateTime to = target;

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("exchange", "NSE");
            requestBody.addProperty("symboltoken", symbolToken);
            requestBody.addProperty("interval", "FIVE_MINUTE");
            requestBody.addProperty("fromdate", from.format(API_FORMAT));
            requestBody.addProperty("todate", to.format(API_FORMAT));

            String jwtToken = tokenManager.getValidJwtToken();

            // ✅ Retry-enabled API call
            HistoricalDataResponse response = fetchWithRetry(requestBody, jwtToken);

            if (response == null || !response.isStatus() || response.getData() == null) {
                System.out.println("Invalid response for " + symbol);
                return null;
            }

            List<Candle> candles = response.getData().stream()
                    .map(this::mapToCandle)
                    .sorted(Comparator.comparing(Candle::getTimestamp))
                    .toList();

            System.out.println("Candles fetched: " + candles.size());

            int index = findNearestCandleIndex(candles, target);
            if (index < 0) return null;

            Candle current = candles.get(index);
            Candle previous = index > 0 ? candles.get(index - 1) : null;

            // ✅ Candle color
            String color;
            if (current.getClose() > current.getOpen()) {
                color = "GREEN";
            } else if (current.getClose() < current.getOpen()) {
                color = "RED";
            } else {
                color = "DOJI";
            }

            boolean isGreen = "GREEN".equals(color);
            boolean isRed = "RED".equals(color);

            double sma10 = calculateSMA(candles, index, 10);
            double sma20 = calculateSMA(candles, index, 20);

            long prevVolume = previous != null ? previous.getVolume() : 0;

            // ✅ Strategy Decision
            String decision = "NO_TRADE";

            if (isGreen && current.getVolume() > sma10) {
                decision = "TAKE_LONG";
            } else if (isRed && current.getVolume() < sma10) {
                decision = "TAKE_SHORT";
            }

            BacktestResultRow row = new BacktestResultRow();

            row.setTriggeredAt(triggeredAt);
            row.setStock(symbol);

            row.setOpen(current.getOpen());
            row.setHigh(current.getHigh());
            row.setLow(current.getLow());
            row.setClose(current.getClose());
            row.setVolume(current.getVolume());

            row.setColor(color);
            row.setPrevVolume(prevVolume);

            row.setSma10(sma10);
            row.setSma20(sma20);

            row.setVolGtSma10(current.getVolume() > sma10);
            row.setVolGtSma20(current.getVolume() > sma20);
            row.setVolLtSma10(current.getVolume() < sma10);
            row.setVolLtSma20(current.getVolume() < sma20);

            row.setTradeDecision(decision);

            return row;

        } catch (Exception e) {
            System.out.println("Error processing " + symbol + " : " + e.getMessage());
            return null;
        }
    }

    private HistoricalDataResponse fetchWithRetry(JsonObject requestBody, String jwtToken) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                HistoricalDataResponse response =
                        brokerApiClient.getHistoricalCandleData(requestBody, jwtToken).block();

                if (response != null && response.isStatus() && response.getData() != null) {
                    return response;
                }

            } catch (Exception e) {
                System.out.println("Retry attempt " + attempts);
            }

            attempts++;

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
        }

        return null;
    }


    private int findNearestCandleIndex(List<Candle> candles, LocalDateTime target) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        int closestIndex = -1;
        long minDiff = Long.MAX_VALUE;

        for (int i = 0; i < candles.size(); i++) {

            LocalDateTime candleTime = LocalDateTime.parse(
                    candles.get(i).getTimestamp(),
                    formatter
            );

            long diff = Math.abs(Duration.between(candleTime, target).toMinutes());

            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }

        return closestIndex;
    }
}
