package com.onepercentgrowth.local_to_smartapi.config;

import com.onepercentgrowth.local_to_smartapi.model.LoginRequest;
import com.onepercentgrowth.local_to_smartapi.service.LoginService;
import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import com.onepercentgrowth.local_to_smartapi.service.ScripMasterStorageService;
import com.onepercentgrowth.local_to_smartapi.service.TokenStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenManagerService {

    private static final Logger log = LoggerFactory.getLogger(TokenManagerService.class);

    @Autowired
    private TokenStorageService tokenStorageService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private ScripMasterStorageService scripMasterStorageService;

    @Autowired
    private ScripMasterService scripMasterService;

    @PostConstruct
    public void init() {
        tokenStorageService.loadTokensFromFile();

        if (isTokenExpired()) {
            try {
                refreshTokens().block();
            } catch (Exception e) {
                log.error("Token refresh at startup failed: {}", e.getMessage());
            }
        }

        scripMasterStorageService.loadFromFile();

        if (scripMasterStorageService.getCachedRawList() != null &&
                scripMasterStorageService.isFileFromToday()) {

            log.info("✔ Loading today's cached ScripMaster");
            scripMasterService.setRawScripList(scripMasterStorageService.getCachedRawList());

        } else {
            log.info("⚠ Cached ScripMaster old/missing. Fetching from API...");
            refreshScripMasterList().block();
        }
    }

    public synchronized String getValidJwtToken() {
        if (isTokenExpired()) {
            refreshTokens();
        }
        return tokenStorageService.getJwtToken();
    }

    private boolean isTokenExpired() {
        return tokenStorageService.isTokenExpired();
    }

    private Mono<Void> refreshTokens() {
        return loginService
                .loginWithTotp(new LoginRequest("AACA450749", "6200"))
                .flatMap(resp -> {

                    if (resp == null) {
                        return Mono.error(new RuntimeException("Login failed: Null response"));
                    }

                    if (resp.getData() == null) {
                        return Mono.error(new RuntimeException(
                                "Login failed: " + resp.getMessage()
                        ));
                    }

                    tokenStorageService.storeTokens(resp.getData());
                    return Mono.empty();
                })
                .doOnError(err -> log.error("Token refresh failed: {}", err.getMessage())).then();
    }

    private Mono<Void> refreshScripMasterList() {
        return scripMasterService
                .downloadRawScripMaster()
                .flatMap(rawList -> {

                    if (rawList == null) {
                        return Mono.error(new RuntimeException("ScripMaster API returned null"));
                    }

                    scripMasterStorageService.saveRawScripMaster(rawList);
                    scripMasterService.setRawScripList(rawList);

                    return Mono.empty();
                })
                .doOnError(err -> log.error("ScripMaster refresh failed: {}", err.getMessage()))
                .then();
    }
}

