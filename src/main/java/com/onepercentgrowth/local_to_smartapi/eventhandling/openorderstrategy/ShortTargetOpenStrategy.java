package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.execution.OrderActionExecutor;
import com.onepercentgrowth.local_to_smartapi.exit.AggressiveExitManager;
import com.onepercentgrowth.local_to_smartapi.exit.ExitType;
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
public class ShortTargetOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortTargetOpenStrategy.class);

    private final BalanceService balanceService;
    private final TokenManager tokenManager;
    private final OrderExecutionService executionService;
    private final ApplicationProperties properties;
    private final LeverageService leverageService;
    private final OrderActionExecutor orderActionExecutor;
    private final AggressiveExitManager aggressiveExitManager;

    public ShortTargetOpenStrategy(
            BalanceService balanceService,
            TokenManager tokenManager,
            OrderExecutionService executionService,
            ApplicationProperties properties,
            LeverageService leverageService,
            OrderActionExecutor orderActionExecutor,
            AggressiveExitManager aggressiveExitManager) {

        this.balanceService = balanceService;
        this.tokenManager = tokenManager;
        this.executionService = executionService;
        this.properties = properties;
        this.leverageService = leverageService;
        this.orderActionExecutor = orderActionExecutor;
        this.aggressiveExitManager = aggressiveExitManager;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "BUY".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getBuyOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
        int lastBuyFilled = ctx.getLastBuyFilledQty();
        int delta = filledQty - ctx.getLastBuyFilledQty();

        if (delta <= 0) return;

        if (delta > 0 && ctx.isSellOpen()) {
            cancelRemainingEntrySell(ctx);
        }

        int remainingQty = Math.max(0, ctx.getLastBuyFilledQty() - filledQty);

        if (remainingQty > 0 && ctx.isSlPlaced() && ctx.isSLOpen() && !ctx.isTradeCompleted()) {

            log.info(
                    "BUY partial fill | stock={} | delta={} | totalFilled={}",
                    ctx.getTradingSymbol(), delta, filledQty
            );

            String jwt = tokenManager.getValidJwtToken();

            orderActionExecutor.safeModifyOrReplace(
                    executionService.modifyStopLossOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            remainingQty,
                            ctx.getStoplossTriggerPrice().doubleValue(),
                            ctx.getStoplossLimitPrice().doubleValue(),
                            ctx.getStopLossOrderId(),
                            tokenManager.getValidJwtToken()
                    ),
                    () -> aggressiveExitManager.placeAggressiveExit(
                            ctx,
                            remainingQty,
                            ExitType.MODIFY_FAILED
                    )
            ).subscribe();

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());

            // ===== BOOK PROFIT =====
            balanceService.onBuy(
                    executedPrice,
                    delta,
                    properties.getLeverageMultiplierToUseForShort(),
                    balanceService.getUsableBalance(),
                    leverageService.get(
                            Utility.normalize(ctx.getTradingSymbol())
                    ).multiplier()
            );

//        cancelRemainingEntrySell(ctx);

            ctx.setLastBuyFilledQty(filledQty);

            log.info("SHORT TARGET HIT | orderId={}", ctx.getBuyOrderId());
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
            log.info("Remaining SELL cancelled as exit started | sellOrderId={}",
                    ctx.getSellOrderId());
        }).subscribe();

        ctx.setSellOpen(false);
    }
}
