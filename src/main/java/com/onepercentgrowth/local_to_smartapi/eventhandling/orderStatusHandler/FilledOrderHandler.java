package com.onepercentgrowth.local_to_smartapi.eventhandling.orderStatusHandler;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.orderFillStrategy.OrderFillStrategy;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.OrderCalculationService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.service.OrderService_v2;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

//@Component
//public class FilledOrderHandler implements OrderStatusHandler {
//
//    private static final Logger log = LoggerFactory.getLogger(FilledOrderHandler.class);
//    private final OrderRegistry orderRegistry;
//    private final OrderExecutionService executionService;
//    private final OrderCalculationService calculationService;
//    private final TokenManager tokenManager;
//
//    public FilledOrderHandler(
//            OrderRegistry orderRegistry,
//            OrderExecutionService executionService,
//            OrderCalculationService calculationService,
//            TokenManager tokenManager
//    ) {
//        this.orderRegistry = orderRegistry;
//        this.executionService = executionService;
//        this.calculationService = calculationService;
//        this.tokenManager = tokenManager;
//    }
//
//    @Override
//    public String status() {
//        return "FILLED";
//    }
//
//    @Override
//    public void handle(OrderStatusResponse response) {
//
//        String buyOrderId =
//                response.getOrderStatusData().getOrderid();
//
//        double executedPrice =
//                Double.parseDouble(
//                        response.getOrderStatusData().getPrice()
//                );
//
//        orderRegistry.getByBuyId(buyOrderId)
//                .ifPresent(ctx -> {
//
//                    String jwtToken =
//                            tokenManager.getValidJwtToken();
//
//                    double sellPrice =
//                            calculationService
//                                    .calculateProfitPrice(executedPrice);
//
//                    double slPrice =
//                            calculationService
//                                    .calculateStopLossPrice(executedPrice);
//
//                    Mono<OrderResponse> sellMono =
//                            executionService
//                                    .placeSellOrder(
//                                            ctx.getTradingSymbol(),
//                                            ctx.getSymbolToken(),
//                                            ctx.getQuantity(),
//                                            sellPrice,
//                                            jwtToken
//                                    )
//                                    .retry(3);
//
//                    Mono<OrderResponse> slMono =
//                            executionService
//                                    .placeStopLossOrder(
//                                            ctx.getTradingSymbol(),
//                                            ctx.getSymbolToken(),
//                                            ctx.getQuantity(),
//                                            slPrice,
//                                            jwtToken
//                                    )
//                                    .retry(3);
//
//                    Mono.zip(sellMono, slMono)
//                            .doOnSuccess(tuple -> {
//
//                                ctx.setSellOrderId(
//                                        tuple.getT1().getData().getOrderid()
//                                );
//                                ctx.setStopLossOrderId(
//                                        tuple.getT2().getData().getOrderid()
//                                );
//
//                                log.info(
//                                        "🎯 SELL & SL placed for BUY={}",
//                                        buyOrderId
//                                );
//                            })
//                            .subscribe();
//                });
//    }
//}


@Component
public class FilledOrderHandler implements OrderStatusHandler {

    private static final Logger log = LoggerFactory.getLogger(FilledOrderHandler.class);

    private final OrderRegistry orderRegistry;
    private final List<OrderFillStrategy> strategies;

    public FilledOrderHandler(OrderRegistry orderRegistry, List<OrderFillStrategy> strategies) {
        this.orderRegistry = orderRegistry;
        this.strategies = strategies;
    }

    @Override
    public String status() {
        return "COMPLETE";
    }

    @Override
    public void handle(OrderStatusResponse response) {

        log.info("websocket status response: {} ", response);

        String orderId =
                response.getOrderStatusData().getOrderid();

//        orderRegistry.getByBuyId(orderId)
//                .ifPresent(ctx -> {
//                    log.info("Retrieved object from registry: {}", ctx);
//                    strategies.stream()
//                            .filter(s -> s.supports(ctx, response))
//                            .findFirst()
//                            .ifPresent(s -> s.onFilled(ctx, response));
//                });

        orderRegistry.getByAnyOrderId(orderId)
                .ifPresentOrElse(ctx -> {

                    log.info("Retrieved object from registry: {}", ctx);

                    strategies.stream()
                            .filter(s -> s.supports(ctx, response))
                            .findFirst()
                            .ifPresent(s -> s.onFilled(ctx, response));

                }, () -> log.warn("No OrderContext found for orderId={}", orderId));

    }
}


