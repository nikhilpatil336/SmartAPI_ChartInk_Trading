package com.onepercentgrowth.local_to_smartapi.controller;

import com.onepercentgrowth.local_to_smartapi.model.RmsData;
import com.onepercentgrowth.local_to_smartapi.model.ScripMasterRecord;
import com.onepercentgrowth.local_to_smartapi.service.RmsService;
import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/scripmaster")
public class ScripMasterController {

    private final ScripMasterService scripMasterService;
    private final RmsService rmsService;

    public ScripMasterController(ScripMasterService scripMasterService, RmsService rmsService) {
        this.scripMasterService = scripMasterService;
        this.rmsService = rmsService;
    }

    @GetMapping("/nse/download")
//    public Mono<Map<String, String>> getNseScripMaster() {
//        return scripMasterService.fetchNseScripMaster();
//    }
    public Mono<Map<String, ScripMasterRecord>> getNseScripMaster() {
        return scripMasterService.fetchNseScripMaster();
    }

    @GetMapping("/rms/balance")
    public Mono<RmsData> getRmsBalance() {
        return rmsService.refreshNow();
    }
}
