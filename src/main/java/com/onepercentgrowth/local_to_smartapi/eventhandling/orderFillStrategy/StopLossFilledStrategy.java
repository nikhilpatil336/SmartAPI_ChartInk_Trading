package com.onepercentgrowth.local_to_smartapi.eventhandling.orderFillStrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.BalanceService;
import com.onepercentgrowth.local_to_smartapi.service.LeverageService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.utility.Utility;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StopLossFilledStrategy implements OrderFillStrategy {

    private static final Logger log = LoggerFactory.getLogger(StopLossFilledStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public StopLossFilledStrategy(
            OrderExecutionService executionService,
            TokenManager tokenManager,
            OrderRegistry orderRegistry,
            BalanceService balanceService,
            LeverageService leverageService,
            ApplicationProperties applicationProperties
    ) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return "SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        log.warn(
                "STOPLOSS hit | stock={} | buyOrderId={} | slOrderId={} | qty={}",
                ctx.getTradingSymbol(),
                ctx.getBuyOrderId(),
                ctx.getStopLossOrderId(),
                ctx.getQuantity()
        );


        String sellOrderId = ctx.getSellOrderId();
        String sellVariety = ctx.getSellVariety();

        if (sellOrderId == null) {
            log.warn(
                    "No SELL to cancel after SL | stock={} | buyOrderId={}",
                    ctx.getTradingSymbol(),
                    ctx.getBuyOrderId()
            );
            return;
        }


        String jwtToken = tokenManager.getValidJwtToken();

        executionService
                .placeCancelOrder(sellOrderId, sellVariety, jwtToken)
                .retry(3)
                .doOnSuccess(resp -> {
                    log.info(
                            "SELL cancelled after SL | stock={} | buyOrderId={} | sellOrderId={}",
                            ctx.getTradingSymbol(),
                            ctx.getBuyOrderId(),
                            sellOrderId
                    );

//                    orderRegistry.remove(ctx); // trade complete
                })
                .subscribe();

        double executedPrice =
                Double.parseDouble(response.getOrderStatusData().getPrice());

//        int quantity = ctx.getQuantity();
        int quantity = Integer.parseInt(response.getOrderStatusData().getFilledshares());

        String normalizedSymbol =
                Utility.normalize(ctx.getTradingSymbol());

        // 🔑 BALANCE UPDATE
//        balanceService.onSLSell(
//                BigDecimal.valueOf(executedPrice),
//                quantity,
//                balanceService.getUsableBalance(),
//                leverageService.get(normalizedSymbol).multiplier(),
//                ctx.getBuyPrice()
//        );

        balanceService.onSell(
                BigDecimal.valueOf(executedPrice),
                BigDecimal.valueOf(ctx.getBuyPrice()),
                quantity,
                applicationProperties.getLeverageMultiplierToUse(),
                balanceService.getUsableBalance(),
                leverageService.get(normalizedSymbol).multiplier()
        );

        log.info(
                "Balance updated after SL | stock={} | price={} | qty={} | leveragedUsed={} | leverage={}",
                ctx.getTradingSymbol(),
                executedPrice,
                quantity,
                applicationProperties.getLeverageMultiplierToUse(),
                leverageService.get(normalizedSymbol).multiplier()
        );


//        log.warn("STOPLOSS hit, balance updated: price={}, qty={}",
//                executedPrice, quantity);
    }
}

