package com.onepercentgrowth.local_to_smartapi.controller;

import com.onepercentgrowth.local_to_smartapi.model.RmsResponse;
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

    public ScripMasterController(ScripMasterService scripMasterService) {
        this.scripMasterService = scripMasterService;
    }

    @GetMapping("/nse/download")
    public Mono<Map<String, String>> getNseScripMaster() {
        return scripMasterService.fetchNseScripMaster();
    }

    @GetMapping("/rms/balance")
    public Mono<RmsResponse> getRmsBalance() {
        return scripMasterService.getCurrentBalance();
    }
}
