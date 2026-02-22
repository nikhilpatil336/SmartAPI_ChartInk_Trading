package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy;

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
public class SellFilledOrderStrategy implements IFillOrderStrategy {

    private static final Logger log = LoggerFactory.getLogger(SellFilledOrderStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public SellFilledOrderStrategy(
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
                && response.getOrderStatusData().getOrderid().equals(ctx.getSellOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        if (ctx.isTradeCompleted()) {
            log.warn("Duplicate SELL completion ignored | orderId={}",
                    response.getOrderStatusData().getOrderid());
            return;
        }
        ctx.setTradeCompleted(true);

        log.info(
                "SELL filled | stock={} | buyOrderId={} | sellOrderId={} | qty={}",
                ctx.getTradingSymbol(),
                ctx.getBuyOrderId(),
                ctx.getSellOrderId(),
                ctx.getQuantity()
        );

        String stopLossOrderId = ctx.getStopLossOrderId();
        String stopLossVariety = ctx.getStopLossVariety();

        if (stopLossOrderId == null) {
            log.warn(
                    "No SL to cancel | stock={} | buyOrderId={}",
                    ctx.getTradingSymbol(),
                    ctx.getBuyOrderId()
            );
            return;
        }

        String jwtToken = tokenManager.getValidJwtToken();

        executionService
                .placeCancelOrder(stopLossOrderId, stopLossVariety, jwtToken, "StopLoss")
                .retry(3)
                .doOnSuccess(resp -> {
                    log.info(
                            "SL cancelled | stock={} | buyOrderId={} | slOrderId={}",
                            ctx.getTradingSymbol(),
                            ctx.getBuyOrderId(),
                            stopLossOrderId
                    );

                    ctx.setSLOpen(false);
//                    orderRegistry.remove(ctx); // trade complete
                })
                .subscribe();

        ctx.setSellOpen(false);

//        double executedPrice =
//                Double.parseDouble(response.getOrderStatusData().getPrice());

        BigDecimal executedPrice =
                new BigDecimal(response.getOrderStatusData().getPrice());

//        int quantity = ctx.getQuantity();
        int quantity = Integer.parseInt(response.getOrderStatusData().getFilledshares());

        String normalizedSymbol =
                Utility.normalize(ctx.getTradingSymbol());

        // 🔑 BALANCE UPDATE
        balanceService.onSell(
                executedPrice,
                ctx.getBuyPrice(),
                quantity,
                applicationProperties.getLeverageMultiplierToUseForLong(),
                balanceService.getUsableBalance(),
                leverageService.get(normalizedSymbol).multiplier()
        );

        log.info(
                "Balance updated after SELL | stock={} | price={} | qty={} | leveragedUsed={} | leverage={}",
                ctx.getTradingSymbol(),
                executedPrice,
                quantity,
                applicationProperties.getLeverageMultiplierToUseForLong(),
                leverageService.get(normalizedSymbol).multiplier()
        );

//        log.info("Balance updated after SELL: price={}, qty={}",
//                executedPrice, quantity);
    }
}

