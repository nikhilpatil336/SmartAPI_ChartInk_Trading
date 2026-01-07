package com.onepercentgrowth.local_to_smartapi.eventhandling.orderFillStrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.OrderCalculationService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class BuyFilledStrategy implements OrderFillStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyFilledStrategy.class);

    private final OrderRegistry orderRegistry;
    private final OrderExecutionService executionService;
    private final OrderCalculationService calculationService;
    private final TokenManager tokenManager;

    public BuyFilledStrategy(OrderExecutionService orderExecutionService, OrderCalculationService orderCalculationService, TokenManager tokenManager, OrderRegistry orderRegistry) {
        this.executionService = orderExecutionService;
        this.calculationService = orderCalculationService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return response.getOrderStatusData().getTransactiontype().equals("BUY")
                && ctx.getBuyOrderId().equals(
                response.getOrderStatusData().getOrderid()
        );
    }

//    @Override
//    public void onFilled(OrderContext ctx1, OrderStatusResponse response) {
//
////        double executedPrice =
////                Double.parseDouble(response.getOrderStatusData().getPrice());
////
////        String jwtToken = tokenManager.getValidJwtToken();
////
////        double sellPrice =
////                calculationService.calculateProfitPrice(executedPrice);
////        double slPrice =
////                calculationService.calculateStopLossPrice(executedPrice);
////
////        Mono<OrderResponse> sellMono =
////                executionService.placeSellOrder(
////                        ctx.getTradingSymbol(),
////                        ctx.getSymbolToken(),
////                        ctx.getQuantity(),
////                        sellPrice,
////                        jwtToken
////                );
////
////        Mono<OrderResponse> slMono =
////                executionService.placeStopLossOrder(
////                        ctx.getTradingSymbol(),
////                        ctx.getSymbolToken(),
////                        ctx.getQuantity(),
////                        slPrice,
////                        jwtToken
////                );
////
////        Mono.zip(sellMono, slMono)
////                .doOnSuccess(tuple -> {
////                    ctx.setSellOrderId(tuple.getT1().getData().getOrderid());
////                    ctx.setStopLossOrderId(tuple.getT2().getData().getOrderid());
////                })
////                .subscribe();
////    }
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
//                                ctx.setSellVariety("NORMAL");
//                                ctx.setStopLossVariety("STOPLOSS");
//
//                                log.info(
//                                        "SELL and SL order successfully placed, current order context: {}",
//                                        ctx.toString()
//                                );
//                            })
//                            .subscribe();
//                });
//    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        double executedPrice =
                Double.parseDouble(response.getOrderStatusData().getPrice());

        String jwtToken = tokenManager.getValidJwtToken();

        double sellPrice =
                calculationService.calculateProfitPrice(executedPrice);

        double slPrice =
                calculationService.calculateStopLossPrice(executedPrice);

        Mono<OrderResponse> sellMono =
                executionService.placeSellOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
                                ctx.getQuantity(),
                                sellPrice,
                                jwtToken
                        )
                        .retry(3)
                        .doOnSuccess(resp -> {

                            ctx.setSellOrderId(
                                    resp.getData().getOrderid()
                            );
                            ctx.setSellVariety("NORMAL");

                            orderRegistry.registerSell(ctx);

                            log.info("SELL order registered: {}", ctx.getSellOrderId());
                        });

        Mono<OrderResponse> slMono =
                executionService.placeStopLossOrder(
                                ctx.getTradingSymbol(),
                                ctx.getSymbolToken(),
                                ctx.getQuantity(),
                                slPrice,
                                jwtToken
                        )
                        .retry(3)
                        .doOnSuccess(resp -> {

                            ctx.setStopLossOrderId(
                                    resp.getData().getOrderid()
                            );
                            ctx.setStopLossVariety("STOPLOSS");

                            orderRegistry.registerStopLoss(ctx);

                            log.info("SL order registered: {}", ctx.getStopLossOrderId());
                        });

        // Fire both independently
        sellMono.subscribe();
        slMono.subscribe();
    }

}

