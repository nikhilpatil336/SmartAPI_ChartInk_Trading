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
        return response.getOrderStatusData().getTransactiontype().equals("SELL")
                && ctx.getSellOrderId().equals(
                response.getOrderStatusData().getOrderid()
        );
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        if (response.getOrderStatusData().getVariety().equalsIgnoreCase("STOPLOSS")) {
            orderRegistry.getBySlId(response.getOrderStatusData().getOrderid())
                    .ifPresent(x -> {
                                String jwtToken =
                                        tokenManager.getValidJwtToken();

                                String sellOrderID = ctx.getSellOrderId();
                                String sellVariety = ctx.getSellVariety();

                                Mono<OrderResponse> sellMono =
                                        executionService
                                                .placeCancelOrder(
                                                        sellOrderID,
                                                        sellVariety,
                                                        jwtToken
                                                )
                                                .retry(3);
                            }
                    );
        } else if (response.getOrderStatusData().getVariety().equalsIgnoreCase("SELL")) {
            orderRegistry.getBySlId(response.getOrderStatusData().getOrderid())
                    .ifPresent(x -> {
                                String jwtToken =
                                        tokenManager.getValidJwtToken();

                                String stopLossOrderID = ctx.getStopLossOrderId();
                                String stopLossVariety = ctx.getStopLossVariety();

                                Mono<OrderResponse> sellMono =
                                        executionService
                                                .placeCancelOrder(
                                                        stopLossOrderID,
                                                        stopLossVariety,
                                                        jwtToken
                                                )
                                                .retry(3);
                            }
                    );
        }
    }
}

