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
public class ShortStopLossOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortStopLossOpenStrategy.class);

    private final BalanceService balanceService;
    private final TokenManager tokenManager;
    private final OrderExecutionService executionService;
    private final ApplicationProperties properties;
    private final LeverageService leverageService;

    public ShortStopLossOpenStrategy(
            BalanceService balanceService,
            TokenManager tokenManager,
            OrderExecutionService executionService,
            ApplicationProperties properties,
            LeverageService leverageService) {

        this.balanceService = balanceService;
        this.tokenManager = tokenManager;
        this.executionService = executionService;
        this.properties = properties;
        this.leverageService = leverageService;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "BUY".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//        int lastStoplossFilled = ctx.getLastStoplossFilledQty();
        int delta = filledQty - ctx.getLastStoplossFilledQty();

        if (delta <= 0) return;

        if (delta > 0 && ctx.isSellOpen()) {
            cancelRemainingEntrySell(ctx);
        }

        int remainingQty = Math.max(0, ctx.getLastSellFilledQty() - filledQty);

        if (remainingQty > 0 && ctx.isBuyPlaced() && ctx.isBuyOpen() && !ctx.isTradeCompleted()) {

            log.warn(
                    "STOPLOSS partial hit | stock={} | delta={} | totalFilled={}",
                    ctx.getTradingSymbol(), delta, filledQty
            );

            String jwt = tokenManager.getValidJwtToken();

            executionService.modifyBuyOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            remainingQty,
                            ctx.getBuyPrice().toString(),
                            ctx.getBuyOrderId(),
                            jwt
                    )
                    .doOnError(e -> log.error("Failed to modify Buy order", e.getMessage()))
                    .subscribe();

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());

            // ===== BOOK LOSS =====
            balanceService.onShortCover(
                    executedPrice,
                    ctx.getSellPrice(),
                    delta,
                    properties.getLeverageMultiplierToUseForShort(),
                    balanceService.getUsableBalance(),
                    leverageService.get(normalizedSymbol).multiplier()
            );

    //        cancelRemainingEntrySell(ctx);

            ctx.setLastStoplossFilledQty(filledQty);

            log.info("SHORT STOPLOSS HIT | orderId={}", ctx.getStopLossOrderId());
        }
    }

    private void cancelRemainingEntrySell(OrderContext ctx) {

        if (!ctx.isSellOpen()) return;

        String jwt = tokenManager.getValidJwtToken();

        executionService.placeCancelOrder(
                        ctx.getSellOrderId(),
                        ctx.getSellVariety(),
                        jwt,
                        "SELL"
                )
                .doOnSuccess(resp -> {
                    ctx.setSellOpen(false);
                    log.info("Remaining BUY cancelled as exit started | buyOrderId={}",
                            ctx.getSellOrderId());
                }).subscribe();

//        ctx.setSellOpen(false);
    }
}


