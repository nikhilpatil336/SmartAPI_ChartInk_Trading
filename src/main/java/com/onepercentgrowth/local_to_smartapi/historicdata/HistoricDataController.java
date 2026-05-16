package com.onepercentgrowth.local_to_smartapi.historicdata;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/chartink")
public class HistoricDataController {

    private final HistoricalDataService service;

    public HistoricDataController(HistoricalDataService service) {
        this.service = service;
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

}
