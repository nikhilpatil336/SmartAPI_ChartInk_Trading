package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.model.OrderBookResponse;
import com.onepercentgrowth.local_to_smartapi.model.TradeBookResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderBookService {

    private final BrokerApiClient brokerApiClient;
    private final TokenStorageService tokenStorageService;

    public OrderBookService(BrokerApiClient brokerApiClient, TokenStorageService tokenStorageService) {
        this.brokerApiClient = brokerApiClient;
        this.tokenStorageService = tokenStorageService;
    }

    public Mono<OrderBookResponse> fetchOrderBook() {
        String token = tokenStorageService.getJwtToken();

        if (token == null) {
            return Mono.error(new RuntimeException("No JWT token found. Please log in."));
        }

        return brokerApiClient.getOrderBook(token);
    }

    public Mono<TradeBookResponse> fetchTradeBook() {
        String token = tokenStorageService.getJwtToken();

        if (token == null) {
            return Mono.error(new RuntimeException("No JWT token found. Please log in."));
        }

        return brokerApiClient.getTradeBook(token);
    }
}
