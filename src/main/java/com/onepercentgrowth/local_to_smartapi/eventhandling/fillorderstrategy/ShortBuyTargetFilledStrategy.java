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
public class ShortBuyTargetFilledStrategy implements IFillOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortBuyTargetFilledStrategy.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public ShortBuyTargetFilledStrategy(
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
                && response.getOrderStatusData().getOrderid().equals(ctx.getBuyOrderId());
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
//        log.info("SHORT TARGET HIT | stock={}", ctx.getTradingSymbol());
//
//        cancelStopLoss(ctx);
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

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        if (ctx.isTradeCompleted()) {
            log.warn("Duplicate SHORT TARGET completion ignored | orderId={}",
                    response.getOrderStatusData().getOrderid());
            return;
        }

        ctx.setTradeCompleted(true);

//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
        BigDecimal executedPrice =
                new BigDecimal(response.getOrderStatusData().getPrice());

        int qty =
                Integer.parseInt(response.getOrderStatusData().getFilledshares());

        log.info(
                "SHORT TARGET filled | stock={} | sellOrderId={} | buyOrderId={} | qty={}",
                ctx.getTradingSymbol(),
                ctx.getSellOrderId(),
                ctx.getBuyOrderId(),
                qty
        );

        /* ========= CANCEL STOPLOSS ========= */

        if (ctx.getStopLossOrderId() != null && ctx.isSLOpen()) {

            String jwt = tokenManager.getValidJwtToken();

            executionService.placeCancelOrder(
                            ctx.getStopLossOrderId(),
                            ctx.getStopLossVariety(),
                            jwt,
                            "StopLoss"
                    )
                    .retry(3)
                    .doOnSuccess(resp -> {
                        log.info(
                                "SHORT SL cancelled | stock={} | slOrderId={} | sellOrderId={}",
                                ctx.getTradingSymbol(),
                                ctx.getStopLossOrderId(),
                                ctx.getSellOrderId()
                                );
                        ctx.setSLOpen(false);
                    })
                    .subscribe();
        }

        ctx.setBuyOpen(false);

        /* ========= BALANCE UPDATE ========= */

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
                "Balance updated after SHORT TARGET | stock={} | price={} | qty={} | leveragedUsed={} | leverage={}",
                ctx.getTradingSymbol(),
                executedPrice,
                qty,
                applicationProperties.getLeverageMultiplierToUseForLong(),
                leverageService.get(normalizedSymbol).multiplier()
        );

        // orderRegistry.remove(ctx); // optional cleanup
    }

//    private void cancelStopLoss(OrderContext ctx) {
//        if (ctx.getStopLossOrderId() == null) return;
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        executionService.placeCancelOrder(
//                ctx.getStopLossOrderId(),
//                ctx.getStopLossVariety(),
//                jwt,
//                "StopLoss"
//        ).subscribe();
//    }
}
