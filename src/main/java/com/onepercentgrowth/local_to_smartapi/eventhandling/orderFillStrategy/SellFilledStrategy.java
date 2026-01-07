package com.onepercentgrowth.local_to_smartapi.eventhandling.orderFillStrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SellFilledStrategy implements OrderFillStrategy {

    private static final Logger log = LoggerFactory.getLogger(SellFilledStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;

    public SellFilledStrategy(OrderExecutionService executionService, TokenManager tokenManager, OrderRegistry orderRegistry) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return "SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getSellOrderId());
    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        log.info("order type got hit was: {}", response.getOrderStatusData().getVariety());
//        log.info("Context Object is: {}", ctx.toString());
//
//        if (response.getOrderStatusData().getVariety().equalsIgnoreCase("NORMAL")) {
//            log.info("inside stoploss.");
//            orderRegistry.getBySlId(response.getOrderStatusData().getOrderid())
//                    .ifPresent(x -> {
//                                String jwtToken =
//                                        tokenManager.getValidJwtToken();
//
//                                String sellOrderID = ctx.getSellOrderId();
//                                String sellVariety = ctx.getSellVariety();
//
//                                Mono<OrderResponse> sellMono =
//                                        executionService
//                                                .placeCancelOrder(
//                                                        sellOrderID,
//                                                        sellVariety,
//                                                        jwtToken
//                                                )
//                                                .retry(3);
//                            }
//                    );
//        } else if (response.getOrderStatusData().getVariety().equalsIgnoreCase("SELL")) {
//            log.info("inside sell.");
//            orderRegistry.getBySlId(response.getOrderStatusData().getOrderid())
//                    .ifPresent(x -> {
//                                String jwtToken =
//                                        tokenManager.getValidJwtToken();
//
//                                String stopLossOrderID = ctx.getStopLossOrderId();
//                                String stopLossVariety = ctx.getStopLossVariety();
//
//                                Mono<OrderResponse> sellMono =
//                                        executionService
//                                                .placeCancelOrder(
//                                                        stopLossOrderID,
//                                                        stopLossVariety,
//                                                        jwtToken
//                                                )
//                                                .retry(3);
//                            }
//                    );
//        }
//    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        log.info("SELL filled for {}", ctx.getTradingSymbol());

        String stopLossOrderId = ctx.getStopLossOrderId();
        String stopLossVariety = ctx.getStopLossVariety();

        if (stopLossOrderId == null) {
            log.warn("No SL order to cancel for {}", ctx.getBuyOrderId());
            return;
        }

        String jwtToken = tokenManager.getValidJwtToken();

        executionService
                .placeCancelOrder(stopLossOrderId, stopLossVariety, jwtToken)
                .retry(3)
                .doOnSuccess(resp -> {
                    log.info("STOPLOSS cancelled for buyOrder={}", ctx.getBuyOrderId());
                    orderRegistry.remove(ctx); // trade is complete
                })
                .subscribe();
    }
}

