package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.exceptions.AuthExpiredException;
import com.onepercentgrowth.local_to_smartapi.model.RmsData;
import com.onepercentgrowth.local_to_smartapi.storage.RmsStorageService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RmsService {

//    private final BrokerApiClient brokerApiClient;
//    private final TokenStorageService tokenStorageService;
//    private final RmsStorageService rmsStorageService;
//    private final BalanceService balanceService;
//
//    public RmsService(BrokerApiClient brokerApiClient,
//                      TokenStorageService tokenStorageService,
//                      RmsStorageService rmsStorageService,
//                      BalanceService balanceService) {
//        this.brokerApiClient = brokerApiClient;
//        this.tokenStorageService = tokenStorageService;
//        this.rmsStorageService = rmsStorageService;
//        this.balanceService = balanceService;
//    }

    private final BrokerApiClient brokerApiClient;
    private final TokenManager tokenManager;
    private final RmsStorageService rmsStorageService;
    private final BalanceService balanceService;

    public RmsService(
            BrokerApiClient brokerApiClient,
            TokenManager tokenManager,
            RmsStorageService rmsStorageService,
            BalanceService balanceService
    ) {
        this.brokerApiClient = brokerApiClient;
        this.tokenManager = tokenManager;
        this.rmsStorageService = rmsStorageService;
        this.balanceService = balanceService;
    }

    /** Manual refresh */
//    public Mono<RmsData> refreshNow() {
//        String token = tokenStorageService.getJwtToken();
//        if (token == null) {
//            return Mono.error(new RuntimeException("Login required"));
//        }
//
//        return brokerApiClient.fetchRmsBalance(token)
//                .map(resp -> resp.getData())
//                .doOnNext(rms -> {
//                    rmsStorageService.save(rms);
//                    balanceService.syncFromRms(rms); // VERY IMPORTANT
//                });
//    }

    public Mono<RmsData> refreshNow() {

        return brokerApiClient
                .fetchRmsBalance(tokenManager.getValidJwtToken())
                .onErrorResume(
                        AuthExpiredException.class,
                        ex -> tokenManager.refreshTokens()
                                .then(
                                        brokerApiClient.fetchRmsBalance(
                                                tokenManager.getValidJwtToken()
                                        )
                                )
                )
                .map(resp -> resp.getData())
                .doOnNext(rms -> {
                    rmsStorageService.save(rms);
                    balanceService.syncFromRms(rms);
                });
    }

    public RmsData getCachedRms() {
        return rmsStorageService.getCached();
    }
}

