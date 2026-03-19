package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
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
public class StopLossOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log = LoggerFactory.getLogger(StopLossOpenStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties properties;

    public StopLossOpenStrategy(OrderExecutionService executionService,
                                TokenManager tokenManager,
                                BalanceService balanceService,
                                LeverageService leverageService,
                                ApplicationProperties properties) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.properties = properties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isLong()
                &&"SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
        int lastStoplossFilled = ctx.getLastStoplossFilledQty();
        int delta = filledQty - lastStoplossFilled;

        if (delta <= 0) return;

        if (delta > 0 && ctx.isBuyOpen()) {
            cancelRemainingBuy(ctx);
        }

        int remainingQty = Math.max(0, ctx.getLastBuyFilledQty() - filledQty);

//        if (remainingQty > 0 && ctx.getSellOrderId() != null) {
        if (remainingQty > 0 && ctx.isSellPlaced() && ctx.isSellOpen() && !ctx.isTradeCompleted()) {

//            BigDecimal executedPrice =
//                    new BigDecimal(response.getOrderStatusData().getPrice());

            log.warn(
                    "STOPLOSS partial hit | stock={} | delta={} | totalFilled={}",
                    ctx.getTradingSymbol(), delta, filledQty
            );


//        int remainingQty = ctx.getQuantity() - filledQty;

            String jwt = tokenManager.getValidJwtToken();

            executionService.modifySellOrder(
                    ctx.getTradingSymbol(),
                    ctx.getSymbolToken(),
                    remainingQty,
                    ctx.getSellPrice().doubleValue(),
                    ctx.getSellOrderId(),
                    jwt
            )
            .doOnError(e -> log.error("Failed to modify Sell order", e.getMessage()))
            .subscribe();

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());

            balanceService.onSell(
                    executedPrice,
                    ctx.getBuyPrice(),
                    delta,
                    properties.getLeverageMultiplierToUseForLong(),
                    balanceService.getUsableBalance(),
                    leverageService.get(normalizedSymbol).multiplier()
            );

            ctx.setLastStoplossFilledQty(filledQty);
        }
    }

    private void cancelRemainingBuy(OrderContext ctx) {
        String jwt = tokenManager.getValidJwtToken();

        executionService.placeCancelOrder(
                        ctx.getBuyOrderId(),
                        ctx.getBuyVariety(),
                        jwt,
                        "BUY"
                )
                .doOnSuccess(resp -> {
                    ctx.setBuyOpen(false);
                    log.info("Remaining BUY cancelled as exit started | buyOrderId={}",
                            ctx.getBuyOrderId());
                })
                .subscribe();
    }

}
