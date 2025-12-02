package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.model.LoginRequest;
import com.onepercentgrowth.local_to_smartapi.model.LoginResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LoginService {

    private final BrokerApiClient brokerApiClient;

    private final TokenStorageService tokenStorageService;

    public LoginService(BrokerApiClient brokerApiClient, TokenStorageService tokenStorageService) {
        this.brokerApiClient = brokerApiClient;
        this.tokenStorageService = tokenStorageService;
    }

    public Mono<LoginResponse> loginWithTotp(LoginRequest loginRequest) {
        return brokerApiClient.loginWithTotp(loginRequest.getClientcode(), loginRequest.getPassword());
    }

    public Mono<LoginResponse> refreshTokens(String refreshToken, String authToken) {
        return brokerApiClient.generateTokens(refreshToken, authToken);
    }

    public Mono<LoginResponse> logout(String clientCode, String authToken) {
        return brokerApiClient.logout(clientCode, authToken);
    }
}
