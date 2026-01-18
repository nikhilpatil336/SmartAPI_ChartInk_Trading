package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.client.BrokerApiClient;
import com.onepercentgrowth.local_to_smartapi.factory.OrderRequestFactory;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.model.chartink_request.IOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderExecutionService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderExecutionService.class);

    private final BrokerApiClient brokerApiClient;
    private final OrderRequestFactory orderRequestFactory;

    public OrderExecutionService(
            BrokerApiClient brokerApiClient,
            OrderRequestFactory orderRequestFactory
    ) {
        this.brokerApiClient = brokerApiClient;
        this.orderRequestFactory = orderRequestFactory;
    }

    // ----------------------------------------------------
    // BUY ORDER
    // ----------------------------------------------------

    public Mono<OrderResponse> placeBuyOrder(
            String stockName,
            String symbolToken,
            int quantity,
            String price,
            String jwtToken
    ) {
        IOrderRequest buyRequest =
                orderRequestFactory.createBuyOrder(
                        stockName,
                        symbolToken,
                        quantity,
                        price
                );

        log.info("Placing BUY order for {} qty={} price={}",
                stockName, quantity, price);

        return brokerApiClient
                .chartinkPlaceOrder(buyRequest, jwtToken)
                .flatMap(this::validateOrderResponse);
    }

    public Mono<OrderResponse> modifyBuyOrder(
            String stockName,
            String symbolToken,
            int quantity,
            String price,
            String orderId,
            String jwtToken
    ) {
        IOrderRequest request =
                orderRequestFactory.modifyBuyOrder(
                        stockName, symbolToken, quantity, price, orderId
                );

        log.info("Modifying BUY order {}", request);

        return brokerApiClient
                .chartinkModifyOrder(request, jwtToken)
                .flatMap(this::validateOrderResponse);
    }

    // ----------------------------------------------------
    // SELL ORDER
    // ----------------------------------------------------

    public Mono<OrderResponse> placeSellOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double price,
            String jwtToken
    ) {
        IOrderRequest sellRequest =
                orderRequestFactory.createSellLimitOrder(
                        stockName,
                        symbolToken,
                        quantity,
                        price
                );

        log.info("Placing SELL order: {}", sellRequest.toString());

        return brokerApiClient
                .chartinkPlaceOrder(sellRequest, jwtToken)
                .flatMap(this::validateOrderResponse);
    }

    public Mono<OrderResponse> modifySellOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double limitPrice,
            String orderId,
            String jwtToken
    ) {
        IOrderRequest request =
                orderRequestFactory.modifySellLimitOrder(
                        stockName, symbolToken, quantity, 0, orderId, limitPrice
                );

        log.info("Modifying SELL order {}", request);

        return brokerApiClient
                .chartinkModifyOrder(request, jwtToken)
                .flatMap(this::validateOrderResponse);
    }

    // ----------------------------------------------------
    // STOP LOSS ORDER
    // ----------------------------------------------------

    public Mono<OrderResponse> placeStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double triggerPrice,
            double limitPrice,
            String jwtToken
    ) {
        IOrderRequest slRequest =
                orderRequestFactory.createStopLossLimitOrder(
                        stockName,
                        symbolToken,
                        quantity,
                        triggerPrice,
                        limitPrice
                );

        log.info("Placing STOP LOSS order: {}", slRequest.toString());

        return brokerApiClient
                .chartinkPlaceOrder(slRequest, jwtToken)
                .flatMap(this::validateOrderResponse);
    }

    public Mono<OrderResponse> modifyStopLossOrder(
            String stockName,
            String symbolToken,
            int quantity,
            double newTriggerPrice,
            double newLimitPrice,
            String orderId,
            String jwtToken
    ) {
        IOrderRequest request =
                orderRequestFactory.modifyLimitStopLossOrder(
                        stockName,
                        symbolToken,
                        quantity,
                        newTriggerPrice,
                        orderId,
                        newLimitPrice
                );

        log.info("Modifying STOP LOSS order {}", request);

        return brokerApiClient
                .chartinkModifyOrder(request, jwtToken)
                .flatMap(this::validateOrderResponse);
    }

    // ----------------------------------------------------
    // CANCEL
    // ----------------------------------------------------

    public Mono<OrderResponse> placeCancelOrder(
            String orderId,
            String variety,
            String jwtToken
    ) {
        IOrderRequest request =
                orderRequestFactory.createCancelOrder(orderId, variety);

        log.info("Placing CANCEL order {}", request);

        return brokerApiClient.chartinkCancelOrder(request, jwtToken);
    }

    // ----------------------------------------------------
    // RESPONSE VALIDATION
    // ----------------------------------------------------

    private Mono<OrderResponse> validateOrderResponse(OrderResponse response) {

        if (response == null) {
            return Mono.error(
                    new IllegalStateException("Broker returned null response")
            );
        }

        if (!response.isStatus()) {
            String message =
                    response.getMessage() != null
                            ? response.getMessage()
                            : "Unknown broker error";

            log.error("Order failed: {}", message);

            return Mono.error(
                    new IllegalStateException("Order failed: " + message)
            );
        }

        if (response.getData() == null ||
                response.getData().getOrderid() == null) {

            return Mono.error(
                    new IllegalStateException("Order placed but orderId missing")
            );
        }

        log.info("Order placed successfully. orderId={}",
                response.getData().getOrderid());

        return Mono.just(response);
    }

    public Mono<OrderResponse> executeBracketFlow(
            String stockName,
            String symbolToken,
            int quantity,
            String price,
            String jwtToken
    ) {

        return brokerApiClient
                .chartinkPlaceOrder(
                        orderRequestFactory.createBuyOrder(
                                stockName,
                                symbolToken,
                                quantity,
                                price
                        ),
                        jwtToken
                );
    }


    // ----------------------------------------------------
    // MODIFY STOP LOSS
    // ----------------------------------------------------

//    public Mono<OrderResponse> modifyStopLossOrder(
//            String stockName,
//            String symbolToken,
//            int quantity,
//            double newTriggerPrice,
//            String orderId,
//            String jwtToken
//    ) {
//        IOrderRequest modifyRequest =
//                orderRequestFactory.modifyStopLossOrder(
//                        stockName,
//                        symbolToken,
//                        quantity,
//                        newTriggerPrice,
//                        orderId
//                );
//
//        log.info("Modifying STOP LOSS order {} newTrigger={}",
//                orderId, newTriggerPrice);
//
//        return brokerApiClient
//                .chartinkModifyOrder(modifyRequest, jwtToken)
//                .flatMap(this::validateOrderResponse);
//    }

//    public Mono<OrderResponse> modifyLimitStopLossOrder(
//            String stockName,
//            String symbolToken,
//            int quantity,
//            double newTriggerPrice,
//            String orderId,
//            String jwtToken
//    ) {
//        IOrderRequest modifyRequest =
//                orderRequestFactory.modifyLimitStopLossOrder(
//                        stockName,
//                        symbolToken,
//                        quantity,
//                        newTriggerPrice,
//                        orderId
//                );
//
//        log.info("Modifying STOP LOSS order {} newTrigger={}",
//                orderId, newTriggerPrice);
//
//        return brokerApiClient
//                .chartinkModifyOrder(modifyRequest, jwtToken)
//                .flatMap(this::validateOrderResponse);
//    }

    // ----------------------------------------------------
    // COMMON RESPONSE VALIDATION
    // ----------------------------------------------------


}
