package com.onepercentgrowth.local_to_smartapi.service;

//import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
//import com.onepercentgrowth.local_to_smartapi.model.LoginRequest;
//import com.onepercentgrowth.local_to_smartapi.model.LoginResponse;
//import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//@Service
//public class LoginService {
//
//    private final BrokerApiClient brokerApiClient;
//
//    private final TokenStorageService tokenStorageService;
//
//    public LoginService(BrokerApiClient brokerApiClient, TokenStorageService tokenStorageService) {
//        this.brokerApiClient = brokerApiClient;
//        this.tokenStorageService = tokenStorageService;
//    }
//
//    public Mono<LoginResponse> loginWithTotp(LoginRequest loginRequest) {
//        return brokerApiClient.loginWithTotp(loginRequest.getClientcode(), loginRequest.getPassword());
//    }
//
//    public Mono<LoginResponse> refreshTokens(String refreshToken, String authToken) {
//        return brokerApiClient.generateTokens(refreshToken, authToken);
//    }
//
//    public Mono<LoginResponse> logout(String clientCode, String authToken) {
//        return brokerApiClient.logout(clientCode, authToken);
//    }
//}

import com.onepercentgrowth.local_to_smartapi.model.LoginRequest;
import com.onepercentgrowth.local_to_smartapi.model.LoginResponse;
import com.onepercentgrowth.local_to_smartapi.properties.AngelApiProperties;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final WebClient authWebClient;
    private final AngelApiProperties angelConfig;
    private final TokenStorageService tokenStorageService;

    public LoginService(
            WebClient authWebClient,
            AngelApiProperties angelConfig,
            TokenStorageService tokenStorageService
    ) {
        this.authWebClient = authWebClient;
        this.angelConfig = angelConfig;
        this.tokenStorageService = tokenStorageService;
    }

    // ---------------- LOGIN ----------------

    public Mono<LoginResponse> loginWithTotp(LoginRequest loginRequest) {

        log.info("Logging in using TOTP for client={}", loginRequest.getClientcode());

        int totp = new GoogleAuthenticator()
                .getTotpPassword(angelConfig.getTotpCode());

        loginRequest.setTotp(String.valueOf(totp));

        return authWebClient.post()
                .uri("/rest/auth/angelbroking/user/v1/loginByPassword")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .doOnNext(resp -> {
                    if (resp != null && resp.getData() != null) {
                        tokenStorageService.storeTokens(resp.getData());
                        log.info("Login successful, tokens stored");
                    }
                });
    }

    // ---------------- REFRESH TOKENS ----------------

    public Mono<LoginResponse> refreshTokens(String refreshToken, String authToken) {

        log.info("Refreshing JWT tokens");

        return authWebClient.post()
                .uri("/rest/auth/angelbroking/jwt/v1/generateTokens")
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .bodyValue("{\"refreshToken\":\"" + refreshToken + "\"}")
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .doOnNext(resp -> {
                    if (resp != null && resp.getData() != null) {
                        tokenStorageService.storeTokens(resp.getData());
                        log.info("Token refresh successful");
                    }
                });
    }

    // ---------------- LOGOUT ----------------

    public Mono<LoginResponse> logout(String clientCode, String authToken) {

        log.info("Logging out client={}", clientCode);

        return authWebClient.post()
                .uri("/rest/secure/angelbroking/user/v1/logout")
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .bodyValue("{\"clientcode\":\"" + clientCode + "\"}")
                .retrieve()
                .bodyToMono(LoginResponse.class);
    }
}
