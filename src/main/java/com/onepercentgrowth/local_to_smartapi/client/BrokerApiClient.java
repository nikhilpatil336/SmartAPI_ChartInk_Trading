package com.onepercentgrowth.local_to_smartapi.client;

import com.google.gson.JsonObject;
import com.onepercentgrowth.local_to_smartapi.exceptions.AuthExpiredException;
import com.onepercentgrowth.local_to_smartapi.historicdata.HistoricalDataResponse;
import com.onepercentgrowth.local_to_smartapi.properties.AngelApiProperties;
import com.onepercentgrowth.local_to_smartapi.model.*;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.IOrderRequest;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class BrokerApiClient {

    private static final Logger log = LoggerFactory.getLogger(BrokerApiClient.class);

    private final WebClient directClient;
    private final WebClient proxyClient;
    private AngelApiProperties angelConfig;
    private TokenStorageService tokenStorageService;

    public BrokerApiClient(
            @Qualifier("directBrokerWebClient") WebClient directClient,
            @Qualifier("proxyBrokerWebClient") WebClient proxyClient,
            AngelApiProperties angelConfig,
            TokenStorageService tokenStorageService
    ) {
        this.directClient = directClient;
        this.proxyClient = proxyClient;
        this.angelConfig = angelConfig;
        this.tokenStorageService = tokenStorageService;
    }

//--------------- Login and other Basic Methods --------------------------

    /*public Mono<LoginResponse> loginWithTotp(String clientCode, String mpin) {

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
//                    .doOnSuccess(resp -> {
//                        log.info("LoginWithTotp Response: {}", resp);
//                        if (resp != null && resp.getData() != null) {
//                            tokenStorageService.storeTokens(resp.getData());
//                            log.info("Tokens stored successfully and written to file for client={}", clientCode);
//                        }
//                    })
                    .doOnError(err -> log.error("LoginWithTotp failed: {}", err.getMessage(), err));

        } catch (Exception e) {
            log.error("Error generating TOTP: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Failed to generate TOTP", e));
        }
    }*/

    public Mono<LoginResponse> login(LoginRequest request) {

        log.info("Calling Angel login API for client={}", request.getClientcode());

//        return brokerWebClient.post()
        return
                directClient.post()
//                proxyClient.post()
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
                .doOnError(err ->
                        log.error("Angel login API failed | error: {}", err.getMessage())
                );
    }

    public Mono<LoginResponse> refreshTokens(String refreshToken, String authToken) {

        log.info("Calling Angel token refresh API");

//        return brokerWebClient.post()
        return
                directClient.post()
//                proxyClient.post()
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
                .bodyValue(Map.of("refreshToken", refreshToken))
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .doOnError(err ->
                        log.error("Angel token refresh API failed | error: {}", err.getMessage())
                );
    }

    public Mono<LoginResponse> generateTokens(String refreshToken, String authToken) {

        log.info("Generating new JWT tokens for refreshToken={}", refreshToken);

//        return brokerWebClient.post()
        return
                directClient.post()
//                proxyClient.post()
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
                .doOnError(err -> log.error("Error generating JWT Tokens: {}", err.getMessage()));
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

    public Mono<LoginResponse> logout(String clientCode, String authToken) {

        log.info("Logging out client={}", clientCode);

//        return brokerWebClient.post()
        return
                directClient.post()
//                proxyClient.post()
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
                .doOnError(err -> log.error("Logout failed: {}", err.getMessage()));
    }


//----------------- Startup and status related methods ------------------------

    public Mono<List<Map<String, Object>>> downloadScripMaster(String accessToken) {

        log.info("Starting ScripMaster download request...");

//        return brokerWebClient.get()
        return
                directClient.get()
//                proxyClient.get()
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

                    log.info("Received HTTP status: {}", response.statusCode());

                    if (response.statusCode().is2xxSuccessful()) {
                        log.info("ScripMaster request successful. Parsing JSON...");
                        return response.bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {});
                    } else {
                        log.error("ScripMaster download failed! HTTP Status: {}",
                                response.statusCode());
                        return response.createException().flatMapMany(Flux::error);
                    }
                })
                .collectList()
                .doOnSuccess(list -> {
                    log.info("Successfully downloaded ScripMaster.");
                    log.info("Total entries received: {}", list.size());
                })
                .doOnError(err -> {
                    log.error("Error while downloading ScripMaster: {}", err.getMessage());
                });
    }

    public Flux<Map<String, Object>> downloadScripMasterStream(String accessToken) {

        log.info("Starting ScripMaster STREAM download request...");

//        return brokerWebClient.get()
        return
                directClient.get()
//                proxyClient.get()
                .uri("https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json")
                .headers(headers -> {
                    headers.add("Authorization", "Bearer " + accessToken);
                    headers.add("Accept", "application/json");
                    headers.add("X-UserType", "USER");
                    headers.add("X-SourceID", "WEB");
                    headers.add("X-ClientLocalIP", "127.0.0.1");
                    headers.add("X-ClientPublicIP", "127.0.0.1");
                    headers.add("X-MACAddress", "aa:bb:cc:dd:ee:ff");
                    headers.add("X-PrivateKey", angelConfig.getPrivateKey());
                })
                .exchangeToFlux(response -> {

                    log.info("Received HTTP status: {}", response.statusCode());

                    if (response.statusCode().is2xxSuccessful()) {
                        log.info("ScripMaster STREAM parsing started...");
                        return response.bodyToFlux(
                                new ParameterizedTypeReference<Map<String, Object>>() {}
                        );
                    } else {
                        log.error("ScripMaster download failed! HTTP Status: {}",
                                response.statusCode());
                        return response.createException().flatMapMany(Flux::error);
                    }
                })
                .doOnComplete(() -> log.info("ScripMaster STREAM completed"))
                .doOnError(err ->
                        log.error("Error during ScripMaster STREAM | error: {}", err.getMessage())
                );
    }


   /* public Mono<RmsResponse> fetchRmsBalance(String authToken) {

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
                .doOnSuccess(resp -> log.info("RMS Response: {}", resp))
                .doOnError(err -> log.error("Error fetching RMS: {}", err.getMessage()));
    }*/

    public Mono<RmsResponse> fetchRmsBalance(String authToken) {

        return directClient.get()  // RMS needs GET

//        return
//                directClient.mutate() // creates a copy of this client
//                .filter((request, next) -> {
//                    log.info("➡️ Request: {} {}", request.method(), request.url());
//
//                    request.headers().forEach((name, values) ->
//                            values.forEach(value -> log.info("➡️ Header: {}={}", name, value))
//                    );
//
//                    return next.exchange(request);
//                })
//                .filter((request, next) ->
//                        next.exchange(request).doOnNext(response -> {
//                            log.info("⬅️ Response Status: {}", response.statusCode());
//
//                            response.headers().asHttpHeaders().forEach((name, values) ->
//                                    values.forEach(value -> log.info("⬅️ Header: {}={}", name, value))
//                            );
//                        })
//                )
//                .build().get()

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
                .onStatus(
                        status -> status.value() == 401 || status.value() == 403,
                        resp -> Mono.error(new AuthExpiredException("JWT expired"))
                )
                .bodyToMono(RmsResponse.class)
                .doOnSuccess(resp -> log.info("RMS Response: {}", resp))
                .doOnError(err -> log.error("Error fetching RMS: {}", err.getMessage()));
    }


    public Mono<OrderBookResponse_v2> getOrderBook(String token) {
//        return brokerWebClient.get()
        return
                directClient.get()
//                proxyClient.get()
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
                .bodyToMono(OrderBookResponse_v2.class);
    }

    public Mono<TradeBookResponse> getTradeBook(String token) {
//        return brokerWebClient.get()
        return
                directClient.get()
//                proxyClient.get()
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

    public Mono<TradeBookResponse_v1> getTradeBook_v1(String token) {
//        return brokerWebClient.get()
        return
                directClient.get()
//                proxyClient.get()
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
                .bodyToMono(TradeBookResponse_v1.class);
    }

    public Mono<JsonNode> getIndividualOrderStatus(String orderId, String authToken) {

        log.info("Fetching order status for orderId={}", orderId);

        ObjectMapper objectMapper = new ObjectMapper();

//        return brokerWebClient.get()
        return
                directClient.get()
//                proxyClient.get()
                .uri("/rest/secure/angelbroking/order/v1/details/{orderId}", orderId)
                .header("Authorization", "Bearer " + authToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .exchangeToMono(response -> {

                    String contentType = response.headers()
                            .contentType()
                            .map(MediaType::toString)
                            .orElse("UNKNOWN");

                    log.info("AngelOne Content-Type: {}", contentType);

                    return response.bodyToMono(String.class)
                            .flatMap(body -> {

                                log.debug("RAW ANGELONE RESPONSE:\n{}", body);

                                // ❌ HTML response (session expired, WAF, Cloudflare, etc.)
                                if (contentType.contains(MediaType.TEXT_HTML_VALUE)) {
                                    return Mono.error(
                                            new RuntimeException(
                                                    "AngelOne returned HTML instead of JSON. Possible auth/session issue."
                                            )
                                    );
                                }

                                // ✅ JSON → parse to JsonNode
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(body);
                                    return Mono.just(jsonNode);
                                } catch (Exception e) {
                                    return Mono.error(
                                            new RuntimeException("Failed to parse AngelOne JSON response", e)
                                    );
                                }
                            });
                });
    }


//------------------------- Order related methods -----------------------------

//    public Mono<OrderResponse> placeOrder(BracketOrderRequest orderRequest, String authToken) {
//
//        log.info("Placing order: {}", orderRequest);
//
//        return brokerWebClient.post()
//                .uri("/rest/secure/angelbroking/order/v1/placeOrder")
//                .header("Authorization", "Bearer " + authToken)
//                .header("Content-Type", "application/json")
//                .header("Accept", "application/json")
//                .header("X-UserType", angelConfig.getUserType())
//                .header("X-SourceID", angelConfig.getSourceId())
//                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
//                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
//                .header("X-MACAddress", angelConfig.getClientMacAddress())
//                .header("X-PrivateKey", angelConfig.getPrivateKey())
//                .bodyValue(orderRequest)
//                .retrieve()
//                .bodyToMono(OrderResponse.class)
//                .doOnSuccess(resp -> log.info("Order Response: {}", resp))
//                .doOnError(err -> log.error("Error placing order: {}", err.getMessage(), err));
//    }

//    public Mono<OrderResponse> chartinkPlaceOrder(IOrderRequest orderRequest, String authToken) {
//
////        log.info("Placing order: {}", orderRequest);
//        log.info("Placing order Request Body JSON: {}", new ObjectMapper().writeValueAsString(orderRequest));
//
////        return brokerWebClient.post()
//        return proxyClient.post()
//                .uri("/rest/secure/angelbroking/order/v1/placeOrder")
//                .header("Authorization", "Bearer " + authToken)
//                .header("Content-Type", "application/json")
//                .header("Accept", "application/json")
//                .header("X-UserType", angelConfig.getUserType())
//                .header("X-SourceID", angelConfig.getSourceId())
//                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
//                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
//                .header("X-MACAddress", angelConfig.getClientMacAddress())
//                .header("X-PrivateKey", angelConfig.getPrivateKey())
//                .bodyValue(orderRequest)
////                .retrieve()
////                .bodyToMono(OrderResponse.class)
////                .doOnSuccess(resp -> log.info("Order Response: {}", resp))
////                .doOnError(err -> log.error("Error placing order: {}", err.getMessage(), err));
//
//                .exchangeToMono(response ->
//                        response.bodyToMono(String.class)
//                                .doOnNext(body -> log.info("Raw Response: {}", body))
//                                .flatMap(body -> {
//                                    try {
//                                        return Mono.just(new ObjectMapper().readValue(body, OrderResponse.class));
//                                    } catch (Exception e) {
//                                        log.error("Parsing failed. Raw response: {}", body, e);
//                                        return Mono.error(e);
//                                    }
//                                })
//                );
//    }

    public Mono<OrderResponse> chartinkPlaceOrder(IOrderRequest orderRequest, String authToken) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            String requestJson = mapper.writeValueAsString(orderRequest);
            log.info("Placing order Request Body JSON: {}", requestJson);
        } catch (Exception e) {
            log.warn("Failed to serialize request body", e);
        }

        return proxyClient.post()
                .uri("/rest/secure/angelbroking/order/v1/placeOrder")
                .headers(headers -> {
                    headers.set("Authorization", "Bearer " + authToken);
                    headers.set("Content-Type", "application/json");
                    headers.set("Accept", "application/json");
                    headers.set("X-UserType", angelConfig.getUserType());
                    headers.set("X-SourceID", angelConfig.getSourceId());
                    headers.set("X-ClientLocalIP", angelConfig.getClientLocalIp());
                    headers.set("X-ClientPublicIP", angelConfig.getClientPublicIp());
                    headers.set("X-MACAddress", angelConfig.getClientMacAddress());
                    headers.set("X-PrivateKey", angelConfig.getPrivateKey());

                    // 🔥 Log request headers (mask sensitive ones)
                    log.info("Request Headers: Authorization=Bearer ****, X-PrivateKey=****, Others={}",
                            headers);
                })
                .bodyValue(orderRequest)
                .exchangeToMono(response -> {

                    // 🔥 Log response status
                    log.info("Response Status: {}", response.statusCode());

                    // 🔥 Log response headers
                    log.info("Response Headers: {}", response.headers().asHttpHeaders());

                    return response.bodyToMono(String.class)
                            .doOnNext(body -> log.info("Raw Response Body: {}", body))
                            .flatMap(body -> {
                                try {
                                    OrderResponse resp = mapper.readValue(body, OrderResponse.class);
                                    return Mono.just(resp);
                                } catch (Exception e) {
                                    log.error("Parsing failed. Raw response: {}", body, e);
                                    return Mono.error(e);
                                }
                            });
                })
                .doOnError(err -> log.error("Error placing order: {}", err.getMessage(), err));
    }

//    public Mono<OrderResponse> chartinkModifyOrder(IOrderRequest orderRequest, String authToken) {
//
////        log.info("Modifying order: {}", orderRequest);
//        log.info("Modifying order Request Body JSON: {}", new ObjectMapper().writeValueAsString(orderRequest));
//
////        return brokerWebClient.post()
//        return proxyClient.post()
//                .uri("/rest/secure/angelbroking/order/v1/modifyOrder")
//                .header("Authorization", "Bearer " + authToken)
//                .header("Content-Type", "application/json")
//                .header("Accept", "application/json")
//                .header("X-UserType", angelConfig.getUserType())
//                .header("X-SourceID", angelConfig.getSourceId())
//                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
//                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
//                .header("X-MACAddress", angelConfig.getClientMacAddress())
//                .header("X-PrivateKey", angelConfig.getPrivateKey())
//                .bodyValue(orderRequest)
////                .retrieve()
////                .bodyToMono(OrderResponse.class)
////                .doOnSuccess(resp -> log.info("Order Response: {}", resp))
////                .doOnError(err -> log.error("Error Modifying order: {}", err.getMessage(), err));
//                .exchangeToMono(response ->
//                        response.bodyToMono(String.class)
//                                .doOnNext(body -> log.info("Raw Response: {}", body))
//                                .flatMap(body -> {
//                                    try {
//                                        return Mono.just(new ObjectMapper().readValue(body, OrderResponse.class));
//                                    } catch (Exception e) {
//                                        log.error("Parsing failed. Raw response: {}", body, e);
//                                        return Mono.error(e);
//                                    }
//                                })
//                );
//    }
//
//    public Mono<OrderResponse> chartinkCancelOrder(IOrderRequest cancelOrderRequest, String authToken) {
//
////        log.info("Cancelling order: {}", cancelOrderRequest);
//        log.info("Cancelling order Request Body JSON: {}", new ObjectMapper().writeValueAsString(cancelOrderRequest));
//
////        return brokerWebClient.post()
//        return proxyClient.post()
//                .uri("/rest/secure/angelbroking/order/v1/cancelOrder")
//                .header("Authorization", "Bearer " + authToken)
//                .header("Content-Type", "application/json")
//                .header("Accept", "application/json")
//                .header("X-UserType", angelConfig.getUserType())
//                .header("X-SourceID", angelConfig.getSourceId())
//                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
//                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
//                .header("X-MACAddress", angelConfig.getClientMacAddress())
//                .header("X-PrivateKey", angelConfig.getPrivateKey())
//                .bodyValue(cancelOrderRequest)
////                .retrieve()
////                .bodyToMono(OrderResponse.class)
////                .doOnSuccess(resp -> log.info("Cancel Order Response: {}", resp))
////                .doOnError(err -> log.error("Error cancelling order: {}", err.getMessage(), err));
//                .exchangeToMono(response ->
//                        response.bodyToMono(String.class)
//                                .doOnNext(body -> log.info("Raw Response: {}", body))
//                                .flatMap(body -> {
//                                    try {
//                                        return Mono.just(new ObjectMapper().readValue(body, OrderResponse.class));
//                                    } catch (Exception e) {
//                                        log.error("Parsing failed. Raw response: {}", body, e);
//                                        return Mono.error(e);
//                                    }
//                                })
//                );
//    }

    public Mono<OrderResponse> chartinkModifyOrder(IOrderRequest orderRequest, String authToken) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            String requestJson = mapper.writeValueAsString(orderRequest);
            log.info("Modifying order Request Body JSON: {}", requestJson);
        } catch (Exception e) {
            log.warn("Failed to serialize modify request", e);
        }

        return proxyClient.post()
                .uri("/rest/secure/angelbroking/order/v1/modifyOrder")
                .headers(headers -> {
                    headers.set("Authorization", "Bearer " + authToken);
                    headers.set("Content-Type", "application/json");
                    headers.set("Accept", "application/json");
                    headers.set("X-UserType", angelConfig.getUserType());
                    headers.set("X-SourceID", angelConfig.getSourceId());
                    headers.set("X-ClientLocalIP", angelConfig.getClientLocalIp());
                    headers.set("X-ClientPublicIP", angelConfig.getClientPublicIp());
                    headers.set("X-MACAddress", angelConfig.getClientMacAddress());
                    headers.set("X-PrivateKey", angelConfig.getPrivateKey());

                    // Mask sensitive headers
                    HttpHeaders safeHeaders = new HttpHeaders();
                    safeHeaders.putAll(headers);
                    safeHeaders.set("Authorization", "Bearer ****");
                    safeHeaders.set("X-PrivateKey", "****");

                    log.info("Modify Request Headers: {}", safeHeaders);
                })
                .bodyValue(orderRequest)
                .exchangeToMono(response -> {

                    log.info("Modify Response Status: {}", response.statusCode());
                    log.info("Modify Response Headers: {}", response.headers().asHttpHeaders());

                    return response.bodyToMono(String.class)
                            .doOnNext(body -> log.info("Modify Raw Response Body: {}", body))
                            .flatMap(body -> {
                                try {
                                    return Mono.just(mapper.readValue(body, OrderResponse.class));
                                } catch (Exception e) {
                                    log.error("Modify parsing failed. Raw response: {}", body, e);
                                    return Mono.error(e);
                                }
                            });
                })
                .doOnError(err -> log.error("Error modifying order: {}", err.getMessage(), err));
    }

    public Mono<OrderResponse> chartinkCancelOrder(IOrderRequest cancelOrderRequest, String authToken) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            String requestJson = mapper.writeValueAsString(cancelOrderRequest);
            log.info("Cancelling order Request Body JSON: {}", requestJson);
        } catch (Exception e) {
            log.warn("Failed to serialize cancel request", e);
        }

        return proxyClient.post()
                .uri("/rest/secure/angelbroking/order/v1/cancelOrder")
                .headers(headers -> {
                    headers.set("Authorization", "Bearer " + authToken);
                    headers.set("Content-Type", "application/json");
                    headers.set("Accept", "application/json");
                    headers.set("X-UserType", angelConfig.getUserType());
                    headers.set("X-SourceID", angelConfig.getSourceId());
                    headers.set("X-ClientLocalIP", angelConfig.getClientLocalIp());
                    headers.set("X-ClientPublicIP", angelConfig.getClientPublicIp());
                    headers.set("X-MACAddress", angelConfig.getClientMacAddress());
                    headers.set("X-PrivateKey", angelConfig.getPrivateKey());

                    // Mask sensitive headers
                    HttpHeaders safeHeaders = new HttpHeaders();
                    safeHeaders.putAll(headers);
                    safeHeaders.set("Authorization", "Bearer ****");
                    safeHeaders.set("X-PrivateKey", "****");

                    log.info("Cancel Request Headers: {}", safeHeaders);
                })
                .bodyValue(cancelOrderRequest)
                .exchangeToMono(response -> {

                    log.info("Cancel Response Status: {} | Cancel Response Headers: {}", response.statusCode(), response.headers().asHttpHeaders());

                    return response.bodyToMono(String.class)
                            .doOnNext(body -> log.info("Cancel Raw Response Body: {}", body))
                            .flatMap(body -> {
                                try {
                                    return Mono.just(mapper.readValue(body, OrderResponse.class));
                                } catch (Exception e) {
                                    log.error("Cancel parsing failed. Raw response: {}", body, e);
                                    return Mono.error(e);
                                }
                            });
                })
                .doOnError(err -> log.error("Error cancelling order: {}", err.getMessage(), err));
    }

    public Mono<Map<String, Object>> fetchNseIntradayLeverage(String authToken) {

        log.info("Fetching NSE Intraday leverage");

//        return brokerWebClient.get()
        return
                directClient.get()
//                proxyClient.get()
                .uri("/rest/secure/angelbroking/marketData/v1/nseIntraday")
                .header("Authorization", "Bearer " + authToken)
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnSuccess(resp ->
                        log.info("NSE Intraday leverage fetched successfully")
                )
                .doOnError(err ->
                        log.error("Failed to fetch NSE Intraday leverage", err)
                );
    }

    public Mono<BigDecimal> getLtp(String exchange, String token, String authToken, String mode) {

        Map<String, Object> body = Map.of(
                "mode", mode,
                "exchangeTokens", Map.of(exchange, List.of(token))
        );

//        return brokerWebClient.post()
        return
                directClient.post()
//                proxyClient.post()
                .uri("/rest/secure/angelbroking/market/v1/quote/")
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json ->
                        new BigDecimal(
                                json.at("/data/fetched/0/ltp").asText()
                        )
                );
    }

    public Mono<MarketQuote> getQuote(
            String exchange,
            String symbolToken,
            String authToken,
            String mode
    ) {

        Map<String, Object> body = Map.of(
                "mode", mode,
                "exchangeTokens", Map.of(exchange, List.of(symbolToken))
        );

//        return brokerWebClient.post()
        return
                directClient.post()
//                proxyClient.post()
                .uri("/rest/secure/angelbroking/market/v1/quote/")
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {

                    JsonNode fetched = json.at("/data/fetched/0");

                    BigDecimal ltp = fetched.get("ltp").decimalValue();

                    BigDecimal bestBid = fetched
                            .at("/depth/buy/0/price")
                            .decimalValue();

                    BigDecimal bestAsk = fetched
                            .at("/depth/sell/0/price")
                            .decimalValue();

                    return new MarketQuote(ltp, bestBid, bestAsk);
                });
    }

    public Mono<HistoricalDataResponse> getHistoricalCandleData(
            JsonObject request, String authToken
    ) {

        log.info("Calling Historical Candle API request: {}", request);

        return directClient.post()
                .uri("/rest/secure/angelbroking/historical/v1/getCandleData")
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-UserType", angelConfig.getUserType())
                .header("X-SourceID", angelConfig.getSourceId())
                .header("X-ClientLocalIP", angelConfig.getClientLocalIp())
                .header("X-ClientPublicIP", angelConfig.getClientPublicIp())
                .header("X-MACAddress", angelConfig.getClientMacAddress())
                .header("X-PrivateKey", angelConfig.getPrivateKey())
                .bodyValue(request.toString()) // ✅ IMPORTANT: don't use toString() if it's a POJO
//                .exchangeToMono(response -> {
//
//                    // ✅ Log status + headers
//                    log.info("Status Code: {}", response.statusCode());
//                    log.info("Response Headers: {}", response.headers().asHttpHeaders());
//
//                    return response.bodyToMono(String.class)
//                            .flatMap(body -> {
//                                // ✅ Log raw response
//                                log.info("RAW RESPONSE: {}", body);
//
//                                try {
//                                    ObjectMapper mapper = new ObjectMapper();
//                                    HistoricalDataResponse parsed =
//                                            mapper.readValue(body, HistoricalDataResponse.class);
//
//                                    return Mono.just(parsed);
//
//                                } catch (Exception e) {
//                                    log.error("Failed to parse response. Body was: {}", body, e);
//                                    return Mono.error(new RuntimeException("Invalid JSON response"));
//                                }
//                            });
//                })
//                .doOnError(err ->
//                        log.error("Historical API failed for request={}", request, err)
//                );
                .retrieve()
                .bodyToMono(HistoricalDataResponse.class)
                .doOnSuccess(resp -> log.info("Order Response: {}", resp))
                .doOnError(err -> log.error("Error placing order: {}", err.getMessage()));
    }

//    private Mono<OrderResponse> handleResponse(ClientResponse response) {
//        return response.bodyToMono(String.class)
//                .doOnNext(body -> log.info("Raw Response: {}", body))
//                .flatMap(body -> {
//                    try {
//                        ObjectMapper mapper = new ObjectMapper();
//
//                        // Optional: handle "" → null globally
//                        mapper.coercionConfigFor(LogicalType.POJO)
//                                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
//
//                        OrderResponse resp = mapper.readValue(body, OrderResponse.class);
//                        return Mono.just(resp);
//
//                    } catch (Exception e) {
//                        log.error("Parsing failed. Raw response: {}", body, e);
//                        return Mono.error(e);
//                    }
//                });
//    }
}