package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.StopLossPrice;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.BalanceService;
import com.onepercentgrowth.local_to_smartapi.service.LeverageService;
import com.onepercentgrowth.local_to_smartapi.service.OrderCalculationService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.utility.Utility;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ShortSellFilledOrderStrategy implements IFillOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortSellFilledOrderStrategy.class);

    private final OrderExecutionService executionService;
    private final OrderCalculationService calculationService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties applicationProperties;

    public ShortSellFilledOrderStrategy(
            OrderExecutionService executionService,
            OrderCalculationService calculationService,
            TokenManager tokenManager,
            OrderRegistry orderRegistry,
            BalanceService balanceService,
            LeverageService leverageService,
            ApplicationProperties applicationProperties
    ) {
        this.executionService = executionService;
        this.calculationService = calculationService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getSellOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        if (!ctx.isSellOpen()) return;

        BigDecimal intenedePrice =
                new BigDecimal(response.getOrderStatusData().getPrice());

        BigDecimal executedPrice =
                new BigDecimal(response.getOrderStatusData().getAverageprice());

        int filledQty =
                Integer.parseInt(response.getOrderStatusData().getFilledshares());

        ctx.setSellPrice(executedPrice);
        int lastSellFilledQty = ctx.getLastSellFilledQty();
        ctx.setLastSellFilledQty(filledQty);
        ctx.setSellOpen(false);

        log.info("SHORT ENTRY SELL filled | stock={} | orderId={} | filledQty={} | executedPrice={}",
                ctx.getTradingSymbol(),
                ctx.getSellOrderId(),
                filledQty,
                executedPrice);

//        int incrementalQty = filledQty - lastQty;

        String jwt = tokenManager.getValidJwtToken();

        BigDecimal buyTargetPrice =
                calculationService.calculateSellProfitPrice(executedPrice);

//        BigDecimal buyStopLossPrice =
//                calculationService.calculateSellStopLossPrice(executedPrice);

        StopLossPrice slPrice =
                calculationService.calculateShortStopLossPrice(
                        executedPrice,
                        BigDecimal.valueOf(applicationProperties.getTradingStoplossPercent()),
                        BigDecimal.valueOf(applicationProperties.getTradingStoplossBufferPercent())
                );

        log.info(
                "TP/SL calculated | stock={} | TP={} | SL={}",
                ctx.getTradingSymbol(),
                buyTargetPrice,
                slPrice
        );

    /* =========================================================
       TARGET BUY (Cover Order)
    ========================================================= */

        if (!ctx.isBuyPlaced()) {

            executionService.placeBuyOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            buyTargetPrice.toString(),
                            jwt
                    )
                    .retry(3)
                    .doOnSuccess(resp -> {
                        ctx.setBuyOrderId(resp.getData().getOrderid());
                        ctx.setBuyVariety("NORMAL");
                        ctx.setBuyPlaced(true);
                        ctx.setBuyOpen(true);
                        ctx.setBuyPrice(buyTargetPrice);

                        orderRegistry.registerBuy(ctx);

                        log.info("SHORT TARGET BUY placed | buyOrderId={}",
                                ctx.getBuyOrderId());
                    })
                    .subscribe();

        } else if (ctx.isBuyOpen()) {

            executionService.modifyBuyOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            buyTargetPrice.toString(),
                            ctx.getBuyOrderId(),
                            jwt
                    )
                    .retry(3)
                    .doOnSuccess(resp ->
                            log.info("SHORT TARGET BUY modified | buyOrderId={}",
                                    ctx.getBuyOrderId())
                    )
                    .subscribe();
        }

    /* =========================================================
       STOP LOSS BUY
    ========================================================= */

        if (!ctx.isSlPlaced()) {

            executionService.placeStopLossOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            slPrice.triggerPrice().doubleValue(),
                            slPrice.limitPrice().doubleValue(),
                            jwt
                    )
                    .retry(3)
                    .doOnSuccess(resp -> {
                        ctx.setStopLossOrderId(resp.getData().getOrderid());
                        ctx.setStopLossVariety("STOPLOSS");
                        ctx.setSlPlaced(true);
                        ctx.setSLOpen(true);
                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
                        ctx.setStoplossLimitPrice(slPrice.limitPrice());

                        orderRegistry.registerStopLoss(ctx);

                        log.info("SHORT SL BUY placed | slOrderId={}",
                                ctx.getStopLossOrderId());
                    })
                    .subscribe();

        } else if (ctx.isSLOpen()) {

            executionService.modifyStopLossOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            slPrice.triggerPrice().doubleValue(),
                            slPrice.limitPrice().doubleValue(),
                            ctx.getStopLossOrderId(),
                            jwt
                    )
                    .retry(3)
                    .doOnSuccess(resp ->
                            log.info("SHORT SL BUY modified on SELL complete | slOrderId={}",
                                    ctx.getStopLossOrderId())
                    )
                    .subscribe();
        }

    /* =========================================================
       BALANCE UPDATE
    ========================================================= */

        String normalizedSymbol =
                Utility.normalize(ctx.getTradingSymbol());

        balanceService.onSell(
                executedPrice,
                BigDecimal.ZERO,
                filledQty - lastSellFilledQty,
                applicationProperties.getLeverageMultiplierToUseForShort(),
                balanceService.getUsableBalance(),
                leverageService.get(normalizedSymbol).multiplier()
        );
    }
}
