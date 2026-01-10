package com.onepercentgrowth.local_to_smartapi.eventhandling.orderFillStrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.BalanceService;
import com.onepercentgrowth.local_to_smartapi.service.OrderCalculationService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class BuyFilledStrategy implements OrderFillStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyFilledStrategy.class);

    private final OrderRegistry orderRegistry;
    private final OrderExecutionService executionService;
    private final OrderCalculationService calculationService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;

    public BuyFilledStrategy(
            OrderExecutionService executionService,
            OrderCalculationService calculationService,
            TokenManager tokenManager,
            OrderRegistry orderRegistry,
            BalanceService balanceService
    ) {
        this.executionService = executionService;
        this.calculationService = calculationService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
        this.balanceService = balanceService;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return response.getOrderStatusData().getTransactiontype().equals("BUY")
                && ctx.getBuyOrderId().equals(
                response.getOrderStatusData().getOrderid()
        );
    }


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

        int quantity = ctx.getQuantity();

        // 🔑 BALANCE UPDATE
        balanceService.onBuy(
                BigDecimal.valueOf(executedPrice),
                quantity,
                balanceService.getCurrentBalance()
        );

//        log.info("Balance updated after BUY: price={}, qty={}",
//                executedPrice, quantity);
    }

}

