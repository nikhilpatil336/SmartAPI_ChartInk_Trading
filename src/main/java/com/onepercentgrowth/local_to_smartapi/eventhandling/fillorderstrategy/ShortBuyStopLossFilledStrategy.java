package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy;

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
public class ShortBuyStopLossFilledStrategy implements IFillOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortBuyStopLossFilledStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public ShortBuyStopLossFilledStrategy(
            OrderExecutionService executionService,
            TokenManager tokenManager,
            BalanceService balanceService,
            LeverageService leverageService,
            ApplicationProperties applicationProperties
    ) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "BUY".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getStopLossOrderId());
    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        if (ctx.isTradeCompleted()) return;
//        ctx.setTradeCompleted(true);
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        int qty =
//                Integer.parseInt(response.getOrderStatusData().getFilledshares());
//
//        log.warn("SHORT STOPLOSS HIT | stock={}", ctx.getTradingSymbol());
//
//        cancelTarget(ctx);
//
//        String normalizedSymbol =
//                Utility.normalize(ctx.getTradingSymbol());
//
//        balanceService.onBuy(
//                executedPrice,
//                qty,
//                applicationProperties.getLeverageMultiplierToUseForShort(),
//                balanceService.getUsableBalance(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//    }
//
//    private void cancelTarget(OrderContext ctx) {
//        if (ctx.getBuyOrderId() == null) return;
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        executionService.placeCancelOrder(
//                ctx.getBuyOrderId(),
//                ctx.getBuyVariety(),
//                jwt,
//                "BUY"
//        ).subscribe();
//    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        if (ctx.isTradeCompleted()) {
            log.warn("Duplicate SHORT SL completion ignored | orderId={}",
                    response.getOrderStatusData().getOrderid());
            return;
        }

        ctx.setTradeCompleted(true);

        BigDecimal executedPrice =
                new BigDecimal(response.getOrderStatusData().getAverageprice());

        int qty =
                Integer.parseInt(response.getOrderStatusData().getFilledshares());

        log.warn("SHORT STOPLOSS HIT | stock={} | sellOrderId={} | slOrderId={} |  qty={}",
                ctx.getTradingSymbol(),
                ctx.getSellOrderId(),
                ctx.getStopLossOrderId(),
                ctx.getQuantity()
        );

        String buyOrderId = ctx.getBuyOrderId();
        String buyVariety = ctx.getBuyVariety();

        if (buyOrderId == null) {
            log.warn(
                    "No SELL to cancel after SL | stock={} | buyOrderId={}",
                    ctx.getTradingSymbol(),
                    ctx.getBuyOrderId()
            );
            return;
        }

        /* ================= CANCEL TARGET BUY ================= */

        if (ctx.getBuyOrderId() != null && ctx.isBuyOpen()) {

            String jwt = tokenManager.getValidJwtToken();

            executionService.placeCancelOrder(
                            ctx.getBuyOrderId(),
                            ctx.getBuyVariety(),
                            jwt,
                            "BUY"
                    )
                    .retry(3)
                    .doOnSuccess(resp -> {
                        log.info(
                            "SHORT TARGET cancelled after SL | stock={} | sellOrderId={} | buyOrderId={}",
                            ctx.getTradingSymbol(),
                            ctx.getSellOrderId(),
                            ctx.getBuyOrderId()
                    );
                    ctx.setBuyOpen(false);
                })
                .subscribe();
        }

        ctx.setSLOpen(false);

        /* ================= BALANCE UPDATE ================= */

//        int quantity = ctx.getQuantity();
        int quantity = Integer.parseInt(response.getOrderStatusData().getFilledshares());

        String normalizedSymbol =
                Utility.normalize(ctx.getTradingSymbol());

        balanceService.onBuy(
                executedPrice,
                qty,
                applicationProperties.getLeverageMultiplierToUseForShort(),
                balanceService.getUsableBalance(),
                leverageService.get(normalizedSymbol).multiplier()
        );

        log.info(
                "Balance updated after SHORT SL | stock={} | price={} | qty={} | leveragedUsed={} | leverage={}",
                ctx.getTradingSymbol(),
                executedPrice,
                qty,
                applicationProperties.getLeverageMultiplierToUseForLong(),
                leverageService.get(normalizedSymbol).multiplier()
        );

        /* ================= CLEANUP (Optional but recommended) ================= */

        // orderRegistry.remove(ctx);
    }
}
