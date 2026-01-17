package com.onepercentgrowth.local_to_smartapi.config;

import com.onepercentgrowth.local_to_smartapi.model.LoginRequest;
import com.onepercentgrowth.local_to_smartapi.properties.AngelApiProperties;
import com.onepercentgrowth.local_to_smartapi.service.LoginService;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@EnableScheduling
public class TokenManager {

    private static final Logger log =
            LoggerFactory.getLogger(TokenManager.class);

    private final TokenStorageService tokenStorageService;
    private final LoginService loginService;
    private final AngelApiProperties angelApiProperties;
    private final Object loginLock = new Object();
//    private final TokenScheduler tokenScheduler;

    public TokenManager(
            TokenStorageService tokenStorageService,
            LoginService loginService,
            AngelApiProperties angelApiProperties
//            TokenScheduler tokenScheduler
    ) {
        this.tokenStorageService = tokenStorageService;
        this.loginService = loginService;
        this.angelApiProperties = angelApiProperties;
//        this.tokenScheduler = tokenScheduler;
    }

    @PostConstruct
    public void init() {
        tokenStorageService.loadTokensFromFile();

        if (isTokenExpired()) {
            refreshTokens().block(); // ✅ block here
        }
    }

    public String getValidJwtToken() {
        String token = tokenStorageService.getJwtToken();

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "JWT token not initialized. Application startup login failed."
            );
        }

        return token;
    }


    public Mono<String> getValidJwtTokenAsync() {

        if (!isTokenExpired()) {
            return Mono.just(tokenStorageService.getJwtToken());
        }

        return refreshTokens()
                .then(Mono.fromSupplier(tokenStorageService::getJwtToken));
    }

    private boolean isTokenExpired() {
        return tokenStorageService.isTokenExpired();
    }

    public Mono<Void> refreshTokens() {
        return Mono.defer(() -> {
            synchronized (loginLock) {
                if (!isTokenExpired()) {
                    return Mono.empty();
                }

                return loginService
                        .loginWithTotp(new LoginRequest(
                                angelApiProperties.getClientId(),
                                angelApiProperties.getPassword()
                        ))
                        .doOnNext(resp -> tokenStorageService.storeTokens(resp.getData()))
                        .then();
            }
        });
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void refreshIfNeeded() {

        if (!tokenStorageService.willExpireIn(Duration.ofMinutes(5))) {
            return;
        }

        log.info("JWT expiring soon → refreshing using refresh token");

        loginService.refreshTokens(
                        tokenStorageService.getRefreshToken(),
                        tokenStorageService.getJwtToken()
                )
                .doOnError(e -> log.error("Token refresh failed", e))
                .block();
    }
}