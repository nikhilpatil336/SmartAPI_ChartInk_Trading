package com.onepercentgrowth.local_to_smartapi.client;

import com.onepercentgrowth.local_to_smartapi.config.AngelApiProperties;
import com.onepercentgrowth.local_to_smartapi.model.*;
import com.onepercentgrowth.local_to_smartapi.service.TokenStorageService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class BrokerApiClient {

    private static final Logger log = LoggerFactory.getLogger(BrokerApiClient.class);

    private final WebClient brokerWebClient;
    private AngelApiProperties angelConfig;
    private TokenStorageService tokenStorageService;

    public BrokerApiClient(
            WebClient brokerWebClient,
            AngelApiProperties angelConfig,
            TokenStorageService tokenStorageService
    ) {
        this.brokerWebClient = brokerWebClient;
        this.angelConfig = angelConfig;
        this.tokenStorageService = tokenStorageService;
    }

    public Mono<LoginResponse> loginWithTotp(String clientCode, String mpin) {

        try {
            log.info("Generating TOTP for client={}", clientCode);

            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            int totpCode = gAuth.getTotpPassword(angelConfig.getTotpCode());

            // Build request
            LoginRequest request = new LoginRequest();
            request.setClientcode(clientCode);
            request.setPassword(mpin);
            request.setTotp(String.valueOf(totpCode));
            request.setState("state"); // optional, can be static or dynamic

            log.info("Final LoginRequest JSON: {}", request);

            return brokerWebClient.post()
                    .uri("/rest/auth/angelbroking/user/v1/loginByPassword")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-UserType", angelConfig.getUserType())
                    .header("X-SourceID", angelConfig.getSourceId())
                    .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                    .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                    .header("X-MACAddress", angelConfig.getClientMacAddress())
                    .header("X-PrivateKey", angelConfig.getPrivateKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LoginResponse.class)
                    .doOnSuccess(resp -> {
                        log.info("LoginWithTotp Response: {}", resp);
                        if (resp != null && resp.getData() != null) {
                            tokenStorageService.storeTokens(resp.getData());
                            log.info("Tokens stored successfully and written to file for client={}", clientCode);
                        }
                    })
                    .doOnError(err -> log.error("LoginWithTotp failed: {}", err.getMessage(), err));

        } catch (Exception e) {
            log.error("Error generating TOTP: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Failed to generate TOTP", e));
        }
    }

    public Mono<OrderResponse> placeOrder(OrderRequest_v2 orderRequest, String authToken) {

        log.info("Placing order: {}", orderRequest);

        return brokerWebClient.post()
                .uri("/rest/secure/angelbroking/order/v1/placeOrder")
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .bodyValue(orderRequest)
                .retrieve()
                .bodyToMono(OrderResponse.class)
                .doOnSuccess(resp -> log.info("Order Response: {}", resp))
                .doOnError(err -> log.error("Error placing order: {}", err.getMessage(), err));
    }

    public Mono<LoginResponse> generateTokens(String refreshToken, String authToken) {

        log.info("Generating new JWT tokens for refreshToken={}", refreshToken);

        return brokerWebClient.post()
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
                .doOnSuccess(resp -> log.info("GenerateTokens Response: {}", resp))
                .doOnError(err -> log.error("Error generating JWT Tokens: {}", err.getMessage(), err));
    }

    public Mono<LoginResponse> logout(String clientCode, String authToken) {

        log.info("Logging out client={}", clientCode);

        return brokerWebClient.post()
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
                .bodyToMono(LoginResponse.class)
                .doOnSuccess(resp -> log.info("Logout Response: {}", resp))
                .doOnError(err -> log.error("Logout failed: {}", err.getMessage(), err));
    }

    public Mono<List<Map<String, Object>>> downloadScripMaster(String accessToken) {

        log.info("➡ Starting ScripMaster download request...");

        return brokerWebClient.get()
                .uri("https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json")
                .headers(headers -> {
                    headers.add("Authorization", "Bearer " + accessToken);
                    headers.add("Content-Type", "application/json");
                    headers.add("Accept", "application/json");
                    headers.add("X-UserType", "USER");
                    headers.add("X-SourceID", "WEB");
                    headers.add("X-ClientLocalIP", "127.0.0.1");
                    headers.add("X-ClientPublicIP", "127.0.0.1");
                    headers.add("X-MACAddress", "aa:bb:cc:dd:ee:ff");
                    headers.add("X-PrivateKey", angelConfig.getPrivateKey());
                })
                .exchangeToFlux(response -> {

                    log.info("⬅ Received HTTP status: {}", response.statusCode());

                    if (response.statusCode().is2xxSuccessful()) {
                        log.info("✔ ScripMaster request successful. Parsing JSON...");
                        return response.bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {});
                    } else {
                        log.error("❌ ScripMaster download failed! HTTP Status: {}",
                                response.statusCode());
                        return response.createException().flatMapMany(Flux::error);
                    }
                })
                .collectList()
                .doOnSuccess(list -> {
                    log.info("✔ Successfully downloaded ScripMaster.");
                    log.info("📦 Total entries received: {}", list.size());
                })
                .doOnError(err -> {
                    log.error("❌ Error while downloading ScripMaster: {}", err.getMessage(), err);
                });
    }

    public int generateTotp(String secret) {

        log.info("Generating TOTP manually...");

        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(30000)   // 30 seconds
                .build();

        GoogleAuthenticator gAuth = new GoogleAuthenticator(config);
        int code = gAuth.getTotpPassword(secret);

        log.info("Generated TOTP = {}", code);
        return code;
    }

    public Mono<RmsResponse> fetchRmsBalance(String authToken) {

        return brokerWebClient
                .get()   // RMS needs GET
                .uri("/rest/secure/angelbroking/user/v1/getRMS")
                .header("Authorization", "Bearer " + authToken)
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .retrieve()
                .bodyToMono(RmsResponse.class)
                .doOnSuccess(resp -> log.info("✔ RMS Response: {}", resp))
                .doOnError(err -> log.error("❌ Error fetching RMS: {}", err.getMessage(), err));
    }

    public Mono<OrderBookResponse> getOrderBook(String token) {
        return brokerWebClient.get()
                .uri("/rest/secure/angelbroking/order/v1/getOrderBook")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .retrieve()
                .bodyToMono(OrderBookResponse.class);
    }

    public Mono<TradeBookResponse> getTradeBook(String token) {
        return brokerWebClient.get()
                .uri("/rest/secure/angelbroking/order/v1/getTradeBook")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .retrieve()
                .bodyToMono(TradeBookResponse.class);
    }

}