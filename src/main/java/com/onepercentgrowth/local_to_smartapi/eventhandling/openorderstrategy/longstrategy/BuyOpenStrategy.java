package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.longstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.IOpenOrderStrategy;
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
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BuyOpenStrategy implements IOpenOrderStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyOpenStrategy.class);

    private final OrderExecutionService executionService;
    private final OrderCalculationService calculationService;
    private final TokenManager tokenManager;
    private final OrderRegistry orderRegistry;
    private final BalanceService balanceService;
    private final LeverageService leverageService;
    private final ApplicationProperties properties;

    public BuyOpenStrategy(OrderExecutionService executionService,
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
        return ctx.isLong()
                &&"BUY".equalsIgnoreCase(response.getOrderStatusData().getTransactiontype())
                && ctx.getBuyOrderId().equals(response.getOrderStatusData().getOrderid());
    }

//    @Override
//    public void onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//        int lastBuyFilled = ctx.getLastBuyFilledQty();
//        int delta = filledQty - lastBuyFilled;
//
//        if (!ctx.isBuyOpen()) {
//            ctx.setBuyOpen(true);
//            log.info("BUY order is now OPEN | buyOrderId={}", ctx.getBuyOrderId());
//        }
//
//        if (delta <= 0) return; // duplicate / stale WS update
//
//        BigDecimal intenedPrice =
//                new BigDecimal(response.getOrderStatusData().getPrice());
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        ctx.setBuyPrice(executedPrice);
//
//        log.info(
//                "BUY for long partial fill | stock={} | delta={} | totalFilled={}",
//                ctx.getTradingSymbol(), delta, filledQty
//        );
//
//        // ===== BALANCE UPDATE (DELTA ONLY) =====
//        String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
//
//        balanceService.onBuy(
//                executedPrice,
//                delta,
//                properties.getLeverageMultiplierToUseForLong(),
//                balanceService.getUsableBalance(),
//                leverageService.get(normalizedSymbol).multiplier()
//        );
//
//        // ===== TP / SL CALC =====
//        BigDecimal sellPrice =
//                calculationService.calculateBuyProfitPrice(intenedPrice);
//
//        StopLossPrice slPrice =
//                calculationService.calculateStopLossPrice(
//                        intenedPrice,
//                        BigDecimal.valueOf(properties.getTradingStoplossPercent()),
//                        BigDecimal.valueOf(properties.getTradingStoplossBufferPercent())
//                );
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        // ===== SELL =====
//        if (!ctx.isSellPlaced() && !ctx.isSellOpen()) {
//
//            executionService.placeSellOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            sellPrice.doubleValue(),
//                            jwt
//                    )
//                    .doOnSuccess(resp -> {
//                        ctx.setSellOrderId(resp.getData().getOrderid());
//                        ctx.setSellVariety("NORMAL");
//                        ctx.setSellPlaced(true);
//                        ctx.setSellOpen(true);
//                        ctx.setSellPrice(sellPrice);
//                        orderRegistry.registerSell(ctx);
//
//                        log.info("SELL order placed successfully sellOrderId={}",
//                                ctx.getSellOrderId());
//                    })
//                    .doOnError(err -> {
//                        log.error("SELL order placement failed buyOrderId={}",
//                                ctx.getBuyOrderId(), err);
//                    })
//                    .onErrorResume(err -> Mono.empty())
//                    .subscribe();
//
//        } else {
//            executionService.modifySellOrder(
//                    ctx.getTradingSymbol(),
//                    ctx.getSymbolToken(),
//                    filledQty,
//                    sellPrice.doubleValue(),
//                    ctx.getSellOrderId(),
//                    jwt
//            )
//            .doOnSuccess(resp ->
//                    log.info("SELL order modified successfully sellOrderId={}",
//                            ctx.getSellOrderId())
//            )
//            .doOnError(err ->
//                    log.error("SELL order modification failed sellOrderId={}",
//                            ctx.getSellOrderId(), err)
//            )
//            .onErrorResume(err -> Mono.empty())
//            .subscribe();
//        }
//
//        // ===== STOP LOSS =====
//        if (!ctx.isSlPlaced()  && !ctx.isSLOpen()) {
//
//            executionService.placeStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            filledQty,
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            jwt
//                    )
//                    .doOnSuccess(resp -> {
//                        ctx.setStopLossOrderId(resp.getData().getOrderid());
//                        ctx.setStopLossVariety("STOPLOSS");
//                        ctx.setSlPlaced(true);
//                        ctx.setSLOpen(true);
//                        ctx.setStoplossLimitPrice(slPrice.limitPrice());
//                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
//                        orderRegistry.registerStopLoss(ctx);
//
//                        log.info("STOP LOSS placed successfully slOrderId={}",
//                                ctx.getStopLossOrderId());
//                    })
//                    .doOnError(err -> {
//                        log.error("STOP LOSS placement failed buyOrderId={}",
//                                ctx.getBuyOrderId(), err);
//                    })
//                    .onErrorResume(err -> Mono.empty())
//                    .subscribe();
//
//        } else {
//            executionService.modifyStopLossOrder(
//                    ctx.getTradingSymbol(),
//                    ctx.getSymbolToken(),
//                    filledQty,
//                    slPrice.triggerPrice().doubleValue(),
//                    slPrice.limitPrice().doubleValue(),
//                    ctx.getStopLossOrderId(),
//                    jwt
//            )
//            .doOnSuccess(resp ->
//                    log.info("STOP LOSS modified successfully slOrderId={}",
//                            ctx.getStopLossOrderId())
//            )
//            .doOnError(err ->
//                    log.error("STOP LOSS modification failed slOrderId={}",
//                            ctx.getStopLossOrderId(), err)
//            )
//            .onErrorResume(err -> Mono.empty())
//            .subscribe();
//        }
//
//        ctx.setLastBuyFilledQty(filledQty);
//    }


//    @Override
//    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {
//
//        int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//        int lastBuyFilled = ctx.getLastBuyFilledQty().get();
//
//        int delta = filledQty - lastBuyFilled;
//
//        if (!ctx.isBuyOpen()) {
//            ctx.setBuyOpen(true);
//            log.info("BUY order is now OPEN | buyOrderId={}", ctx.getBuyOrderId());
//        }
//
//        if (delta <= 0) return Mono.empty();
//
//        BigDecimal intendedPrice =
//                new BigDecimal(response.getOrderStatusData().getPrice());
//
//        BigDecimal executedPrice =
//                new BigDecimal(response.getOrderStatusData().getAverageprice());
//
//        ctx.setBuyPrice(executedPrice);
//
//        log.info("BUY partial fill | stock={} | delta={} | totalFilled={}",
//                ctx.getTradingSymbol(), delta, filledQty);
//
//        String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());
//
//        // ===== UPDATE POSITION FIRST (CRITICAL) =====
//        ctx.getNetPositionQty().addAndGet(delta);
//        ctx.getLastBuyFilledQty().set(filledQty);
//
//        // ===== BALANCE =====
//        Mono<Void> balanceMono = Mono.fromRunnable(() ->
//                balanceService.onBuy(
//                        executedPrice,
//                        delta,
//                        properties.getLeverageMultiplierToUseForLong(),
//                        balanceService.getUsableBalance(),
//                        leverageService.get(normalizedSymbol).multiplier()
//                )
//        );
//
//        // ===== CALCULATIONS =====
//        BigDecimal sellPrice =
//                calculationService.calculateBuyProfitPrice(intendedPrice);
//
//        StopLossPrice slPrice =
//                calculationService.calculateStopLossPrice(
//                        intendedPrice,
//                        BigDecimal.valueOf(properties.getTradingStoplossPercent()),
//                        BigDecimal.valueOf(properties.getTradingStoplossBufferPercent())
//                );
//
//        String jwt = tokenManager.getValidJwtToken();
//
//        // ===== SELL FLOW =====
//        Mono<Void> sellMono;
//
//        if (!ctx.isSellPlaced() && !ctx.isSellOpen()) {
//
//            sellMono = executionService.placeSellOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            ctx.getNetPositionQty().get(), // 🔥 FIXED
//                            sellPrice.doubleValue(),
//                            jwt
//                    )
//                    .doOnSuccess(resp -> {
//                        ctx.setSellOrderId(resp.getData().getOrderid());
//                        ctx.setSellVariety("NORMAL");
//                        ctx.setSellPlaced(true);
//                        ctx.setSellOpen(true);
//                        ctx.setSellPrice(sellPrice);
//                        orderRegistry.registerSell(ctx);
//
//                        log.info("SELL placed | orderId={}", ctx.getSellOrderId());
//                    })
//                    .then();
//
//        } else {
//
//            sellMono = executionService.modifySellOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            ctx.getNetPositionQty().get(), // 🔥 FIXED
//                            sellPrice.doubleValue(),
//                            ctx.getSellOrderId(),
//                            jwt
//                    )
//                    .doOnSuccess(resp ->
//                            log.info("SELL modified | orderId={}", ctx.getSellOrderId())
//                    )
//                    .then();
//        }
//
//        // ===== SL FLOW =====
//        Mono<Void> slMono;
//
//        if (!ctx.isSlPlaced() && !ctx.isSLOpen()) {
//
//            slMono = executionService.placeStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            ctx.getNetPositionQty().get(), // 🔥 FIXED
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            jwt
//                    )
//                    .doOnSuccess(resp -> {
//                        ctx.setStopLossOrderId(resp.getData().getOrderid());
//                        ctx.setStopLossVariety("STOPLOSS");
//                        ctx.setSlPlaced(true);
//                        ctx.setSLOpen(true);
//                        ctx.setStoplossLimitPrice(slPrice.limitPrice());
//                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
//                        orderRegistry.registerStopLoss(ctx);
//
//                        log.info("SL placed | orderId={}", ctx.getStopLossOrderId());
//                    })
//                    .then();
//
//        } else {
//
//            slMono = executionService.modifyStopLossOrder(
//                            ctx.getTradingSymbol(),
//                            ctx.getSymbolToken(),
//                            ctx.getNetPositionQty().get(), // 🔥 FIXED
//                            slPrice.triggerPrice().doubleValue(),
//                            slPrice.limitPrice().doubleValue(),
//                            ctx.getStopLossOrderId(),
//                            jwt
//                    )
//                    .doOnSuccess(resp ->
//                            log.info("SL modified | orderId={}", ctx.getStopLossOrderId())
//                    )
//                    .then();
//        }
//
//        // ✅ FINAL CHAIN (NO SUBSCRIBE)
//        return balanceMono
//                .onErrorResume(e -> {
//                    log.error("Balance update failed", e);
//                    return Mono.empty();
//                })
//                .then(
//                        sellMono.onErrorResume(e -> {
//                            log.error("SELL flow failed | orderId={}", ctx.getBuyOrderId(), e);
//                            return Mono.empty();
//                        })
//                )
//                .then(
//                        slMono.onErrorResume(e -> {
//                            log.error("SL flow failed | orderId={}", ctx.getBuyOrderId(), e);
//                            return Mono.empty();
//                        })
//                );
//    }


    @Override
    public Mono<Void> onFilled(OrderContext ctx, OrderStatusResponse response) {

        return Mono.defer(() -> {

            // ✅ ATOMIC GUARD
//            if (!ctx.tryStartBuyFill()) {
//                return Mono.empty();
//            }

            int filledQty = Integer.parseInt(response.getOrderStatusData().getFilledshares());
//            int lastBuyFilled = ctx.getLastBuyFilledQty().get();
//            int delta = filledQty - lastBuyFilled;

//            OrderContext.BuyUpdate update = ctx.reduceBuy(filledQty);
//
//            int delta = update.delta;
            int delta = filledQty - ctx.getLastBuyFilledQty().get();

            if (delta <= 0) return Mono.empty();

            ctx.setBuyPartiallyFilled(true);
            ctx.getLastBuyFilledQty().set(filledQty);

            if (!ctx.isBuyOpen()) {
                ctx.setBuyOpen(true);
                log.info("BUY order is now OPEN | buyOrderId={}", ctx.getBuyOrderId());
            }

            BigDecimal intendedPrice =
                    new BigDecimal(response.getOrderStatusData().getPrice());

            BigDecimal executedPrice =
                    new BigDecimal(response.getOrderStatusData().getAverageprice());

            ctx.setBuyPrice(executedPrice);

            log.info("BUY partial fill | stock={} | delta={} | totalFilled={}",
                    ctx.getTradingSymbol(), delta, filledQty);

            String normalizedSymbol = Utility.normalize(ctx.getTradingSymbol());

            // ✅ UPDATE POSITION (SAFE)
            ctx.getNetPositionQty().addAndGet(delta);
            ctx.getLastBuyFilledQty().set(filledQty);

            // ===== CALCULATIONS =====
            BigDecimal sellPrice =
                    calculationService.calculateBuyProfitPrice(intendedPrice);

            StopLossPrice slPrice =
                    calculationService.calculateStopLossPrice(
                            intendedPrice,
                            BigDecimal.valueOf(properties.getTradingStoplossPercent()),
                            BigDecimal.valueOf(properties.getTradingStoplossBufferPercent())
                    );

            AtomicBoolean failed = new AtomicBoolean(false);

            int remainingQty = ctx.longBuyRemainingQty(delta);

            String jwt = tokenManager.getValidJwtToken();

            // ===== SELL FLOW =====
            Mono<Void> sellMono = Mono.defer(() -> {
//                        String jwt = tokenManager.getValidJwtToken();

                        if (!ctx.isSellPlaced() && !ctx.isSellOpen()) {

                            return executionService.placeSellOrder(
                                            ctx.getTradingSymbol(),
                                            ctx.getSymbolToken(),
//                                            ctx.getNetPositionQty().get(),
//                                            update.remainingQty,
                                            remainingQty,
                                            sellPrice.doubleValue(),
                                            jwt
                                    )
                                    .doOnSuccess(resp -> {
                                        ctx.setSellOrderId(resp.getData().getOrderid());
                                        ctx.setSellVariety("NORMAL");
                                        ctx.setSellPlaced(true);
                                        ctx.setSellOpen(true);
                                        ctx.setSellPrice(sellPrice);
                                        orderRegistry.registerSell(ctx);

                                        log.info("SELL placed | orderId={}", ctx.getSellOrderId());
                                    });

                        } else {

                            return executionService.modifySellOrder(
                                            ctx.getTradingSymbol(),
                                            ctx.getSymbolToken(),
//                                            ctx.getNetPositionQty().get(),
//                                            update.remainingQty,
                                            remainingQty,
                                            sellPrice.doubleValue(),
                                            ctx.getSellOrderId(),
                                            jwt
                                    )
                                    .doOnSuccess(resp ->
                                            log.info("SELL modified | orderId={}", ctx.getSellOrderId())
                                    );
                        }
                    })
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(
                            Retry.backoff(3, Duration.ofMillis(200))
                                    .doBeforeRetry(rs ->
                                            log.warn("Retrying SELL... attempt={}", rs.totalRetries())
                                    )
                    )
                    .onErrorResume(e -> {
                        failed.set(true);
                        ctx.markInconsistent();
                        log.error("SELL failed", e);
                        return Mono.empty();
                    })
                    .then();

            // ===== SL FLOW =====
            Mono<Void> slMono = Mono.defer(() -> {
//                        String jwt = tokenManager.getValidJwtToken();

                        if (!ctx.isSlPlaced() && !ctx.isSLOpen()) {

                            return executionService.placeStopLossOrder(
                                            ctx.getTradingSymbol(),
                                            ctx.getSymbolToken(),
//                                            ctx.getNetPositionQty().get(),
//                                            update.remainingQty,
                                            remainingQty,
                                            slPrice.triggerPrice().doubleValue(),
                                            slPrice.limitPrice().doubleValue(),
                                            jwt,
                                            "SELL"
                                    )
                                    .doOnSuccess(resp -> {
                                        ctx.setStopLossOrderId(resp.getData().getOrderid());
                                        ctx.setStopLossVariety("STOPLOSS");
                                        ctx.setSlPlaced(true);
                                        ctx.setSLOpen(true);
                                        ctx.setStoplossLimitPrice(slPrice.limitPrice());
                                        ctx.setStoplossTriggerPrice(slPrice.triggerPrice());
                                        orderRegistry.registerStopLoss(ctx);

                                        log.info("SL placed | orderId={}", ctx.getStopLossOrderId());
                                    });

                        } else {

                            return executionService.modifyStopLossOrder(
                                            ctx.getTradingSymbol(),
                                            ctx.getSymbolToken(),
//                                            ctx.getNetPositionQty().get(),
//                                            update.remainingQty,
                                            remainingQty,
                                            slPrice.triggerPrice().doubleValue(),
                                            slPrice.limitPrice().doubleValue(),
                                            ctx.getStopLossOrderId(),
                                            jwt,
                                    "SELL"
                                    )
                                    .doOnSuccess(resp ->
                                            log.info("SL modified | orderId={}", ctx.getStopLossOrderId())
                                    );
                        }
                    })
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(
                            Retry.backoff(3, Duration.ofMillis(200))
                                    .doBeforeRetry(rs ->
                                            log.warn("Retrying SL... attempt={}", rs.totalRetries())
                                    )
                    )
                    .onErrorResume(e -> {
                        failed.set(true);
                        ctx.markInconsistent();
                        log.error("SL failed", e);
                        return Mono.empty();
                    })
                    .then();

            // ===== PARALLEL EXECUTION =====
            Mono<Void> ordersMono = Mono.when(sellMono, slMono);

            // ===== BALANCE =====
            Mono<Void> balanceMono = Mono.fromRunnable(() ->
                    balanceService.onBuy(
                            executedPrice,
                            delta,
                            properties.getLeverageMultiplierToUseForLong(),
                            balanceService.getUsableBalance(),
                            leverageService.get(normalizedSymbol).multiplier()
                    )
            );

            // ===== FINAL FLOW =====
            return ordersMono
                    .then(Mono.defer(() -> {

                        if (failed.get()) {
                            log.warn("Skipping balance due to order failure");
                            return Mono.empty();
                        }

                        return balanceMono
                                .onErrorResume(e -> {
                                    log.error("Balance update failed", e);
                                    return Mono.empty();
                                });
                    }));
        });
    }
}
