package com.onepercentgrowth.local_to_smartapi.eventhandling.orderFillStrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.BalanceService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StopLossFilledStrategy implements OrderFillStrategy {

//    private static final Logger log = LoggerFactory.getLogger(StopLossFilledStrategy.class);
//
//    @Override
//    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
//        return response.getOrderStatusData().getTransactiontype().equals("SELL")
//                && ctx.getStopLossOrderId().equals(
//                response.getOrderStatusData().getOrderid()
//        );
//    }
//
//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
////        ctx.markStoppedOut();
//
//        log.warn(
//                "STOP LOSS HIT for symbol={}, buyOrder={}",
//                ctx.getTradingSymbol(),
//                ctx.getBuyOrderId()
//        );
//    }

    private static final Logger log = LoggerFactory.getLogger(StopLossFilledStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;
    private final BalanceService balanceService;

    public StopLossFilledStrategy(
            OrderExecutionService executionService,
            TokenManager tokenManager,
            OrderRegistry orderRegistry,
            BalanceService balanceService
    ) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
        this.balanceService = balanceService;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return "SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        log.warn("STOPLOSS hit for {}", ctx.getTradingSymbol());

        String sellOrderId = ctx.getSellOrderId();
        String sellVariety = ctx.getSellVariety();

        if (sellOrderId == null) {
            log.warn("No SELL order to cancel for {}", ctx.getBuyOrderId());
            return;
        }

        String jwtToken = tokenManager.getValidJwtToken();

        executionService
                .placeCancelOrder(sellOrderId, sellVariety, jwtToken)
                .retry(3)
                .doOnSuccess(resp -> {
                    log.info("SELL cancelled after SL hit for buyOrder={}", ctx.getBuyOrderId());
                    orderRegistry.remove(ctx); // trade is complete
                })
                .subscribe();

        double executedPrice =
                Double.parseDouble(response.getOrderStatusData().getPrice());

        int quantity = ctx.getQuantity();

        // 🔑 BALANCE UPDATE
        balanceService.onSell(
                BigDecimal.valueOf(executedPrice),
                quantity,
                balanceService.getCurrentBalance()
        );

//        log.warn("STOPLOSS hit, balance updated: price={}, qty={}",
//                executedPrice, quantity);
    }
}

