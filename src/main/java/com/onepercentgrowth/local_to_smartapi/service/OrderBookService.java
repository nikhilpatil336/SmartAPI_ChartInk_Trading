package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.model.OrderBookResponse_v2;
import com.onepercentgrowth.local_to_smartapi.model.OrderStatusItem;
import com.onepercentgrowth.local_to_smartapi.model.TradeBookResponse;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
public class OrderBookService {

    private final BrokerApiClient brokerApiClient;
    private final TokenStorageService tokenStorageService;

    public OrderBookService(BrokerApiClient brokerApiClient, TokenStorageService tokenStorageService) {
        this.brokerApiClient = brokerApiClient;
        this.tokenStorageService = tokenStorageService;
    }

    public Mono<List<OrderStatusItem>> fetchOrderBook() {
        String token = tokenStorageService.getJwtToken();

        if (token == null) {
            return Mono.error(new RuntimeException("No JWT token found. Please log in."));
        }

//        return brokerApiClient.getOrderBook(token)
//                .map(OrderBookResponse_v2::getData);

        return brokerApiClient.getOrderBook(token)
                .flatMap(resp -> {

                    // 🚨 Case 2: Broker-side failure
                    if (!resp.isStatus()) {
                        return Mono.error(
                                new RuntimeException(
                                        "Broker order book failed: " + resp.getMessage()
                                )
                        );
                    }

                    // ✅ Case 1: Valid response (possibly empty)
                    List<OrderStatusItem> orders = resp.getData();

                    if (orders == null || orders.isEmpty()) {
                        return Mono.just(Collections.emptyList());
                    }

                    return Mono.just(orders);
                });
    }

    public Mono<TradeBookResponse> fetchTradeBook() {
        String token = tokenStorageService.getJwtToken();

        if (token == null) {
            return Mono.error(new RuntimeException("No JWT token found. Please log in."));
        }

        return brokerApiClient.getTradeBook(token);
    }
}
