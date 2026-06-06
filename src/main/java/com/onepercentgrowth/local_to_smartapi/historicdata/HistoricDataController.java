package com.onepercentgrowth.local_to_smartapi.historicdata;

import com.onepercentgrowth.local_to_smartapi.analytics.AlertAnalyticsService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/chartink")
public class HistoricDataController {

    private final HistoricalDataService service;
    private final AlertAnalyticsService analyticsService;

    public HistoricDataController(HistoricalDataService service, AlertAnalyticsService analyticsService) {
        this.service = service;
        this.analyticsService = analyticsService;
    }

    @PostMapping("/test-historical")
    public Mono<HistoricalDataResponse> test(@RequestBody HistoricalDataRequest request) {

        return service.fetch2DayData(
                request.getSymbolToken(),
                request.getFromDate(),
                request.getToDate(),
                request.getInterval()
        );
    }

    @PostMapping("/analyze")
    public Mono<CandleAnalysisResponse> analyze(@RequestBody CandleAnalysisRequest request) {

        return service.analyzeCandle(
                request.getSymbol(),
                request.getDate(),
                request.getInterval()
        );
    }

    @GetMapping("/process-excel")
    public String processExcel() {
        try {
            service.processExcelAndWriteBack();
            return "Excel processed successfully";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/analytics/run-today")
    public String runTodayAnalytics() {
        try {
            analyticsService.runForToday();
            return "EOD analytics completed for today";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/analytics/run-historical")
    public String runHistoricalAnalytics() {
        try {
            analyticsService.runHistorical();
            return "Historical analytics backfill completed";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/analytics/run-for-date")
    public String runAnalyticsForDate(@RequestParam String date) {
        try {
            analyticsService.runForDate(LocalDate.parse(date));
            return "EOD analytics completed for " + date;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

}
