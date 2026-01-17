package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.model.OrderResult;
import com.onepercentgrowth.local_to_smartapi.model.OrderStatusItem;
import com.onepercentgrowth.local_to_smartapi.model.OrderBookResponse_v2;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class OrderPollingService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderPollingService.class);
    private final BrokerApiClient brokerApiClient;
    private final ApplicationProperties props;

    public OrderPollingService(
            BrokerApiClient brokerApiClient,
            ApplicationProperties props
    ) {
        this.brokerApiClient = brokerApiClient;
        this.props = props;
    }

    public Mono<Double> waitUntilCompleted(String orderId, String jwtToken) {
//        return Flux.interval(
//                        Duration.ZERO,
//                        Duration.ofMillis(props.getOrderBookRetryMilliseconds())
//                )
//                .flatMap(tick -> getOrder(orderId, jwtToken))
////                .flatMap(tick ->
////                        getOrderById(orderId, jwtToken)
////                                .map(this::mapToResult)        // OrderStatusItem → OrderResult
////                                .defaultIfEmpty(OrderResult.notFound())
////                )
//                .filter(OrderResult::isTerminal)
//                .map(OrderResult::getResult)
//                .next();

        return getOrderSafe(orderId, jwtToken)
                .flatMap(result -> {

                    if (result.isTerminal() && result.getResult() == -1) {
                        return Mono.error(
                                new IllegalStateException("Order rejected: " + orderId)
                        );
                    }

                    if (result.isTerminal()) {
                        return Mono.just(result.getResult());
                    }

                    // ⏳ Pending → WAIT before next attempt
                    return Mono.delay(Duration.ofSeconds(4))
                            .then(waitUntilCompleted(orderId, jwtToken));
                });
    }

    private Mono<OrderResult> getOrderSafe(
            String orderId,
            String jwtToken
    ) {
        return brokerApiClient.getOrderBook(jwtToken)
                .onErrorResume(
                        WebClientResponseException.Forbidden.class,
                        ex -> {
                            log.warn("OrderBook 403 → backing off");
                            return Mono.empty(); // treat as pending
                        }
                )
                .map(OrderBookResponse_v2::getData)
                .flatMapMany(Flux::fromIterable)
                .filter(o -> orderId.equals(o.getOrderid()))
                .map(this::mapToResult)
                .defaultIfEmpty(OrderResult.pending())
                .next();
    }


    private Mono<OrderResult> getOrder(String orderId, String jwtToken) {
        return brokerApiClient.getOrderBook(jwtToken)
                .map(OrderBookResponse_v2::getData)
                .flatMapMany(Flux::fromIterable)
                .filter(o -> orderId.equals(o.getOrderid()))
                .map(this::mapToResult)
                .next();
    }

    private OrderResult mapToResult(OrderStatusItem order) {
//        switch (order.getStatus().toUpperCase()) {
//            case "COMPLETE":
//            case "FILLED":
//                return OrderResult.completed(
//                        Double.parseDouble(order.getPrice())
//                );
//
//            case "REJECTED":
//            case "CANCELLED":
//                return OrderResult.rejected();
//
//            case "OPEN":
//            case "PENDING":
//            case "TRIGGER PENDING":
//            case "PARTIALLY FILLED":
//            case "MODIFIED":
//            case "OPEN PENDING":
//                return OrderResult.pending();
//
//            default:
//                return OrderResult.pending();
//        }
        String status =
                (order.getStatus() != null
                        ? order.getStatus()
                        : order.getOrderstatus())
                        .toUpperCase()
                        .trim();

        // Normalize Angel One weird statuses
        if (status.contains("OPEN") || status.contains("PENDING") || status.equals("AB01")) {
            return OrderResult.pending();
        }

        if (status.contains("COMPLETE") || status.contains("FILLED")) {
            return OrderResult.completed(
                    Double.parseDouble(order.getPrice())
            );
        }

        if (status.contains("REJECTED") || status.contains("CANCELLED")) {
            return OrderResult.rejected();
        }

        return OrderResult.pending();
    }

    private Mono<OrderStatusItem> getOrderById(String orderId, String jwtToken) {
        return brokerApiClient.getOrderBook(jwtToken)
                .map(OrderBookResponse_v2::getData)
                .flatMapMany(Flux::fromIterable)
                .filter(item -> orderId.equals(item.getOrderid()))
                .next();
    }
}

