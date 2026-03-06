package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy;

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
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class ShortEntryOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(ShortEntryOpenStrategy.class);

    private final OrderExecutionService executionService;
    private final OrderCalculationService calculationService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties properties;

    public ShortEntryOpenStrategy(
            OrderExecutionService executionService,
            OrderCalculationService calculationService,
            TokenManager tokenManager,
            OrderRegistry orderRegistry,
            BalanceService balanceService,
            LeverageService leverageService,
            ApplicationProperties properties) {

        this.executionService = executionService;
        this.calculationService = calculationService;
        this.tokenManager = tokenManager;
        this.orderRegistry = orderRegistry;
        this.balanceService = balanceService;
        this.leverageService = leverageService;
        this.properties = properties;
    }

    @Override
    public boolean supports(OrderContext ctx, OrderStatusResponse response) {
        return ctx.isShort()
                && "SELL".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && response.getOrderStatusData().getOrderid().equals(ctx.getSellOrderId());
    }

    @Override
    public void onFilled(OrderContext ctx, OrderStatusResponse response) {

        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//        int lastSellFilled = ctx.getLastSellFilledQty();
        int delta = filledQty - ctx.getLastSellFilledQty();

        if (!ctx.isSellOpen()) {
            ctx.setSellOpen(true);
            log.info("SHORT ENTRY SELL OPEN | orderId={}", ctx.getSellOrderId());
        }

        if (delta <= 0) return;

        BigDecimal intenedPrice =
                new BigDecimal(response.getOrderStatusData().getPrice());

        BigDecimal executedPrice =
                new BigDecimal(response.getOrderStatusData().getAverageprice());

        ctx.setSellPrice(executedPrice);

        log.info(
                "SELL for short partial fill | stock={} | delta={} | totalFilled={}",
                ctx.getTradingSymbol(), delta, filledQty
        );

        String normalizedSymbol =
                Utility.normalize(ctx.getTradingSymbol());

        // ===== BALANCE UPDATE (SELL ENTRY) =====
        balanceService.onShortSell(
                executedPrice,
                delta,
                properties.getLeverageMultiplierToUseForShort(),
                balanceService.getUsableBalance(),
                leverageService.get(normalizedSymbol).multiplier()
        );

        // ===== CALCULATE TARGET & SL =====
//        BigDecimal buyTargetPrice =
//                calculationService.calculateSellProfitPrice(executedPrice);
//
//        BigDecimal buyStopLossPrice =
//                calculationService.calculateSellStopLossPrice(executedPrice);
        BigDecimal buyPrice =
                calculationService.calculateSellProfitPrice(intenedPrice);

        StopLossPrice slPrice =
                calculationService.calculateShortStopLossPrice(
                        intenedPrice,
                        BigDecimal.valueOf(properties.getTradingStoplossPercent()),
                        BigDecimal.valueOf(properties.getTradingStoplossBufferPercent())
                );

        String jwt = tokenManager.getValidJwtToken();

        // ===== TARGET BUY =====
        if (!ctx.isBuyPlaced() && !ctx.isBuyOpen()) {

            executionService.placeBuyOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            buyPrice.toString(),
                            jwt
                    )
                    .doOnSuccess(resp -> {
                        ctx.setBuyOrderId(resp.getData().getOrderid());
                        ctx.setBuyPlaced(true);
                        ctx.setBuyOpen(true);
                        ctx.setBuyPrice(buyPrice);
                        ctx.setBuyVariety("NORMAL");
                        orderRegistry.registerBuy(ctx);
                        log.info("SHORT TARGET BUY placed | {}", ctx.getBuyOrderId());
                    })
                    .doOnError(err -> {
                        log.error("BUY order placement failed sellOrderId={}",
                                ctx.getSellOrderId(), err);
                    })
                    .onErrorResume(err -> Mono.empty())
                    .subscribe();
        }else {
            executionService.modifyBuyOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            buyPrice.toString(),
                            ctx.getBuyOrderId(),
                            jwt
                    )
                    .doOnSuccess(resp ->
                            log.info("BUY order modified successfully buyOrderId={}",
                                    ctx.getBuyOrderId())
                    )
                    .doOnError(err ->
                            log.error("BUY order modification failed buyOrderId={}",
                                    ctx.getBuyOrderId(), err)
                    )
                    .onErrorResume(err -> Mono.empty())
                    .subscribe();
        }

        // ===== STOPLOSS BUY =====
        if (!ctx.isSlPlaced()  && !ctx.isSLOpen()) {

            executionService.placeStopLossOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            slPrice.triggerPrice().doubleValue(),
                            slPrice.limitPrice().doubleValue(),
                            jwt
                    )
                    .doOnSuccess(resp -> {
                        ctx.setStopLossOrderId(resp.getData().getOrderid());
                        ctx.setSlPlaced(true);
                        ctx.setSLOpen(true);
                        ctx.setStopLossVariety("STOPLOSS");
                        ctx.setStoplossLimitPrice(slPrice.limitPrice());
                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
                        orderRegistry.registerStopLoss(ctx);
                        log.info("SHORT STOPLOSS BUY placed | {}", ctx.getStopLossOrderId());
                    })
                    .subscribe();
        }else {
            executionService.modifyStopLossOrder(
                            ctx.getTradingSymbol(),
                            ctx.getSymbolToken(),
                            filledQty,
                            slPrice.triggerPrice().doubleValue(),
                            slPrice.limitPrice().doubleValue(),
                            ctx.getStopLossOrderId(),
                            jwt
                    )
                    .doOnSuccess(resp ->
                            log.info("STOP LOSS modified successfully slOrderId={}",
                                    ctx.getStopLossOrderId())
                    )
                    .doOnError(err ->
                            log.error("STOP LOSS modification failed slOrderId={}",
                                    ctx.getStopLossOrderId(), err)
                    )
                    .onErrorResume(err -> Mono.empty())
                    .subscribe();
        }

        ctx.setLastSellFilledQty(filledQty);
    }
}
